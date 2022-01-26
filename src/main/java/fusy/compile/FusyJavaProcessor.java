package fusy.compile;

import suite.suite.Subject;

import java.util.Stack;

import static suite.suite.$uite.$;

public class FusyJavaProcessor extends FusProcessor {

    enum State {
        DEFAULT, F, U, S, Y, FUSY
    }

    enum Result {
        COMPLETE
    }

    Stack<State> state;
    StringBuilder result;
    FusProcessor parentProcessor;

    public FusyJavaProcessor(FusProcessor parentProcessor) {
        this.parentProcessor = parentProcessor;
    }

    @Override
    public void getReady() {
        result = new StringBuilder();
        state = new Stack<>();
        state.push(State.DEFAULT);
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
            case DEFAULT -> {
                if(i == '@') {
                    state.pop();
                    state.push(State.F);
                } else {
                    result.appendCodePoint(i);
                }
            }
            case F -> {
                state.pop();
                if(i == 'f') {
                    state.push(State.U);
                } else {
                    result.append("@");
                    return advance(i);
                }
            }
            case U -> {
                state.pop();
                if(i == 'u') {
                    state.push(State.S);
                } else {
                    result.append("@f");
                    return advance(i);
                }
            }
            case S -> {
                state.pop();
                if(i == 's') {
                    state.push(State.Y);
                } else {
                    result.append("@fu");
                    return advance(i);
                }
            }
            case Y -> {
                state.pop();
                if(i == 'y') {
                    state.push(State.FUSY);
                } else {
                    result.append("@fus");
                    return advance(i);
                }
            }
            case FUSY -> {
                state.pop();
                if(Character.isJavaIdentifierPart(i)) {
                    result.append("@fusy");
                    return advance(i);
                } else {
                    parentProcessor.terminateSubProcess();
                }
            }
        }
        return 0;
    }


    public void terminateSubProcess() {
    }

    @Override
    public Subject finish() {
        return $(Result.COMPLETE, $(result.toString()));
    }
}
