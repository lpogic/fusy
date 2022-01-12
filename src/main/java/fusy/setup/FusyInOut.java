package fusy.setup;

import java.io.PrintStream;
import java.util.Scanner;

public record FusyInOut (Scanner scanner, PrintStream printer) {

    public String readln() {
        return scanner.nextLine();
    }

    public String readln(String prompt) {
        printer.print(prompt);
        return scanner.nextLine();
    }

    public void print(String str) {
        printer.print(str);
    }

    public void println(String str) {
        printer.println(str);
    }

    public void println() {
        printer.println();
    }
}
