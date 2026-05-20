package algorithm.recursion;

/**
 * Adapter contract to convert algorithm-specific recursion state into generic stack snapshots.
 * Each recursive algorithm can keep its own internal frame-state type, then map it into
 * a UI-friendly {@link StackFramesSnapshot} through this interface.
 */
public interface RecursiveStackAdapter<T> {
    /** Converts algorithm-specific recursion state into a stack snapshot. */
    StackFramesSnapshot toSnapshot(T state, String title);
}
