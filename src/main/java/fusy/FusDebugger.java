package fusy;
import suite.suite.Subject;

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

    @Override
    public void getReady() {
        processor = new FusBodyProcessor(this);
        processor.getReady();
        line = new StringBuilder();
        lineCounter = 1;
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
            processor.advance(i);
        } catch (Exception e) {
            String str = "EXCEPTION AT LINE " + lineCounter + ": " + line.toString();
            throw new RuntimeException(str);
        }
    }

    @Override
    public void terminateSubProcess() {

    }

    @Override
    public Subject finish() {
        var $program = processor.finish();
        var program = """
                import java.nio.file.*;
                import java.io.File;
                import static fusy.Fusy.*;
                import static fusy.FusyFun.*;
                import static java.lang.Thread.*;
                import suite.suite.$uite;
                import suite.suite.Suite;
                import suite.suite.action.*;
                import suite.suite.Subject;
                import suite.suite.util.Series;
                import suite.suite.util.Sequence;
                import java.util.Objects;
                import java.util.Arrays;
                
                @SuppressWarnings("unchecked")
                class fusy {
                    public static void main(String[] args) throws Exception {
                        new fusy();
                    }
                    
                    fusy() { //// STATEMENTS ////
                    """ + $program.in(FusBodyProcessor.Result.STATEMENTS).asString()
                + "}\n//// DEFINITIONS ////\n" +
                $program.in(FusBodyProcessor.Result.DEFINITIONS).asString() + """
                }
                """;
        return $(Result.COMPLETE, $(program));
    }
}
