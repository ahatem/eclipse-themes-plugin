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

public final class JavaEditorThemeAdapter extends EclipseThemeAdapter {

	@Override
	public String getPreferencesId() {
		return "org.eclipse.jdt.ui";
	}

	private static final List<ManagedPreference> MAPPINGS = List.of(
		// Legacy style mappings (using underscores)
		legacyStyle(TokenKey.FOREGROUND, "java_default"),
		legacyStyle(TokenKey.COMMENT, "java_single_line_comment"),
		legacyStyle(TokenKey.MULTILINE_COMMENT, "java_multi_line_comment"),
		legacyStyle(TokenKey.TASK_TAG, "java_comment_task_tag"),
		legacyStyle(TokenKey.KEYWORD, "java_keyword"),
		legacyStyle(TokenKey.KEYWORD, "java_keyword_return"),
		legacyStyle(TokenKey.NUMBER, "java_number"),
		legacyStyle(TokenKey.STRING, "java_string"),
		legacyStyle(TokenKey.BRACKET, "java_bracket"),
		legacyStyle(TokenKey.OPERATOR, "java_operator"),
		legacyStyle(TokenKey.DOC, "java_doc_default"),
		legacyStyle(TokenKey.DOC_TAG, "java_doc_keyword"),
		legacyStyle(TokenKey.DOC_LINK, "java_doc_link"),
		legacyStyle(TokenKey.DOC_XML_TAG, "java_doc_tag"),

		// Semantic style mappings (using dots)
		semanticStyle(TokenKey.KEYWORD, "semanticHighlighting.restrictedKeywords"),
		semanticStyle(TokenKey.NUMBER, "semanticHighlighting.number"),
		semanticStyle(TokenKey.CLASS, "semanticHighlighting.class"),
		semanticStyle(TokenKey.ABSTRACT_CLASS, "semanticHighlighting.abstractClass"),
		semanticStyle(TokenKey.INTERFACE, "semanticHighlighting.interface"),
		semanticStyle(TokenKey.ENUM, "semanticHighlighting.enum"),
		semanticStyle(TokenKey.METHOD, "semanticHighlighting.method"),
		semanticStyle(TokenKey.STATIC_METHOD, "semanticHighlighting.staticMethodInvocation"),
		semanticStyle(TokenKey.ABSTRACT_METHOD, "semanticHighlighting.abstractMethodInvocation"),
		semanticStyle(TokenKey.METHOD_DECLARATION, "semanticHighlighting.methodDeclarationName"),
		semanticStyle(TokenKey.FIELD, "semanticHighlighting.field"),
		semanticStyle(TokenKey.STATIC_FIELD, "semanticHighlighting.staticField"),
		// Separate Java static-final fields; omitted role inherits CONSTANT, never the reverse.
		semanticStyle(TokenKey.STATIC_FINAL_FIELD, "semanticHighlighting.staticFinalField"),
		semanticStyle(TokenKey.LOCAL_VARIABLE, "semanticHighlighting.localVariable"),
		semanticStyle(TokenKey.LOCAL_VARIABLE_DECLARATION, "semanticHighlighting.localVariableDeclaration"),
		semanticStyle(TokenKey.ARGUMENT, "semanticHighlighting.parameterVariable"),
		semanticStyle(TokenKey.ANNOTATION, "semanticHighlighting.annotation"),
		semanticStyle(TokenKey.ANNOTATION_KEY, "semanticHighlighting.annotationElementReference"),
		semanticStyle(TokenKey.TEMPLATE_PARAMETER, "semanticHighlighting.typeParameter"),

		// Optional semantic style mappings (only applied if theme has the token)
		optionalSemanticStyle(TokenKey.INHERITED_METHOD, "semanticHighlighting.inheritedMethodInvocation"),
		optionalSemanticStyle(TokenKey.INHERITED_FIELD, "semanticHighlighting.inheritedField"),
		optionalSemanticStyle(TokenKey.TEMPLATE_ARGUMENT, "semanticHighlighting.typeArgument"),
		optionalSemanticStyle(TokenKey.DEPRECATED, "semanticHighlighting.deprecatedMember"),

		// Eclipse JDT R4_33 PreferenceConstants / SourceViewerInformationControl:
		// this pair controls only the source-viewer hover background. Explicit-only;
		// omission exposes editor defaults. Color first, then the system-default switch.
		new ManagedPreference(List.of("sourceHoverBackgroundColor", "sourceHoverBackgroundColor.SystemDefault"),
				(theme, prefs) -> {
					if (theme.has(TokenKey.SOURCE_HOVER_BACKGROUND)) {
						putColor(prefs, "sourceHoverBackgroundColor", theme.get(TokenKey.SOURCE_HOVER_BACKGROUND));
						prefs.putBoolean("sourceHoverBackgroundColor.SystemDefault", false);
					} else {
						prefs.remove("sourceHoverBackgroundColor");
						prefs.remove("sourceHoverBackgroundColor.SystemDefault");
					}
				}),

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
