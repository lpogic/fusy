package fusy;

import suite.suite.Subject;

import static suite.suite.$uite.$;

public class ArrayProcessor extends FusProcessor {

    enum State {
        DIM_EXP, FUSY_TYPE, DIMENSION, TERMINATED, DISCARD
    }

    enum Result {
        COMPLETE
    }

    String componentType;
    Subject dimensions;
    FusProcessor parentProcessor;
    FusProcessor subProcessor;

    public ArrayProcessor(FusProcessor parentProcessor) {
        this.parentProcessor = parentProcessor;
    }

    @Override
    public void getReady() {
        componentType = "";
        dimensions = $();
        $state = $($(State.DIMENSION));
    }

    @Override
    public FusDebugger getDebugger() {
        return parentProcessor.getDebugger();
    }

    @Override
    public void advance(int i) {
        switch ($state.in().as(State.class)) {
            case DISCARD -> $state.unset($state.raw());
            case DIMENSION -> {
                if(i == '(') {
                    var bodyProcessor = new FusBodyProcessor(this);
                    bodyProcessor.getReady(FusBodyProcessor.State.EXPRESSION);
                    subProcessor = bodyProcessor;
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.DIM_EXP);
                } else if(!Character.isWhitespace(i)){
                    var fusyTypeProcessor = new FusyTypeProcessor(this);
                    fusyTypeProcessor.getReady();
                    fusyTypeProcessor.arrayTypeEnabled(false);
                    subProcessor = fusyTypeProcessor;
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.FUSY_TYPE);
                    advance(i);
                }
            }
            case DIM_EXP, FUSY_TYPE -> subProcessor.advance(i);
            case TERMINATED -> parentProcessor.advance(i);
        }
    }


    public void terminateSubProcess() {
        if($state.in().raw() == State.FUSY_TYPE) {
            var $ = subProcessor.finish();
            componentType = $.in(FusyTypeProcessor.Result.COMPLETE).asString();
            $state.aimedAdd($state.raw(), State.TERMINATED);
            parentProcessor.terminateSubProcess();
        }
        if($state.in().raw() == State.DIM_EXP) {
            var $ = subProcessor.finish();
            dimensions.add($.in(FusBodyProcessor.Result.STATEMENTS).asString());
            $state.unset($state.raw());
            $state.aimedAdd($state.raw(), State.DIMENSION);
            $state.aimedAdd($state.raw(), State.DISCARD);
        }
    }

    @Override
    public Subject finish() {
        var sb = new StringBuilder(componentType);
        for(var d : dimensions.eachIn().eachString()) {
            sb.append("[").append(d).append("]");
        }
        return $(Result.COMPLETE, $(sb.toString()));
    }
}
