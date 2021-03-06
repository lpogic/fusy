package fusy.compile;

import suite.suite.Subject;

import java.util.Stack;

import static suite.suite.$uite.$;

public class FusArrayProcessor extends FusProcessor {

    enum State {
        DIM_EXP, AFTER_DIM_EXP, FUSY_TYPE, DIMENSION, TERMINATED, DISCARD
    }

    enum Result {
        COMPLETE
    }

    Stack<State> state;
    String componentType;
    Subject dimensions;
    FusProcessor parentProcessor;
    FusProcessor subProcessor;

    public FusArrayProcessor(FusProcessor parentProcessor) {
        this.parentProcessor = parentProcessor;
    }

    @Override
    public void getReady() {
        componentType = "";
        dimensions = $();
        state = new Stack<>();
        state.push(State.DIMENSION);
    }

    @Override
    public FusDebugger getDebugger() {
        return parentProcessor.getDebugger();
    }


    @Override
    public String getCatchVar(String symbol) {
        return parentProcessor.getCatchVar(symbol);
    }

    @Override
    public int advance(int i) {
        switch (state.peek()) {
            case DISCARD -> state.pop();
            case DIMENSION -> {
                if(i == '(') {
                    var bodyProcessor = new FusBodyProcessor(this);
                    bodyProcessor.getReady(FusBodyProcessor.State.LAMBDA_EXP);
                    subProcessor = bodyProcessor;
                    state.push(State.AFTER_DIM_EXP);
                    state.push(State.DIM_EXP);
                } else if(!Character.isWhitespace(i)){
                    var fusyTypeProcessor = new FusyTypeProcessor(this);
                    fusyTypeProcessor.getReady();
                    fusyTypeProcessor.arrayTypeEnabled(false);
                    subProcessor = fusyTypeProcessor;
                    state.pop();
                    state.push(State.FUSY_TYPE);
                    advance(i);
                }
            }
            case DIM_EXP, FUSY_TYPE -> subProcessor.advance(i);
            case AFTER_DIM_EXP -> {
              state.pop();
              if(i == ',') return advance('(');
            }
            case TERMINATED -> parentProcessor.advance(i);
        }
        return 0;
    }


    public void terminateSubProcess() {
        if(state.peek() == State.FUSY_TYPE) {
            var $ = subProcessor.finish();
            componentType = $.in(FusyTypeProcessor.Result.COMPLETE).asString();
            state.push(State.TERMINATED);
            parentProcessor.terminateSubProcess();
        }
        if(state.peek() == State.DIM_EXP) {
            var $ = subProcessor.finish();
            dimensions.add($.in(FusBodyProcessor.Result.STATEMENTS).asString());
            state.pop();
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
