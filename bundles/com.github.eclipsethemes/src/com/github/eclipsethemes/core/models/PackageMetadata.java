package com.github.eclipsethemes.core.models;

import java.util.Objects;
import java.util.Optional;

/**
 * Metadata only a theme package carries. Every value is untrusted author text: the package id is a source claim,
 * never catalog identity, and an absent license is never assumed to be the plugin's own.
 *
 * @param packageId the author's dotted package id
 * @param version   author-facing display version, or {@code null}
 * @param license   SPDX expression or {@code LicenseRef-} text, or {@code null} when not stated
 * @param copyright the rights holder, or {@code null} when not stated
 * @param source    the upstream source URL as written, or {@code null}
 */
public record PackageMetadata(String packageId, String version, String license, String copyright, String source) {

	public PackageMetadata {
		Objects.requireNonNull(packageId, "packageId");
	}

	public Optional<String> versionText() {
		return Optional.ofNullable(version);
	}

	public Optional<String> licenseText() {
		return Optional.ofNullable(license);
	}

	public Optional<String> copyrightText() {
		return Optional.ofNullable(copyright);
	}

	public Optional<String> sourceText() {
		return Optional.ofNullable(source);
	}
}
