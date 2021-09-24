package fusy;

import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.util.Cascade;
import suite.suite.util.Sequence;
import suite.suite.util.Series;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;


public class Fusy {
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {

    }

    public static<T> T idle(T t) {
        return t;
    }

    public static void hold(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

    public static long time() {
        return System.currentTimeMillis();
    }

    public static InputStreamReader inputFile(String path) {
        try {
            return new InputStreamReader(new FileInputStream(path));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStreamReader inputUrl(String path) {
        try {
            var url = new URL(path);
            return new InputStreamReader(url.openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static OutputStreamWriter outputFile(String path) {
        try {
            return new OutputStreamWriter(new FileOutputStream(path));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String rln() {
        return scanner.nextLine();
    }

    public static String rln(Object prompt) {
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

    public static void pln(Object o) {
        System.out.println(o);
    }

    public static void pln() {
        System.out.println();
    }

    public static void pln(Object ... on) {
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
        rln("Kliknij ENTER aby kontynuowac");
    }

    public static Subject mix(Series s) {
        var list = Sequence.ofEntire(s).toList();
        Collections.shuffle(list);
        return Suite.alter(list);
    }

    public static Sequence<Series> words(Subject sub, int size, boolean repetition) {
        if(!repetition) return words(sub, size);
        var m = new Series[size];
        for(int i = 0;i < size; ++i) m[i] = sub.front();
        return manifold(m);
    }

    public static Sequence<Series> words(Subject sub) {
        return words(sub, sub.size());
    }

    public static Sequence<Series> words(Subject sub, int size) {
        if(size < 1) return Sequence.of(Suite.set());
        if(size > sub.size()) throw new IndexOutOfBoundsException();
        return () -> new Iterator<>(){
            int i = 0;
            final Subject indexed = sub.index(range(0, sub.size() - 1)).set();

            public boolean hasNext() {
                return i < factorial(sub.size());
            }

            public Subject next() {
                var subSize = sub.size();
                var ix = new int[subSize];
                for(int x = 0; x < ix.length; ++x) ix[x] = x;
                for(int d = 0; d < size; ++d) {
                    int sd = d + i / factorial(subSize - 1 - d) % (subSize - d);
                    if(sd > d) {
                        int t = ix[d]; ix[d] = ix[sd]; ix[sd] = t;
                    }
                }
                i += factorial(subSize - size);
                var result = Suite.set();
                for(int in = 0; in < size; ++in) {
                    result.alter(indexed.in(ix[in]));
                }
                return result;
            }
        };
    }

    public static Sequence<Series> manifold(Series ... s) {
        return () -> new Iterator<>(){
            final Subject export = Suite.set();
            final Subject sc = Suite.set();
            {
                for(int i = 0 ;i < s.length; ++i) {
                    var c = s[i].cascade();
                    sc.put(c, s[i]);
                    if(i == 0) export.set(s[i]);
                    else export.inset(s[i], c.next());
                }
            }

            public boolean hasNext() {
                for(var c : sc.eachAs(Cascade.class)) {
                    if(c.hasNext()) return true;
                }
                return false;
            }

            public Series next() {
                var scc = sc.cascade();
                for (var $c : scc) {
                    Cascade<Subject> c = $c.asExpected();
                    if (c.hasNext()) {
                        export.inset($c.in().raw(), c.next());
                        return export.eachIn();
                    } else {
                        var s1 = $c.in().as(Series.class);
                        var c1 = s1.cascade();
                        sc.swap(c, c1);
                        export.inset(s1, c1.next());
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

    public static Sequence<Integer> range(boolean fromInclusive, int from, int to, boolean toInclusive) {
        return fromInclusive ? toInclusive ? range(from, to) : range(from, to - 1) :
            toInclusive ? range(from + 1, to) : range(from + 1, to - 1);
    }

    public static Sequence<Long> range(long from, long to) {
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

    public static Sequence<Long> range(boolean fromInclusive, long from, long to, boolean toInclusive) {
        return fromInclusive ? toInclusive ? range(from, to) : range(from, to - 1L) :
            toInclusive ? range(from + 1L, to) : range(from + 1L, to - 1L);
    }

    public static Sequence<Integer> steps() {
        return () -> new Repeater<>() {
            int f = 0;

            public Integer next() {
                return f++;
            }
        };
    }

    public static Sequence<Integer> steps(int s0) {
        return steps(s0 , 1);
    }

    public static Sequence<Integer> steps(int s0, int step) {
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

    public static Sequence<Long> steps(long s0, long step) {
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

    public static Sequence<Float> steps(float s0, float step) {
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

    public static Sequence<Integer> letters(String str) {
        return Sequence.ofEntire(() -> str.codePoints().iterator());
    }

    public static Sequence<String> lines(Reader reader) {
        var bufferedReader = reader instanceof BufferedReader b ? b : new BufferedReader(reader);
        return () -> new Iterator<>() {
            String next = null;

            @Override
            public boolean hasNext() {
                try {
                    return (next = bufferedReader.readLine()) != null;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String next() {
                try {
                    return next != null ? next : bufferedReader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static Sequence<String> split(String splitted, String splitter) {
        return Sequence.ofEntire(List.of(splitted.split(splitter)));
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

    public static Series random(Subject s, boolean repetitions) {
        if(repetitions) return Series.pull(() -> random(s));
        var options = Suite.alter(s);
        return Series.pull(() -> {
            var o = random(options);
            options.unset(o.raw());
            return o;
        });
    }
}
