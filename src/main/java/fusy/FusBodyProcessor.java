package fusy;

import suite.suite.Subject;

import static suite.suite.$uite.$;

public class FusBodyProcessor implements FusProcessor {

    enum State {
        EMPTY_STATEMENT, STATEMENT, EXPRESSION, ID, AFTER_ID, STRING, STRING_AT, STRING_AT_END, CHARACTER, STR_BACKSLASH,
        SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT, BT, BT_NEXT,  BT_TOKEN, BRACKET_INDEX,
        SCOPE_EXP, SCOPE, SCOPE_END, CASE_SCOPE, SCOPE_STAT, SCOPE_EXP_STAT, CASE_SCOPE_STAT, INLINE_STATEMENT, NAKED_EXP,
        BACKSLASH, CLOSE_BRACE, DEFINITION, DOUBLE_BACKSLASH, MLC_BACKSLASH, MLC_DOUBLE_BACKSLASH
    }

    enum Result {
        STATEMENTS, DEFINITIONS
    }

    Subject $state;
    StringBuilder result;
    StringBuilder token;
    FusProcessor parentProcess;
    FusDefinitionProcessor defProcessor;
    FusDebugger debugger;
    Subject definitions;

    public FusBodyProcessor(FusProcessor parentProcess) {
        this.parentProcess = parentProcess;
    }

    @Override
    public void getReady() {
        result = new StringBuilder();
        definitions = $();
        debugger = new FusDebugger();
        debugger.getReady();
        $state = $($(State.EMPTY_STATEMENT));
    }

