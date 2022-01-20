package fusy;

import fusy.compile.FusDebugger;
import fusy.compile.FusyThread;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FusyWindows implements Fusy {

    @Override
    public void runFus(String[] args) throws IOException, InterruptedException {
        var fus = new File(args[0]);
        var debugger = new FusDebugger();
        var program = debugger.process(fus.getPath());
        var output = new FileOutputStream(javaHome + "\\fusy.java");
        var writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        writer.write(program.in(FusDebugger.Result.CODE).asString());
        writer.flush();
        output.close();
        var pb = new ProcessBuilder();
        var cmd = new ArrayList<String>();
        var setup = program.in(FusDebugger.Result.SETUP).asString();
        cmd.add("cmd");
        cmd.add("/c");
        if("graphic".equals(setup) && System.console() == null) {
            cmd.add(javaHome + "\\bin\\javaw.exe");
        } else {
            cmd.add(javaHome + "\\bin\\java.exe");
        }
        var cp = debugger.getClasspath();
        if(!cp.isEmpty()) {
            cmd.add("-classpath");
            cmd.add(cp);
        }
        cmd.add(javaHome + "\\fusy.java");
        cmd.addAll(List.of(args));
        Process process = pb.
                command(cmd).
                directory(fus.getParentFile()).
                inheritIO().
                start();
        process.waitFor();
    }

    @Override
    public void showLastCompiledSource() throws IOException, InterruptedException {
        var pb = new ProcessBuilder();
        var cmd = new ArrayList<String>();
        cmd.add("cmd");
        cmd.add("/c");
        cmd.add("powershell");
        cmd.add("-command");
        cmd.add("\"start -verb edit '" + javaHome + "\\fusy.java'\"");
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
                var process = powerShell(Path.of(javaHome, "rsc", "ps1", "chooseFile.ps1").toString());
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
        var process = powerShell(Path.of(javaHome, "rsc", "ps1", "chooseFile.ps1").toString());
        var br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        process.waitFor();
        if("OK".equals(br.readLine())) {
            return br.readLine();
        } else {
            return "";
        }
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
}
