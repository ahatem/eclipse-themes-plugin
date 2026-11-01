package com.github.eclipsethemes.eclipse;

import java.util.List;
import java.util.Objects;

import com.github.eclipsethemes.eclipse.fonts.FontOutcome;
import com.github.eclipsethemes.eclipse.workbench.WorkbenchOutcome;

/**
 * The aggregate result of applying a theme: the editor part (adapter preferences), the workbench part
 * (coordinated Eclipse theme) and the optional font part. Fully successful only when every requested part succeeded.
 */
public record ThemeApplyOutcome(EditorOutcome editor, WorkbenchOutcome workbench, FontOutcome fonts) {

	/**
	 * One editor preference that was changed outside Eclipse Themes after this plugin wrote it. The external value
	 * is preserved and ownership of the key is released.
	 */
	public record EditorConflict(String integration, String nodeId, String key, String message) {
		public EditorConflict {
			Objects.requireNonNull(integration, "integration");
			Objects.requireNonNull(nodeId, "nodeId");
			Objects.requireNonNull(key, "key");
			Objects.requireNonNull(message, "message");
		}
	}

	/** The adapters whose preferences could not be written, and keys preserved because someone else changed them. */
	public record EditorOutcome(List<String> failedAdapters, List<EditorConflict> conflicts) {

		public EditorOutcome {
			failedAdapters = List.copyOf(Objects.requireNonNull(failedAdapters, "failedAdapters"));
			conflicts = List.copyOf(Objects.requireNonNull(conflicts, "conflicts"));
		}

		public EditorOutcome(List<String> failedAdapters) {
			this(failedAdapters, List.of());
		}

		public boolean succeeded() {
			return failedAdapters.isEmpty() && conflicts.isEmpty();
		}
	}

	public ThemeApplyOutcome {
		Objects.requireNonNull(editor, "editor");
		Objects.requireNonNull(workbench, "workbench");
		Objects.requireNonNull(fonts, "fonts");
	}

	/** An outcome for which fonts were not requested. */
	public ThemeApplyOutcome(EditorOutcome editor, WorkbenchOutcome workbench) {
		this(editor, workbench, FontOutcome.NOT_REQUESTED);
	}

	public boolean fullySucceeded() {
		return editor.succeeded() && workbench.succeeded() && fonts.succeeded();
	}
}
