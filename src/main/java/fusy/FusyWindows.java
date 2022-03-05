package fusy;

import fusy.compile.FusDebugger;
import fusy.compile.FusyThread;
import fusy.setup.ChildProcess;
import fusy.setup.FusyInOut;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class FusyWindows implements Fusy {

    public String[] parseProgramCall(String args) {
        var m = Pattern.compile("(?:\"(.*?)\"|(\\S+))\\s*").matcher(args);
        var a = new ArrayList<String>();
        while(m.find()) {
            var g = m.group(1);
            if(g == null) g = m.group(2);
            a.add(g);
        }
        return a.toArray(new String[0]);
    }

    @Override
    public void runFus(String[] args) throws IOException, InterruptedException {
        var fus = new File(args[0]);
        var debugger = new FusDebugger();
        var program = debugger.process(fus.getPath());
        var output = new FileOutputStream(home + "\\fusy.java");
        var writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        writer.write(program.in(FusDebugger.Result.CODE).asString());
        writer.flush();
        output.close();
        var pb = new ProcessBuilder();
        var cmd = new ArrayList<String>();
        var setup = program.in(FusDebugger.Result.SETUP).asString();
        cmd.add("cmd");
        cmd.add("/c");
        if (("graphic".equals(setup) || "robot".equals(setup)) && System.console() == null) {
            cmd.add(home + "\\bin\\javaw.exe");
        } else {
            cmd.add(home + "\\bin\\java.exe");
        }
        var cp = debugger.getClasspath();
        if (!cp.isEmpty()) {
            cmd.add("-classpath");
            cmd.add(cp);
        }
        if (debugger.assertionsEnabled()) {
            cmd.add("-enableassertions");
        }
        cmd.add(home + "\\fusy.java");
        cmd.addAll(List.of(args));
        pb.command(cmd).directory(fus.getParentFile()).inheritIO().start().waitFor();
    }

    @Override
    public ChildProcess runFusApart(String[] args) throws IOException {
        var fus = new File(args[0]);
        var debugger = new FusDebugger();
        var program = debugger.process(fus.getPath());
        var output = new FileOutputStream(home + "\\fusy.java");
        var writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        writer.write(program.in(FusDebugger.Result.CODE).asString());
        writer.flush();
        output.close();
        var pb = new ProcessBuilder();
        var cmd = new ArrayList<String>();
        var setup = program.in(FusDebugger.Result.SETUP).asString();
        cmd.add("cmd");
        cmd.add("/c");
        if(("graphic".equals(setup) || "robot".equals(setup)) && System.console() == null) {
            cmd.add(home + "\\bin\\javaw.exe");
        } else {
            cmd.add(home + "\\bin\\java.exe");
        }
        var cp = debugger.getClasspath();
        if(!cp.isEmpty()) {
            cmd.add("-classpath");
            cmd.add(cp);
        }
        if(debugger.assertionsEnabled()) {
            cmd.add("-enableassertions");
        }
        cmd.add(home + "\\fusy.java");
        cmd.addAll(List.of(args));

        Process process = pb.
                command(cmd).
                directory(fus.getParentFile()).
                redirectErrorStream(true).
                start();

        var out = new PrintStream(process.getOutputStream(), true);
        return new ChildProcess(process, new FusyInOut(new Scanner(process.getInputStream()), out, out));
    }

    @Override
    public String stdinCharset() {
        return "Cp852";
    }

    @Override
    public void showLastCompiledSource() {
        var pb = new ProcessBuilder();
        var cmd = new ArrayList<String>();
        cmd.add("cmd");
        cmd.add("/c");
        cmd.add("powershell");
        cmd.add("-command");
        cmd.add("\"start -verb edit '" + home + "\\fusy.java'\"");
        try {
            pb.command(cmd).directory(new File(".")).inheritIO().start().waitFor();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public FusyThread chooseFile(Consumer<String> fileConsumer) {
        return run(() -> {
            try {
                var process = powerShell(Path.of(home, "rsc", "ps1", "chooseFile.ps1").toString());
                var br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                if("OK".equals(br.readLine())) {
                    fileConsumer.accept(br.readLine());
                }
                process.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public String chooseFile() throws IOException, InterruptedException {
        var process = powerShell(Path.of(home, "rsc", "ps1", "chooseFile.ps1").toString());
        var br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        process.waitFor();
        if("OK".equals(br.readLine())) {
            return br.readLine();
        } else {
            return "";
        }
    }

    public void showNotification() throws IOException, InterruptedException {
        var process = powerShell(Path.of(home, "rsc", "ps1", "showNotification.ps1").toString());
        process.waitFor();
    }

    private Process powerShell(String file) throws IOException {
        var pb = new ProcessBuilder();
        var cmd = new ArrayList<String>();
        cmd.add("cmd");
        cmd.add("/c");
        cmd.add("powershell");
        cmd.add("-ExecutionPolicy");
        cmd.add("Bypass");
        cmd.add("-File");
        cmd.add(file);
        return pb.
                command(cmd).
                directory(new File(".")).
                start();
    }

    @Override
    public ChildProcess cmdApart(String cmd) throws IOException {
        var pb = new ProcessBuilder();
        var c = new ArrayList<String>();
        c.add("cmd");
        c.add("/c");
        c.addAll(List.of(cmd.split(" +")));

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
        var c = new ArrayList<String>();
        c.add("cmd");
        c.add("/c");
        c.addAll(List.of(cmd.split(" +")));

        Process process = pb.
                command(c).
                inheritIO().
                start();
        process.waitFor();
    }

    @Override
    public void cleanConsole() {
        try {
            cmd("cls");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
