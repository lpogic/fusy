package fusy.setup;

public class Console extends Daemon {

    public static void pause() {
        io.println("Kliknij ENTER aby kontynuowac");
        io.readln();
    }
}
