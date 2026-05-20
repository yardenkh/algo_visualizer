package algorithm.recursion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable representation of one frame in the call stack.
 *
 * Frames are generic UI data, independent of any specific algorithm implementation.
 */
public final class StackFrameView {
    private final String methodName;
    private final Map<String, String> variables;

    /** Creates one immutable display frame. */
    public StackFrameView(String methodName, Map<String, String> variables) {
        this.methodName = methodName == null ? "" : methodName;
        this.variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
    }

    /** @return display name of the method/algorithm frame. */
    public String getMethodName() {
        return methodName;
    }

    /** @return immutable variable map shown in the stack dialog. */
    public Map<String, String> getVariables() {
        return variables;
    }
}
