package fusy;

import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.util.Browser;
import suite.suite.util.Cascade;
import suite.suite.util.Sequence;
import suite.suite.util.Series;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;


public class Fusy {
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        for(var s : variations(Suite.set('a','b','c','d','e'), 3, true)) {
            s.print();
        }
    }

    public static String readln() {
        return scanner.nextLine();
    }

    public static String readln(Object prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    public static int readInt(Object prompt) {
        while(true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException ignored) {}
        }
    }

    public static int readInt(Object prompt, Object errorMsg) {
        while(true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException ignored) {
                System.err.println(errorMsg);
            }
        }
    }

    public static int readInt(Object prompt, int min) {
        while(true) {
            try {
                System.out.print(prompt);
                var r = Integer.parseInt(scanner.nextLine());
                if(r >= min) return r;
            } catch (NumberFormatException ignored) {}
        }
    }

    public static int readInt(Object prompt, int min, Object errorMsg) {
        while(true) {
            try {
                System.out.print(prompt);
                var r = Integer.parseInt(scanner.nextLine());
                if(r >= min) return r;
            } catch (NumberFormatException ignored) {
                System.err.println(errorMsg);
            }
        }
    }

    public static int readInt(Object prompt, int min, int max) {
        while(true) {
            try {
                System.out.print(prompt);
                var r = Integer.parseInt(scanner.nextLine());
                if(r >= min && r <= max) return r;
            } catch (NumberFormatException ignored) {}
        }
    }

    public static int readInt(Object prompt, int min, int max, Object errorMsg) {
        while(true) {
            try {
                System.out.print(prompt);
                var r = Integer.parseInt(scanner.nextLine());
                if(r >= min && r <= max) return r;
            } catch (NumberFormatException ignored) {
                System.err.println(errorMsg);
            }
        }
    }

    public static void println(Object o) {
        System.out.println(o);
    }

    public static void println() {
        System.out.println();
    }

    public static void println(Object ... on) {
        print(on);
        System.out.println();
    }

    public static void print(Object o) {
        System.out.print(o);
    }

    public static void print(Object ... on) {
        for(var o : on) {
            System.out.print(o);
            System.out.print('\t');
        }
    }

    public static void pause() {
        readln("Kliknij ENTER aby kontynuowac");
    }

    public static Subject mix(Series s) {
        var list = Sequence.ofEntire(s).toList();
        Collections.shuffle(list);
        return Suite.alter(list);
    }

    public static Series variations(Subject sub) {
        return variations(sub, sub.size());
    }

    public static Series variations(Subject sub, int setSize) {
        if(setSize < 1) return Series.of(Suite.set());
        if(setSize > sub.size()) throw new IndexOutOfBoundsException();
        return () -> new Browser(){
            int i = 0;
            final Subject indexed = sub.index(range(0, sub.size() - 1)).set();

            public boolean hasNext() {
                return i < factorial(sub.size());
            }

            public Subject next() {
                var subSize = sub.size();
                var ix = new int[subSize];
                for(int x = 0; x < ix.length; ++x) ix[x] = x;
                for(int d = 0; d < setSize; ++d) {
                    int sd = d + i / factorial(subSize - 1 - d) % (subSize - d);
                    if(sd > d) {
                        int t = ix[d]; ix[d] = ix[sd]; ix[sd] = t;
                    }
                }
                i += factorial(subSize - setSize);
                var result = Suite.set();
                for(int in = 0; in < setSize; ++in) {
                    result.alter(indexed.in(ix[in]));
                }
                return result;
            }
        };
    }

    public static Series variations(Subject sub, int setSize, boolean repetition) {
        if(!repetition) return variations(sub, setSize);
        return () -> new Browser(){
            final Subject export = Suite.set();
            final Subject sc = Suite.set();
            {
                for(int i = 0 ;i < setSize; ++i) {
                    var auto = new Suite.Auto();
                    var c = sub.cascade();
                    sc.put(c, auto);
                    if(i == 0) export.set(auto);
                    else export.inset(auto, c.next());
                }
            }

            public boolean hasNext() {
                for(var c : sc.eachAs(Cascade.class)) {
                    if(c.hasNext()) return true;
                }
                return false;
            }

            public Subject next() {
                var scc = sc.cascade();
                for (var $c : scc) {
                    Cascade<Subject> c = $c.asExpected();
                    if (c.hasNext()) {
                        export.inset($c.in().raw(), c.next());
                        return export;
                    } else {
                        var c1 = sub.cascade();
                        sc.swap(c, c1);
                        export.inset($c.in().raw(), c1.next());
                    }
                }
                throw new IllegalStateException();
            }
        };
    }

    public static int factorial(int in) {
        return switch (in) {
            case 0, 1 -> 1;
            case 2 -> 2;
            case 3 -> 6;
            case 4 -> 24;
            case 5 -> 120;
            case 6 -> 720;
            case 7 -> 5040;
            case 8 -> 40320;
            case 9 -> 362880;
            case 10 -> 3628800;
            case 11 -> 39916800;
            case 12 -> 479001600;
            default -> throw new ArithmeticException();
        };
    }

    public static Sequence<Integer> range(int from, int to) {
        return from < to ? () -> new Iterator<>() {
            int f = from;
            public boolean hasNext() {
                return f <= to;
            }

            public Integer next() {
                return f++;
            }
        } : () -> new Iterator<>() {
            int f = from;
            public boolean hasNext() {
                return f >= to;
            }

            public Integer next() {
                return f--;
            }
        };
    }

    public static void swap(Subject s, Object k1, Object k2) {
        var s1 = s.in(k1).get();
        var s2 = s.in(k2).get();
        if(s1.absent()) System.err.println("Subject in " + k1 + "is absent");
        if(s2.absent()) System.err.println("Subject in " + k2 + "is absent");
        s.inset(k1, s2);
        s.inset(k2, s1);
    }

    public static Series codePoints(String str) {
        return Sequence.ofEntire(() -> str.codePoints().iterator()).series();
    }

    public static<T> Sequence<T> until(Supplier<T> sup, T stop) {
        return () -> new Iterator<T>() {
            boolean hasNext = false;
            T next;

            @Override
            public boolean hasNext() {
                if(hasNext) return true;
                next = sup.get();
                return hasNext = !Objects.equals(next, stop);
            }

            @Override
            public T next() {
                hasNext = false;
                return next;
            }
        };
    }

    public static<T> T min(Iterable<T> it, BiFunction<T, T, Integer> comparator) {
        var c = new Cascade<>(it.iterator());
        if(!c.hasNext()) return null;
        var min = c.next();
        for(var i : c) {
            if(comparator.apply(min, i) > 0) {
                min = i;
            }
        }
        return min;
    }

    public static boolean random(double chance) {
        return Math.random() < chance;
    }

    public static Subject random(Subject s) {
        return s.select((int)Math.floor(Math.random() * s.size()));
    }
}
