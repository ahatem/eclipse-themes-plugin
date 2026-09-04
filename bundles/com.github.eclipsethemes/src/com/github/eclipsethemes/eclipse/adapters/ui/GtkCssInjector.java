package com.github.eclipsethemes.eclipse.adapters.ui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Best-effort bridge to SWT's GTK PI. SWT intentionally has no public API for
 * application GTK providers, so all native access is isolated and fail-soft.
 */
final class GtkCssInjector {

	private static final List<String> GTK_CLASSES = List.of(
			"org.eclipse.swt.internal.gtk.GTK",
			"org.eclipse.swt.internal.gtk3.GTK3",
			"org.eclipse.swt.internal.gtk4.GTK4");
	private static final String GDK_CLASS = "org.eclipse.swt.internal.gtk.GDK";

	private static long currentProvider;
	private static Listener showListener;

	private GtkCssInjector() {
	}

	static boolean apply(Display display, String css, ILog log) {
		try {
			byte[] data = (css + '\0').getBytes(StandardCharsets.UTF_8);
			Class<?> gtk = loadClassWithMethod("gtk_css_provider_new");
			long provider = ((Number) invoke(gtk, "gtk_css_provider_new")).longValue();
			boolean loaded = loadCss(gtk, provider, data);
			if (provider == 0 || !loaded) {
				throw new IllegalStateException("GTK rejected the generated stylesheet");
			}

			int priority = readInt(gtk, "GTK_STYLE_PROVIDER_PRIORITY_USER", 800);
			boolean globallyApplied = applyGlobally(provider, priority);
			if (!globallyApplied) {
				currentProvider = provider;
				installShowListener(display, log);
				Arrays.stream(display.getShells()).forEach(shell -> applyRecursively(shell, provider, priority, log));
			}
			return true;
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			log.warn("Could not inject GTK CSS; Eclipse CSS will remain active", e);
			return false;
		}
	}

	private static boolean loadCss(Class<?> providerGtk, long provider, byte[] data)
			throws ReflectiveOperationException {
		// SWT keeps gtk_css_provider_new on GTK; load_from_data lives on GTK3/GTK4.
		List<Class<?>> searchOrder = new ArrayList<>();
		boolean gtk4 = false;
		try {
			gtk4 = providerGtk.getField("GTK4").getBoolean(null);
		} catch (ReflectiveOperationException ignored) {
			// Fall back to probing every available GTK binding class.
		}
		if (gtk4) {
			loadAvailableGtkClasses().stream().filter(c -> c.getName().endsWith(".GTK4")).forEach(searchOrder::add);
		}
		for (Class<?> type : loadAvailableGtkClasses()) {
			if (!searchOrder.contains(type)) {
				searchOrder.add(type);
			}
		}
		if (!searchOrder.contains(providerGtk)) {
			searchOrder.add(providerGtk);
		}

		for (Class<?> type : searchOrder) {
			for (Method method : type.getMethods()) {
				if (!method.getName().equals("gtk_css_provider_load_from_data")
						|| !Modifier.isStatic(method.getModifiers())) {
					continue;
				}
				Object result;
				// SWT passes -1 so GTK treats the buffer as NUL-terminated.
				if (method.getParameterCount() == 4) {
					result = method.invoke(null, provider, data, -1L, null);
				} else if (method.getParameterCount() == 3) {
					Object length = method.getParameterTypes()[2] == int.class ? -1 : -1L;
					result = method.invoke(null, provider, data, length);
				} else {
					continue;
				}
				return !(result instanceof Boolean success) || success;
			}
		}
		return false;
	}

	private static boolean applyGlobally(long provider, int priority) throws ReflectiveOperationException {
		Class<?> gdk = Class.forName(GDK_CLASS);
		for (Class<?> gtk : loadAvailableGtkClasses()) {
			Method addDisplay = findMethod(gtk, "gtk_style_context_add_provider_for_display", 3);
			Method getDisplay = findMethod(gdk, "gdk_display_get_default", 0);
			if (addDisplay != null && getDisplay != null) {
				long nativeDisplay = ((Number) getDisplay.invoke(null)).longValue();
				addDisplay.invoke(null, nativeDisplay, provider, priority);
				return true;
			}

			Method addScreen = findMethod(gtk, "gtk_style_context_add_provider_for_screen", 3);
			Method getScreen = findMethod(gdk, "gdk_screen_get_default", 0);
			if (addScreen != null && getScreen != null) {
				long screen = ((Number) getScreen.invoke(null)).longValue();
				addScreen.invoke(null, screen, provider, priority);
				return true;
			}
		}
		return false;
	}

	private static void installShowListener(Display display, ILog log) {
		if (showListener != null) {
			return;
		}
		showListener = event -> applyShownControl(event, log);
		display.addFilter(SWT.Show, showListener);
	}

	private static void applyShownControl(Event event, ILog log) {
		if (event.widget instanceof Control control && currentProvider != 0) {
			applyRecursively(control, currentProvider, 800, log);
		}
	}

	private static void applyRecursively(Control control, long provider, int priority, ILog log) {
		try {
			long handle = readHandle(control);
			if (handle != 0) {
				Class<?> gtk = loadClassWithMethod("gtk_widget_get_style_context");
				long context = ((Number) invoke(gtk, "gtk_widget_get_style_context", handle)).longValue();
				if (context != 0) {
					invoke(loadClassWithMethod("gtk_style_context_add_provider"),
							"gtk_style_context_add_provider", context, provider, priority);
				}
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			log.warn("Could not apply GTK CSS to " + control.getClass().getSimpleName(), e);
		}

		if (control instanceof Composite composite) {
			Arrays.stream(composite.getChildren()).forEach(child -> applyRecursively(child, provider, priority, log));
		}
	}

	private static long readHandle(Control control) throws ReflectiveOperationException {
		Class<?> type = control.getClass();
		while (type != null) {
			try {
				Field field = type.getDeclaredField("handle");
				field.setAccessible(true);
				return field.getLong(control);
			} catch (NoSuchFieldException e) {
				type = type.getSuperclass();
			}
		}
		return 0;
	}

	private static Class<?> loadClassWithMethod(String methodName) throws ClassNotFoundException {
		for (Class<?> type : loadAvailableGtkClasses()) {
			if (findMethod(type, methodName, -1) != null) {
				return type;
			}
		}
		throw new ClassNotFoundException("No SWT GTK binding for " + methodName);
	}

	private static List<Class<?>> loadAvailableGtkClasses() {
		List<Class<?>> available = new ArrayList<>();
		for (String name : GTK_CLASSES) {
			try {
				available.add(Class.forName(name));
			} catch (ClassNotFoundException | LinkageError e) {
				// SWT exposes different PI classes for GTK3 and GTK4 releases.
			}
		}
		return available;
	}

	private static Object invoke(Class<?> type, String name, Object... arguments) throws ReflectiveOperationException {
		Method method = findMethod(type, name, arguments.length);
		if (method == null) {
			throw new NoSuchMethodException(type.getName() + "." + name);
		}
		return method.invoke(null, arguments);
	}

	private static Method findMethod(Class<?> type, String name, int parameterCount) {
		return Arrays.stream(type.getMethods())
				.filter(method -> method.getName().equals(name))
				.filter(method -> parameterCount < 0 || method.getParameterCount() == parameterCount)
				.findFirst().orElse(null);
	}

	private static int readInt(Class<?> type, String name, int fallback) {
		try {
			return type.getField(name).getInt(null);
		} catch (ReflectiveOperationException e) {
			return fallback;
		}
	}
}
