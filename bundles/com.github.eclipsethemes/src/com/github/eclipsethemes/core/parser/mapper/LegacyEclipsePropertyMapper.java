package com.github.eclipsethemes.core.parser.mapper;

import java.util.HashMap;
import java.util.Map;

import com.github.eclipsethemes.core.models.TokenKey;

public class LegacyEclipsePropertyMapper {

	private static final Map<String, String> PROPERTY_MAP = new HashMap<>();

	static {
        // Background and Basic Colors
        PROPERTY_MAP.put("foreground", TokenKey.FOREGROUND.getName());
        PROPERTY_MAP.put("background", TokenKey.BACKGROUND.getName());
        PROPERTY_MAP.put("selectionForeground", TokenKey.SELECTION_FOREGROUND.getName());
        PROPERTY_MAP.put("selectionBackground", TokenKey.SELECTION_BACKGROUND.getName());
        PROPERTY_MAP.put("currentLine", TokenKey.CURRENT_LINE.getName());
        PROPERTY_MAP.put("lineNumber", TokenKey.LINE_NUMBER.getName());

        // Occurrences and Search
        PROPERTY_MAP.put("occurrenceIndication", TokenKey.OCCURRENCE.getName());
        PROPERTY_MAP.put("writeOccurrenceIndication", TokenKey.WRITE_OCCURRENCE.getName());
        PROPERTY_MAP.put("deletionIndication", TokenKey.REMOVED_LINE.getName());
        PROPERTY_MAP.put("findScope", TokenKey.FIND_SCOPE.getName());
        PROPERTY_MAP.put("searchResultIndication", TokenKey.SEARCH_RESULT.getName());
        PROPERTY_MAP.put("filteredSearchResultIndication", TokenKey.FILTERED_SEARCH_RESULT.getName());

        // Debug
        PROPERTY_MAP.put("debugCurrentInstructionPointer", TokenKey.CURRENT_INSTRUCTION_POINTER.getName());
        PROPERTY_MAP.put("debugSecondaryInstructionPointer", TokenKey.DEBUG_CALL_STACK.getName());

        // Comments and Documentation
        PROPERTY_MAP.put("singleLineComment", TokenKey.COMMENT.getName());
        PROPERTY_MAP.put("multiLineComment", TokenKey.MULTILINE_COMMENT.getName());
        PROPERTY_MAP.put("commentTaskTag", TokenKey.TASK_TAG.getName());
        PROPERTY_MAP.put("javadoc", TokenKey.DOC.getName());
        PROPERTY_MAP.put("javadocTag", TokenKey.DOC_XML_TAG.getName());
        PROPERTY_MAP.put("javadocKeyword", TokenKey.DOC_TAG.getName());
        PROPERTY_MAP.put("javadocLink", TokenKey.DOC_LINK.getName());

        // Java Language Elements
        PROPERTY_MAP.put("class", TokenKey.CLASS.getName());
        PROPERTY_MAP.put("abstractClass", TokenKey.ABSTRACT_CLASS.getName());
        PROPERTY_MAP.put("interface", TokenKey.INTERFACE.getName());
        PROPERTY_MAP.put("enum", TokenKey.ENUM.getName());
        PROPERTY_MAP.put("method", TokenKey.METHOD.getName());
        PROPERTY_MAP.put("methodDeclaration", TokenKey.METHOD_DECLARATION.getName());
        PROPERTY_MAP.put("inheritedMethod", TokenKey.INHERITED_METHOD.getName());
        PROPERTY_MAP.put("abstractMethod", TokenKey.ABSTRACT_METHOD.getName());
        PROPERTY_MAP.put("staticMethod", TokenKey.STATIC_METHOD.getName());

        // Variables and Fields
        PROPERTY_MAP.put("localVariable", TokenKey.LOCAL_VARIABLE.getName());
        PROPERTY_MAP.put("localVariableDeclaration", TokenKey.LOCAL_VARIABLE_DECLARATION.getName());
        PROPERTY_MAP.put("field", TokenKey.FIELD.getName());
        PROPERTY_MAP.put("staticField", TokenKey.STATIC_FIELD.getName());
        PROPERTY_MAP.put("staticFinalField", TokenKey.CONSTANT.getName());
        PROPERTY_MAP.put("constant", TokenKey.CONSTANT.getName());
        PROPERTY_MAP.put("parameterVariable", TokenKey.ARGUMENT.getName());

        // Language Constructs
        PROPERTY_MAP.put("keyword", TokenKey.KEYWORD.getName());
        PROPERTY_MAP.put("operator", TokenKey.OPERATOR.getName());
        PROPERTY_MAP.put("bracket", TokenKey.BRACKET.getName());
        PROPERTY_MAP.put("number", TokenKey.NUMBER.getName());
        PROPERTY_MAP.put("string", TokenKey.STRING.getName());
        PROPERTY_MAP.put("annotation", TokenKey.ANNOTATION.getName());

        // Generic/Template Types
        PROPERTY_MAP.put("typeArgument", TokenKey.TEMPLATE_ARGUMENT.getName());
        PROPERTY_MAP.put("typeParameter", TokenKey.TEMPLATE_PARAMETER.getName());

        // Special Cases
        PROPERTY_MAP.put("deprecated", TokenKey.DEPRECATED.getName());
        PROPERTY_MAP.put("deprecatedMember", TokenKey.DEPRECATED.getName());

        // XML Support
        PROPERTY_MAP.put("xmlTag", TokenKey.XML_TAG.getName());
        PROPERTY_MAP.put("xmlAttribute", TokenKey.XML_ATTRIBUTE.getName());
        PROPERTY_MAP.put("xmlComment", TokenKey.COMMENT.getName());

        // Additional Editor Support
        PROPERTY_MAP.put("sourceHoverBackground", TokenKey.SELECTION_BACKGROUND.getName());
    }

	public static String mapProperty(String property) {
		String newProperty = PROPERTY_MAP.get(property);
		return newProperty == null ? property : newProperty;
	}
}
