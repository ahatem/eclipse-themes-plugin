package smoke;

import java.nio.file.Path;
import org.eclipse.core.runtime.Platform;

/** Preconditions for the disposable R1 process, shared only by probe applications. */
public final class RuntimeState {
    private RuntimeState() {}
    public static void verify() throws Exception {
        Path root = Path.of(System.getProperty("r1.scratch")).toRealPath();
        if (!System.getProperty("java.specification.version").equals("21")) throw new AssertionError("Expected JDK 21 launcher");
        for (var entry : java.util.Map.of("configuration", Platform.getConfigurationLocation(),
                "workspace", Platform.getInstanceLocation(), "user", Platform.getUserLocation()).entrySet()) {
            Path path = Path.of(entry.getValue().getURL().toURI()).toAbsolutePath().normalize();
            if (!path.startsWith(root)) throw new AssertionError("Non-disposable " + entry.getKey() + " location: " + path);
            System.out.println("STATE " + entry.getKey() + "=" + path);
        }
        Path home = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        if (!home.startsWith(root)) throw new AssertionError("Non-disposable user.home: " + home);
        System.out.println("STATE user.home=" + home);
        System.out.println("RUNTIME Java=" + System.getProperty("java.runtime.version") + " OS=" + System.getProperty("os.name")
                + " arch=" + System.getProperty("os.arch"));
    }
}
