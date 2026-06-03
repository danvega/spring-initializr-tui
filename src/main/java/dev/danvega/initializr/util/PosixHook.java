package dev.danvega.initializr.util;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Replaces the current (JVM) process with a shell command using the POSIX
 * {@code execvp} system call via the Foreign Function &amp; Memory API.
 *
 * <p>The post-generate hook (e.g. Claude Code) is an interactive terminal app.
 * Launched directly from a shell it works fine, but run from this app it could
 * render yet take no input and was eventually killed by SIGHUP. The difference
 * is that it was running as a child of the still-alive JVM, which keeps JLine's
 * native terminal state and signal handling around. {@code exec} discards the
 * entire JVM image (threads, JLine native state, signal dispositions) while
 * keeping the same PID, foreground process group, session and controlling
 * terminal — exactly as if the hook had been typed at the shell.
 *
 * <p>The {@link FunctionDescriptor} and {@link MethodHandle} are {@code static
 * final} constants so GraalVM's native-image analysis registers the foreign
 * downcall at build time (otherwise the call fails at runtime in the native
 * image).
 */
public final class PosixHook {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final FunctionDescriptor EXECVP_DESC =
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS);
    // Address-taking handle: first parameter is the target function pointer.
    private static final MethodHandle EXECVP = LINKER.downcallHandle(EXECVP_DESC);
    private static final MemorySegment EXECVP_ADDR =
            LINKER.defaultLookup().find("execvp").orElse(MemorySegment.NULL);

    private PosixHook() {
    }

    /**
     * Replace this process with {@code /bin/sh -c "<command>"} running in
     * {@code workingDir}. On success this method never returns. If exec is
     * unsupported or fails, it throws so the caller can fall back.
     */
    public static void exec(Path workingDir, String command) throws Throwable {
        if (EXECVP_ADDR.equals(MemorySegment.NULL)) {
            throw new UnsupportedOperationException("execvp not available");
        }

        // Reset the terminal to a sane line discipline (only when stdin is a
        // tty), enter the project dir, then exec the hook so the shell process
        // itself is replaced by it.
        String script = "[ -t 0 ] && stty sane 2>/dev/null; cd "
                + shQuote(workingDir.toString()) + " && exec " + command;

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment sh = arena.allocateFrom("/bin/sh");
            MemorySegment dashC = arena.allocateFrom("-c");
            MemorySegment scriptArg = arena.allocateFrom(script);

            // argv: { "/bin/sh", "-c", script, NULL }
            MemorySegment argv = arena.allocate(ADDRESS, 4);
            argv.setAtIndex(ADDRESS, 0, sh);
            argv.setAtIndex(ADDRESS, 1, dashC);
            argv.setAtIndex(ADDRESS, 2, scriptArg);
            argv.setAtIndex(ADDRESS, 3, MemorySegment.NULL);

            int rc = (int) EXECVP.invoke(EXECVP_ADDR, sh, argv);
            // execvp only returns on failure.
            throw new IllegalStateException("execvp returned " + rc);
        }
    }

    /** Single-quote a string for safe use inside an sh command. */
    private static String shQuote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }
}
