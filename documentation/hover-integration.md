# Hover Documentation Integration

<a id="doc-hover-integration"></a>

You asked for documentation to appear when hovering symbols like `ui.dialogs` or `graph`.

There are two parts to make this work professionally:

## 1) Source-level docs (implemented)

JetBrains hover documentation uses Javadoc from source.

To support hover popups:

- package summaries live in `src/**/package-info.java`,
- class summaries live in each type Javadoc,
- method-level summaries live in method Javadocs for key flows,
- anchors and deep explanations live in `documentation/` for full reading.

That means hovering package and class symbols in code now has meaningful content to show.

## 2) IDE setting (user-side toggle)

In JetBrains IDEs, enable automatic hover popups:

- **Settings / Editor / Code Editing / Quick Documentation**
- enable **Show quick documentation on mouse move**

(Exact wording can vary slightly by IDE version.)

## Recommended professional workflow

1. Hover a symbol/package for a short summary.
2. Use quick doc navigation to jump to source Javadocs.
3. Open the matching page under `documentation/packages/` for deep architecture context.
4. Use `documentation/reference/file-coverage.md` when you need full-project coverage.

## Linking model

This documentation system is built around stable anchors such as:

- `pkg-ui-dialogs`
- `cls-ui-grapheditorcontroller`
- `method-ui-grapheditorcontroller-pasteclipboard`

These anchors are safe targets for future editor integrations, scripts, or plugin-based symbol-to-doc navigation.

