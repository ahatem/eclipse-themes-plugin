package com.github.eclipsethemes.eclipse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.github.eclipsethemes.core.exceptions.ThemeParseException;
import com.github.eclipsethemes.core.models.Theme;
import com.github.eclipsethemes.core.parser.ThemePackageParser;
import com.github.eclipsethemes.core.parser.ThemePackageWriter;

/**
 * Writes a theme as a new {@code .ectheme} file the user chose. The document is read back with the package parser
 * before anything is written, so an invalid package is never produced, and the file appears only when complete.
 */
final class ThemeExporter {

	private ThemeExporter() {
	}

	static void export(Theme theme, File target) throws IOException {
		byte[] bytes;
		try {
			bytes = ThemePackageWriter.write(theme).getBytes(StandardCharsets.UTF_8);
			new ThemePackageParser().parse(new ByteArrayInputStream(bytes), null);
		} catch (ThemeParseException | IllegalArgumentException e) {
			throw new IOException("The theme cannot be written as a valid theme package: " + e.getMessage(), e);
		}

		Path destination = target.toPath().toAbsolutePath();
		Path temporary = Files.createTempFile(destination.getParent(), ".export-", ".part");
		try {
			Files.write(temporary, bytes);
			try {
				Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}
}
