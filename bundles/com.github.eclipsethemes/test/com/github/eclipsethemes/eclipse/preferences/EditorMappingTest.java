package com.github.eclipsethemes.eclipse.preferences;

import static com.github.eclipsethemes.eclipse.preferences.AdapterTestSupport.sparse;
import static com.github.eclipsethemes.eclipse.preferences.AdapterTestSupport.token;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.github.eclipsethemes.core.models.*;
import com.github.eclipsethemes.eclipse.adapters.editor.TextEditorThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.editor.*;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;

public class EditorMappingTest {
    static void assertStyle(MemoryPreferences prefs, String base, String separator, int blue, boolean flags) {
        assertEquals(base, "10,20," + blue, prefs.values.get(base + (separator.equals(".") ? ".color" : "")));
        for (String flag : java.util.List.of("bold", "italic", "underline", "strikethrough"))
            assertEquals(base + flag, Boolean.toString(flags), prefs.values.get(base + separator + flag));
    }

    static void assertXmlStyle(MemoryPreferences prefs, String base, int blue, boolean flags) {
        assertEquals("10,20," + blue, prefs.values.get(base));
        for (String flag : java.util.List.of("bold", "italic"))
            assertEquals(Boolean.toString(flags), prefs.values.get(base + "_" + flag));
        for (String flag : java.util.List.of("underline", "strikethrough"))
            assertFalse(prefs.values.containsKey(base + "_" + flag));
    }

    @Test public void separatedRolesReachExactDestinationsAndOneWayFallbacks() throws Exception {
        for (int presence = 0; presence < 4; presence++) {
            var theme = sparse();
            if ((presence & 1) != 0) {
                token(theme, TokenKey.COMMENT, 30, true);
                token(theme, TokenKey.CONSTANT, 40, true);
            }
            if ((presence & 2) != 0) {
                token(theme, TokenKey.XML_COMMENT, 50, false);
                token(theme, TokenKey.STATIC_FINAL_FIELD, 60, false);
            }
            int generic = (presence & 1) != 0 ? 30 : 2;
            int constant = (presence & 1) != 0 ? 40 : 2;
            int xml = (presence & 2) != 0 ? 50 : generic;
            int field = (presence & 2) != 0 ? 60 : constant;
            MemoryPreferences jdt = new MemoryPreferences(), cdt = new MemoryPreferences();
            MemoryPreferences pde = new MemoryPreferences(), ant = new MemoryPreferences();
            new JavaEditorThemeAdapter().apply(theme.build(), jdt.node);
            new CppEditorThemeAdapter().apply(theme.build(), cdt.node);
            PdeAdapterFixture.create().apply(theme.build(), pde.node);
            new AntEditorThemeAdapter().apply(theme.build(), ant.node);
            assertStyle(jdt, "java_single_line_comment", "_", generic, (presence & 1) != 0);
            assertStyle(cdt, "c_single_line_comment", "_", generic, (presence & 1) != 0);
            assertStyle(jdt, "semanticHighlighting.staticFinalField", ".", field, (presence & 2) == 0 && (presence & 1) != 0);
            assertStyle(cdt, "semanticHighlighting.enumerator", ".", constant, (presence & 1) != 0);
            assertXmlStyle(pde, "editor.color.xml_comment", xml, (presence & 2) == 0 && (presence & 1) != 0);
            assertXmlStyle(ant, "org.eclipse.ant.ui.commentsColor", xml, (presence & 2) == 0 && (presence & 1) != 0);
        }
    }

    @Test public void hoverIsExplicitOnlyOrderedAndIndependentOfSelection() throws Exception {
        JavaEditorThemeAdapter adapter = new JavaEditorThemeAdapter();
        MemoryPreferences jdt = new MemoryPreferences();
        jdt.values.put("unrelated", "keep");
        for (boolean present : java.util.List.of(true, false, false, true)) {
            var theme = sparse();
            token(theme, TokenKey.SELECTION_BACKGROUND, 70, false);
            if (present) token(theme, TokenKey.SOURCE_HOVER_BACKGROUND, 80, true);
            jdt.mutations.clear();
            adapter.apply(theme.build(), jdt.node);
            assertEquals(present ? "10,20,80" : null, jdt.values.get("sourceHoverBackgroundColor"));
            assertEquals(present ? "false" : null, jdt.values.get("sourceHoverBackgroundColor.SystemDefault"));
            if (present) assertTrue(jdt.mutations.indexOf("put sourceHoverBackgroundColor=10,20,80")
                    < jdt.mutations.indexOf("put sourceHoverBackgroundColor.SystemDefault=false"));
            MemoryPreferences text = new MemoryPreferences();
            new TextEditorThemeAdapter().apply(theme.build(), text.node);
            assertEquals("10,20,70", text.values.get("AbstractTextEditor.Color.SelectionBackground"));
            assertEquals("keep", jdt.values.get("unrelated"));
        }
        assertTrue(adapter.getManagedKeys().containsAll(java.util.List.of("sourceHoverBackgroundColor",
                "sourceHoverBackgroundColor.SystemDefault")));
    }

