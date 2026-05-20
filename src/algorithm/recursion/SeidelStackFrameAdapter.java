package algorithm.recursion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Seidel-specific adapter that exposes conceptual recursion depth frames.
 */
public final class SeidelStackFrameAdapter implements RecursiveStackAdapter<List<SeidelStackFrameAdapter.SeidelFrameState>> {
    /** Converts Seidel recursion state into generic stack frames for the UI. */
    @Override
    public StackFramesSnapshot toSnapshot(List<SeidelFrameState> state, String title) {
        List<StackFrameView> frames = new ArrayList<>();
        for (int i = state.size() - 1; i >= 0; i--) {
            SeidelFrameState frame = state.get(i);
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("depth", String.valueOf(frame.depth));
            vars.put("n", String.valueOf(frame.problemSize));
            vars.put("phase", frame.phase);
            frames.add(new StackFrameView("SEIDEL-APSP", vars));
        }
        return new StackFramesSnapshot(title, frames);
    }

    /** Immutable Seidel recursion-frame state used during timeline generation. */
    public static final class SeidelFrameState {
        private final int depth;
        private final int problemSize;
        private final String phase;

        /** Captures one logical Seidel recursion frame. */
        public SeidelFrameState(int depth, int problemSize, String phase) {
            this.depth = depth;
            this.problemSize = problemSize;
            this.phase = phase == null ? "" : phase;
        }
    }
}
