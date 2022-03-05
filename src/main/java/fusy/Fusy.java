package fusy;

import fusy.compile.DebuggerException;
import fusy.compile.FusyThread;
import fusy.setup.ChildProcess;
import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.util.Cascade;
import suite.suite.util.Sequence;

import java.io.*;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Consumer;
import sun.misc.Signal;

public interface Fusy {

    String home = System.getProperty("java.home"); //@Deploy
//    String home = "C:\\Users\\1\\Desktop\\PRO\\PRO_Java\\fusy\\jre"; //@Test
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
            Signal.handle(new Signal("INT"), sig -> {});
            Scanner scanner = new Scanner(System.in);
            String str = "";
            while(true) {
                System.out.print("fusy> ");
                try {
                    str = scanner.nextLine();
                } catch (Exception e) {
                    return;
                }
                switch (str.trim()) {
                    case "" -> {
                    }
                    case "help" -> System.out.println("""
                            help - dostepne opcje
                            exit - wyjscie
                            last - wyświetla ostatni skompilowany program w trybie edycji
                            Inne napisy interpretowane sa jako sciezka do pliku ze skryptem i uruchamiane
                            """);
                    case "exit" -> System.exit(0);
                    case "last" -> local.showLastCompiledSource();
                    case "clean" -> local.cleanConsole();
                    default -> {
                        try {
                            local.runFus(local.parseProgramCall(str));
                        } catch (DebuggerException de) {
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

    static Subject parseArgs(String[] args) {
        var c = new Cascade<>(Sequence.ofEntire(args));
        var r = Suite.set();
        var i = 0;
        var ap = "";
        for(var a : c) {
            if(a.startsWith("-")) {
                if(ap.startsWith("-")) {
                    r.set(ap.substring(1));
                }
                if(c.hasNext()) {
                    ap = a;
                } else {
                    r.set(a.substring(1));
                }
            } else {
                if(ap.startsWith("-")) {
                    r.put(ap.substring(1), a);
                } else {
                    r.put(i++, a);
                }
                ap = a;
            }
        }
        return r;
    }

    String[] parseProgramCall(String fus);
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

    ChildProcess runFusApart(String[] args) throws IOException, InterruptedException;

    default ChildProcess runFusApart(String path) throws IOException, InterruptedException {
        return runFusApart(new String[]{path});
    }
    default ChildProcess runFusApart(String path, String ... args) throws IOException, InterruptedException {
        var a = new String[args.length + 1];
        a[0] = path;
        System.arraycopy(args, 0, a, 1, args.length);
        return runFusApart(a);
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

    String stdinCharset();

    void showLastCompiledSource();
    FusyThread chooseFile(Consumer<String> fileConsumer);
    String chooseFile() throws IOException, InterruptedException;
    ChildProcess cmdApart(String cmd) throws IOException;
    void cmd(String cmd) throws IOException, InterruptedException;
    void cleanConsole();
}
