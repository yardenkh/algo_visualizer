package algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Canonical subroutine definition plus a replayable timeline used for on-demand expansion.
 */
public final class SubroutineTimeline {
    private final String id;
    private final String displayName;
    private final List<String> pseudocodeLines;
    private final List<AlgorithmStep> steps;

    public SubroutineTimeline(String id, String displayName, List<String> pseudocodeLines, List<AlgorithmStep> steps) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Subroutine id cannot be blank.");
        }
        this.id = id;
        this.displayName = Objects.requireNonNullElse(displayName, id);
        this.pseudocodeLines = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNullElse(pseudocodeLines, List.of())));
        this.steps = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNullElse(steps, List.of())));
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getPseudocodeLines() {
        return pseudocodeLines;
    }

    public List<AlgorithmStep> getSteps() {
        return steps;
    }
}

