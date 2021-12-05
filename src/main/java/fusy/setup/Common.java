package fusy.setup;

import fusy.FusyThread;
import fusy.Repeater;
import suite.suite.action.Statement;
import suite.suite.util.Sequence;

import java.util.Iterator;
import java.util.function.Supplier;

public interface Common {

    default <T> T idle(T t) {
        return t;
    }

    default long time() {
        return System.currentTimeMillis();
    }

    default Sequence<Integer> range(int from, int to) {
        return () -> new Iterator<>() {
            int f = from;
            public boolean hasNext() {
                return f <= to;
            }

            public Integer next() {
                return f++;
            }
        };
    }

    default Sequence<Integer> range(boolean fromInclusive, int from, int to, boolean toInclusive) {
        return fromInclusive ? toInclusive ? range(from, to) : range(from, to - 1) :
                toInclusive ? range(from + 1, to) : range(from + 1, to - 1);
    }

    default Sequence<Long> range(long from, long to) {
        return () -> new Iterator<>() {
            long f = from;
            public boolean hasNext() {
                return f <= to;
            }

            public Long next() {
                return f++;
            }
        };
    }

    default Sequence<Long> range(boolean fromInclusive, long from, long to, boolean toInclusive) {
        return fromInclusive ? toInclusive ? range(from, to) : range(from, to - 1L) :
                toInclusive ? range(from + 1L, to) : range(from + 1L, to - 1L);
    }

    default Sequence<Integer> steps() {
        return () -> new Repeater<>() {
            int f = 0;

            public Integer next() {
                return f++;
            }
        };
    }

    default Sequence<Integer> steps(int s0) {
        return steps(s0 , 1);
    }

    default Sequence<Integer> steps(int s0, int step) {
        return () -> new Repeater<>() {
            int last = s0;

            @Override
            public Integer next() {
                int l = last;
                last += step;
                return l;
            }
        };
    }

    default Sequence<Long> steps(long s0, long step) {
        return () -> new Repeater<>() {
            long last = s0;

            @Override
            public Long next() {
                long l = last;
                last += step;
                return l;
            }
        };
    }

    default Sequence<Float> steps(float s0, float step) {
        return () -> new Repeater<>() {
            float last = s0;

            @Override
            public Float next() {
                float l = last;
                last += step;
                return l;
            }
        };
    }

    default <T> Sequence<T> pull(Supplier<T> supplier) {
        return Sequence.pull(supplier);
    }

    default FusyThread timer(long delay, Statement callback) {
        var thread = new FusyThread(() -> {
            try {
                Thread.sleep(delay);
                callback.play();
            } catch (InterruptedException ignored) {}
        });
        thread.start();
        return thread;
    }
}
