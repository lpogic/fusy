package fusy.compile;

import suite.processor.IntProcessor;

public abstract class FusProcessor implements IntProcessor {

    public abstract void terminateSubProcess();
    public abstract FusDebugger getDebugger();
    public abstract String getCatchVar(String symbol);
}
