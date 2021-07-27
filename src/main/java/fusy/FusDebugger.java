package fusy;

import suite.processor.IntProcessor;
import suite.suite.Subject;

import static suite.suite.$uite.$;

public class FusDebugger implements IntProcessor {

    enum State {
    }

    enum Result {
        COMPLETE
    }

    StringBuilder line;
    int lineCounter;

    @Override
    public void getReady() {
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
    }

    @Override
    public Subject finish() {
        String str = "AT LINE " + lineCounter + ": " + line.toString();
        return $(Result.COMPLETE, $(str));
    }
}
