package fusy.setup;

import java.io.PrintStream;
import java.util.Scanner;

public record FusyInOut (Scanner in, PrintStream out, PrintStream err) {

    public String readln() {
        return in != null ? in.nextLine() : "";
    }

    public String readln(String prompt) {
        out.print(prompt);
        return readln();
    }

    public void print(Object o) {
        out.print(o);
    }

    public void println(Object o) {
        out.println(o);
    }

    public void println() {
        out.println();
    }

    public void prinf(String format, Object ... objects) {
        out.printf(format, objects);
    }

    public void prinfln(String format, Object ... objects) {
        out.printf(format + "%n", objects);
    }

    public void hitException(Exception e) {
        err.println(e.getMessage());
    }
}
