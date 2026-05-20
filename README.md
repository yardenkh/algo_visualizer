# Graphs - Graph Editor and Algorithm Visualizer

This project is a Java Swing desktop application for creating graphs and visualizing algorithm
execution as step-by-step timelines.

## Primary Documentation

- Start here: `PROJECT_UNDERSTANDING.md`
- Package-level reference: `src/**/package-info.java`

`PROJECT_UNDERSTANDING.md` is the maintained source of truth for architecture, navigation,
extension playbooks, and API usage patterns.

## Quick Run

```powershell
$files = Get-ChildItem -Recurse -Path src -Filter *.java | ForEach-Object { $_.FullName }
javac -d out $files
java -cp out Main
```

