package fusy;

import suite.processor.IntProcessor;
import suite.suite.Subject;

public abstract class FusProcessor implements IntProcessor {
    Subject $state;

    public abstract void terminateSubProcess();
    public abstract FusDebugger getDebugger();
}
