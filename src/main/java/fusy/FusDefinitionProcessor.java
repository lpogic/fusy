package fusy;

import suite.suite.Subject;

import static suite.suite.$uite.$;

public class FusDefinitionProcessor extends FusProcessor {

    enum State {
        BEFORE_TYPE, TYPE, HEADER, BODY, ENUM_HEADER, ENUM_PENDING, ENUM_OPTION,
        ENUM_OPTION_CSTR, ENUM_AFTER_CSTR, ENUM_BODY, BEAK, BACKBEAK, FUSY_FUN,
        BACKSLASH, DOUBLE_BACKSLASH, SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT, MLC_BACKSLASH, MLC_DOUBLE_BACKSLASH
    }

    enum Result {
        COMPLETE
    }

    StringBuilder result;
    FusBodyProcessor parentProcessor;
    FusProcessor subProcessor;

    public FusDefinitionProcessor(FusBodyProcessor parentProcessor) {
        this.parentProcessor = parentProcessor;
    }

    @Override
    public void getReady() {
        result = new StringBuilder();
        $state = $($(State.BEFORE_TYPE));
    }

    public void getReady(String type) {
        result = new StringBuilder();
        $state = $($(State.TYPE));
        typeComplete(type);
    }

    @Override
    public void advance(int i) {
        switch ($state.in().as(State.class)) {
            case BEFORE_TYPE -> {
                if(i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if(Character.isWhitespace(i)){
                    result.appendCodePoint(i);
                } else if(i == '>') {
                    $state.aimedAdd($state.raw(), State.BODY);
                    result.append("{");
                    subProcessor = new FusBodyProcessor(this);
                    subProcessor.getReady();
                } else if(i == '.') {
                    result.append("private ");
                } else if(i == ':') {
                    result.append("protected ");
                } else if(i == '!') {
                    result.append("public ");
                } else if(i == '^') {
                    result.append("static ");
                } else {
                    subProcessor = new FusyTypeProcessor(this);
                    subProcessor.getReady();
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.TYPE);
                    advance(i);
                }
            }
            case HEADER -> {
                if(i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if(i == '\n') {
                    $state.aimedAdd($state.raw(), State.BODY);
                    result.append("{\n");
                    subProcessor = new FusBodyProcessor(this);
                    subProcessor.getReady();
                } else if (i == '<') {
                    $state.aimedAdd($state.raw(), State.BEAK);
                } else if(i == '{') {
                    subProcessor = new FusyFunProcessor(this);
                    subProcessor.getReady();
                    $state.aimedAdd($state.raw(), State.FUSY_FUN);
                } else {
                    result.appendCodePoint(i);
                }
            }
            case ENUM_HEADER -> {
                if(i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if(i == '\n') {
                    $state.aimedAdd($state.raw(), State.ENUM_PENDING);
                    result.append("{\n");
                } else if (i == '<') {
                    $state.aimedAdd($state.raw(), State.BEAK);
                } else {
                    result.appendCodePoint(i);
                }
            }
            case BEAK -> {
                if (i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if(i == '\n') {
                    $state.unset($state.raw());
                    advance('\n');
                    advance('<');
                } else if(i == '<') {
                    $state.aimedAdd($state.raw(), State.BEAK);
                } else if(Character.isWhitespace(i)) {
                    result.appendCodePoint(i);
                } else {
                    result.append("<");
                    $state.unset($state.raw());
                    advance(i);
                }
            }
            case ENUM_PENDING -> {
                if(Character.isJavaIdentifierPart(i)) {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.ENUM_OPTION);
                    advance(i);
                } else if(i == '>') {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.BACKBEAK);
                } else if(i == '<') {
                    result.append("}");
                    parentProcessor.terminateSubProcess();
                }
            }
            case ENUM_OPTION -> {
                if(i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if(Character.isJavaIdentifierPart(i)) {
                    result.appendCodePoint(i);
                } else if(i == ',') {
                    result.appendCodePoint(i);
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.ENUM_PENDING);
                } else if(i == '\n') {
                    result.append(",\n");
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.ENUM_PENDING);
                } else if(i == '(') {
                    result.append("(");
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.ENUM_OPTION_CSTR);
                    var fusBodyProcessor = new FusBodyProcessor(this);
                    fusBodyProcessor.getReady(FusBodyProcessor.State.EXPRESSION);
                    subProcessor = fusBodyProcessor;
                } else if(i == '>') {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.BACKBEAK);
                } else if(i == '<') {
                    result.append("}");
                    parentProcessor.terminateSubProcess();
                }
            }
            case ENUM_AFTER_CSTR -> {
                if(i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if(Character.isJavaIdentifierPart(i)) {
                    result.append(",");
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.ENUM_OPTION);
                    advance(i);
                } else if(i == ',' || i == '\n') {
                    result.append(",");
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.ENUM_PENDING);
                } else if(i == '>') {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.BACKBEAK);
                } else if(i == '<') {
                    result.append("}");
                    parentProcessor.terminateSubProcess();
                }
            }
            case BACKBEAK -> {
                if (i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if(i == '\n') {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.ENUM_BODY);
                    result.append(";\n");
                    subProcessor = new FusBodyProcessor(this);
                    subProcessor.getReady();
                } else if(i == '<') {
                    $state.aimedAdd($state.raw(), FusBodyProcessor.State.BEAK);
                } else if(Character.isWhitespace(i)) {
                    result.appendCodePoint(i);
                } else {
                    result.append(">");
                    $state.unset($state.raw());
                    advance(i);
                }
            }
            case TYPE, BODY, ENUM_OPTION_CSTR, ENUM_BODY, FUSY_FUN -> subProcessor.advance(i);
            case BACKSLASH -> {
                if(i == '\\') {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), FusyTypeProcessor.State.DOUBLE_BACKSLASH);
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
                    $state.aimedAdd($state.raw(), FusyTypeProcessor.State.MULTI_LINE_COMMENT);
                } else {
                    $state.unset($state.raw());
                    advance('\n');
                    $state.aimedAdd($state.raw(), FusyTypeProcessor.State.SINGLE_LINE_COMMENT);
                }
            }
            case SINGLE_LINE_COMMENT -> {
                if (i == '\n') {
                    $state.unset($state.raw());
                }
            }
            case MULTI_LINE_COMMENT -> {
                if (i == '<') {
                    $state.aimedAdd($state.raw(), FusyTypeProcessor.State.MLC_BACKSLASH);
                }
            }
            case MLC_BACKSLASH -> {
                $state.unset($state.raw());
                if (i == '\\') {
                    $state.aimedAdd($state.raw(), FusyTypeProcessor.State.MLC_DOUBLE_BACKSLASH);
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
        }
    }

    public void terminateSubProcess() {
        if($state.in().raw() == State.TYPE) {
            var str = subProcessor.finish().in(FusyTypeProcessor.Result.COMPLETE).asString();
            typeComplete(str);
        } else if($state.in().raw() == State.BODY) {
            var $ = subProcessor.finish();
            String stats = $.in(FusBodyProcessor.Result.STATEMENTS).asString();
            String defs = $.in(FusBodyProcessor.Result.DEFINITIONS).asString();
            result.append(stats).append(defs).append("}");
            parentProcessor.terminateSubProcess();
        } else if($state.in().raw() == State.ENUM_OPTION_CSTR) {
            var $ = subProcessor.finish();
            String stats = $.in(FusBodyProcessor.Result.STATEMENTS).asString();
            result.append(stats);
            $state.unset($state.raw());
            $state.aimedAdd($state.raw(), State.ENUM_AFTER_CSTR);
        } else if($state.in().raw() == State.ENUM_BODY) {
            var $ = subProcessor.finish();
            String stats = $.in(FusBodyProcessor.Result.STATEMENTS).asString();
            String defs = $.in(FusBodyProcessor.Result.DEFINITIONS).asString();
            result.append(stats).append(defs).append("}");
            parentProcessor.terminateSubProcess();
        } else if($state.in().raw() == State.FUSY_FUN) {
            var $ = subProcessor.finish();
            String fun = $.in(FusyFunProcessor.Result.COMPLETE).asString();
            result.append(fun);
            $state.unset($state.raw());
        }
    }

    private void typeComplete(String complete) {
        switch (complete) {
            case "enum" -> {
                result.append(complete).append(" ");
                $state.unset($state.raw());
                $state.aimedAdd($state.raw(), State.ENUM_HEADER);
            }
            case "new" -> {
                $state.unset($state.raw());
                $state.aimedAdd($state.raw(), State.HEADER);
            }
            default -> {
                result.append(complete).append(" ");
                $state.unset($state.raw());
                $state.aimedAdd($state.raw(), State.HEADER);
            }
        }
    }

    @Override
    public Subject finish() {
        return $(Result.COMPLETE, $(result.toString()));
    }
}
