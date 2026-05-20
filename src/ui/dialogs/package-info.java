/**
 * Dialog and embeddable-panel components for secondary UI workflows.
 *
 * <p>Deep reference: see {@code documentation/packages/ui-dialogs.md} for full class-by-class
 * behavior, workflows, and extension guidance.
 *
 * <p>This package contains modal configuration dialogs and modeless algorithm viewers. Most
 * classes can provide embeddable panels so the same UI content works in floating windows and the
 * workspace tabs.
 *
 * <p><b>Key components</b>
 * <ul>
 *   <li>Run/configuration: {@link ui.dialogs.AlgorithmRunDialog},
 *       {@link ui.dialogs.AlgorithmRunConfiguration}</li>
 *   <li>Pseudocode and subroutines: {@link ui.dialogs.AlgorithmPseudocodeDialog}</li>
 *   <li>Data views: matrix, stack frames, residual graph, level graph, topological order dialogs</li>
 *   <li>Utility dialogs: shortcuts, extras panel, and premade graph picker</li>
 * </ul>
 *
 * <p><b>Public API surface</b>
 * <ul>
 *   <li>Construct dialogs with owner frame, then read selected result objects on close</li>
 *   <li>Viewer dialogs expose update/show methods used by playback synchronization</li>
 * </ul>
 *
 * <p><b>Interactions</b>
 * <ul>
 *   <li>Driven by {@link ui.GraphEditorFrame} and {@link ui.AlgorithmWorkspacePanel}</li>
 *   <li>Consumes timeline payloads from {@code algorithm} package</li>
 *   <li>Premade picker consumes template catalog from {@code ui.templates}</li>
 * </ul>
 *
 * <p><b>Design notes</b>
 * <ul>
 *   <li>Dialogs keep presentation logic separate from graph mutation/algorithm execution</li>
 *   <li>Pseudocode dialog supports expandable subroutine overlays using line metadata</li>
 * </ul>
 */
package ui.dialogs;
