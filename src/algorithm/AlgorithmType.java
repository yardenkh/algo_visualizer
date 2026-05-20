package algorithm;

/** Category used by the algorithm picker filter. */
public enum AlgorithmType {
    ALL_ALGORITHMS("All Algorithms"),
    APSP("APSP"),
    SSSP("SSSP"),
    MST("MST"),
    MAXIMUM_FLOW("maximum-flow"),
    GENERAL("General");

    private final String label;

    AlgorithmType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
