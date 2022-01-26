package fusy.setup;

import java.util.*;


public class Console extends Daemon {
    public static final FusyInOut io = new FusyInOut(new Scanner(System.in), System.out, System.err);


//    public Console() {
//        this(new FusyInOut(new Scanner(System.in), System.out), Fusy.local, new FusyFiles(), new FusyAlgorithm());
//    }

//    public Console(FusyInOut fusyInOut, Fusy os, FusyFiles fusyFiles, FusyAlgorithm algorithms) {
//        this.io = fusyInOut;
//        this.os = os;
//        this.files = fusyFiles;
//        this.alg = algorithms;
//    }

    public static void pause() {
        io.println("Kliknij ENTER aby kontynuowac");
        io.readln();
    }
}
