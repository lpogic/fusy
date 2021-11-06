package fusy;

import suite.suite.Subject;

import java.util.Stack;

import static suite.suite.$uite.$;

public class FusyTypeProcessor extends FusProcessor {

    enum State {
        BEFORE, ID, BEFORE_GENERIC, BEFORE_NEXT_GENERIC, GENERIC, AFTER_GENERIC, FUSY_FUN, BEAK,
        BACKSLASH, DOUBLE_BACKSLASH, SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT, MLC_BACKSLASH, MLC_DOUBLE_BACKSLASH,
        TERMINATED, ARRAY
    }

    enum Result {
        COMPLETE
    }

    Stack<State> state;
    Subject $types;
    StringBuilder result;
    boolean arrayTypes;
    FusProcessor parentProcessor;
    FusProcessor subProcessor;

    public FusyTypeProcessor(FusProcessor parentProcessor) {
        this.parentProcessor = parentProcessor;
    }

    @Override
    public void getReady() {
        result = new StringBuilder();
        arrayTypes = true;
        state = new Stack<>();
        state.push(State.BEFORE);
        $types = $();
    }

    public void arrayTypeEnabled(boolean enabled) {
        arrayTypes = enabled;
    }

    @Override
    public FusDebugger getDebugger() {
        return parentProcessor.getDebugger();
    }

    @Override
    public int advance(int i) {
        switch (state.peek()) {
            case BEFORE -> {
                if(i == '\\') {
                    state.push(State.BACKSLASH);
                } else if(i == '{') {
                    subProcessor = new FusyFunProcessor(this);
                    subProcessor.getReady();
                    state.push(State.FUSY_FUN);
                } else if(Character.isJavaIdentifierStart(i)) {
                    result.appendCodePoint(i);
                    state.push(State.ID);
                }
            }
            case FUSY_FUN, GENERIC -> subProcessor.advance(i);
            case ID -> {
                if(i == '\\') {
                    state.push(State.BACKSLASH);
                } else if(i == '<') {
                    state.push(State.BEAK);
                } else if(Character.isJavaIdentifierPart(i) || i == '.') {
                    result.appendCodePoint(i);
                } else if(i == '{') {
                    if(arrayTypes) {
                        result.append("[");
                        state.push(State.ARRAY);
                    } else {
                        parentProcessor.terminateSubProcess();
                        parentProcessor.advance(i);
                    }
                } else {
                    parentProcessor.terminateSubProcess();
                    parentProcessor.advance(i);
                }
            }
            case ARRAY -> {
                if(i == '}') {
                    result.append("]");
                }
                state.pop();
            }
            case BEFORE_GENERIC -> {
                if(i == '\\') {
                    state.push(State.BACKSLASH);
                } else if(i == '>') {
                    result.append('>');
                    parentProcessor.terminateSubProcess();
                } else if(Character.isJavaIdentifierStart(i) || i == '{'){
                    state.pop();
                    state.push(State.GENERIC);
                    subProcessor = new FusyTypeProcessor(this);
                    subProcessor.getReady();
                    advance(i);
                }
            }
            case BEFORE_NEXT_GENERIC -> {
                if(i == '\\') {
                    state.push(State.BACKSLASH);
                } else if(i == '{') {
                    result.append(',');
                    subProcessor = new FusyFunProcessor(this);
                    subProcessor.getReady();
                    state.push(State.FUSY_FUN);
                } else if(i == '>') {
                    result.append('>');
                    parentProcessor.terminateSubProcess();
                } else if(Character.isJavaIdentifierStart(i)){
                    result.append(',');
                    state.pop();
                    state.push(State.GENERIC);
                    subProcessor = new FusyTypeProcessor(this);
                    subProcessor.getReady();
                    advance(i);
                }
            }
            case AFTER_GENERIC -> {
                if(i == '\\') {
                    state.push(State.BACKSLASH);
                } else if(i == '>') {
                    result.append('>');
                    parentProcessor.terminateSubProcess();
                } else if(i == ',') {
                    state.pop();
                    state.push(State.BEFORE_NEXT_GENERIC);
                } else {
                    result.appendCodePoint(i);
                }
            }
            case BEAK -> {
                if (i == '\\') {
                    state.push(State.BACKSLASH);
                } else if(i == '\n') {
                    parentProcessor.terminateSubProcess();
                    parentProcessor.advance('<');
                    parentProcessor.advance('\n');
                } else if(i == '<') {
                    parentProcessor.terminateSubProcess();
                    parentProcessor.advance('<');
                    parentProcessor.advance('<');
                } else if(i == '>') {
                    result.append("<>");
                    parentProcessor.terminateSubProcess();
                } else if(Character.isWhitespace(i)) {
                    result.appendCodePoint(i);
                } else {
                    result.append("<");
                    state.pop();
                    state.pop();
                    state.push(State.BEFORE_GENERIC);
                    advance(i);
                }
            }
            case BACKSLASH -> {
                if(i == '\\') {
                    state.pop();
                    state.push(State.DOUBLE_BACKSLASH);
                } else if (i == '\n') {
                    state.pop();
                } else if (Character.isWhitespace(i)) {
                    result.appendCodePoint(i);
                } else {
                    state.pop();
                    advance('\n');
                    advance(i);
                }
            }
            case DOUBLE_BACKSLASH -> {
                if (i == '>') {
                    state.pop();
                    state.push(State.MULTI_LINE_COMMENT);
                } else {
                    state.pop();
                    advance('\n');
                    state.push(State.SINGLE_LINE_COMMENT);
                }
            }
            case SINGLE_LINE_COMMENT -> {
                if (i == '\n') {
                    state.pop();
                }
            }
            case MULTI_LINE_COMMENT -> {
                if (i == '<') {
                    state.push(State.MLC_BACKSLASH);
                }
            }
            case MLC_BACKSLASH -> {
                state.pop();
                if (i == '\\') {
                    state.push(State.MLC_DOUBLE_BACKSLASH);
                }
            }
            case MLC_DOUBLE_BACKSLASH -> {
                state.pop();
                if (i == '\\') {
                    state.pop();
                } else {
                    advance(i);
                }
            }
            case TERMINATED -> parentProcessor.advance(i);
        }
        return 0;
    }


    public void terminateSubProcess() {
        if(state.peek() == State.FUSY_FUN) {
            var $ = subProcessor.finish();
            String type = $.in(FusyFunProcessor.Result.COMPLETE).asString();
            result.append(type);
            parentProcessor.terminateSubProcess();
            state.push(State.TERMINATED);
        } else if(state.peek() == State.GENERIC) {
            var $ = subProcessor.finish();
            String type = $.in(Result.COMPLETE).asString();
            result.append(type);
            state.pop();
            state.push(State.AFTER_GENERIC);
        }
    }

    @Override
    public Subject finish() {
        return $(Result.COMPLETE, $(result.toString()));
    }
}