    @Test public void optionalRolesStayDisabledAcrossDefaultsAndRichSparseRichTransitions() throws Exception {
        String[] bases = {"inheritedMethodInvocation", "inheritedField", "typeArgument", "deprecatedMember"};
        TokenKey[] keys = {TokenKey.INHERITED_METHOD, TokenKey.INHERITED_FIELD, TokenKey.TEMPLATE_ARGUMENT, TokenKey.DEPRECATED};
        JavaEditorThemeAdapter adapter = new JavaEditorThemeAdapter();
        for (boolean editorDefault : java.util.List.of(true, false)) {
            MemoryPreferences prefs = new MemoryPreferences();
            prefs.values.put("semanticHighlighting.unmapped.enabled", "user-value");
            for (int step : new int[] {0, 1, 0, 0, 2, 3}) {
                var theme = sparse();
                token(theme, TokenKey.METHOD, 90, true);
                token(theme, TokenKey.FIELD, 91, true);
                token(theme, TokenKey.CLASS, 92, true);
                if (step > 0) for (TokenKey key : keys) {
                    if (step == 3) theme.add(new Token(key, Color.ofRgb(10,20,step), null));
                    else token(theme, key, step, step == 1);
                }
                adapter.apply(theme.build(), prefs.node);
                for (String role : bases) {
                    String base = "semanticHighlighting." + role;
                    assertEquals(base, step > 0, prefs.node.getBoolean(base + ".enabled", editorDefault));
                    assertEquals(Boolean.toString(step > 0), prefs.values.get(base + ".enabled"));
                    if (step > 0) assertStyle(prefs, base, ".", step, step == 1);
                    else for (String suffix : java.util.List.of("color", "bold", "italic", "underline", "strikethrough"))
                        assertFalse(base + suffix, prefs.values.containsKey(base + "." + suffix));
                }
                assertEquals("user-value", prefs.values.get("semanticHighlighting.unmapped.enabled"));
                assertEquals("true", prefs.values.get("semanticHighlighting.method.enabled"));
                assertStyle(prefs, "semanticHighlighting.method", ".", 90, true);
            }
        }
    }
    @Test
    public void debugColorsPreserveEveryHistoricalIndicationState() throws Exception {
        TextEditorThemeAdapter adapter = new TextEditorThemeAdapter();
        var theme = sparse();
        token(theme, TokenKey.CURRENT_INSTRUCTION_POINTER, 30, true);
        token(theme, TokenKey.DEBUG_CALL_STACK, 40, false);
        for (String current : Arrays.asList("true", "false", null, "9,8,7")) {
            for (String secondary : Arrays.asList("true", "false", null, "6,5,4")) {
                MemoryPreferences prefs = new MemoryPreferences();
                if (current != null) prefs.values.put("currentIPIndication", current);
                if (secondary != null) prefs.values.put("secondaryIPIndication", secondary);
                for (String key : new String[] {"currentIPHighlight", "secondaryIPHighlight",
                        "currentIPTextStyle", "secondaryIPTextStyle", "currentIPVerticalRuler",
                        "secondaryIPVerticalRuler", "unrelated"}) prefs.values.put(key, "keep");
                Map<String, String> originals = new LinkedHashMap<>(prefs.values);
                adapter.apply(theme.build(), prefs.node);
                adapter.apply(theme.build(), prefs.node);
                assertEquals("10,20,30", prefs.values.get("currentIPColor"));
                assertEquals("10,20,40", prefs.values.get("secondaryIPColor"));
                originals.forEach((key, value) -> assertEquals(key, value, prefs.values.get(key)));
                assertEquals(current, prefs.values.get("currentIPIndication"));
                assertEquals(secondary, prefs.values.get("secondaryIPIndication"));
                assertFalse(adapter.getManagedKeys().contains("currentIPIndication"));
                assertFalse(adapter.getManagedKeys().contains("secondaryIPIndication"));
                assertTrue(adapter.getManagedKeys().containsAll(Arrays.asList("currentIPColor", "secondaryIPColor")));
            }
        }
    }
}
