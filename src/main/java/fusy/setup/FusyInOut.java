package fusy.setup;

import java.io.PrintStream;
import java.util.Scanner;

public record FusyInOut (Scanner scanner, PrintStream printer, PrintStream err) {

    public String readln() {
        return scanner != null ? scanner.nextLine() : "";
    }

    public String readln(String prompt) {
        printer.print(prompt);
        return readln();
    }

    public void print(Object o) {
        printer.print(o);
    }

    public void println(Object o) {
        printer.println(o);
    }

    public void println() {
        printer.println();
    }

    public void prinf(String format, Object ... objects) {
        printer.printf(format, objects);
    }

    public void prinfln(String format, Object ... objects) {
        printer.printf(format + "%n", objects);
    }

    public void hitException(Exception e) {
        err.println(e.getMessage());
    }
}
