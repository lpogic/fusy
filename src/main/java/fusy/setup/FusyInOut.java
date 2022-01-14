package fusy.setup;

import java.io.PrintStream;
import java.util.Scanner;

public record FusyInOut (Scanner scanner, PrintStream printer, PrintStream errPrinter) {

    public String readln() {
        return scanner.nextLine();
    }

    public String readln(String prompt) {
        printer.print(prompt);
        return scanner.nextLine();
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

    public void printf(String format, Object ... objects) {
        printer.printf(format, objects);
    }

    public void printfln(String format, Object ... objects) {
        printer.printf(format + "%n", objects);
    }

    public void hitException(Exception e) {
        errPrinter.println(e.getMessage());
    }
}
