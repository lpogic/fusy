package fusy;

import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.util.Browser;
import suite.suite.util.Sequence;
import suite.suite.util.Series;

import java.text.ParseException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Scanner;

import static suite.suite.$uite.$;

public class FusEnvironment {
    static Scanner scanner = new Scanner(System.in);

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

    public static void print(Object o) {
        System.out.print(o);
    }

    public static void pause() {
        readln("Kliknij ENTER aby kontynuowac");
    }

    public static Subject mix(Series s) {
        var list = Sequence.ofEntire(s).toList();
        Collections.shuffle(list);
        return Suite.alter(list);
    }

    public static Series range(int from, int to) {
        return from < to ? () -> new Browser() {
            int f = from;
            public boolean hasNext() {
                return f <= to;
            }

            public Subject next() {
                return $(f++);
            }
        } : () -> new Browser() {
            int f = from;
            public boolean hasNext() {
                return f >= to;
            }

            public Subject next() {
                return $(f--);
            }
        };
    }

    public static Series rail(Iterable<?> k, Series v) {
        return () -> new Browser() {
            final Iterator<?> ki = k.iterator();
            final Browser vb = v.iterator();

            @Override
            public boolean hasNext() {
                return ki.hasNext() && vb.hasNext();
            }

            @Override
            public Subject next() {
                return Suite.inset(ki.next(), vb.next());
            }
        };
    }
}
