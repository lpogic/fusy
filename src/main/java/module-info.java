module fusy {
    requires suite;
    requires brackettree;
    requires airbricks;
    requires bricks;
    requires jdk.compiler;
    requires jdk.unsupported;
    exports fusy;
    exports fusy.setup;
    exports fusy.compile;
}