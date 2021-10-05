package fusy;
import suite.suite.Subject;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static suite.suite.$uite.$;

public class FusDebugger extends FusProcessor {

    enum State {
    }

    enum Result {
        COMPLETE
    }

    StringBuilder line;
    int lineCounter;
    FusBodyProcessor processor;
    Subject sources;

    @Override
    public void getReady() {
        processor = new FusBodyProcessor(this);
        processor.getReady();
        line = new StringBuilder();
        lineCounter = 1;
        sources = $();
    }

    public void pushSource(String source) {
        if (sources.present(source))
            throw new DebuggerException(source + " EXCEPTION AT LINE " + lineCounter + ": cyclic insert " + source);
        try {
            var file = new File(source);
            var fis = new FileInputStream(file);
            var reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            sources.aimedPut(sources.raw(), source, reader);
        } catch (FileNotFoundException e) {
            throw new DebuggerException(source + " EXCEPTION AT LINE " + lineCounter + ": insert " + source + " not found");
        }
    }

    @Override
    public FusDebugger getDebugger() {
        return this;
    }

    @Override
    public Subject process(String str) {
        getReady();
        pushSource(str);
        while(sources.present()) {
            try {
                int cp = sources.in().as(BufferedReader.class).read();
                if (cp == -1) {
                    sources.unset(sources.raw());
                    advance('\n');
                } else if(cp != '\r') {
                    advance(cp);
                }
            } catch (IOException ioe) {
                throw new DebuggerException(sources.asString() + " EXCEPTION AT LINE " + lineCounter + ": io error");
            }
        }
        return finish();
    }

    @Override
    public void advance(int i) {
        if(i == '\n') {
            line = new StringBuilder();
            ++lineCounter;
        } else {
            line.appendCodePoint(i);
        }
        try {
            advance(i, false);
        } catch (DebuggerException de) {
            throw de;
        } catch (Exception e) {
            throw new DebuggerException(sources.asString("") + " EXCEPTION AT LINE " + lineCounter + ": " + line.toString());
        }
    }

    public void advance(int i, boolean outputAppend) {
        if(outputAppend) { 
            advance(i);
        } else {
            processor.advance(i);
        }
    }

    @Override
    public void terminateSubProcess() {

    }

    @Override
    public Subject finish() {
        var $program = processor.finish();
        var setup = $program.in(FusBodyProcessor.Result.SETUP).asString("");
        var program = switch (setup) {
            case "graphic" -> """
                    import java.nio.file.*;
                    import java.io.File;
                    import static fusy.Fusy.*;
                    import static fusy.FusyFun.*;
                    import fusy.FusyDrop;
                    import suite.suite.$uite;
                    import suite.suite.Suite;
                    import suite.suite.action.*;
                    import suite.suite.Subject;
                    import suite.suite.util.Series;
                    import suite.suite.util.Sequence;
                    import java.util.Objects;
                    import java.util.Arrays;
                    import airbricks.Wall;
                    import fusy.graphic.Graphic;
                    """ + $program.in(FusBodyProcessor.Result.IMPORTS).asString() + """
                                    
                    @SuppressWarnings("unchecked")
                    class fusy extends Graphic {
                        public static void main(String[] args) throws Exception {
                            Wall.play($uite.$(
                                                Wall.class, $uite.$(new fusy()),
                                                "w", $uite.$(800),
                                                "h", $uite.$(800),
                                                "title", $uite.$("Fusy window")
                                        ));
                        }
                        
                        public void setup() {
                        """ + $program.in(FusBodyProcessor.Result.STATEMENTS).asString()
                    + "}\n" +
                    $program.in(FusBodyProcessor.Result.DEFINITIONS).asString() + """
                }
                """;
            case "console", "" -> """
                import java.nio.file.*;
                import java.io.File;
                import fusy.Fusy;
                import static fusy.FusyFun.*;
                import fusy.FusyDrop;
                import suite.suite.$uite;
                import suite.suite.Suite;
                import suite.suite.action.*;
                import suite.suite.Subject;
                import suite.suite.util.Series;
                import suite.suite.util.Sequence;
                import java.util.Objects;
                import java.util.Arrays;
                """ + $program.in(FusBodyProcessor.Result.IMPORTS).asString() + """
                
                @SuppressWarnings("unchecked")
                class fusy extends Fusy {
                    public static void main(String[] args) throws Exception {
                        new fusy(args);
                    }
                    
                    fusy(String[] args) throws Exception {
                    """ + $program.in(FusBodyProcessor.Result.STATEMENTS).asString()
                    + "}\n" +
                    $program.in(FusBodyProcessor.Result.DEFINITIONS).asString() + """
                }
                """;
            default -> throw new DebuggerException("Unsupported setup " + setup);
        };
        return $(Result.COMPLETE, $(program));
    }
}
