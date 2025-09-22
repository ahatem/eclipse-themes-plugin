package com.github.eclipsethemes.theme.adapters;


import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.github.eclipsethemes.theme.models.Theme;
import com.github.eclipsethemes.theme.models.TokenKey;

public final class CppEditorThemeAdapter extends EclipseThemeAdapter {

    @Override
    public String getPreferencesId() {
        return "org.eclipse.cdt.ui"; // CDT preferences node
    }

    @Override
    public void apply(Theme theme, IEclipsePreferences preferences) throws BackingStoreException {
        // Legacy style mappings (using underscores)
        mapLegacyStyle(preferences, theme, TokenKey.FOREGROUND, "c_default");
        mapLegacyStyle(preferences, theme, TokenKey.FOREGROUND, "pp_default");
        mapLegacyStyle(preferences, theme, TokenKey.COMMENT, "c_single_line_comment");
        mapLegacyStyle(preferences, theme, TokenKey.MULTILINE_COMMENT, "c_multi_line_comment");
        mapLegacyStyle(preferences, theme, TokenKey.TASK_TAG, "c_comment_task_tag");
        mapLegacyStyle(preferences, theme, TokenKey.KEYWORD, "c_keyword");
        mapLegacyStyle(preferences, theme, TokenKey.KEYWORD, "c_type");
        mapLegacyStyle(preferences, theme, TokenKey.DIRECTIVE, "pp_directive");
        mapLegacyStyle(preferences, theme, TokenKey.NUMBER, "c_numbers");
        mapLegacyStyle(preferences, theme, TokenKey.STRING, "c_string");
        mapLegacyStyle(preferences, theme, TokenKey.STRING, "pp_header");
        mapLegacyStyle(preferences, theme, TokenKey.BRACKET, "c_braces");
        mapLegacyStyle(preferences, theme, TokenKey.OPERATOR, "c_operators");
        mapLegacyStyle(preferences, theme, TokenKey.DOC, "org.eclipse.cdt.internal.ui.text.doctools.doxygen.single");
        mapLegacyStyle(preferences, theme, TokenKey.DOC, "org.eclipse.cdt.internal.ui.text.doctools.doxygen.multi");
        mapLegacyStyle(preferences, theme, TokenKey.DOC_TAG, "org.eclipse.cdt.internal.ui.text.doctools.doxygen.recognizedTag");
        
        // Semantic style mappings (using dots)
        mapSemanticStyle(preferences, theme, TokenKey.CLASS, "semanticHighlighting.class");
        mapSemanticStyle(preferences, theme, TokenKey.CLASS, "semanticHighlighting.typedef");
        mapSemanticStyle(preferences, theme, TokenKey.ENUM, "semanticHighlighting.enumClass");
        mapSemanticStyle(preferences, theme, TokenKey.ENUM, "semanticHighlighting.enum");
        mapSemanticStyle(preferences, theme, TokenKey.METHOD, "semanticHighlighting.method");
        mapSemanticStyle(preferences, theme, TokenKey.METHOD, "semanticHighlighting.externalSDK");
        mapSemanticStyle(preferences, theme, TokenKey.METHOD, "semanticHighlighting.function");
        mapSemanticStyle(preferences, theme, TokenKey.STATIC_METHOD, "semanticHighlighting.staticMethod");
        mapSemanticStyle(preferences, theme, TokenKey.METHOD_DECLARATION, "semanticHighlighting.methodDeclaration");
        mapSemanticStyle(preferences, theme, TokenKey.METHOD_DECLARATION, "semanticHighlighting.functionDeclaration");
        mapSemanticStyle(preferences, theme, TokenKey.FIELD, "semanticHighlighting.field");
        mapSemanticStyle(preferences, theme, TokenKey.FIELD, "semanticHighlighting.globalVariable");
        mapSemanticStyle(preferences, theme, TokenKey.STATIC_FIELD, "semanticHighlighting.staticField");
        mapSemanticStyle(preferences, theme, TokenKey.CONSTANT, "semanticHighlighting.enumerator");
        mapSemanticStyle(preferences, theme, TokenKey.LOCAL_VARIABLE, "semanticHighlighting.localVariable");
        mapSemanticStyle(preferences, theme, TokenKey.LOCAL_VARIABLE_DECLARATION, "semanticHighlighting.localVariableDeclaration");
        mapSemanticStyle(preferences, theme, TokenKey.ARGUMENT, "semanticHighlighting.parameterVariable");
        mapSemanticStyle(preferences, theme, TokenKey.TEMPLATE_PARAMETER, "semanticHighlighting.templateParameter");
        mapSemanticStyle(preferences, theme, TokenKey.MACRO, "semanticHighlighting.macroSubstitution");
        mapSemanticStyle(preferences, theme, TokenKey.MACRO_DECLARATION, "semanticHighlighting.macroDefinition");
        mapSemanticStyle(preferences, theme, TokenKey.NAMESPACE, "semanticHighlighting.namespace");
        
        // Special case - direct color mapping for matching brackets
        putColor(preferences, "matchingBracketsColor", theme.get(TokenKey.MATCHING_BRACKET));
        
        flushPreferences(preferences);
    }
}
