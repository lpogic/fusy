package fusy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
                    case "help" -> {
                        System.out.println("""
                                help - dostepne opcje
                                exit - wyjscie
                                Inne napisy interpretowane sa jako sciezka do pliku ze skryptem i uruchamiane
                                """);
                    }
                    case "exit" -> {
                        return;
                    }
                    default -> {
                        try {
                            str = str.replaceAll("\"", "");
                            run(str);
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
        var proc = new FusBodyProcessor(null);
        proc.getReady();
        Files.readAllLines(Path.of(fus)).forEach(str -> {
            str.codePoints().forEach(proc::advance);
            proc.advance('\n');
        });
        var $program = proc.finish();
        var program = """
                import java.nio.file.*;
                import static fusy.Fusy.*;
                import static fusy.FusyFun.*;
                import static java.lang.Thread.*;
                import suite.suite.$uite;
                import suite.suite.action.*;
                import suite.suite.Subject;
                import suite.suite.util.Series;
                import java.util.Objects;
                
                class fusy {
                    public static void main(String[] args) throws Exception {
                        new fusy();
                    }
                    
                    fusy() { //// STATEMENTS ////
                    """ + $program.in(FusBodyProcessor.Result.STATEMENTS).asString()
                + "}\n//// DEFINITIONS ////\n" +
                $program.in(FusBodyProcessor.Result.DEFINITIONS).asString() + """
                }
                """;
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
}
