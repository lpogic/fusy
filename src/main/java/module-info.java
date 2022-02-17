module fusy {
    requires suite;
    requires brackettree;
    requires airbricks;
    requires bricks;
    requires jdk.compiler;
    exports fusy;
    exports fusy.setup;
    exports fusy.compile;
}