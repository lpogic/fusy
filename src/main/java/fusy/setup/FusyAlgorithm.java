package fusy.setup;

import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.util.Cascade;
import suite.suite.util.Sequence;
import suite.suite.util.Series;

import java.util.Iterator;

public class FusyAlgorithm implements Common {

    public Sequence<Series> words(Subject sub, int size, boolean repetition) {
        if(!repetition) return words(sub, size);
        var m = new Series[size];
        for(int i = 0;i < size; ++i) m[i] = sub.front();
        return manifold(m);
    }

    public Sequence<Series> words(Subject sub) {
        return words(sub, sub.size());
    }

    public Sequence<Series> words(Subject sub, int size) {
        if(size < 1) return Sequence.of(Suite.set());
        if(size > sub.size()) throw new IndexOutOfBoundsException();
        return () -> new Iterator<>(){
            int i = 0;
            final Subject indexed = sub.index(Common.range(0, sub.size() - 1)).set();

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

    public Sequence<Series> manifold(Series ... s) {
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

    public int factorial(int in) {
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
}
