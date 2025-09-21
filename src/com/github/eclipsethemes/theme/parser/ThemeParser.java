package com.github.eclipsethemes.theme.parser;

import com.github.eclipsethemes.theme.models.Theme;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;


public interface ThemeParser {

    /**
     * The core parsing logic that all implementations must provide.
     * @param inputStream The stream to parse.
     * @param sourceFile The original file, which can be null.
     * @return A constructed Theme object.
     * @throws ThemeParseException if parsing fails.
     */
    Theme parse(InputStream inputStream, File sourceFile) throws ThemeParseException;

    /**
     * Convenience method for parsing a stream without a source file.
     */
    default Theme parse(InputStream inputStream) throws ThemeParseException {
        return parse(inputStream, null);
    }

    /**
     * Convenience method for parsing directly from a file.
     */
    default Theme parse(File file) throws ThemeParseException {
        try (InputStream inputStream = new FileInputStream(file)) {
            return parse(inputStream, file);
        } catch (Exception e) {
            throw new ThemeParseException("Failed to read theme file: " + file.getName(), e);
        }
    }
}
