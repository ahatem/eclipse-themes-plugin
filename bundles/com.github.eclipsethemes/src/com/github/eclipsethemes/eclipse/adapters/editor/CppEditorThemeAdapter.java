package com.github.eclipsethemes.eclipse.adapters.editor;


import java.util.List;
import java.util.Set;

import static com.github.eclipsethemes.eclipse.adapters.ManagedPreference.*;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.ManagedPreference;

public final class CppEditorThemeAdapter extends EclipseThemeAdapter {

    @Override
    public String getPreferencesId() {
        return "org.eclipse.cdt.ui"; // CDT preferences node
    }

    private static final List<ManagedPreference> MAPPINGS = List.of(
        // Legacy style mappings (using underscores)
        legacyStyle(TokenKey.FOREGROUND, "c_default"),
        legacyStyle(TokenKey.FOREGROUND, "pp_default"),
        legacyStyle(TokenKey.COMMENT, "c_single_line_comment"),
        legacyStyle(TokenKey.MULTILINE_COMMENT, "c_multi_line_comment"),
        legacyStyle(TokenKey.TASK_TAG, "c_comment_task_tag"),
        legacyStyle(TokenKey.KEYWORD, "c_keyword"),
        legacyStyle(TokenKey.KEYWORD, "c_type"),
        legacyStyle(TokenKey.DIRECTIVE, "pp_directive"),
        legacyStyle(TokenKey.NUMBER, "c_numbers"),
        legacyStyle(TokenKey.STRING, "c_string"),
        legacyStyle(TokenKey.STRING, "pp_header"),
        legacyStyle(TokenKey.BRACKET, "c_braces"),
        legacyStyle(TokenKey.OPERATOR, "c_operators"),
        legacyStyle(TokenKey.DOC, "org.eclipse.cdt.internal.ui.text.doctools.doxygen.single"),
        legacyStyle(TokenKey.DOC, "org.eclipse.cdt.internal.ui.text.doctools.doxygen.multi"),
        legacyStyle(TokenKey.DOC_TAG, "org.eclipse.cdt.internal.ui.text.doctools.doxygen.recognizedTag"),
        
        // Semantic style mappings (using dots)
        semanticStyle(TokenKey.CLASS, "semanticHighlighting.class"),
        semanticStyle(TokenKey.CLASS, "semanticHighlighting.typedef"),
        semanticStyle(TokenKey.ENUM, "semanticHighlighting.enumClass"),
        semanticStyle(TokenKey.ENUM, "semanticHighlighting.enum"),
        semanticStyle(TokenKey.METHOD, "semanticHighlighting.method"),
        semanticStyle(TokenKey.METHOD, "semanticHighlighting.externalSDK"),
        semanticStyle(TokenKey.METHOD, "semanticHighlighting.function"),
        semanticStyle(TokenKey.STATIC_METHOD, "semanticHighlighting.staticMethod"),
        semanticStyle(TokenKey.METHOD_DECLARATION, "semanticHighlighting.methodDeclaration"),
        semanticStyle(TokenKey.METHOD_DECLARATION, "semanticHighlighting.functionDeclaration"),
        semanticStyle(TokenKey.FIELD, "semanticHighlighting.field"),
        semanticStyle(TokenKey.FIELD, "semanticHighlighting.globalVariable"),
        semanticStyle(TokenKey.STATIC_FIELD, "semanticHighlighting.staticField"),
        semanticStyle(TokenKey.CONSTANT, "semanticHighlighting.enumerator"),
        semanticStyle(TokenKey.LOCAL_VARIABLE, "semanticHighlighting.localVariable"),
        semanticStyle(TokenKey.LOCAL_VARIABLE_DECLARATION, "semanticHighlighting.localVariableDeclaration"),
        semanticStyle(TokenKey.ARGUMENT, "semanticHighlighting.parameterVariable"),
        semanticStyle(TokenKey.TEMPLATE_PARAMETER, "semanticHighlighting.templateParameter"),
        semanticStyle(TokenKey.MACRO, "semanticHighlighting.macroSubstitution"),
        semanticStyle(TokenKey.MACRO_DECLARATION, "semanticHighlighting.macroDefinition"),
        semanticStyle(TokenKey.NAMESPACE, "semanticHighlighting.namespace"),
        
        // Special case - direct color mapping for matching brackets
        color(TokenKey.MATCHING_BRACKET, "matchingBracketsColor")
    );

    @Override
    public Set<String> getManagedKeys() {
        return managedKeys(MAPPINGS);
    }

    @Override
    public void apply(Theme theme, IEclipsePreferences preferences) throws BackingStoreException {
        applyMappings(MAPPINGS, theme, preferences);
    }
}
