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

    public static void main(String[] args) throws IOException, InterruptedException {
//        args = new String[]{"skrypt.txt"};
        String fus;
        if(args.length < 1) {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Podaj fus: ");
            fus = scanner.nextLine();
        } else {
            fus = args[0];
        }
        var proc = new FusBodyProcessor(null);
        proc.getReady();
        Files.readAllLines(Path.of(fus)).forEach(str -> {
            str.codePoints().forEach(proc::advance);
            proc.advance('\n');
        });
        var $program = proc.finish();
        var program = """
                import java.nio.file.*;
                import static fusy.FusEnvironment.*;
                import static java.lang.Thread.*;
                import suite.suite.$uite;
                import suite.suite.action.*;
                
                class fusy {
                    public static void main(String[] args) throws Exception {
                        new fusy();
                    }
                    
                    fusy() {
                    """ + $program.in(FusBodyProcessor.Result.STATEMENTS).asString()
                        + "}"
                        + $program.in(FusBodyProcessor.Result.DEFINITIONS).asString() + """
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
