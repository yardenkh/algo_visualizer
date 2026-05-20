## Code Organization & Commentary Guide

This document describes the code organization patterns used throughout the project and how comments are structured to aid future development.

### Package Organization

The project is organized into **three main packages**, each with clear responsibilities:

#### 1. `graph` Package - Core Domain Model
**Purpose**: Pure data structures representing graphs
**Files**:
- `Vertex.java` - Represents a single graph vertex with reusable state
- `Edge.java` - Represents a directed edge between two vertices
- `DirectedGraph.java` - Container for vertices and edges

**Design principle**: Vertices hold both *structural* data (neighbors, edges) and *temporary* algorithm state (distance, color, times). This allows algorithms to use vertex state without modifying the graph structure.

**Key methods**:
- `Vertex.reset()` - Clears all algorithm state
- `Vertex.getEdgeTo(Vertex)` - O(1) edge lookup via neighbor list
- `DirectedGraph.getVertex(id)` - O(1) vertex lookup via map

#### 2. `algorithm` Package - Framework + Category Subpackages
**Purpose**: Shared playback framework + grouped algorithm implementations

**Core framework** (`algorithm`):
- `GraphAlgorithm` - Base interface; all algorithms implement this
- `AlgorithmRequest` - Input specification (graph, scope, source vertex)
- `AlgorithmResult` - Output: timeline steps + pseudocode lines
- `AlgorithmStep` - Single immutable snapshot in the timeline
- `AlgorithmPlaybackController` - Manages timeline stepping

**Algorithm subpackages**:
- `algorithm.sssp` - BFS, DFS, Dijkstra, Bellman-Ford, DAG-shortest-path
- `algorithm.apsp` - Floyd-Warshall, Matrix-Multiplication, Recursive-Doubling, Johnson, Repeated-Dijkstra
- `algorithm.mst` - Prim, Kruskal
- `algorithm.general` - Topological-Order, Transitive-Closure, A*, Seidel
- `algorithm.recursion` - stack-frame adapters and models for recursive timeline visualization

**Visualization payloads** (attached to steps):
- `MatrixSnapshot` - For APSP algorithms (matrix state at each step)
- `StackFramesSnapshot` - For recursive algorithms (call stack frames)

**Design pattern - Timeline Generation**:
```java
// Pseudocode for timeline generation pattern:
List<AlgorithmStep> steps = new ArrayList<>();

// Initialize
steps.add(snapshot(..., line=1, "Initialize", "Setting up..."));

// Main loop
while (condition) {
    // Mark line in pseudocode
    steps.add(snapshot(..., line=3, "Compute X", "Computing..."));
    
    // Perform computation
    int result = compute();
    
    // Show result state
    steps.add(snapshot(..., line=4, "Result", "Result is " + result));
}

// Return result with timeline
return new AlgorithmResult(getName(), PSEUDOCODE, steps);
```

Each `snapshot()` call creates an immutable `AlgorithmStep` with:
- **Pseudocode line index** - for UI highlighting
- **Vertex colors** - from `AlgorithmPalette` (NEUTRAL, DISCOVERED, FRONTIER, FINISHED, ACTIVE)
- **Vertex annotations** - labels like "dist=5", "d=2 f=7"
- **Edge colors** - highlight tree/back/cross/forward edges
- **Optional payloads** - matrix snapshots, stack frames

#### 3. `ui` Package - Root + Dialog/Template Subpackages
**Purpose**: Swing GUI for graph editing and algorithm visualization

**Root components** (`ui`):
- `GraphEditorFrame` - Main window (composition root)
- `GraphEditorController` - MVC controller (model operations, selection)
- `GraphCanvasPanel` - Canvas rendering + mouse/keyboard input
- `EditorTool` - Enum: Select, Add Vertex, Connect, Delete, Pan

**Dialogs** (`ui.dialogs`):
- `AlgorithmRunDialog`
- `AlgorithmPseudocodeDialog`
- `AlgorithmMatrixDialog`
- `AlgorithmStackFramesDialog`
- `TopologicalOrderDialog`
- `PremadeGraphsDialog`
- `ShortcutsDialog`

**Templates** (`ui.templates`):
- `GraphTemplate`
- `GraphTemplateType`
- `GraphTemplateLibrary`
- `GraphTemplatePreviewPanel`

### Comment Structure

#### Class-level Comments
Every class has a comprehensive javadoc explaining:
1. **What it represents/does** (1-2 sentences)
2. **Key responsibilities** (bulleted)
3. **Design patterns** (if applicable)
4. **Time/space complexity** (if relevant)

Example:
```java
/**
 * Immutable snapshot of one step in an algorithm execution timeline.
 * 
 * Each step captures:
 * - Pseudocode line being executed
 * - Vertex colors and annotations
 * - Optional matrix/stack snapshots
 * 
 * Steps are created as an algorithm executes and then played back
 * with forward/backward stepping via AlgorithmPlaybackController.
 * 
 * The timeline model ensures:
 * - All state at each step is immutable
 * - UI can render any step independently
 * - Stepping forward/backward is stateless and efficient
 */
```

