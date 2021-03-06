package fusy.compile;

public class FusyThread extends Thread implements AutoCloseable {
    long startTime;

    public FusyThread() {
    }

    public FusyThread(Runnable target) {
        super(target);
    }

    public FusyThread(ThreadGroup group, Runnable target) {
        super(group, target);
    }

    public FusyThread(String name) {
        super(name);
    }

    public FusyThread(ThreadGroup group, String name) {
        super(group, name);
    }

    public FusyThread(Runnable target, String name) {
        super(target, name);
    }

    public FusyThread(ThreadGroup group, Runnable target, String name) {
        super(group, target, name);
    }

    public FusyThread(ThreadGroup group, Runnable target, String name, long stackSize) {
        super(group, target, name, stackSize);
    }

    public FusyThread(ThreadGroup group, Runnable target, String name, long stackSize, boolean inheritThreadLocals) {
        super(group, target, name, stackSize, inheritThreadLocals);
    }

    @Override
    public synchronized void start() {
        startTime = System.currentTimeMillis();
        super.start();
    }

    public long startTime() {
        return startTime;
    }

    public long length() {
        return System.currentTimeMillis() - startTime;
    }

    @Override
    public void close() throws Exception {
        join();
    }
}
