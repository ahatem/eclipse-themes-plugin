package com.github.eclipsethemes.eclipse.adapters.editor;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.models.TokenKey;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;

public final class JavaEditorThemeAdapter extends EclipseThemeAdapter {

	@Override
	public String getPreferencesId() {
		return "org.eclipse.jdt.ui";
	}

	@Override
	public void apply(Theme theme, IEclipsePreferences preferences) throws BackingStoreException {
		// Legacy style mappings (using underscores)
		mapLegacyStyle(preferences, theme, TokenKey.FOREGROUND, "java_default");
		mapLegacyStyle(preferences, theme, TokenKey.COMMENT, "java_single_line_comment");
		mapLegacyStyle(preferences, theme, TokenKey.MULTILINE_COMMENT, "java_multi_line_comment");
		mapLegacyStyle(preferences, theme, TokenKey.TASK_TAG, "java_comment_task_tag");
		mapLegacyStyle(preferences, theme, TokenKey.KEYWORD, "java_keyword");
		mapLegacyStyle(preferences, theme, TokenKey.KEYWORD, "java_keyword_return");
		mapLegacyStyle(preferences, theme, TokenKey.NUMBER, "java_number");
		mapLegacyStyle(preferences, theme, TokenKey.STRING, "java_string");
		mapLegacyStyle(preferences, theme, TokenKey.BRACKET, "java_bracket");
		mapLegacyStyle(preferences, theme, TokenKey.OPERATOR, "java_operator");
		mapLegacyStyle(preferences, theme, TokenKey.DOC, "java_doc_default");
		mapLegacyStyle(preferences, theme, TokenKey.DOC_TAG, "java_doc_keyword");
		mapLegacyStyle(preferences, theme, TokenKey.DOC_LINK, "java_doc_link");
		mapLegacyStyle(preferences, theme, TokenKey.DOC_XML_TAG, "java_doc_tag");

		// Semantic style mappings (using dots)
		mapSemanticStyle(preferences, theme, TokenKey.KEYWORD, "semanticHighlighting.restrictedKeywords");
		mapSemanticStyle(preferences, theme, TokenKey.NUMBER, "semanticHighlighting.number");
		mapSemanticStyle(preferences, theme, TokenKey.CLASS, "semanticHighlighting.class");
		mapSemanticStyle(preferences, theme, TokenKey.CLASS, "semanticHighlighting.record");
		mapSemanticStyle(preferences, theme, TokenKey.ABSTRACT_CLASS, "semanticHighlighting.abstractClass");
		mapSemanticStyle(preferences, theme, TokenKey.INTERFACE, "semanticHighlighting.interface");
		mapSemanticStyle(preferences, theme, TokenKey.ENUM, "semanticHighlighting.enum");
		mapSemanticStyle(preferences, theme, TokenKey.METHOD, "semanticHighlighting.method");
		mapSemanticStyle(preferences, theme, TokenKey.STATIC_METHOD, "semanticHighlighting.staticMethodInvocation");
		mapSemanticStyle(preferences, theme, TokenKey.ABSTRACT_METHOD, "semanticHighlighting.abstractMethodInvocation");
		mapSemanticStyle(preferences, theme, TokenKey.METHOD_DECLARATION, "semanticHighlighting.methodDeclarationName");
		mapSemanticStyle(preferences, theme, TokenKey.FIELD, "semanticHighlighting.field");
		mapSemanticStyle(preferences, theme, TokenKey.STATIC_FIELD, "semanticHighlighting.staticField");
		mapSemanticStyle(preferences, theme, TokenKey.CONSTANT, "semanticHighlighting.staticFinalField");
		mapSemanticStyle(preferences, theme, TokenKey.LOCAL_VARIABLE, "semanticHighlighting.localVariable");
		mapSemanticStyle(preferences, theme, TokenKey.LOCAL_VARIABLE_DECLARATION,
				"semanticHighlighting.localVariableDeclaration");
		mapSemanticStyle(preferences, theme, TokenKey.ARGUMENT, "semanticHighlighting.parameterVariable");
		mapSemanticStyle(preferences, theme, TokenKey.ANNOTATION, "semanticHighlighting.annotation");
		mapSemanticStyle(preferences, theme, TokenKey.ANNOTATION_KEY,
				"semanticHighlighting.annotationElementReference");
		mapSemanticStyle(preferences, theme, TokenKey.TEMPLATE_PARAMETER, "semanticHighlighting.typeParameter");

		// Optional semantic style mappings (only applied if theme has the token)
		mapSemanticStyleOptional(preferences, theme, TokenKey.INHERITED_METHOD,
				"semanticHighlighting.inheritedMethodInvocation");
		mapSemanticStyleOptional(preferences, theme, TokenKey.INHERITED_FIELD, "semanticHighlighting.inheritedField");
		mapSemanticStyleOptional(preferences, theme, TokenKey.TEMPLATE_ARGUMENT, "semanticHighlighting.typeArgument");
		mapSemanticStyleOptional(preferences, theme, TokenKey.DEPRECATED, "semanticHighlighting.deprecatedMember");

		// Special case - direct color mapping for matching brackets
		putColor(preferences, "matchingBracketsColor", theme.get(TokenKey.MATCHING_BRACKET));

		flushPreferences(preferences);

	}

}
