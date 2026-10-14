package com.immortalstorage.immortalstorage.dimension;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/** Thread-local realm scope, bounded nested calls, and weak extra-tick failure quarantine. */
public final class RealmTickSafety {
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();
    private static final Map<Object, Map<Object, Boolean>> FAILED = new WeakHashMap<>();
    private static final class Frame {
        final Object level;
        final Set<Object> running = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean extra;
        Frame(Object level) { this.level = level; }
    }
    public static AutoCloseableScope open(Object level) {
        Frame previous = CURRENT.get(); CURRENT.set(new Frame(level));
        return new AutoCloseableScope(previous);
    }
    public static final class AutoCloseableScope implements AutoCloseable {
        private final Frame previous;
        private AutoCloseableScope(Frame previous) { this.previous = previous; }
        @Override public void close() { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
    public static boolean scoped(Object level) { Frame frame = CURRENT.get(); return frame != null && frame.level == level; }
    public static void extra(Object level, Runnable work) {
        Frame frame = CURRENT.get();
        if (frame == null || frame.level != level) throw new IllegalStateException("Extra ticks require their realm scope");
        boolean previous = frame.extra; frame.extra = true;
        try { work.run(); } finally { frame.extra = previous; }
    }
    public static void execute(Object level, Object target, boolean blocked, Runnable work, Consumer<Throwable> failed) {
        Frame frame = CURRENT.get();
        if (frame == null || frame.level != level) { work.run(); return; }
        Map<Object, Boolean> quarantine = FAILED.computeIfAbsent(level, ignored -> new WeakHashMap<>());
        if (frame.extra && (blocked || quarantine.containsKey(target))) return;
        if (frame.running.size() >= 64 || !frame.running.add(target)) return;
        try { work.run(); }
        catch (RuntimeException | StackOverflowError exception) {
            if (!frame.extra) throw exception;
            quarantine.put(target, true); failed.accept(exception);
        } finally { frame.running.remove(target); }
    }
    private RealmTickSafety() {}
}
