package com.github.eclipsethemes.eclipse.preferences;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

/** An isolated preference double: unexpected operations (including clear) fail. */
final class MemoryPreferences {

	final Map<String, String> values = new LinkedHashMap<>();
	final List<String> mutations = new ArrayList<>();
	String failingKey;
	boolean failFlush;
	int flushes;
	int clears;
	final IEclipsePreferences node = (IEclipsePreferences) Proxy.newProxyInstance(
			IEclipsePreferences.class.getClassLoader(), new Class<?>[] { IEclipsePreferences.class },
			(proxy, method, args) -> {
				String name = method.getName();
				switch (name) {
				case "put", "putBoolean":
					String value = String.valueOf(args[1]);
					values.put((String) args[0], value);
					mutations.add("put " + args[0] + "=" + value);
					return null;
				case "get":
					return values.getOrDefault(args[0], (String) args[1]);
				case "getBoolean":
					return values.containsKey(args[0]) ? Boolean.parseBoolean(values.get(args[0])) : args[1];
				case "remove":
					if (args[0].equals(failingKey)) throw new IllegalStateException("Injected key failure");
					values.remove(args[0]);
					mutations.add("remove " + args[0]);
					return null;
				case "flush":
					flushes++;
					if (failFlush) throw new BackingStoreException("Injected persistence failure");
					return null;
				case "clear":
					clears++;
					throw new AssertionError("Whole-node clear is forbidden");
				default:
					throw new AssertionError("Unexpected preference operation: " + name);
				}
			});
}
