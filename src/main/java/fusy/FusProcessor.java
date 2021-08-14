package fusy;

import suite.processor.IntProcessor;

public interface FusProcessor extends IntProcessor {
    void terminateSubProcess();
    default void getReady(Object initState){
        getReady();
    }
}
