package com.github.eclipsethemes.core;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

/** The {@code core} layer stays free of SWT, JFace and the Eclipse UI. */
public class CoreLayeringTest {

	private static final Path CORE = Paths.get("src/com/github/eclipsethemes/core");
	private static final String[] FORBIDDEN = { "org.eclipse.swt", "org.eclipse.jface", "org.eclipse.ui" };

	@Test public void coreSourcesDoNotImportSwtJFaceOrEclipseUi() throws IOException {
		List<String> violations = new ArrayList<>();
		int scanned = 0;
		try (Stream<Path> files = Files.walk(CORE)) {
			for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
				scanned++;
				for (String line : Files.readAllLines(file)) {
					String trimmed = line.trim();
					if (!trimmed.startsWith("import ")) {
						continue;
					}
					for (String forbidden : FORBIDDEN) {
						if (trimmed.matches("import\\s+(static\\s+)?" + forbidden.replace(".", "\\.") + "[.;].*")) {
							violations.add(file + ": " + trimmed);
						}
					}
				}
			}
		}
		assertTrue("The scan found no core sources", scanned > 0);
		assertTrue("core must not depend on SWT, JFace or the Eclipse UI: " + violations, violations.isEmpty());
	}
}