    @Override
    public void advance(int i) {
        debugger.advance(i);
        try {
            switch ($state.in().as(State.class)) {
                case EMPTY_STATEMENT -> {
                    if (Character.isWhitespace(i)) {
                        result.appendCodePoint(i);
                    } else if (i == '<') {
                        $state.unset($state.raw());
                        if ($state.absent() && parentProcess != null) {
                            parentProcess.terminateSubProcess();
                        }
                    } else if (i == '@') {
                        defProcessor = new FusDefinitionProcessor(this);
                        defProcessor.getReady();
                        $state.aimedAdd($state.raw(), State.DEFINITION);
                    } else if (i == '#') {
                        result.append("var ");
                    } else if (i == '\\') {
                        $state.aimedAdd($state.raw(), State.BACKSLASH);
                    } else {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.STATEMENT);
                        advance(i);
                    }
                }
                case DEFINITION -> defProcessor.advance(i);
                case STATEMENT -> {
                    if (i == '"') {
                        $state.aimedAdd($state.raw(), State.STRING);
                        result.appendCodePoint(i);
                    } else if (i == '\'') {
                        $state.aimedAdd($state.raw(), State.CHARACTER);
                        result.appendCodePoint(i);
                    } else if (i == '[') {
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append("$uite.$(");
                    } else if (i == '(') {
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.appendCodePoint(i);
                    } else if (i == '\n') {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                        result.append(";\n");
                    } else if (i == '\\') {
                        $state.aimedAdd($state.raw(), State.BACKSLASH);
                    } else if (Character.isJavaIdentifierPart(i)) {
                        $state.aimedAdd($state.raw(), State.ID);
                        token = new StringBuilder();
                        token.appendCodePoint(i);
                    } else {
                        result.appendCodePoint(i);
                    }
                }
                case INLINE_STATEMENT -> {
                    if (i == '"') {
                        $state.aimedAdd($state.raw(), State.STRING);
                        result.appendCodePoint(i);
                    } else if (i == '\'') {
                        $state.aimedAdd($state.raw(), State.CHARACTER);
                        result.appendCodePoint(i);
                    } else if (i == '[') {
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append("$uite.$(");
                    } else if (i == '(') {
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.appendCodePoint(i);
                    } else if (i == '\n') {
                        $state.unset($state.raw());
                        result.append(";");
                        advance(i);
                    } else if (i == '\\') {
                        $state.aimedAdd($state.raw(), State.BACKSLASH);
                    } else if (Character.isJavaIdentifierPart(i)) {
                        $state.aimedAdd($state.raw(), State.ID);
                        token = new StringBuilder();
                        token.appendCodePoint(i);
                    } else {
                        result.appendCodePoint(i);
                    }
                }
                case EXPRESSION -> {
                    if (i == '"') {
                        $state.aimedAdd($state.raw(), State.STRING);
                        result.appendCodePoint(i);
                    } else if (i == '\'') {
                        $state.aimedAdd($state.raw(), State.CHARACTER);
                        result.appendCodePoint(i);
                    } else if (i == '[') {
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append("$uite.$(");
                    } else if (i == '(') {
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.appendCodePoint(i);
                    } else if (i == ')') {
                        $state.unset($state.raw());
                        result.appendCodePoint(i);
                    } else if (i == '#') {
                        result.append("var ");
                    } else if (i == '\\') {
                        $state.aimedAdd($state.raw(), State.BACKSLASH);
                    } else if (Character.isJavaIdentifierPart(i)) {
                        $state.aimedAdd($state.raw(), State.ID);
                        token = new StringBuilder();
                        token.appendCodePoint(i);
                    } else {
                        result.appendCodePoint(i);
                    }
                }
                case NAKED_EXP -> {
                    if (i == '"') {
                        $state.aimedAdd($state.raw(), State.STRING);
                        result.appendCodePoint(i);
                    } else if (i == '\'') {
                        $state.aimedAdd($state.raw(), State.CHARACTER);
                        result.appendCodePoint(i);
                    } else if (i == '[') {
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append("$uite.$(");
                    } else if (i == '(') {
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.appendCodePoint(i);
                    } else if (i == ')') {
                        $state.unset($state.raw());
                    } else if (i == '#') {
                        result.append("var ");
                    } else if (i == '\\') {
                        $state.aimedAdd($state.raw(), State.BACKSLASH);
                    } else if (Character.isJavaIdentifierPart(i)) {
                        $state.aimedAdd($state.raw(), State.ID);
                        token = new StringBuilder();
                        token.appendCodePoint(i);
                    } else {
                        result.appendCodePoint(i);
                    }
                }
                case ID -> {
                    if (Character.isJavaIdentifierPart(i)) {
                        token.appendCodePoint(i);

                    } else {
                        switch (token.toString()) {
                            case "if", "for", "switch", "catch", "while" -> {
                                $state.unset($state.raw());
                                $state.unset($state.raw());
                                $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                                $state.aimedAdd($state.raw(), State.SCOPE_EXP);
                                result.append(token);
                                advance(i);
                            }
                            case "sync" -> {
                                $state.unset($state.raw());
                                $state.unset($state.raw());
                                $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                                $state.aimedAdd($state.raw(), State.SCOPE_EXP);
                                result.append("synchronised");
                                advance(i);
                            }
                            case "do", "else", "try", "finally" -> {
                                $state.unset($state.raw());
                                $state.unset($state.raw());
                                $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                                $state.aimedAdd($state.raw(), State.SCOPE);
                                result.append(token).append("{");
                                advance(i);
                            }
                            case "case" -> {
                                $state.unset($state.raw());
                                $state.unset($state.raw());
                                $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                                $state.aimedAdd($state.raw(), State.CASE_SCOPE);
                                result.append(token);
                                advance(i);
                            }
                            case "elf" -> {
                                $state.unset($state.raw());
                                $state.unset($state.raw());
                                $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                                $state.aimedAdd($state.raw(), State.SCOPE_EXP);
                                result.append("else if");
                                advance(i);
                            }
                            case "enum", "interface", "class", "record" -> {
                                $state.unset($state.raw());
                                $state.unset($state.raw());
                                $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                                $state.aimedAdd($state.raw(), State.SCOPE_STAT);
                                result.append(token);
                                advance(i);
                            }
                            default -> {
                                $state.unset($state.raw());
                                $state.aimedAdd($state.raw(), State.AFTER_ID);
                                result.append(token);
                                advance(i);
                            }
                        }
                    }
                }
                case AFTER_ID -> {
                    if (i == '[') {
                        $state.aimedAdd($state.raw(), State.BRACKET_INDEX);
                        result.appendCodePoint(i);
                    } else if (i == '.') {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.ID);
                        token = new StringBuilder();
                        token.appendCodePoint(i);
                    } else if (i == '(') {
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.appendCodePoint(i);
                    } else if (i == '@') {
                        result.append(".class");
                    } else if (Character.isWhitespace(i) && i != '\n') {
                        result.appendCodePoint(i);
                    } else {
                        $state.unset($state.raw());
                        advance(i);
                    }
                }
                case CLOSE_BRACE -> {
                    result.appendCodePoint(')');
                    $state.unset($state.raw());
                    advance(i);
                }
                case STRING -> {
                    if (i == '"') {
                        $state.unset($state.raw());
                        result.appendCodePoint(i);
                    } else if (i == '\\') {
                        $state.aimedAdd($state.raw(), State.STR_BACKSLASH);
                        result.appendCodePoint(i);
                    } else if (i == '#') {
                        $state.aimedAdd($state.raw(), State.STRING_AT);
                        result.append("\" + ");
                        token = new StringBuilder();
                    } else {
                        result.appendCodePoint(i);
                    }
                }
                case STRING_AT -> {
                    if (Character.isJavaIdentifierPart(i)) {
                        token.appendCodePoint(i);
                    } else if (i == '(') {
                        result.append(token).appendCodePoint(i);
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.STRING_AT_END);
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                    } else {
                        $state.unset($state.raw());
                        result.append(token).append(" + \"");
                        advance(i);
                    }
                }
                case STRING_AT_END -> {
                    $state.unset($state.raw());
                    result.append(" + \"");
                }
                case CHARACTER -> {
                    if (i == '\'') {
                        $state.unset($state.raw());
                        result.appendCodePoint(i);
                    } else if (i == '\\') {
                        $state.aimedAdd($state.raw(), State.STR_BACKSLASH);
                        result.appendCodePoint(i);
                    } else {
                        result.appendCodePoint(i);
                    }
                }
                case STR_BACKSLASH -> {
                    $state.unset($state.raw());
                    result.appendCodePoint(i);
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
                    if (i == '\\') {
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
                case BT -> {
                    if (i == '(') {
                        $state.in($state.raw()).reset(State.BT_NEXT);
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.appendCodePoint(i);
                    } else if (i == '[') {
                        $state.in($state.raw()).reset(State.BT_NEXT);
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append("$uite.$(");
                    } else if (i == ']') {
                        $state.unset($state.raw());
                        result.append(")");
                    } else if (Character.isWhitespace(i)) {
                        result.appendCodePoint(i);
                    } else {
                        $state.in($state.raw()).reset(State.BT_NEXT);
                        $state.aimedAdd($state.raw(), State.BT_TOKEN);
                        result.append("\"");
                        token = new StringBuilder();
                        advance(i);
                    }
                }
                case BT_NEXT -> {
                    if (i == '(') {
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.append(",").appendCodePoint(i);
                    } else if (i == '[') {
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append(",$uite.$(");
                    } else if (i == ']') {
                        $state.unset($state.raw());
                        result.append(")");
                    } else if (Character.isWhitespace(i)) {
                        result.appendCodePoint(i);
                    } else {
                        $state.aimedAdd($state.raw(), State.BT_TOKEN);
                        result.append(",\"");
                        token = new StringBuilder();
                        advance(i);
                    }
                }
                case BT_TOKEN -> {
                    if (i == '[' || i == ']') {
                        $state.unset($state.raw());
                        result.append(token.toString().trim()).append("\"");
                        advance(i);
                    } else if (i == '"') {
                        token.append("\\\"");
                    } else {
                        token.appendCodePoint(i);
                    }
                }
                case BRACKET_INDEX -> {
                    if (i == '"') {
                        $state.aimedAdd($state.raw(), State.STRING);
                        result.appendCodePoint(i);
                    } else if (i == '\'') {
                        $state.aimedAdd($state.raw(), State.CHARACTER);
                        result.appendCodePoint(i);
                    } else if (i == '[') {
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append("$uite.$(");
                    } else if (i == '(') {
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.appendCodePoint(i);
                    } else if (i == ']') {
                        $state.unset($state.raw());
                        result.appendCodePoint(i);
                    } else if (Character.isJavaIdentifierPart(i)) {
                        $state.aimedAdd($state.raw(), State.ID);
                        token = new StringBuilder();
                        token.appendCodePoint(i);
                    } else {
                        result.appendCodePoint(i);
                    }
                }
                case SCOPE_EXP -> {
                    if (i == '(') {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.SCOPE_EXP_STAT);
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.append("(");
                    } else {
                        result.appendCodePoint(i);
                    }
                }
                case SCOPE_EXP_STAT -> {
                    if (i == '\n') {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.SCOPE_END);
                        $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                        result.append("{\n");
                    } else if (Character.isWhitespace(i)) {
                        result.appendCodePoint(i);
                    } else {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.SCOPE_END);
                        $state.aimedAdd($state.raw(), State.INLINE_STATEMENT);
                        result.append("{");
                        advance(i);
                    }
                }
                case SCOPE_END -> {
                    $state.unset($state.raw());
                    result.append("}");
                    advance(i);
                }
                case SCOPE -> {
                    if (i == '\n') {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.SCOPE_END);
                        $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                        result.append("\n");
                    } else if (Character.isWhitespace(i)) {
                        result.appendCodePoint(i);
                    } else {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.SCOPE_END);
                        $state.aimedAdd($state.raw(), State.INLINE_STATEMENT);
                        advance(i);
                    }
                }
                case CASE_SCOPE -> {
                    if (i == '(') {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.CASE_SCOPE_STAT);
                        $state.aimedAdd($state.raw(), State.NAKED_EXP);
                        result.append(" ");
                    } else {
                        result.appendCodePoint(i);
                    }
                }
                case CASE_SCOPE_STAT -> {
                    if (i == '\n') {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.SCOPE_END);
                        $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                        result.append("->{\n");
                    } else if (Character.isWhitespace(i)) {
                        result.appendCodePoint(i);
                    } else {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.SCOPE_END);
                        $state.aimedAdd($state.raw(), State.INLINE_STATEMENT);
                        result.append("->{");
                        advance(i);
                    }
                }
                case SCOPE_STAT -> {
                    if (i == '\n') {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.SCOPE_END);
                        $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                        result.append("{\n");
                    } else if (Character.isWhitespace(i)) {
                        result.appendCodePoint(i);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(debugger.finish().in(FusDebugger.Result.COMPLETE).asString(), e);
        }
    }

    public void terminateSubProcess() {
        if($state.in().raw() != State.DEFINITION) return;
        var $ = defProcessor.finish();
        String def = $.in(FusDefinitionProcessor.Result.COMPLETE).asString();
        definitions.add(def);
        $state.unset($state.raw());
    }

    @Override
    public Subject finish() {
        var defSb = new StringBuilder();
        for(var def : definitions.eachIn().eachAs(String.class)) {
            defSb.append(def);
        }
        return $(
                Result.STATEMENTS, $(result.toString()),
                Result.DEFINITIONS, $(defSb.toString())
        );
    }
}
