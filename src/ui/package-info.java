/**
 * Root Swing UI package for graph editing and algorithm playback.
 *
 * <p>This package contains the main application shell and core editor components. It wires the
 * graph model, algorithm framework, and all secondary views into one interactive desktop flow.
 *
 * <p><b>Key components</b>
 * <ul>
 *   <li>{@link ui.GraphEditorFrame} - composition root, toolbar/actions, playback orchestration</li>
 *   <li>{@link ui.GraphEditorController} - model mutation, selection state, undo snapshots</li>
 *   <li>{@link ui.GraphCanvasPanel} - rendering pipeline and direct manipulation input handling</li>
 *   <li>{@link ui.AlgorithmWorkspacePanel} - docked/tabbed host for algorithm side views</li>
 *   <li>{@link ui.AlgorithmDetailsFormatter} and {@link ui.EditorTool} - UI utility abstractions</li>
 * </ul>
 *
 * <p><b>Public API surface</b>
 * <ul>
 *   <li>{@link ui.GraphEditorFrame} as primary startup window</li>
 *   <li>{@link ui.GraphEditorController} operations used by canvas and extras handlers</li>
 * </ul>
 *
 * <p><b>Interactions</b>
 * <ul>
 *   <li>Consumes graph model from {@code graph} and algorithm timelines from {@code algorithm}</li>
 *   <li>Delegates modal/modeless windows to {@code ui.dialogs}</li>
 *   <li>Delegates premade graph metadata to {@code ui.templates}</li>
 *   <li>Invokes selection utilities in {@code ui.extras}</li>
 * </ul>
 *
 * <p><b>Design notes</b>
 * <ul>
 *   <li>Controller-view split keeps mutation logic outside painting/input classes</li>
 *   <li>Playback sync follows observer/listener callbacks from the playback controller</li>
 *   <li>Workspace supports docked and tabbed layouts without changing algorithm APIs</li>
 * </ul>
 */
package ui;

