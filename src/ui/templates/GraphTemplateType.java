package ui.templates;

/** Categories used by the premade graph library filter. */
public enum GraphTemplateType {
    ALL_GRAPHS("All Graphs"),
    BASICS("Basics"),
    TREES("Trees"),
    DAGS("DAGs"),
    WEIGHTED("Weighted"),
    GRIDS("Grids"),
    BIPARTITE("Bipartite"),
    FLOW("Flow / Networks"),
    SPECIAL("Special");

    private final String label;

    GraphTemplateType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
