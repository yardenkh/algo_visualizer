package algorithm.recursion;

import graph.Vertex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DFS-specific adapter that exposes recursive DFS-VISIT frames.
 */
public final class DfsStackFrameAdapter implements RecursiveStackAdapter<List<DfsStackFrameAdapter.DfsFrameState>> {
    /** Converts DFS frame-state objects into generic stack-frame rows. */
    @Override
    public StackFramesSnapshot toSnapshot(List<DfsFrameState> state, String title) {
        List<StackFrameView> frames = new ArrayList<>();
        for (int i = state.size() - 1; i >= 0; i--) {
            DfsFrameState frame = state.get(i);
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("u", frame.vertexName);
            vars.put("depth", String.valueOf(frame.depth));
            vars.put("phase", frame.phase);
            vars.put("time", String.valueOf(frame.time));
            frames.add(new StackFrameView("DFS-VISIT", vars));
        }
        return new StackFramesSnapshot(title, frames);
    }

    /** Immutable DFS call-frame state captured during timeline generation. */
    public static final class DfsFrameState {
        private final String vertexName;
        private final int depth;
        private final String phase;
        private final int time;

        /** Captures one DFS-VISIT frame in algorithm-specific terms. */
        public DfsFrameState(Vertex vertex, int depth, String phase, int time) {
            this.vertexName = vertex == null ? "-" : vertex.getName();
            this.depth = depth;
            this.phase = phase == null ? "" : phase;
            this.time = time;
        }
    }
}
