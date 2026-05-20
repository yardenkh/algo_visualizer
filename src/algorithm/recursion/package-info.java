/**
 * Recursion visualization adapters and stack payload models.
 *
 * <p>This package bridges algorithm-specific recursion state to a generic UI representation.
 * Recursive algorithms keep their own internal frame models and use adapters here to emit
 * immutable {@link algorithm.recursion.StackFramesSnapshot} payloads.
 *
 * <p><b>Key components</b>
 * <ul>
 *   <li>{@link algorithm.recursion.RecursiveStackAdapter} generic conversion contract</li>
 *   <li>{@link algorithm.recursion.DfsStackFrameAdapter} maps DFS frame state</li>
 *   <li>{@link algorithm.recursion.SeidelStackFrameAdapter} maps Seidel recursion state</li>
 *   <li>{@link algorithm.recursion.StackFrameView} and {@link algorithm.recursion.StackFramesSnapshot}
 *       immutable transport objects</li>
 * </ul>
 *
 * <p><b>Public API surface</b>
 * <ul>
 *   <li>Adapters expose {@code toSnapshot(...)} for timeline builders</li>
 *   <li>Snapshot/frame classes expose immutable getters used by stack dialogs</li>
 * </ul>
 *
 * <p><b>Interactions</b>
 * <ul>
 *   <li>Produced by algorithms such as DFS and Seidel in {@code algorithm.sssp}/{@code algorithm.general}</li>
 *   <li>Consumed by {@code ui.dialogs.AlgorithmStackFramesDialog}</li>
 * </ul>
 *
 * <p><b>Design notes</b>
 * <ul>
 *   <li>Adapter pattern avoids coupling UI structures to algorithm internals</li>
 *   <li>Frames are ordered top-first to match how the stack viewer renders data</li>
 * </ul>
 */
package algorithm.recursion;

