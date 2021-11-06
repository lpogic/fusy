package fusy;

import suite.processor.IntProcessor;
import suite.suite.Subject;

import java.util.Stack;

public abstract class FusProcessor implements IntProcessor {

    public abstract void terminateSubProcess();
    public abstract FusDebugger getDebugger();
}
