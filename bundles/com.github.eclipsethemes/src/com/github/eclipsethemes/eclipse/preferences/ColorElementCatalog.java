package com.github.eclipsethemes.eclipse.preferences;

import java.util.List;

import com.github.eclipsethemes.core.models.TokenKey;

public final class ColorElementCatalog {

	public record Category(String name, List<TokenKey> keys) {
	}

	private ColorElementCatalog() {
	}

	public static List<Category> categories() {
		return List.of(
				new Category("Editor and workbench", List.of(
						TokenKey.BACKGROUND,
						TokenKey.FOREGROUND,
						TokenKey.SELECTION_BACKGROUND,
						TokenKey.SELECTION_FOREGROUND,
						TokenKey.CURRENT_LINE,
						TokenKey.LINE_NUMBER,
						TokenKey.SOURCE_HOVER_BACKGROUND,
						TokenKey.MATCHING_BRACKET,
						TokenKey.ERROR)),
				new Category("Occurrences and search", List.of(
						TokenKey.OCCURRENCE,
						TokenKey.WRITE_OCCURRENCE,
						TokenKey.TEXT_OCCURRENCE,
						TokenKey.FIND_SCOPE,
						TokenKey.SEARCH_RESULT,
						TokenKey.FILTERED_SEARCH_RESULT)),
				new Category("Version control and debug", List.of(
						TokenKey.ADDED_LINE,
						TokenKey.MODIFIED_LINE,
						TokenKey.REMOVED_LINE,
						TokenKey.CURRENT_INSTRUCTION_POINTER,
						TokenKey.DEBUG_CALL_STACK)),
				new Category("Comments and documentation", List.of(
						TokenKey.COMMENT,
						TokenKey.MULTILINE_COMMENT,
						TokenKey.TASK_TAG,
						TokenKey.DOC,
						TokenKey.DOC_TAG,
						TokenKey.DOC_LINK,
						TokenKey.DOC_XML_TAG)),
				new Category("Language constructs", List.of(
						TokenKey.KEYWORD,
						TokenKey.NUMBER,
						TokenKey.STRING,
						TokenKey.BRACKET,
						TokenKey.OPERATOR,
						TokenKey.DEPRECATED)),
				new Category("Types", List.of(
						TokenKey.CLASS,
						TokenKey.ABSTRACT_CLASS,
						TokenKey.INTERFACE,
						TokenKey.ENUM,
						TokenKey.ANNOTATION,
						TokenKey.TEMPLATE_PARAMETER,
						TokenKey.TEMPLATE_ARGUMENT)),
				new Category("Methods", List.of(
						TokenKey.METHOD,
						TokenKey.STATIC_METHOD,
						TokenKey.ABSTRACT_METHOD,
						TokenKey.INHERITED_METHOD,
						TokenKey.METHOD_DECLARATION,
						TokenKey.MACRO,
						TokenKey.MACRO_DECLARATION)),
				new Category("Fields and variables", List.of(
						TokenKey.FIELD,
						TokenKey.STATIC_FIELD,
						TokenKey.CONSTANT,
						TokenKey.INHERITED_FIELD,
						TokenKey.LOCAL_VARIABLE,
						TokenKey.LOCAL_VARIABLE_DECLARATION,
						TokenKey.ARGUMENT,
						TokenKey.ANNOTATION_KEY,
						TokenKey.NAMESPACE,
						TokenKey.KEY)),
				new Category("XML and preprocessor", List.of(
						TokenKey.DIRECTIVE,
						TokenKey.XML_DIRECTIVE,
						TokenKey.XML_TAG,
						TokenKey.XML_ATTRIBUTE)));
	}

	public static boolean supportsStyle(TokenKey key) {
		return key != TokenKey.BACKGROUND
				&& key != TokenKey.SELECTION_BACKGROUND
				&& key != TokenKey.CURRENT_LINE
				&& key != TokenKey.SOURCE_HOVER_BACKGROUND
				&& key != TokenKey.OCCURRENCE
				&& key != TokenKey.WRITE_OCCURRENCE
				&& key != TokenKey.TEXT_OCCURRENCE
				&& key != TokenKey.ADDED_LINE
				&& key != TokenKey.MODIFIED_LINE
				&& key != TokenKey.REMOVED_LINE
				&& key != TokenKey.FIND_SCOPE
				&& key != TokenKey.SEARCH_RESULT
				&& key != TokenKey.FILTERED_SEARCH_RESULT
				&& key != TokenKey.CURRENT_INSTRUCTION_POINTER
				&& key != TokenKey.DEBUG_CALL_STACK;
	}
}
