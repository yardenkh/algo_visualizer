package algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Controls stepping through the currently loaded algorithm timeline. */
public final class AlgorithmPlaybackController {
    private final List<Runnable> changeListeners;
    private AlgorithmResult currentResult;
    private AlgorithmResult baseResult;
    private int currentStepIndex;
    private String infoMessage;
    private String activeSubroutineId;
    private SubroutineTimeline activeSubroutineTimeline;
    private int preSubroutineStepIndex;
    private int activeSubroutineStartIndex;
    private int activeSubroutineEndIndex;

    public AlgorithmPlaybackController() {
        this.changeListeners = new ArrayList<>();
        this.currentStepIndex = -1;
        this.infoMessage = "No algorithm running.";
        this.preSubroutineStepIndex = -1;
        this.activeSubroutineStartIndex = -1;
        this.activeSubroutineEndIndex = -1;
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void start(AlgorithmResult result) {
        if (result == null || result.getSteps().isEmpty()) {
            stop("The selected algorithm produced no timeline steps.");
            return;
        }
        this.baseResult = result;
        this.currentResult = result;
        this.currentStepIndex = 0;
        this.activeSubroutineId = null;
        this.activeSubroutineTimeline = null;
        this.preSubroutineStepIndex = -1;
        this.activeSubroutineStartIndex = -1;
        this.activeSubroutineEndIndex = -1;
        this.infoMessage = buildInfoMessage();
        notifyChanged();
    }

    public void stepForward() {
        if (!canStepForward()) {
            return;
        }
        currentStepIndex++;
        infoMessage = buildInfoMessage();
        notifyChanged();
    }

    public void stepBackward() {
        if (!canStepBackward()) {
            return;
        }
        currentStepIndex--;
        infoMessage = buildInfoMessage();
        notifyChanged();
    }

    public void stop() {
        stop("Algorithm playback stopped.");
    }

    public void stop(String reason) {
        currentResult = null;
        baseResult = null;
        currentStepIndex = -1;
        activeSubroutineId = null;
        activeSubroutineTimeline = null;
        preSubroutineStepIndex = -1;
        activeSubroutineStartIndex = -1;
        activeSubroutineEndIndex = -1;
        infoMessage = reason == null || reason.isBlank() ? "Algorithm playback stopped." : reason;
        notifyChanged();
    }

    public boolean hasActiveSession() {
        return currentResult != null && currentStepIndex >= 0;
    }

    public boolean canStepForward() {
        return hasActiveSession() && currentStepIndex < currentResult.getSteps().size() - 1;
    }

    public boolean canStepBackward() {
        return hasActiveSession() && currentStepIndex > 0;
    }

    public AlgorithmStep getCurrentStep() {
        if (!hasActiveSession()) {
            return null;
        }
        return currentResult.getSteps().get(currentStepIndex);
    }

    public List<String> getCurrentPseudocodeLines() {
        if (!hasActiveSession()) {
            return Collections.emptyList();
        }
        return currentResult.getPseudocodeLines();
    }

    public int getCurrentPseudocodeLine() {
        AlgorithmStep step = getCurrentStep();
        return step == null ? -1 : step.getActivePseudocodeLine();
    }

    public String getCurrentAlgorithmName() {
        return hasActiveSession() ? currentResult.getAlgorithmName() : "";
    }

    public String getInfoMessage() {
        return infoMessage;
    }

    public String getProgressText() {
        if (!hasActiveSession()) {
            return "No active timeline";
        }
        return currentResult.getAlgorithmName() + " | Step " + (currentStepIndex + 1) + "/" + currentResult.getSteps().size();
    }

    public boolean isInfiniteLoopLikely() {
        return hasActiveSession() && currentResult.isInfiniteLoopLikely();
    }

    public String getExecutionNotice() {
        if (!hasActiveSession()) {
            return "";
        }
        return currentResult.getExecutionNotice();
    }

    private String buildInfoMessage() {
        AlgorithmStep step = getCurrentStep();
        if (step == null) {
            return "No algorithm running.";
        }
        return step.getTitle() + System.lineSeparator() + step.getMessage();
    }

    private void notifyChanged() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    public int getCurrentStepIndex() {
        return hasActiveSession() ? currentStepIndex : -1;
    }

    public List<AlgorithmStep> getCurrentSteps() {
        if (!hasActiveSession()) {
            return Collections.emptyList();
        }
        return currentResult.getSteps();
    }

    /** @return true when the current step/line has a meaningful subroutine available for expansion. */
    public boolean canOpenSubroutineAtCurrentStep() {
        return resolveCallableSubroutineId() != null && !hasActiveSubroutineOverlay();
    }

    /** @return true when this run exposes any callable sub-routine line in the main pseudocode. */
    public boolean canOpenAnySubroutine() {
        return hasActiveSession()
                && !hasActiveSubroutineOverlay()
                && !currentResult.getCallableSubroutineByLine().isEmpty();
    }

    /** @return true while a temporary subroutine-expanded timeline overlay is active. */
    public boolean hasActiveSubroutineOverlay() {
        return activeSubroutineTimeline != null;
    }

    /** @return currently expanded subroutine metadata, or null when no subroutine overlay is active. */
    public SubroutineTimeline getActiveSubroutineTimeline() {
        return activeSubroutineTimeline;
    }

    /**
     * Opens a single subroutine overlay from the current call site.
     *
     * The overlay is inserted immediately after the current step and can be opened only once
     * per playback session at a time. Closing restores the exact step index before expansion.
     */
    public boolean openSubroutineAtCurrentStep() {
        if (!hasActiveSession() || hasActiveSubroutineOverlay()) {
            return false;
        }
        return openSubroutineById(resolveCallableSubroutineId());
    }

    /**
     * Opens a subroutine overlay targeted by a main pseudocode line index.
     *
     * The overlay is still inserted after the current playback step; this only changes
     * which reusable sub-routine timeline is selected.
     */
    public boolean openSubroutineForLine(int pseudocodeLineIndex) {
        if (!hasActiveSession() || hasActiveSubroutineOverlay() || pseudocodeLineIndex < 0) {
            return false;
        }
        int currentLine = getCurrentPseudocodeLine();
        if (currentLine < 0 || pseudocodeLineIndex != currentLine) {
            return false;
        }

        AlgorithmStep step = getCurrentStep();
        String id = step == null ? null : step.getCallableSubroutineId();
        return openSubroutineById(id);
    }

    /**
     * Closes the active subroutine overlay and removes all injected subroutine steps.
     *
     * The pointer returns to the caller step that was active before the subroutine was opened,
     * so the main pseudocode view can resume from the exact call site.
     */
    public boolean closeActiveSubroutineOverlay() {
        if (!hasActiveSubroutineOverlay() || baseResult == null) {
            return false;
        }
        this.currentResult = baseResult;
        int max = Math.max(0, baseResult.getSteps().size() - 1);
        this.currentStepIndex = Math.max(0, Math.min(preSubroutineStepIndex, max));
        this.activeSubroutineId = null;
        this.activeSubroutineTimeline = null;
        this.preSubroutineStepIndex = -1;
        this.activeSubroutineStartIndex = -1;
        this.activeSubroutineEndIndex = -1;
        this.infoMessage = buildInfoMessage();
        notifyChanged();
        return true;
    }

    /** @return id of the currently opened subroutine overlay, or null when none is active. */
    public String getActiveSubroutineId() {
        return activeSubroutineId;
    }

    /** @return true when the current pointer is on a step injected by the active subroutine overlay. */
    public boolean isCurrentStepInsideActiveSubroutineOverlay() {
        return hasActiveSubroutineOverlay()
                && currentStepIndex >= activeSubroutineStartIndex
                && currentStepIndex <= activeSubroutineEndIndex;
    }

    /**
     * @return zero-based pseudocode line for the active subroutine, or -1 when not inside subroutine steps.
     */
    public int getCurrentSubroutinePseudocodeLine() {
        return isCurrentStepInsideActiveSubroutineOverlay() ? getCurrentPseudocodeLine() : -1;
    }

    private boolean openSubroutineById(String id) {
        if (id == null || baseResult == null) {
            return false;
        }

        SubroutineTimeline subroutine = baseResult.getSubroutineTimelines().get(id);
        if (subroutine == null || subroutine.getSteps().isEmpty()) {
            return false;
        }

        List<AlgorithmStep> baseSteps = baseResult.getSteps();
        int insertionIndex = currentStepIndex + 1;
        List<AlgorithmStep> expanded = new ArrayList<>(baseSteps.size() + subroutine.getSteps().size());
        expanded.addAll(baseSteps.subList(0, insertionIndex));
        expanded.addAll(subroutine.getSteps());
        expanded.addAll(baseSteps.subList(insertionIndex, baseSteps.size()));

        this.preSubroutineStepIndex = currentStepIndex;
        this.currentResult = baseResult.withSteps(expanded);
        this.activeSubroutineId = id;
        this.activeSubroutineTimeline = subroutine;
        this.activeSubroutineStartIndex = insertionIndex;
        this.activeSubroutineEndIndex = insertionIndex + subroutine.getSteps().size() - 1;
        this.currentStepIndex = activeSubroutineStartIndex;
        this.infoMessage = buildInfoMessage();
        notifyChanged();
        return true;
    }

    private String resolveCallableSubroutineId() {
        if (!hasActiveSession()) {
            return null;
        }
        AlgorithmStep step = getCurrentStep();
        return step == null ? null : step.getCallableSubroutineId();
    }
}
