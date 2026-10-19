package com.github.eclipsethemes.core.workbench;

import com.github.eclipsethemes.core.models.Color;

/**
 * A color in OKLCH (the polar form of Oklab, Bjorn Ottosson 2020): lightness, chroma and hue that track perceived
 * lightness and hue far better than RGB blending does. Used only to derive workbench roles that the theme did not
 * author, so a tinted theme stays tinted. Pure and deterministic.
 */
final class PerceptualColor {

	private static final int GAMUT_STEPS = 24;

	final double l;
	final double c;
	/** Hue in radians; meaningless when {@link #c} is close to zero. */
	final double h;

	private PerceptualColor(double l, double c, double h) {
		this.l = l;
		this.c = c;
		this.h = h;
	}

	static PerceptualColor of(Color color) {
		double r = linear(color.red());
		double g = linear(color.green());
		double b = linear(color.blue());
		double lc = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
		double mc = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
		double sc = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
		double l = 0.2104542553 * lc + 0.7936177850 * mc - 0.0040720468 * sc;
		double a = 1.9779984951 * lc - 2.4285922050 * mc + 0.4505937099 * sc;
		double bb = 0.0259040371 * lc + 0.7827717662 * mc - 0.8086757660 * sc;
		return new PerceptualColor(l, Math.hypot(a, bb), Math.atan2(bb, a));
	}

	static PerceptualColor of(double lightness, double chroma, double hue) {
		return new PerceptualColor(lightness, chroma, hue);
	}

	PerceptualColor withLightness(double lightness) {
		return new PerceptualColor(lightness, c, h);
	}

	/** Lightness moved by {@code delta} and clamped to the usable 0..1 range. */
	PerceptualColor shifted(double delta) {
		return withLightness(Math.max(0.0, Math.min(1.0, l + delta)));
	}

	PerceptualColor withChroma(double chroma) {
		return new PerceptualColor(l, chroma, h);
	}

	/** Hue distance in degrees (0..180) between two colors that both carry a meaningful hue. */
	static double hueDistanceDegrees(PerceptualColor a, PerceptualColor b) {
		double d = Math.abs(Math.toDegrees(a.h - b.h)) % 360.0;
		return d > 180.0 ? 360.0 - d : d;
	}

	/** A color that is {@code amount} (0..1) of the way from this one to {@code other}, in Oklab. */
	PerceptualColor mixedToward(PerceptualColor other, double amount) {
		double a1 = c * Math.cos(h);
		double b1 = c * Math.sin(h);
		double a2 = other.c * Math.cos(other.h);
		double b2 = other.c * Math.sin(other.h);
		double a = a1 + (a2 - a1) * amount;
		double b = b1 + (b2 - b1) * amount;
		return new PerceptualColor(l + (other.l - l) * amount, Math.hypot(a, b), Math.atan2(b, a));
	}

	/** The nearest displayable sRGB color: chroma is reduced until the color fits the gamut. */
	Color toColor() {
		double chroma = c;
		for (int i = 0; i < GAMUT_STEPS; i++) {
			Color color = tryConvert(l, chroma, h);
			if (color != null) {
				return color;
			}
			chroma *= 0.85;
		}
		return clamp(l, 0.0, h);
	}

	private static Color tryConvert(double l, double chroma, double h) {
		double[] rgb = toLinearRgb(l, chroma, h);
		for (double v : rgb) {
			if (v < -0.0005 || v > 1.0005) {
				return null;
			}
		}
		return Color.ofRgb(channel(rgb[0]), channel(rgb[1]), channel(rgb[2]));
	}

	private static Color clamp(double l, double chroma, double h) {
		double[] rgb = toLinearRgb(l, chroma, h);
		return Color.ofRgb(channel(rgb[0]), channel(rgb[1]), channel(rgb[2]));
	}

	private static double[] toLinearRgb(double l, double chroma, double h) {
		double a = chroma * Math.cos(h);
		double b = chroma * Math.sin(h);
		double lc = l + 0.3963377774 * a + 0.2158037573 * b;
		double mc = l - 0.1055613458 * a - 0.0638541728 * b;
		double sc = l - 0.0894841775 * a - 1.2914855480 * b;
		double lin = lc * lc * lc;
		double m = mc * mc * mc;
		double s = sc * sc * sc;
		return new double[] { 4.0767416621 * lin - 3.3077115913 * m + 0.2309699292 * s,
				-1.2684380046 * lin + 2.6097574011 * m - 0.3413193965 * s,
				-0.0041960863 * lin - 0.7034186147 * m + 1.7076147010 * s };
	}

	private static double linear(int channel) {
		double v = channel / 255.0;
		return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
	}

	private static int channel(double linear) {
		double v = Math.max(0.0, Math.min(1.0, linear));
		double encoded = v <= 0.0031308 ? v * 12.92 : 1.055 * Math.pow(v, 1.0 / 2.4) - 0.055;
		return (int) Math.round(encoded * 255.0);
	}
}
