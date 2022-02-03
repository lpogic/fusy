package fusy;

import fusy.compile.FusDebugger;
import fusy.compile.FusyThread;
import fusy.setup.ChildProcess;
import fusy.setup.FusyInOut;

import java.io.*;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

public class FusyUnix implements Fusy {


    @Override
    public String[] parseProgramCall(String fus) {
        return fus.split("(?<!\\\\) ");
    }

    @Override
    public void runFus(String[] args) throws IOException, InterruptedException {
        var fus = new File(args[0]);
        var debugger = new FusDebugger();
        var program = debugger.process(fus.getPath());
        var output = new FileOutputStream(home + "/fusy.java");
        var writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        writer.write(program.in(FusDebugger.Result.CODE).asString());
        writer.flush();
        output.close();
        var pb = new ProcessBuilder();
        var cmd = new ArrayList<String>();
        if("graphic".equals(program.in(FusDebugger.Result.SETUP).asString()) && System.console() == null) {
            cmd.add(home + "/bin/javaw");
        } else {
            cmd.add(home + "/bin/java");
        }
        cmd.add(home + "/fusy.java");
        cmd.addAll(List.of(args));
        Process process = pb.
                command(cmd).
                directory(fus.getParentFile()).
                inheritIO().
                start();
        process.waitFor();
    }

    @Override
    public ChildProcess runFusApart(String[] args) throws IOException {
        var fus = new File(args[0]);
        var debugger = new FusDebugger();
        var program = debugger.process(fus.getPath());
        var output = new FileOutputStream(home + "/fusy.java");
        var writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        writer.write(program.in(FusDebugger.Result.CODE).asString());
        writer.flush();
        output.close();
        var pb = new ProcessBuilder();
        var cmd = new ArrayList<String>();
        if("graphic".equals(program.in(FusDebugger.Result.SETUP).asString()) && System.console() == null) {
            cmd.add(home + "/bin/javaw");
        } else {
            cmd.add(home + "/bin/java");
        }
        cmd.add(home + "/fusy.java");
        cmd.addAll(List.of(args));

        Process process = pb.
                command(cmd).
                directory(fus.getParentFile()).
                start();

        var out = new PrintStream(process.getOutputStream(), true);
        return new ChildProcess(process, new FusyInOut(new Scanner(process.getInputStream()), out, out));
    }

    @Override
    public String stdinCharset() {
        return "UTF8";
    }

    @Override
    public void showLastCompiledSource() throws IOException, InterruptedException {
        var pb = new ProcessBuilder();
        var cmd = new ArrayList<String>();
        cmd.add("less");
        cmd.add(home + "/fusy.java");
        Process process = pb.
                command(cmd).
                directory(new File(".")).
                inheritIO().
                start();
        process.waitFor();
    }

    public FusyThread chooseFile(Consumer<String> fileConsumer) {
        return run(() -> {
            try {
                var process = shell(Path.of(home, "rsc", "sh", "chooseFile.sh").toString());
                var br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                if ("OK".equals(br.readLine())) {
                    fileConsumer.accept(br.readLine());
                }
                process.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public String chooseFile() throws IOException, InterruptedException {
        var process = shell(Path.of(home, "rsc", "sh", "chooseFile.sh").toString());
        var br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        process.waitFor();
        if("OK".equals(br.readLine())) {
            return br.readLine();
        } else {
            return "";
        }
    }

    private Process shell(String file) throws IOException {
        throw new RuntimeException("TODO Shell scripts");
    }

    @Override
    public ChildProcess cmdApart(String cmd) throws IOException {
        var pb = new ProcessBuilder();
        var c = List.of(cmd.split(" +"));

        Process process = pb.
                command(c).
                redirectErrorStream(true).
                start();

        var out = new PrintStream(process.getOutputStream(), true);
        return new ChildProcess(process, new FusyInOut(new Scanner(process.getInputStream()), out, out));
    }

    @Override
    public void cmd(String cmd) throws IOException, InterruptedException {
        var pb = new ProcessBuilder();
        var c = List.of(cmd.split(" +"));

        Process process = pb.
                command(c).
                inheritIO().
                start();
        process.waitFor();
    }

    @Override
    public void cleanConsole() throws IOException, InterruptedException {
        cmd("clear");
    }
}
