package fusy;

import suite.suite.Subject;

import static suite.suite.$uite.$;

public class FusyFunProcessor extends FusProcessor {

    enum State {
        ARGUMENTS_TYPE, RETURN_TYPE, TYPE, TERMINATED
    }

    enum Result {
        COMPLETE
    }

    Subject $argumentTypes;
    String returnType;
    StringBuilder result;
    FusProcessor parentProcessor;
    FusProcessor subProcessor;

    public FusyFunProcessor(FusProcessor parentProcessor) {
        this.parentProcessor = parentProcessor;
    }

    @Override
    public void getReady() {
        result = new StringBuilder();
        returnType = "";
        $state = $($(State.ARGUMENTS_TYPE));
        $argumentTypes = $();
    }

    @Override
    public FusDebugger getDebugger() {
        return parentProcessor.getDebugger();
    }

    @Override
    public void advance(int i) {
        switch ($state.in().as(State.class)) {
            case ARGUMENTS_TYPE -> {
                if(i == '}') {
                    subProcessor = new FusyTypeProcessor(this);
                    subProcessor.getReady();
                    $state.aimedAdd($state.raw(), State.RETURN_TYPE);
                } else if(i == ',') {
                } else {
                    subProcessor = new FusyTypeProcessor(this);
                    subProcessor.getReady();
                    $state.aimedAdd($state.raw(), State.TYPE);
                    advance(i);
                }
            }
            case TYPE, RETURN_TYPE -> subProcessor.advance(i);
            case TERMINATED -> parentProcessor.advance(i);
        }
    }


    public void terminateSubProcess() {
        if($state.in().raw() == State.TYPE) {
            var $ = subProcessor.finish();
            String type = $.in(FusyTypeProcessor.Result.COMPLETE).asString();
            $argumentTypes.add(type);
            $state.unset($state.raw());
        }
        if($state.in().raw() == State.RETURN_TYPE) {
            var $ = subProcessor.finish();
            returnType = $.in(FusyTypeProcessor.Result.COMPLETE).asString();
            parentProcessor.terminateSubProcess();
            $state.aimedAdd($state.raw(), State.TERMINATED);
        }
    }

    @Override
    public Subject finish() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("FusyFun").append($argumentTypes.size()).append("<");
        for(var $ : $argumentTypes.eachIn()) {
            stringBuilder.append($.asString()).append(",");
        }
        stringBuilder.append(returnType).append(">");
        return $(Result.COMPLETE, $(stringBuilder.toString()));
    }
}
