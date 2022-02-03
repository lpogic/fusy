package fusy;

import fusy.compile.DebuggerException;
import fusy.compile.FusyThread;
import fusy.setup.ChildProcess;
import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.util.Cascade;
import suite.suite.util.Sequence;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Scanner;
import java.util.function.Consumer;
import java.net.http.HttpRequest;

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
//        try {
//            var hr = HttpRequest.newBuilder(new URI("https://allegro.pl/auth/oauth/token?grant_type=client_credentials"))
//                    .GET()
//                    .build();
//            var hc = HttpClient.newBuilder()
//                    .authenticator(new Authenticator() {
//                        @Override
//                        protected PasswordAuthentication getPasswordAuthentication() {
//                            return new PasswordAuthentication(
//                                    "b21e63ebbb53401db78a4d462bd37bf9",
//                                    "Z749VJVqXaJQWw4nQ1zm9eXWBcB3HtQH6iLiIaPawTF7ZPXkfr1FNbI2sqhoKQMF".toCharArray()
//                            );
//                        }
//                    })
//                    .build()
//                    .send(hr, HttpResponse.BodyHandlers.ofString());
//            var hr = HttpRequest.newBuilder(new URI("https://api.allegro.pl/sale/offers?name=pierniki"))
//                    .header("Authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzY29wZSI6WyJhbGxlZ3JvOmFwaTpzYWxlOm9mZmVyczpyZWFkIl0sImFsbGVncm9fYXBpIjp0cnVlLCJleHAiOjE2NDM3NTEyMTcsImp0aSI6ImIwNzU4Y2JlLWI5ZmMtNDRhZi1hODMxLTViOWExMmU0MGJmNSIsImNsaWVudF9pZCI6ImIyMWU2M2ViYmI1MzQwMWRiNzhhNGQ0NjJiZDM3YmY5In0.j-zB3aqVmZsN_7TR1ZCkl93C8Do36fN-gqliNV7oGkW2xnEdU7TuomwfIbLyGRjpiAK72Ao7ix5RcKZjYqh4oGEW5OZHC9VnfUwfvL6pR_hoekH5_9OsWr7zXErYbz9o-1PqRCdAf33acMDYrE4zcsaW1O1PtULveC66wfeTDRQybUtrD4Ni28XrRnx5_hJrEST1koswAgjPA69tlB0Mb-LzcSdy89Sr66nRqLWoz8p97AYQW9L52ChL9iL_as7qqr3o66e2oqTiZWymp87og7n0-WD8zOdiJWnSGhaqybZ7J11fLo5YSY4VM4swQcQ3z4xHoltZWp0H-djFE8wX1A")
//                    .header("Accept", "application/vnd.allegro.public.v1+json")
//                    .GET()
//                    .build();
//            var hc = HttpClient.newBuilder()
//                    .build()
//                    .send(hr, HttpResponse.BodyHandlers.ofString());
//            System.out.println(hc.body());
//            System.out.println(hc.statusCode());
//        } catch (URISyntaxException | IOException | InterruptedException e) {
//            e.printStackTrace();
//        }
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
                    case "clean" -> {
                        try {
                            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    default -> {
                        try {
                            local.runFus(local.parseProgramCall(str));
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

    void showLastCompiledSource() throws IOException, InterruptedException;
    FusyThread chooseFile(Consumer<String> fileConsumer);
    String chooseFile() throws IOException, InterruptedException;
    ChildProcess cmdApart(String cmd) throws IOException;
    void cmd(String cmd) throws IOException, InterruptedException;
    void cleanConsole() throws IOException, InterruptedException;
}
