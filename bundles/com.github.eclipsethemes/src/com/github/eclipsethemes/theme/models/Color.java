package com.github.eclipsethemes.theme.models;

import java.util.Objects;

/**
 * An immutable, type-safe representation of a color. Use the static factory
 * methods like Color.ofHex() or Color.ofRgb() to create instances.
 */
public final class Color {

	public static final Color BLACK = new Color(0, 0, 0);
	public static final Color WHITE = new Color(255, 255, 255);

	private final int red;
	private final int green;
	private final int blue;

	private Color(int red, int green, int blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	public static Color ofHex(String hex) {
		Objects.requireNonNull(hex, "Hex string cannot be null");

		String cleanHex = hex.trim();

		// Handle shorthand hex like #RGB -> #RRGGBB
		if (cleanHex.length() == 4 && cleanHex.startsWith("#")) {
			char r = cleanHex.charAt(1);
			char g = cleanHex.charAt(2);
			char b = cleanHex.charAt(3);
			cleanHex = "#" + r + r + g + g + b + b;
		}

		if (!cleanHex.startsWith("#") || cleanHex.length() != 7) {
			throw new IllegalArgumentException("Invalid hex color format: " + hex);
		}

		try {
			int r = Integer.parseInt(cleanHex.substring(1, 3), 16);
			int g = Integer.parseInt(cleanHex.substring(3, 5), 16);
			int b = Integer.parseInt(cleanHex.substring(5, 7), 16);
			return Color.ofRgb(r, g, b);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid hex color format: " + hex, e);
		}
	}

	public static Color ofRgb(int red, int green, int blue) {
		if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
			throw new IllegalArgumentException(String.format("Invalid RGB value: r=%d, g=%d, b=%d", red, green, blue));
		}

		// Example of using our cache
		if (red == 0 && green == 0 && blue == 0)
			return BLACK;
		if (red == 255 && green == 255 && blue == 255)
			return WHITE;

		return new Color(red, green, blue);
	}

	public int getRed() {
		return this.red;
	}

	public int getGreen() {
		return this.green;
	}

	public int getBlue() {
		return this.blue;
	}

	public String toEclipseFormat() {
		return String.format("%d,%d,%d", red, green, blue);
	}

	@Override
	public String toString() {
		return String.format("#%02x%02x%02x", this.red, this.green, this.blue);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Color color = (Color) o;
		return red == color.red && green == color.green && blue == color.blue;
	}

	@Override
	public int hashCode() {
		return Objects.hash(red, green, blue);
	}
}
