package fusy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;


public class Main {

    public static void main(String[] args) {
//        args = new String[]{"skrypt.txt"};
        if(args.length < 1) {
            Scanner scanner = new Scanner(System.in);
            while(true) {
                System.out.print("fusy> ");
                String str = scanner.nextLine();
                switch (str.trim()) {
                    case "" -> {}
                    case "help" -> System.out.println("""
                            help - dostepne opcje
                            exit - wyjscie
                            last - wyświetla ostatni skompilowany program w trybie edycji
                            Inne napisy interpretowane sa jako sciezka do pliku ze skryptem i uruchamiane
                            """);
                    case "exit" -> {
                        return;
                    }
                    case "last" -> {
                        try {
                            last();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    default -> {
                        try {
                            str = str.replaceAll("\"", "");
                            run(str);
                        }
                        catch (DebuggerException de) {
                            System.err.println(de.getMessage());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else {
            try {
                run(args[0]);
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
    }

    static void run(String fus) throws IOException, InterruptedException {
        var debugger = new FusDebugger();
        var program = debugger.process(fus).in(FusDebugger.Result.COMPLETE).asString();
        var output = new FileOutputStream("fusy.java");
        var writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        writer.write(program);
        writer.flush();
        output.close();
        var pb = new ProcessBuilder();
        Process process = pb.
                command("cmd",  "/c", "jre\\bin\\java.exe", "fusy.java").
                directory(new File(".")).
                inheritIO().
                start();
        process.waitFor();
    }

    static void last() throws IOException, InterruptedException {
        var pb = new ProcessBuilder();
        Process process = pb.
                command("cmd",  "/c", "powershell", "-command", "\"start -verb edit 'fusy.java'\"").
                directory(new File(".")).
                inheritIO().
                start();
        process.waitFor();
    }
}
