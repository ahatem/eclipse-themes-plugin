package com.github.eclipsethemes.core.models;

import java.util.Locale;

public enum ThemeType {
	LIGHT, DARK;
	
	public String nameCapitalized() {
		return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
	}
	
	public static ThemeType from(String type) {
		return type.toLowerCase(Locale.ROOT).equals("dark") ? DARK : LIGHT;
	}
}
