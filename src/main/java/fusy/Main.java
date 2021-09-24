package fusy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;


public class Main {

    static String javaHome = System.getProperty("java.home");
    static boolean isWindows = System.getProperty("os.name").startsWith("Windows");

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
                            var m = Pattern.compile("([^\\s\"]+)?(?:\"([^\"]*?)\")?").matcher(str);
                            if(m.find()) {
                                var fus = new File(m.group().replaceAll("\"", ""));
                                var a = new ArrayList<String>();
                                while(m.find()) a.add(m.group().replaceAll("\"", ""));
                                run(fus, a.toArray(new String[0]));
                            }
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
                var a = new String[args.length - 1];
                System.arraycopy(args, 1, a, 0, args.length - 1);
                run(new File(args[0]), a);
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
    }

    static void run(File fus, String[] args) throws IOException, InterruptedException {
        var debugger = new FusDebugger();
        var program = debugger.process(fus.getPath()).in(FusDebugger.Result.COMPLETE).asString();
        var sep = isWindows ? "\\" : "/";
        var output = new FileOutputStream(javaHome + sep + "fusy.java");
        var writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        writer.write(program);
        writer.flush();
        output.close();
        var pb = new ProcessBuilder();
        var cmd = new ArrayList<String>();
        if(isWindows) {
            cmd.add("cmd");
            cmd.add("/c");
            cmd.add(javaHome + sep + "bin" + sep + "java.exe");
        } else {
            cmd.add(javaHome + sep + "bin" + sep + "java");
        }
        cmd.add(javaHome + sep + "fusy.java");
        cmd.addAll(List.of(args));
        Process process = pb.
                command(cmd).
                directory(fus.getParentFile()).
                inheritIO().
                start();
        process.waitFor();
    }

    static void last() throws IOException, InterruptedException {
        var sep = isWindows ? "\\" : "/";
        var pb = new ProcessBuilder();
        var cmd = new ArrayList<String>();
        if(isWindows) {
            cmd.add("cmd");
            cmd.add("/c");
            cmd.add("powershell");
            cmd.add("-command");
            cmd.add("\"start -verb edit '" + javaHome + sep + "fusy.java'\"");
        } else {
            cmd.add("less");
            cmd.add(javaHome + sep + "fusy.java");
        }
        Process process = pb.
                command(cmd).
                directory(new File(".")).
                inheritIO().
                start();
        process.waitFor();
    }
}
