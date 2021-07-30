package fusy;

import suite.suite.Subject;

import static suite.suite.$uite.$;

public class FusDefinitionProcessor implements FusProcessor {

    enum State {
        BEFORE_TYPE, TYPE, HEADER, BODY, BACKSLASH, AT, ENUM_HEADER, ENUM_PENDING, ENUM_OPTION,
        ENUM_OPTION_CSTR, ENUM_AFTER_CSTR, ENUM_BODY, ENUM_AFTER_BODY
    }

    enum Result {
        COMPLETE
    }

    Subject $state;
    StringBuilder result;
    StringBuilder token;
    FusBodyProcessor parentProcess;
    FusBodyProcessor subProcess;

    public FusDefinitionProcessor(FusBodyProcessor parentProcess) {
        this.parentProcess = parentProcess;
    }

    @Override
    public void getReady() {
        result = new StringBuilder();
        $state = $($(State.BEFORE_TYPE));
    }

    @Override
    public void advance(int i) {
        switch ($state.in().as(State.class)) {
            case BEFORE_TYPE -> {
                if(i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if(i == '>') {
                    $state.aimedAdd($state.raw(), State.BODY);
                    result.append("{\n");
                    subProcess = new FusBodyProcessor(this);
                    subProcess.getReady();
                } else if(Character.isWhitespace(i)){
                    result.appendCodePoint(i);
                } else {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.TYPE);
                    token = new StringBuilder();
                    advance(i);
                }
            }
            case TYPE -> {
                if(Character.isJavaIdentifierPart(i)) {
                    token.appendCodePoint(i);
                } else {
                    var str = token.toString();
                    switch (str) {
                        case "enum" -> {
                            result.append(str);
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.ENUM_HEADER);
                            advance(i);
                        }
                        default -> {
                            result.append(str);
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.HEADER);
                            advance(i);
                        }
                    }
                }
            }
            case HEADER -> {
                if(i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if(i == '\n') {
                    $state.aimedAdd($state.raw(), State.BODY);
                    result.append("{\n");
                    subProcess = new FusBodyProcessor(this);
                    subProcess.getReady();
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
                } else {
                    result.appendCodePoint(i);
                }
            }
            case ENUM_PENDING -> {
                if(Character.isJavaIdentifierPart(i)) {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.ENUM_OPTION);
                    advance(i);
                } else if(i == '@') {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.AT);
                } else if(i == '<') {
                    result.append("}");
                    parentProcess.terminateSubProcess();
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
                    subProcess = new FusBodyProcessor(this);
                    subProcess.getReady(FusBodyProcessor.State.EXPRESSION);
                } else if(i == '@') {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.AT);
                } else if(i == '<') {
                    result.append("}");
                    parentProcess.terminateSubProcess();
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
                } else if(i == '@') {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.AT);
                } else if(i == '<') {
                    result.append("}");
                    parentProcess.terminateSubProcess();
                }
            }
            case AT -> {
                if(i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if(i == '>') {
                    $state.aimedAdd($state.raw(), State.ENUM_BODY);
                    result.append(";\n");
                    subProcess = new FusBodyProcessor(this);
                    subProcess.getReady();
                }
            }
            case BODY, ENUM_OPTION_CSTR, ENUM_BODY -> subProcess.advance(i);
            case BACKSLASH -> {
                if(i == '\n') {
                    $state.unset($state.raw());
                } else if(Character.isWhitespace(i)) {
                    result.appendCodePoint(i);
                } else {
                    $state.unset($state.raw());
                    advance('\n');
                    advance(i);
                }
            }
            case ENUM_AFTER_BODY -> {
                if(i == '<') {
                    result.append("}");
                    parentProcess.terminateSubProcess();
                }
            }
        }
    }

    public void terminateSubProcess() {
        if($state.in().raw() == State.BODY) {
            var $ = subProcess.finish();
            String stats = $.in(FusBodyProcessor.Result.STATEMENTS).asString();
            String defs = $.in(FusBodyProcessor.Result.DEFINITIONS).asString();
            result.append(stats).append(defs).append("}");
            parentProcess.terminateSubProcess();
        } else if($state.in().raw() == State.ENUM_OPTION_CSTR) {
            var $ = subProcess.finish();
            String stats = $.in(FusBodyProcessor.Result.STATEMENTS).asString();
            result.append(stats);
            $state.unset($state.raw());
            $state.aimedAdd($state.raw(), State.ENUM_AFTER_CSTR);
        } else if($state.in().raw() == State.ENUM_BODY) {
            var $ = subProcess.finish();
            String stats = $.in(FusBodyProcessor.Result.STATEMENTS).asString();
            String defs = $.in(FusBodyProcessor.Result.DEFINITIONS).asString();
            result.append(stats).append(defs);
            $state.unset($state.raw());
            $state.aimedAdd($state.raw(), State.ENUM_AFTER_BODY);
        }
    }

    @Override
    public Subject finish() {
        return $(Result.COMPLETE, $(result.toString()));
    }
}
