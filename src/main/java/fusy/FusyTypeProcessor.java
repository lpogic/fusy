package fusy;

import suite.suite.Subject;

import static suite.suite.$uite.$;

public class FusyTypeProcessor extends FusProcessor {

    enum State {
        BEFORE, ID, BEFORE_GENERIC, BEFORE_NEXT_GENERIC, GENERIC, AFTER_GENERIC, FUSY_FUN, BEAK,
        BACKSLASH, DOUBLE_BACKSLASH, SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT, MLC_BACKSLASH, MLC_DOUBLE_BACKSLASH,
        TERMINATED
    }

    enum Result {
        COMPLETE
    }

    Subject $types;
    StringBuilder result;
    FusProcessor parentProcessor;
    FusProcessor subProcessor;

    public FusyTypeProcessor(FusProcessor parentProcessor) {
        this.parentProcessor = parentProcessor;
    }

    @Override
    public void getReady() {
        result = new StringBuilder();
        $state = $($(State.BEFORE));
        $types = $();
    }

    @Override
    public void advance(int i) {
        switch ($state.in().as(State.class)) {
            case BEFORE -> {
                if(i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if(i == '{') {
                    subProcessor = new FusyFunProcessor(this);
                    subProcessor.getReady();
                    $state.aimedAdd($state.raw(), State.FUSY_FUN);
                } else if(Character.isJavaIdentifierStart(i)) {
                    result.appendCodePoint(i);
                    $state.aimedAdd($state.raw(), State.ID);
                }
            }
            case FUSY_FUN, GENERIC -> subProcessor.advance(i);
            case ID -> {
                if(i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if(i == '<') {
                    $state.aimedAdd($state.raw(), State.BEAK);
                } else if(Character.isJavaIdentifierPart(i) || i == '.') {
                    result.appendCodePoint(i);
                } else {
                    parentProcessor.terminateSubProcess();
                    parentProcessor.advance(i);
                }
            }
            case BEFORE_GENERIC -> {
                if(i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if(i == '{') {
                    subProcessor = new FusyFunProcessor(this);
                    subProcessor.getReady();
                    $state.aimedAdd($state.raw(), State.FUSY_FUN);
                } else if(i == '>') {
                    result.append('>');
                    parentProcessor.terminateSubProcess();
                } else if(Character.isJavaIdentifierStart(i)){
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.GENERIC);
                    subProcessor = new FusyTypeProcessor(this);
                    subProcessor.getReady();
                    advance(i);
                }
            }
            case BEFORE_NEXT_GENERIC -> {
                if(i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if(i == '{') {
                    result.append(',');
                    subProcessor = new FusyFunProcessor(this);
                    subProcessor.getReady();
                    $state.aimedAdd($state.raw(), State.FUSY_FUN);
                } else if(i == '>') {
                    result.append('>');
                    parentProcessor.terminateSubProcess();
                } else if(Character.isJavaIdentifierStart(i)){
                    result.append(',');
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.GENERIC);
                    subProcessor = new FusyTypeProcessor(this);
                    subProcessor.getReady();
                    advance(i);
                }
            }
            case AFTER_GENERIC -> {
                if(i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if(i == '>') {
                    result.append('>');
                    parentProcessor.terminateSubProcess();
                } else if(i == ',') {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.BEFORE_NEXT_GENERIC);
                } else {
                    result.appendCodePoint(i);
                }
            }
            case BEAK -> {
                if (i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
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
                    parentProcessor.advance(i);
                } else if(Character.isWhitespace(i)) {
                    result.appendCodePoint(i);
                } else {
                    result.append("<");
                    $state.unset($state.raw());
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.BEFORE_GENERIC);
                    advance(i);
                }
            }
            case BACKSLASH -> {
                if(i == '\\') {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.DOUBLE_BACKSLASH);
                } else if (i == '\n') {
                    $state.unset($state.raw());
                } else if (Character.isWhitespace(i)) {
                    result.appendCodePoint(i);
                } else {
                    $state.unset($state.raw());
                    advance('\n');
                    advance(i);
                }
            }
            case DOUBLE_BACKSLASH -> {
                if (i == '>') {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.MULTI_LINE_COMMENT);
                } else {
                    $state.unset($state.raw());
                    advance('\n');
                    $state.aimedAdd($state.raw(), State.SINGLE_LINE_COMMENT);
                }
            }
            case SINGLE_LINE_COMMENT -> {
                if (i == '\n') {
                    $state.unset($state.raw());
                }
            }
            case MULTI_LINE_COMMENT -> {
                if (i == '<') {
                    $state.aimedAdd($state.raw(), State.MLC_BACKSLASH);
                }
            }
            case MLC_BACKSLASH -> {
                $state.unset($state.raw());
                if (i == '\\') {
                    $state.aimedAdd($state.raw(), State.MLC_DOUBLE_BACKSLASH);
                }
            }
            case MLC_DOUBLE_BACKSLASH -> {
                $state.unset($state.raw());
                if (i == '\\') {
                    $state.unset($state.raw());
                } else {
                    advance(i);
                }
            }
            case TERMINATED -> parentProcessor.advance(i);
        }
    }


    public void terminateSubProcess() {
        if($state.in().raw() == State.FUSY_FUN) {
            var $ = subProcessor.finish();
            String type = $.in(FusyFunProcessor.Result.COMPLETE).asString();
            result.append(type);
            parentProcessor.terminateSubProcess();
            $state.aimedAdd($state.raw(), State.TERMINATED);
        } else if($state.in().raw() == State.GENERIC) {
            var $ = subProcessor.finish();
            String type = $.in(Result.COMPLETE).asString();
            result.append(type);
            $state.unset($state.raw());
            $state.aimedAdd($state.raw(), State.AFTER_GENERIC);
        }
    }

    @Override
    public Subject finish() {
        return $(Result.COMPLETE, $(result.toString()));
    }
}
