/**
 * Premade graph template model, catalog, and preview components.
 *
 * <p>This package defines the static graph template library used by the premade-graphs workflow.
 * Templates are pure metadata (normalized positions + edge lists) that can be rendered in a small
 * preview or loaded into the editor model.
 *
 * <p><b>Key components</b>
 * <ul>
 *   <li>{@link ui.templates.GraphTemplate} immutable template data model</li>
 *   <li>{@link ui.templates.GraphTemplateLibrary} built-in catalog factory</li>
 *   <li>{@link ui.templates.GraphTemplateType} filter taxonomy used in the picker UI</li>
 *   <li>{@link ui.templates.GraphTemplatePreviewPanel} lightweight visual thumbnail renderer</li>
 * </ul>
 *
 * <p><b>Public API surface</b>
 * <ul>
 *   <li>{@link ui.templates.GraphTemplateLibrary#allTemplates()} full catalog access</li>
 *   <li>{@link ui.templates.GraphTemplateLibrary#byType(ui.templates.GraphTemplateType)} filtered access</li>
 * </ul>
 *
 * <p><b>Interactions</b>
 * <ul>
 *   <li>Consumed by {@code ui.dialogs.PremadeGraphsDialog}</li>
 *   <li>Selected template is materialized by {@link ui.GraphEditorController#loadGraphTemplate(ui.templates.GraphTemplate)}</li>
 * </ul>
 *
 * <p><b>Design notes</b>
 * <ul>
 *   <li>Template coordinates are normalized so previews and full-canvas loading share data</li>
 *   <li>Library is static and deterministic for reproducible demos and screenshots</li>
 * </ul>
 */
package ui.templates;

