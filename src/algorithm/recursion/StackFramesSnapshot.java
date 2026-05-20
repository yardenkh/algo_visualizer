package algorithm.recursion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable stack payload attached to algorithm steps. Frames are ordered top-first.
 */
public final class StackFramesSnapshot {
    private final String title;
    private final List<StackFrameView> frames;

    /** Creates one immutable stack snapshot for a timeline step. */
    public StackFramesSnapshot(String title, List<StackFrameView> frames) {
        this.title = title == null ? "" : title;
        this.frames = Collections.unmodifiableList(new ArrayList<>(frames));
    }

    /** @return stack title shown in the stack dialog. */
    public String getTitle() {
        return title;
    }

    /** @return frames ordered from top of stack to bottom. */
    public List<StackFrameView> getFrames() {
        return frames;
    }
}
