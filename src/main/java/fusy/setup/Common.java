package fusy.setup;

import fusy.Fusy;
import fusy.Repeater;
import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.util.Sequence;
import suite.suite.util.Series;

import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;
import java.util.function.Supplier;

public interface Common {

    class IntegerRange implements Sequence<Integer> {
        int from;
        int to;

        public IntegerRange(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new Iterator<>() {
                int f = from;
                public boolean hasNext() {
                    return f <= to;
                }

                public Integer next() {
                    return f++;
                }
            };
        }

        public Sequence<Integer> countDown() {
            return () -> new Iterator<>() {
                int t = to;
                public boolean hasNext() {
                    return from <= t;
                }

                public Integer next() {
                    return t--;
                }
            };
        }
    }

    class LongRange implements Sequence<Long> {
        long from;
        long to;

        public LongRange(long from, long to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public Iterator<Long> iterator() {
            return new Iterator<>() {
                long f = from;
                public boolean hasNext() {
                    return f <= to;
                }

                public Long next() {
                    return f++;
                }
            };
        }

        public Sequence<Long> countDown() {
            return () -> new Iterator<>() {
                long t = to;
                public boolean hasNext() {
                    return from <= t;
                }

                public Long next() {
                    return t--;
                }
            };
        }
    }

    FusyInOut io = new FusyInOut(System.in != null ? new Scanner(System.in, Fusy.local.stdinCharset()) : null, System.out, System.err);
    Fusy os = Fusy.local;
    FusyFiles files = new FusyFiles();
    FusyAlgorithm alg = new FusyAlgorithm();

    static void hold(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

    static <T> T idle(T t) {
        return t;
    }

    static long time() {
        return System.currentTimeMillis();
    }

    static IntegerRange range(int from, int to) {
        return new IntegerRange(from, to);
    }

    static IntegerRange range(boolean fromInclusive, int from, int to, boolean toInclusive) {
        return fromInclusive ? toInclusive ? range(from, to) : range(from, to - 1) :
                toInclusive ? range(from + 1, to) : range(from + 1, to - 1);
    }

    static Sequence<Long> range(long from, long to) {
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

    static Sequence<Long> range(boolean fromInclusive, long from, long to, boolean toInclusive) {
        return fromInclusive ? toInclusive ? range(from, to) : range(from, to - 1L) :
                toInclusive ? range(from + 1L, to) : range(from + 1L, to - 1L);
    }

    static Sequence<Integer> steps() {
        return () -> new Repeater<>() {
            int f = 0;

            public Integer next() {
                return f++;
            }
        };
    }

    static Sequence<Integer> steps(int s0) {
        return steps(s0 , 1);
    }

    static Sequence<Integer> steps(int s0, int step) {
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

    static Sequence<Long> steps(long s0, long step) {
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

    static Sequence<Float> steps(float s0, float step) {
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

    static <T> Sequence<T> pull(Supplier<T> supplier) {
        return Sequence.pull(supplier);
    }

    static boolean random(double chance) {
        return Math.random() < chance;
    }

    static int random(int limit) {
        return (int)(Math.random() * (limit + 1));
    }

    static Subject random(Subject s) {
        return s.select((int)Math.floor(Math.random() * s.size()));
    }

    static Series random(Subject s, boolean repetitions) {
        if(repetitions) return Series.pull(() -> random(s));
        var options = Suite.alter(s);
        return Series.pull(() -> {
            var o = random(options);
            options.unset(o.raw());
            return o;
        });
    }

    static Subject set(Object ... obs) {
        var s = Suite.set();
        for(var o : obs) {
            s.fusySet(o);
        }
        return s;
    }

    static Subject list(Object ... obs) {
        var s = Suite.set();
        for(var o : obs) {
            s.fusyAdd(o);
        }
        return s;
    }

    static void export(Appendable appendable, Subject subject) {
        try {
            Suite.export(subject, appendable, true, o -> {
                if(o instanceof String s) return "\"" + s + "\"";
                if(o instanceof Integer i) return "" + i;
                if(o instanceof Long l) return "" + l + "L";
                if(o instanceof Double d) return "" + d;
                if(o instanceof Float f) return "" + f + "f";
                if(o instanceof Boolean b) return "" + b;
                if(o instanceof Character c) return "'" + c + "'";
                if(o instanceof Suite.Auto) return "";
                return "";
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void hitException(Exception e) {
        e.printStackTrace();
    }
}