#### Method-level Comments
Methods have javadoc explaining:
1. **What it does** (concise)
2. **@param** documentation
3. **@return** documentation
4. **@throws** if applicable
5. **Time complexity** for performance-critical methods

Example:
```java
/**
 * Finds the outgoing edge from this vertex to the given destination.
 *
 * @param destination destination vertex to search for
 * @return the matching edge, or {@code null} if none exists
 */
public Edge getEdgeTo(Vertex destination) {
    // ...
}
```

#### Inline Comments
Sparse but targeted comments in complex logic:
1. **Why** (not what) - explain non-obvious decisions
2. **State transitions** - mark significant color/state changes
3. **Edge cases** - handle special situations
4. **Algorithm steps** - map code to pseudocode lines

Example:
```java
if (!discovered.contains(destination.getId())) {
    // Mark line 6: "if color[v] = WHITE then..."
    treeEdges.add(EdgeKey.from(edge));
    steps.add(snapshot(..., line=6, "if color[v] = WHITE then..."));
    
    // Recurse to unexplored neighbor
    exploreFrom(destination, ...);
}
```

### Adding New Algorithms

Follow this template:

```java
/**
 * [Algorithm name] for [problem type].
 * 
 * Timeline approach:
 * 1. [Step 1 description]
 * 2. [Step 2 description]
 * ...
 * 
 * Visualization payloads:
 * - Vertex colors: [describe color sequence]
 * - Edge colors: [describe edge types]
 * - Optional: Matrix snapshots / Stack frames
 * 
 * Time complexity: O(...)
 * Space complexity: O(...)
 */
public final class YourAlgorithm implements GraphAlgorithm {
    // Pseudocode lines as List<String>
    private static final List<String> PSEUDOCODE = List.of(
        "procedure YOUR-ALGORITHM(G)",
        "  // ... pseudocode matching UI expectations
        "end procedure"
    );
    
    @Override
    public AlgorithmResult run(AlgorithmRequest request) {
        List<AlgorithmStep> steps = new ArrayList<>();
        
        // Initialize
        steps.add(snapshot(..., 1, "Initialize", "Setting up..."));
        
        // Main logic
        // ... emit steps as algorithm progresses ...
        
        return new AlgorithmResult(getDisplayName(), PSEUDOCODE, steps);
    }
    
    private AlgorithmStep snapshot(
        // ... parameters ...
    ) {
        Map<Integer, Integer> vertexColors = new LinkedHashMap<>();
        Map<Integer, String> labels = new LinkedHashMap<>();
        
        // Populate colors and labels based on algorithm state
        
        // Optional: MatrixSnapshot for APSP
        MatrixSnapshot matrix = null; // or create snapshot
        
        // Optional: StackFramesSnapshot for recursive
        StackFramesSnapshot stack = null; // or create snapshot
        
        return new AlgorithmStep(title, message, line, 
            vertexColors, edgeColors, labels, matrix, stack);
    }
}
```

### Code Navigation Tips

**Finding code related to a specific concept**:
- **Graph model**: `graph/` package
- **Algorithm X**: `algorithm/[AlgorithmName].java`
- **UI rendering**: `ui/GraphCanvasPanel.java`
- **Algorithm UI**: `ui/dialogs/AlgorithmRunDialog.java` and related
- **Playback control**: `algorithm/AlgorithmPlaybackController.java`
- **Premade graphs**: `ui/GraphTemplateLibrary.java`

**Understanding algorithm flow**:
1. Read the pseudocode array at top of algorithm class
2. Read `run()` method to understand step generation
3. Look for `snapshot()` calls mapping to pseudocode lines
4. Check visualization helpers (colors, adapters, payload classes)

### Documentation Files

- `README.md` - High-level project overview and features
- `ARCHITECTURE.md` - This file; detailed code organization
- `package-info.java` - Per-package documentation (in each package directory)
- Javadoc comments throughout - Inline documentation

### Future Maintenance

When modifying code:
1. Update javadoc if behavior changes
2. Keep comments aligned with actual code (they rot quickly!)
3. Add inline comments for "why" decisions, not "what" the code does
4. Maintain the timeline generation pattern for consistency
5. Update `README.md` if user-facing features change
6. Add new algorithms following the established template

### Compilation & Validation

```powershell
# Compile all sources recursively
$files = Get-ChildItem -Recurse -Path src -Filter *.java | ForEach-Object { $_.FullName }
javac -d out $files

# Run smoke tests
java -cp out algorithm.AlgorithmPlaybackSmokeTest

# Launch application
java -cp out Main
```

All code compiles with no errors or warnings. The codebase is self-documenting through javadoc and strategic inline comments.
