package fusy;

import suite.suite.Subject;

import java.util.Stack;

import static suite.suite.$uite.$;

public class FusyFunProcessor extends FusProcessor {

    enum State {
        ARGUMENTS_TYPE, RETURN_TYPE, TYPE, TERMINATED
    }

    enum Result {
        COMPLETE
    }

    Stack<State> state;
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
        state = new Stack<>();
        state.push(State.ARGUMENTS_TYPE);
        $argumentTypes = $();
    }

    @Override
    public FusDebugger getDebugger() {
        return parentProcessor.getDebugger();
    }

    @Override
    public int advance(int i) {
        switch (state.peek()) {
            case ARGUMENTS_TYPE -> {
                if(i == '}') {
                    subProcessor = new FusyTypeProcessor(this);
                    subProcessor.getReady();
                    state.push(State.RETURN_TYPE);
                } else if(i == ',') {
                } else {
                    subProcessor = new FusyTypeProcessor(this);
                    subProcessor.getReady();
                    state.push(State.TYPE);
                    advance(i);
                }
            }
            case TYPE, RETURN_TYPE -> subProcessor.advance(i);
            case TERMINATED -> parentProcessor.advance(i);
        }
        return 0;
    }


    public void terminateSubProcess() {
        if(state.peek() == State.TYPE) {
            var $ = subProcessor.finish();
            String type = $.in(FusyTypeProcessor.Result.COMPLETE).asString();
            $argumentTypes.add(type);
            state.pop();
        }
        if(state.peek() == State.RETURN_TYPE) {
            var $ = subProcessor.finish();
            returnType = $.in(FusyTypeProcessor.Result.COMPLETE).asString();
            parentProcessor.terminateSubProcess();
            state.push(State.TERMINATED);
        }
    }

    @Override
    public Subject finish() {
        StringBuilder stringBuilder = new StringBuilder();
        if("void".equals(returnType)) {
            if($argumentTypes.present()) {
                stringBuilder.append("FusyFun").append($argumentTypes.size()).append("V<");
                var c = $argumentTypes.eachIn().cascade();
                for (var $ : c) {
                    stringBuilder.append($.asString());
                    if(c.hasNext()) stringBuilder.append(",");
                }
                stringBuilder.append(">");
            } else {
                stringBuilder.append("FusyFun0V");
            }
        } else {
            stringBuilder.append("FusyFun").append($argumentTypes.size()).append("<");
            for (var $ : $argumentTypes.eachIn()) {
                stringBuilder.append($.asString()).append(",");
            }
            stringBuilder.append(returnType).append(">");
        }
        return $(Result.COMPLETE, $(stringBuilder.toString()));
    }
}
