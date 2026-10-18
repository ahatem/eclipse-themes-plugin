package com.github.eclipsethemes.eclipse;

import java.util.List;
import java.util.Objects;

import com.github.eclipsethemes.eclipse.workbench.WorkbenchOutcome;

/**
 * The aggregate result of applying a theme: the editor part (adapter preferences) and the workbench part
 * (coordinated Eclipse theme). Fully successful only when every requested part succeeded.
 */
public record ThemeApplyOutcome(EditorOutcome editor, WorkbenchOutcome workbench) {

	/** The adapters whose preferences could not be written; empty means every adapter succeeded. */
	public record EditorOutcome(List<String> failedAdapters) {

		public EditorOutcome {
			failedAdapters = List.copyOf(Objects.requireNonNull(failedAdapters, "failedAdapters"));
		}

		public boolean succeeded() {
			return failedAdapters.isEmpty();
		}
	}

	public ThemeApplyOutcome {
		Objects.requireNonNull(editor, "editor");
		Objects.requireNonNull(workbench, "workbench");
	}

	public boolean fullySucceeded() {
		return editor.succeeded() && workbench.succeeded();
	}
}
