package fusy.setup;

public record ChildProcess(Process process, FusyInOut io) implements AutoCloseable {

    public void close(boolean gently) {
        if(gently) close();
        else {
            process.descendants().forEach(ProcessHandle::destroy);
            process.destroy();
        }
    }

    @Override
    public void close() {
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
