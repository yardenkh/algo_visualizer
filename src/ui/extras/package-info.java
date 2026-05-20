/**
 * Selection -> Extras action handlers and related UI-side utilities.
 *
 * <p>Classes in this package translate user-triggered extras operations into controller/service
 * calls, then report outcomes through dialog messages. They are intentionally thin orchestration
 * layers around reusable domain services.
 *
 * <p><b>Key components</b>
 * <ul>
 *   <li>Coloring handlers that call {@code algorithm.coloring} services</li>
 *   <li>Structural transform handlers (complement graph, line graph, root graph utilities)</li>
 *   <li>Edge crossing reduction and palette utilities for user-visible coloring</li>
 * </ul>
 *
 * <p><b>Public API surface</b>
 * <ul>
 *   <li>Action handler {@code run()} methods invoked by menu/dialog callbacks</li>
 *   <li>Service/helper classes that encapsulate specific extras computations</li>
 * </ul>
 *
 * <p><b>Interactions</b>
 * <ul>
 *   <li>Consumes selection/model operations from {@link ui.GraphEditorController}</li>
 *   <li>Delegates exact coloring to {@code algorithm.coloring}</li>
 *   <li>Launched from extras UI in {@code ui.dialogs.SelectionExtrasDialog}</li>
 * </ul>
 *
 * <p><b>Design notes</b>
 * <ul>
 *   <li>Handlers validate selection scope early and provide user-focused error messages</li>
 *   <li>Domain operations stay outside Swing components for easier reuse and testing</li>
 * </ul>
 */
package ui.extras;

