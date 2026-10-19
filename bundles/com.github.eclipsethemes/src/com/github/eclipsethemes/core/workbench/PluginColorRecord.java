package com.github.eclipsethemes.core.workbench;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The ownership record: which key ids this plugin wrote and the exact value it wrote. Stored as one line per key,
 * {@code node|key|value}. Node and key names come from {@link PluginColorTable} and values are {@code r,g,b} or
 * {@code true}, so none contains a pipe or a newline. Anything unparsable yields an empty record: nothing is owned
 * and therefore nothing is ever removed.
 */
public final class PluginColorRecord {

	private PluginColorRecord() {
	}

	public static String format(Map<String, String> owned) {
		StringBuilder text = new StringBuilder();
		owned.forEach((id, value) -> text.append(id).append('|').append(value).append('\n'));
		return text.toString();
	}

	public static Map<String, String> parse(String text) {
		Map<String, String> owned = new LinkedHashMap<>();
		if (text == null) {
			return owned;
		}
		for (String line : text.split("\n")) {
			if (line.isEmpty()) {
				continue;
			}
			int first = line.indexOf('|');
			int last = line.lastIndexOf('|');
			if (first <= 0 || last <= first || last == line.length() - 1) {
				return new LinkedHashMap<>();
			}
			owned.put(line.substring(0, last), line.substring(last + 1));
		}
		return owned;
	}
}
