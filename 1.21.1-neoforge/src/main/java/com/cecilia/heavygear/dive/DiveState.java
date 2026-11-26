package com.cecilia.heavygear.dive;

import java.util.ArrayDeque;
import java.util.Deque;

/** Per-thread stack holding the original fallDistance while dive distance is exposed. */
public final class DiveState {
    private static final ThreadLocal<Deque<Double>> SAVED = ThreadLocal.withInitial(ArrayDeque::new);

    private DiveState() {
    }

    public static void push(double savedFallDistance) {
        SAVED.get().push(savedFallDistance);
    }

    public static double pop() {
        Deque<Double> stack = SAVED.get();
        return stack.isEmpty() ? Double.NaN : stack.pop();
    }
}
