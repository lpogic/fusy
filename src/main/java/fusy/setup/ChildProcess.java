package fusy.setup;

public record ChildProcess(Process process, FusyInOut io) implements AutoCloseable {

    @Override
    public void close() {
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();
    }
}
