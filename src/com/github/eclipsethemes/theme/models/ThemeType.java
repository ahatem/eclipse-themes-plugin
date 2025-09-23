package com.github.eclipsethemes.theme.models;

public enum ThemeType {
	LIGHT, DARK;
	
	public String nameCapitalized() {
		return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
	}
	
	public static ThemeType from(String type) {
		return type.toLowerCase().equals("dark") ? DARK : LIGHT;
	}
}
