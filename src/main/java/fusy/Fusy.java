package fusy;

import fusy.compile.DebuggerException;
import fusy.compile.FusyThread;

import java.io.*;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.regex.Pattern;


public interface Fusy {

    String javaHome = System.getProperty("java.home"); //@Deploy
//    String javaHome = "C:\\Users\\1\\Desktop\\PRO\\PRO_Java\\fusy\\jre"; //@Test
    Fusy local = getLocalFusy(System.getProperty("os.name"));

    static Fusy getLocalFusy(String osName) {
        if(osName.startsWith("Windows")) {
            return new FusyWindows();
        } else {
            return new FusyUnix();
        }
    }

    static void main(String[] args) {
//        args = new String[]{"C:\\Users\\1\\Desktop\\PRO\\PRO_Java\\fusy\\skrypt.txt"}; //@Test
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
                            local.showLastCompiledSource();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    default -> {
                        try {
                            var m = Pattern.compile("([^\\s\"]+)?(?:\"([^\"]*?)\")?").matcher(str);
                            if(m.find()) {
                                var fus = m.group().replaceAll("\"", "");
                                var a = new ArrayList<String>();
                                while(m.find()) a.add(m.group().replaceAll("\"", ""));
                                local.runFus(fus, a.toArray(new String[0]));
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
                local.runFus(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void runFus(String[] args) throws IOException, InterruptedException;
    default void runFus(String path) throws IOException, InterruptedException {
        runFus(new String[]{path});
    }
    default void runFus(String path, String ... args) throws IOException, InterruptedException {
        var a = new String[args.length + 1];
        a[0] = path;
        System.arraycopy(args, 0, a, 1, args.length);
        runFus(a);
    }

    default FusyThread run(Runnable callback) {
        var thread = new FusyThread(callback);
        thread.start();
        return thread;
    }

    default FusyThread run(long delay, Runnable callback) {
        var thread = new FusyThread(() -> {
            try {
                Thread.sleep(delay);
                callback.run();
            } catch (InterruptedException ignored) {}
        });
        thread.start();
        return thread;
    }

    void showLastCompiledSource() throws IOException, InterruptedException;
    FusyThread chooseFile(Consumer<String> fileConsumer);
    String chooseFile() throws IOException, InterruptedException;


}
