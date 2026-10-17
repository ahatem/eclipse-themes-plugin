package com.github.eclipsethemes.eclipse.preferences;

import java.util.*;
import com.github.eclipsethemes.core.models.*;
import com.github.eclipsethemes.eclipse.adapters.EclipseThemeAdapter;
import com.github.eclipsethemes.eclipse.adapters.editor.*;
import com.github.eclipsethemes.eclipse.adapters.ui.DebugConsoleThemeAdapter;

/** Test-owned fixtures. Stable assignments preserve the reviewed characterization hashes. */
final class AdapterTestSupport {
    private AdapterTestSupport() {}
	static List<EclipseThemeAdapter> adapters() {
		return List.of(new TextEditorThemeAdapter(), new JavaEditorThemeAdapter(), new CppEditorThemeAdapter(),
				PdeAdapterFixture.create(), new AntEditorThemeAdapter(), new DebugConsoleThemeAdapter());
	}

	static Theme theme(ThemeType type, boolean full) {
		Theme theme = new Theme("test", "test", null, null, null, null, type);
		int value = 0;
		for (TokenKey key : Arrays.stream(TokenKey.class.getFields())
				.filter(field -> field.getType() == TokenKey.class).map(field -> {
					try { return (TokenKey) field.get(null); }
					catch (IllegalAccessException e) { throw new AssertionError(e); }
				}).sorted(Comparator.comparing(TokenKey::getName)).toList()) {
			// Stable token-name order avoids relying on reflection order. Increment even
			// for omitted tokens so full and sparse fixtures use the same token values.
			// Preserve the pre-change values for every existing role. New roles use
			// distinct values outside that range, so unrelated mappings keep their digests.
			int tokenValue = switch (key.getName()) {
				case "sourceHoverBackground" -> 200;
				case "staticFinalField" -> 201;
				case "xmlComment" -> 202;
				default -> ++value;
			};
			if (full || key == TokenKey.BACKGROUND || key == TokenKey.FOREGROUND) {
				// Encode a unique RGB value and vary all four style flags by token.
				theme.addToken(new TokenBuilder().setKey(key)
						.setColor(Color.ofRgb((tokenValue >> 16) & 255, (tokenValue >> 8) & 255, tokenValue & 255))
						.setBold((tokenValue & 1) != 0).setItalic((tokenValue & 2) != 0)
						.setUnderline((tokenValue & 4) != 0).setStrikethrough((tokenValue & 8) != 0).build());
			}
		}
		return theme;
	}

    static Theme sparse() {
        Theme theme = new Theme("mapping", "mapping", null, null, null, null, ThemeType.DARK);
        token(theme, TokenKey.BACKGROUND, 1, false);
        token(theme, TokenKey.FOREGROUND, 2, false);
        return theme;
    }

    static void token(Theme theme, TokenKey key, int blue, boolean flags) {
        theme.addToken(new TokenBuilder().setKey(key).setColor(Color.ofRgb(10, 20, blue))
                .setBold(flags).setItalic(flags).setUnderline(flags).setStrikethrough(flags).build());
    }

}
