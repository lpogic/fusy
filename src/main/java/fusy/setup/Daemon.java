package fusy.setup;

import fusy.Fusy;
import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.util.Cascade;
import suite.suite.util.Sequence;
import suite.suite.util.Series;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.*;
import java.util.function.BiFunction;


public class Daemon implements Common {

    public static InputStreamReader inputUrl(String path) {
        try {
            var url = new URL(path);
            return new InputStreamReader(url.openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Subject mix(Series s) {
        var list = Sequence.ofEntire(s).toList();
        Collections.shuffle(list);
        return Suite.alter(list);
    }

    public static Subject sort(Series s, Comparator<Subject> cmp) {
        var list = Sequence.ofEntire(s).toList();
        list.sort(cmp);
        return Suite.alter(list);
    }

    public<T, TE extends T> Sequence<TE> sort(Sequence<TE> s, Comparator<T> cmp) {
        var list = s.toList();
        list.sort(cmp);
        return Sequence.ofEntire(list);
    }

    public<T extends Comparable<T>> Sequence<T> sort(Sequence<T> s) {
        var list = s.toList();
        Collections.sort(list);
        return Sequence.ofEntire(list);
    }

    public static Sequence<Integer> letters(String str) {
        return Sequence.ofEntire(() -> str.codePoints().iterator());
    }

    public static String string(int ... codePoints) {
        return new String(codePoints, 0, codePoints.length);
    }

    public static Cascade<String> lines(Reader reader) {
        var bufferedReader = reader instanceof BufferedReader b ? b : new BufferedReader(reader);
        return new Cascade<>(new Iterator<>() {
            String next = null;

            @Override
            public boolean hasNext() {
                if(next != null) return true;
                try {
                    return (next = bufferedReader.readLine()) != null;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String next() {
                try {
                    if(next != null) {
                        var n = next;
                        next = null;
                        return n;
                    } else return bufferedReader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static Sequence<String> split(String splitted, String splitter) {
        return Sequence.ofEntire(List.of(splitted.split(splitter)));
    }

    public<T> T min(Iterable<T> it, BiFunction<T, T, Integer> comparator) {
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
}
