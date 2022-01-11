package fusy;

import fusy.compile.FusDebugger;
import fusy.compile.FusyThread;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FusyUnix implements Fusy {

    @Override
    public void runFus(String[] args) throws IOException, InterruptedException {
        var fus = new File(args[0]);
        var debugger = new FusDebugger();
        var program = debugger.process(fus.getPath());
        var output = new FileOutputStream(javaHome + "/fusy.java");
        var writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        writer.write(program.in(FusDebugger.Result.CODE).asString());
        writer.flush();
        output.close();
        var pb = new ProcessBuilder();
        var cmd = new ArrayList<String>();
        if("graphic".equals(program.in(FusDebugger.Result.SETUP).asString()) && System.console() == null) {
            cmd.add(javaHome + "/bin/javaw");
        } else {
            cmd.add(javaHome + "/bin/java");
        }
        cmd.add(javaHome + "/fusy.java");
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
        cmd.add("less");
        cmd.add(javaHome + "/fusy.java");
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
                var process = shell(Path.of(javaHome, "rsc", "sh", "chooseFile.sh").toString());
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
        var process = shell(Path.of(javaHome, "rsc", "sh", "chooseFile.sh").toString());
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
}
