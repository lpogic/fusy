package fusy;

import suite.suite.Subject;

import static suite.suite.$uite.$;

public class FusBodyProcessor extends FusProcessor {

    public enum State {
        IGNORE, EMPTY_STATEMENT, STATEMENT, EXPRESSION, ID, AFTER_ID, AFTER_ID_HASH, STRING, STRING_HASH, STRHS_END, STRHS_EXP, STR_ENDLINE,
        CHARACTER, STR_BACKSLASH, SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT, BT, BT_NEXT, AFTER_BT, BT_ID, AFTER_BT_ID, BT_HASH, STAT_END,
        ARRAY_INDEX, ARRAY_INIT, ARRAY_END, COLON, COLON_METHOD, FOR_SCOPE_EXP, FSE_ID, EXP_END, FSE_AFTER_ID,
        SCOPE_EXP, SCOPE, SCOPE_END, CASE_SCOPE, SCOPE_EXP_STAT, NAKED_SCOPE_EXP_STAT, CASE_SCOPE_STAT, ELF_SCOPE, INLINE_STATEMENT,
        BACKSLASH, CLOSE_BRACE, DEFINITION, INLINE_DEFINITION, DOUBLE_BACKSLASH, MLC_BACKSLASH, MLC_DOUBLE_BACKSLASH,
        AT, AT_TYPE, LAMBDA, LAMBDA_EXP, BEAK, BACKBEAK, FUSY_FUN, FUSY_TYPE, HASH, BT_HASH_STRING, BTHS_BACKSLASH,
        ES_PLUS, ES_MINUS, EXCLAMATION, CATCH_SCOPE_EXP, CSE_ID, AFTER_BTI, BT_AT, BT_AT_TYPE
    }

    enum Result {
        STATEMENTS, DEFINITIONS, IMPORTS
    }

    StringBuilder result;
    StringBuilder token;
    FusProcessor parentProcessor;
    FusProcessor subProcessor;
    Subject definitions;
    Subject imports;

    public FusBodyProcessor(FusProcessor parentProcessor) {
        this.parentProcessor = parentProcessor;
    }

    @Override
    public void getReady() {
        getReady(State.EMPTY_STATEMENT);
    }

    public void getReady(Object initialState) {
        result = new StringBuilder();
        definitions = $();
        imports = $();
        $state = $($(initialState));
    }

    @Override
    public FusDebugger getDebugger() {
        return parentProcessor.getDebugger();
    }

    @Override
    public void advance(int i) {
        switch ($state.in().as(State.class)) {
            case IGNORE -> $state.unset($state.raw());
            case EMPTY_STATEMENT -> {
                switch (i) {
                    case '<' -> $state.unset($state.raw());
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '~', '!', '*', '?', '.' -> {
                        $state.aimedAdd($state.raw(), State.STATEMENT);
                        $state.aimedAdd($state.raw(), State.AT);
                        advance(i); return;
                    }
                    case '+' -> $state.aimedAdd($state.raw(), State.ES_PLUS);
                    case '-' -> $state.aimedAdd($state.raw(), State.ES_MINUS);
                    case '(' -> {
                        result.append("idle");
                        $state.aimedAdd($state.raw(), State.STATEMENT);
                        advance(i);
                        return;
                    }
                    default -> {
                        if (Character.isWhitespace(i)) {
                            result.appendCodePoint(i);
                        } else {
                            $state.aimedAdd($state.raw(), State.STATEMENT);
                            advance(i); return;
                        }
                    }
                }
            }
            case ES_PLUS -> {
                if(i == '+') {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.STATEMENT);
                    advance(i);
                    advance(i);
                    return;
                } else {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.STATEMENT);
                    $state.aimedAdd($state.raw(), State.AT);
                    advance('+');
                    advance(i);
                    return;
                }
            }
            case ES_MINUS -> {
                if(i == '-') {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.STATEMENT);
                    advance('-');
                    advance(i);
                    return;
                } else {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.STATEMENT);
                    $state.aimedAdd($state.raw(), State.AT);
                    advance('-');
                    advance(i);
                    return;
                }
            }
            case DEFINITION, INLINE_DEFINITION, FUSY_FUN, FUSY_TYPE -> subProcessor.advance(i);
            case STATEMENT -> {
                switch (i) {
                    case '"' -> {
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.STRING);
                        result.appendCodePoint(i);
                    }
                    case '\'' -> {
                        $state.aimedAdd($state.raw(), State.CHARACTER);
                        result.appendCodePoint(i);
                    }
                    case '(' -> {
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.appendCodePoint(i);
                    }
                    case '@' -> $state.aimedAdd($state.raw(), State.AT);
                    case '#' -> $state.aimedAdd($state.raw(), State.HASH);
                    case '[' -> {
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append("$uite.$(");
                    }
                    case '!' -> $state.aimedAdd($state.raw(), State.EXCLAMATION);
                    case '\n' -> {
                        result.append(";\n");
                        $state.unset($state.raw());
                    }
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '<' -> $state.aimedAdd($state.raw(), State.BEAK);
                    case '>' -> $state.aimedAdd($state.raw(), State.BACKBEAK);
                    case '{' -> {
                        subProcessor = new FusyFunProcessor(this);
                        subProcessor.getReady();
                        $state.aimedAdd($state.raw(), State.FUSY_FUN);
                    }
                    default -> {
                        if (Character.isJavaIdentifierPart(i)) {
                            $state.aimedAdd($state.raw(), State.ID);
                            token = new StringBuilder();
                            token.appendCodePoint(i);
                        } else {
                            result.appendCodePoint(i);
                        }
                    }
                }
            }
            case INLINE_STATEMENT -> {
                switch (i) {
                    case '"' -> {
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.STRING);
                        result.appendCodePoint(i);
                    }
                    case '\'' -> {
                        $state.aimedAdd($state.raw(), State.CHARACTER);
                        result.appendCodePoint(i);
                    }
                    case '(' -> {
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.appendCodePoint(i);
                    }
                    case '\n' -> {
                        $state.unset($state.raw());
                        advance(i);
                        return;
                    }
                    case '#' -> $state.aimedAdd($state.raw(), State.HASH);
                    case '[' -> {
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append("$uite.$(");
                    }
                    case '@' -> $state.aimedAdd($state.raw(), State.AT);
                    case '!' -> $state.aimedAdd($state.raw(), State.EXCLAMATION);
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '<' -> $state.aimedAdd($state.raw(), State.BEAK);
                    case '>' -> $state.aimedAdd($state.raw(), State.BACKBEAK);
                    case '{' -> {
                        subProcessor = new FusyFunProcessor(this);
                        subProcessor.getReady();
                        $state.aimedAdd($state.raw(), State.FUSY_FUN);
                    }
                    default -> {
                        if (Character.isJavaIdentifierPart(i)) {
                            $state.aimedAdd($state.raw(), State.ID);
                            token = new StringBuilder();
                            advance(i); return;
                        } else {
                            result.appendCodePoint(i);
                        }
                    }
                }
            }
            case STAT_END -> {
                result.append(";");
                $state.unset($state.raw());
                advance(i);
                return;
            }
            case BEAK -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '\n' -> {
                        $state.unset($state.raw());
                        getDebugger().advance('\n', false);
                        getDebugger().advance('<', false);
                        return;
                    }
                    case '<' -> $state.aimedAdd($state.raw(), State.BEAK);
                    default -> {
                        if (Character.isWhitespace(i)) {
                            result.appendCodePoint(i);
                        } else {
                            result.append("<");
                            $state.unset($state.raw());
                            advance(i); return;
                        }
                    }
                }
            }
            case BACKBEAK -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '\n' -> {
                        subProcessor = new FusDefinitionProcessor(this);
                        subProcessor.getReady();
                        $state.aimedAdd($state.raw(), State.INLINE_DEFINITION);
                        advance('>'); return;
                    }
                    case '<' -> $state.aimedAdd($state.raw(), State.BEAK);
                    default -> {
                        if (!Character.isWhitespace(i)) {
                            result.append(">");
                            $state.unset($state.raw());
                            advance(i); return;
                        }
                    }
                }
            }
            case AT -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '>' -> {
                        subProcessor = new FusDefinitionProcessor(this);
                        subProcessor.getReady();
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.INLINE_DEFINITION);
                        advance(i); return;
                    }
                    case '(' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.LAMBDA);
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.append("(");
                    }
                    case '+', '-', '~', '!', '*', '?' -> {
                        subProcessor = new FusDefinitionProcessor(this);
                        subProcessor.getReady();
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.DEFINITION);
                        advance(i); return;
                    }
                    default -> {
                        subProcessor = new FusyTypeProcessor(this);
                        subProcessor.getReady();
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.AT_TYPE);
                        $state.aimedAdd($state.raw(), State.FUSY_TYPE);
                        advance(i); return;
                    }
                }
            }
            case AT_TYPE -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '(' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.EXP_END);
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.append("((").append(token.toString()).append(")(");
                    }
                    case '@' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.EXP_END);
                        $state.aimedAdd($state.raw(), State.EXP_END);
                        $state.aimedAdd($state.raw(), State.AT);
                        result.append("((").append(token.toString()).append(")(");
                    }
                    default -> {
                        if (Character.isJavaIdentifierStart(i)) {
                            var fusDefinitionProcessor = new FusDefinitionProcessor(this);
                            fusDefinitionProcessor.getReady(token.toString());
                            subProcessor = fusDefinitionProcessor;
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.DEFINITION);
                            advance(i); return;
                        } else if(!Character.isWhitespace(i)) {
                            result.append(token.toString()).append(".class");
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.AFTER_ID);
                            advance(i); return;
                        }
                    }
                }
            }
            case HASH -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '(' -> {
                        result.append("new Suite.Mask(");
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                    }
                    case '[' -> {
                        result.append(".alter(");
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.IGNORE);
                        $state.aimedAdd($state.raw(), State.EXP_END);
                        $state.aimedAdd($state.raw(), State.STRHS_EXP);
                    }
                    case '.', ':' -> {
                        result.append("Suite.");
                        $state.unset($state.raw());
                    }
                    case '=' -> {
                        result.append(".reset(");
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.EXP_END);
                        $state.aimedAdd($state.raw(), State.LAMBDA_EXP);
                    }
                    default -> {
                        if (Character.isJavaIdentifierStart(i)) {
                            result.append("var ");
                            $state.unset($state.raw());
                            advance(i); return;
                        } else if (!Character.isWhitespace(i)) {
                            $state.unset($state.raw());
                            advance(i);
                            return;
                        }
                    }
                }
            }
            case LAMBDA -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '\n' -> {
                        result.append("->{");
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.SCOPE_END);
                        $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                    }
                    default -> {
                        if (Character.isWhitespace(i)) {
                            result.appendCodePoint(i);
                        } else {
                            result.append("->");
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.LAMBDA_EXP);
                            advance(i); return;
                        }
                    }
                }
            }
            case EXPRESSION -> {
                switch (i) {
                    case '"' -> {
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.STRING);
                        result.appendCodePoint(i);
                    }
                    case '\'' -> {
                        $state.aimedAdd($state.raw(), State.CHARACTER);
                        result.appendCodePoint(i);
                    }
                    case '(' -> {
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.appendCodePoint(i);
                    }
                    case ')' -> {
                        result.appendCodePoint(i);
                        $state.unset($state.raw());
                    }
                    case '@' -> $state.aimedAdd($state.raw(), State.AT);
                    case '#' -> $state.aimedAdd($state.raw(), State.HASH);
                    case '[' -> {
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append("$uite.$(");
                    }
                    case '!' -> $state.aimedAdd($state.raw(), State.EXCLAMATION);
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '<' -> $state.aimedAdd($state.raw(), State.BEAK);
                    case '>' -> $state.aimedAdd($state.raw(), State.BACKBEAK);
                    case '{' -> {
                        subProcessor = new FusyFunProcessor(this);
                        subProcessor.getReady();
                        $state.aimedAdd($state.raw(), State.FUSY_FUN);
                    }
                    default -> {
                        if (Character.isJavaIdentifierPart(i)) {
                            $state.aimedAdd($state.raw(), State.ID);
                            token = new StringBuilder();
                            advance(i); return;
                        } else {
                            result.appendCodePoint(i);
                        }
                    }
                }
            }
            case LAMBDA_EXP -> {
                switch (i) {
                    case '"' -> {
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.STRING);
                        result.appendCodePoint(i);
                    }
                    case '\'' -> {
                        $state.aimedAdd($state.raw(), State.CHARACTER);
                        result.appendCodePoint(i);
                    }
                    case '(' -> {
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.appendCodePoint(i);
                    }
                    case ')', ',', '\n', ']' -> {
                        $state.unset($state.raw());
                        advance(i); return;
                    }
                    case '#' -> $state.aimedAdd($state.raw(), State.HASH);
                    case '[' -> {
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append("$uite.$(");
                    }
                    case '@' -> $state.aimedAdd($state.raw(), State.AT);
                    case '!' -> $state.aimedAdd($state.raw(), State.EXCLAMATION);
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '<' -> $state.aimedAdd($state.raw(), State.BEAK);
                    case '>' -> $state.aimedAdd($state.raw(), State.BACKBEAK);
                    case '{' -> {
                        subProcessor = new FusyFunProcessor(this);
                        subProcessor.getReady();
                        $state.aimedAdd($state.raw(), State.FUSY_FUN);
                    }
                    default -> {
                        if (Character.isJavaIdentifierPart(i)) {
                            $state.aimedAdd($state.raw(), State.ID);
                            token = new StringBuilder();
                            advance(i); return;
                        } else {
                            result.appendCodePoint(i);
                        }
                    }
                }
            }
            case STRHS_EXP -> {
                switch (i) {
                    case '"' -> {
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.STRING);
                        result.appendCodePoint(i);
                    }
                    case '\'' -> {
                        $state.aimedAdd($state.raw(), State.CHARACTER);
                        result.appendCodePoint(i);
                    }
                    case '(' -> {
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.appendCodePoint(i);
                    }
                    case ']' -> {
                        $state.unset($state.raw());
                        advance(i);
                        return;
                    }
                    case '#' -> $state.aimedAdd($state.raw(), State.HASH);
                    case '[' -> {
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append("$uite.$(");
                    }
                    case '@' -> $state.aimedAdd($state.raw(), State.AT);
                    case '!' -> $state.aimedAdd($state.raw(), State.EXCLAMATION);
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '<' -> $state.aimedAdd($state.raw(), State.BEAK);
                    case '>' -> $state.aimedAdd($state.raw(), State.BACKBEAK);
                    case '{' -> {
                        subProcessor = new FusyFunProcessor(this);
                        subProcessor.getReady();
                        $state.aimedAdd($state.raw(), State.FUSY_FUN);
                    }
                    default -> {
                        if (Character.isJavaIdentifierPart(i)) {
                            $state.aimedAdd($state.raw(), State.ID);
                            token = new StringBuilder();
                            advance(i); return;
                        } else {
                            result.appendCodePoint(i);
                        }
                    }
                }
            }
            case EXP_END -> {
                result.append(")");
                $state.unset($state.raw());
                advance(i);
                return;
            }
            case ID -> {
                if (Character.isJavaIdentifierPart(i)) {
                    token.appendCodePoint(i);

                } else {
                    var str = token.toString().toLowerCase();
                    switch (str) {
                        case "return", "new" -> {
                            $state.unset($state.raw());
                            result.append(token);
                            advance(i);
                            return;
                        }
                        case "if", "switch", "while" -> {
                            $state.unset($state.raw());
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.SCOPE_EXP);
                            result.append(token);
                            advance(i); return;
                        }
                        case "for" -> {
                            $state.unset($state.raw());
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.FOR_SCOPE_EXP);
                            result.append(token);
                            advance(i); return;
                        }
                        case "catch" -> {
                            $state.unset($state.raw());
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.CATCH_SCOPE_EXP);
                            result.append(token);
                            advance(i); return;
                        }
                        case "sync" -> {
                            $state.unset($state.raw());
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.SCOPE_EXP);
                            result.append("synchronized");
                            advance(i); return;
                        }
                        case "do", "else", "try", "finally" -> {
                            $state.unset($state.raw());
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.SCOPE);
                            result.append(token).append("{");
                            advance(i); return;
                        }
                        case "extends" -> {
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.AFTER_ID);
                            $state.aimedAdd($state.raw(), State.INLINE_DEFINITION);
                            subProcessor = new FusDefinitionProcessor(this);
                            subProcessor.getReady();
                            subProcessor.advance('>');
                            advance(i);
                            return;
                        }
                        case "case" -> {
                            $state.unset($state.raw());
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.CASE_SCOPE);
                            result.append(token);
                            advance(i); return;
                        }
                        case "rest" -> {
                            $state.unset($state.raw());
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.SCOPE);
                            result.append("default->{");
                            advance(i); return;
                        }
                        case "elf" -> {
                            $state.unset($state.raw());
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.ELF_SCOPE);
                            advance(i); return;
                        }
                        case "drop" -> {
                            result.append("throw new FusyDrop(");
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.EXP_END);
                            $state.aimedAdd($state.raw(), State.LAMBDA_EXP);
                            advance(i); return;
                        }
                        default -> {
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.AFTER_ID);
                            result.append(token);
                            advance(i); return;
                        }
                    }
                }
            }
            case AFTER_ID -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '{' -> {
                        result.append("[");
                        $state.aimedAdd($state.raw(), State.ARRAY_END);
                        $state.aimedAdd($state.raw(), State.ARRAY_INDEX);
                    }
                    case '(' -> {
                        result.appendCodePoint(i);
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                    }
                    case '[' -> {
                        result.append(".alter($uite.$(");
                        $state.aimedAdd($state.raw(), State.EXP_END);
                        $state.aimedAdd($state.raw(), State.BT);
                        return;
                    }
                    case '#' -> $state.aimedAdd($state.raw(), State.AFTER_ID_HASH);
                    case ':' -> $state.aimedAdd($state.raw(), State.COLON);
                    default -> {
                        if (Character.isWhitespace(i) && i != '\n') {
                            result.appendCodePoint(i);
                        } else {
                            $state.unset($state.raw());
                            advance(i);
                            return;
                        }
                    }
                }
            }
            case AFTER_ID_HASH -> {
                $state.unset($state.raw());
                if(Character.isJavaIdentifierPart(i)) {
                    result.append(".in(");
                    token = new StringBuilder();
                    $state.aimedAdd($state.raw(), State.AFTER_BTI);
                    $state.aimedAdd($state.raw(), State.EXP_END);
                    $state.aimedAdd($state.raw(), State.BT_HASH_STRING);
                } else {
                    $state.aimedAdd($state.raw(), State.HASH);
                }
                advance(i);
                return;
            }
            case COLON -> {
                switch (i) {
                    case ':' -> {
                        result.append("::");
                        $state.unset($state.raw());
                    }
                    case '(' -> {
                        result.append(".apply(");
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                    }
                    case '[' -> {
                        result.append(".in(");
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.IGNORE);
                        $state.aimedAdd($state.raw(), State.EXP_END);
                        $state.aimedAdd($state.raw(), State.STRHS_EXP);
                    }
                    default -> {
                        if (Character.isJavaIdentifierPart(i)) {
                            result.append(".");
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.COLON_METHOD);
                        } else {
                            $state.unset($state.raw());
                        }
                        advance(i); return;
                    }
                }
            }
            case COLON_METHOD -> {
                if (Character.isJavaIdentifierPart(i)) {
                    result.appendCodePoint(i);
                } else if (i == '(') {
                    $state.unset($state.raw());
                    result.appendCodePoint('(');
                    $state.aimedAdd($state.raw(), State.EXPRESSION);
                } else {
                    result.append("()");
                    $state.unset($state.raw());
                    advance(i); return;
                }
            }
            case CLOSE_BRACE -> {
                result.appendCodePoint(')');
                $state.unset($state.raw());
                advance(i); return;
            }
            case STRING -> {
                switch (i) {
                    case '"' -> {
                        $state.unset($state.raw());
                        result.appendCodePoint(i);
                    }
                    case '\\' -> $state.aimedAdd($state.raw(), State.STR_BACKSLASH);
                    case '#' -> {
                        $state.aimedAdd($state.raw(), State.STRING_HASH);
                        result.append("\" + ");
                        token = new StringBuilder();
                    }
                    case '\n' -> $state.aimedAdd($state.raw(), State.STR_ENDLINE);
                    default -> result.appendCodePoint(i);
                }
            }
            case STRING_HASH -> {
                if (Character.isJavaIdentifierPart(i)) {
                    token.appendCodePoint(i);
                } else if (i == '[') {
                    result.append("(");
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.STRHS_END);
                    $state.aimedAdd($state.raw(), State.STRHS_EXP);
                } else {
                    $state.unset($state.raw());
                    result.append(token).append(" + \"");
                    advance(i); return;
                }
            }
            case STRHS_END -> {
                $state.unset($state.raw());
                result.append(") + \"");
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
                if (i == '#' || i == ' ') {
                    result.appendCodePoint(i);
                } else {
                    result.append("\\").appendCodePoint(i);
                }
            }
            case STR_ENDLINE -> {
                if (!Character.isWhitespace(i)) {
                    $state.unset($state.raw());
                    advance(i); return;
                }
            }
            case BACKSLASH -> {
                switch (i) {
                    case '\\' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.DOUBLE_BACKSLASH);
                    }
                    case '\n' -> $state.unset($state.raw());
                    case '<' -> {
                        $state.unset($state.raw());
                        advance('\n');
                        return;
                    }
                    default -> {
                        if (Character.isWhitespace(i)) {
                            result.appendCodePoint(i);
                        } else {
                            $state.unset($state.raw());
                            advance(';');
                            advance(i);
                            return;
                        }
                    }
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
                    advance(i); return;
                }
            }
            case BT -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '[' -> {
                        result.append("$uite.$(");
                        $state.in($state.raw()).reset(State.AFTER_BT);
                        $state.aimedAdd($state.raw(), State.BT);
                    }
                    case '#' -> {
                        $state.in($state.raw()).reset(State.AFTER_BT);
                        $state.aimedAdd($state.raw(), State.BT_HASH);
                    }
                    case ',' -> {
                        advance('[');
                        advance(']');
                    }
                    case ';' -> {
                        advance(']');
                        advance('[');
                    }
                    default -> {
                        if (!Character.isWhitespace(i)) {
                            $state.in($state.raw()).reset(State.BT_NEXT);
                            advance(i);
                            return;
                        }
                    }
                }
            }
            case BT_NEXT -> {
                switch (i) {
                    case '"' -> {
                        $state.aimedAdd($state.raw(), State.AFTER_BT_ID);
                        $state.aimedAdd($state.raw(), State.STRING);
                        result.appendCodePoint(i);
                    }
                    case '\'' -> {
                        $state.aimedAdd($state.raw(), State.CHARACTER);
                        result.appendCodePoint(i);
                    }
                    case '(' -> {
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.appendCodePoint(i);
                    }
                    case ']' -> {
                        $state.unset($state.raw());
                        result.append(")");
                    }
                    case '[' -> {
                        result.append(",$uite.$(");
                        $state.in($state.raw()).reset(State.AFTER_BT);
                        $state.aimedAdd($state.raw(), State.BT);
                    }
                    case '@' -> $state.aimedAdd($state.raw(), State.BT_AT);
                    case '#' -> {
                        result.append(",");
                        $state.in($state.raw()).reset(State.AFTER_BT);
                        $state.aimedAdd($state.raw(), State.BT_HASH);
                    }
                    case '!' -> $state.aimedAdd($state.raw(), State.EXCLAMATION);
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '<' -> $state.aimedAdd($state.raw(), State.BEAK);
                    case '>' -> $state.aimedAdd($state.raw(), State.BACKBEAK);
                    case '{' -> {
                        subProcessor = new FusyFunProcessor(this);
                        subProcessor.getReady();
                        $state.aimedAdd($state.raw(), State.FUSY_FUN);
                    }
                    case ',' -> {
                        advance('[');
                        advance(']');
                    }
                    case ';' -> {
                        advance(']');
                        advance('[');
                    }
                    default -> {
                        if (Character.isJavaIdentifierPart(i)) {
                            $state.aimedAdd($state.raw(), State.BT_ID);
                            token = new StringBuilder();
                            advance(i);
                            return;
                        } else if (!Character.isWhitespace(i)) {
                            result.appendCodePoint(i);
                        }
                    }
                }
            }
            case AFTER_BT -> {
                switch (i) {
                    case ']', '[', '#' -> {
                        $state.in($state.raw()).reset(State.BT_NEXT);
                        advance(i);
                        return;
                    }
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case ',' -> {
                        advance('[');
                        advance(']');
                    }
                    case ';' -> {
                        advance(']');
                        advance('[');
                    }
                    default -> {
                        if (!Character.isWhitespace(i)) {
                            result.append(",");
                            $state.in($state.raw()).reset(State.BT_NEXT);
                            advance(i);
                            return;
                        }
                    }
                }
            }
            case BT_ID -> {
                if (Character.isJavaIdentifierPart(i)) {
                    token.appendCodePoint(i);

                } else {
                    var str = token.toString().toLowerCase();
                    switch (str) {
                        case "new" -> {
                            $state.unset($state.raw());
                            result.append(token).append(" ");
                            advance(i);
                            return;
                        }
                        case "extends" -> {
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.AFTER_BT_ID);
                            $state.aimedAdd($state.raw(), State.INLINE_DEFINITION);
                            subProcessor = new FusDefinitionProcessor(this);
                            subProcessor.getReady();
                            subProcessor.advance('>');
                            advance(i);
                            return;
                        }
                        default -> {
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.AFTER_BT_ID);
                            result.append(token);
                            advance(i);
                            return;
                        }
                    }
                }
            }
            case AFTER_BT_ID -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '{' -> {
                        result.append("[");
                        $state.aimedAdd($state.raw(), State.ARRAY_END);
                        $state.aimedAdd($state.raw(), State.ARRAY_INDEX);
                    }
                    case '(' -> {
                        result.appendCodePoint(i);
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                    }
                    case '#' -> $state.aimedAdd($state.raw(), State.AFTER_ID_HASH);
                    case ':' -> $state.aimedAdd($state.raw(), State.COLON);
                    default -> {
                        if (Character.isWhitespace(i) && i != '\n') {
                            result.appendCodePoint(i);
                        } else {
                            $state.unset($state.raw());
                            advance(i);
                            return;
                        }
                    }
                }
            }
            case BT_HASH -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '[' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.IGNORE);
                        $state.aimedAdd($state.raw(), State.STRHS_EXP);
                    }
                    case '(' -> {
                        result.append("new Suite.Mask(");
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                    }
                    default -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.BT_HASH_STRING);
                        token = new StringBuilder();
                        advance(i);
                        return;
                    }
                }
            }
            case BT_AT -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '(' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.LAMBDA);
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.append("(");
                    }
                    default -> {
                        subProcessor = new FusyTypeProcessor(this);
                        subProcessor.getReady();
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.BT_AT_TYPE);
                        $state.aimedAdd($state.raw(), State.FUSY_TYPE);
                        advance(i); return;
                    }
                }
            }
            case BT_AT_TYPE -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '(' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.AFTER_BT_ID);
                        $state.aimedAdd($state.raw(), State.EXP_END);
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.append("((").append(token.toString()).append(")(");
                    }
                    case '@' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.AFTER_BT_ID);
                        $state.aimedAdd($state.raw(), State.EXP_END);
                        $state.aimedAdd($state.raw(), State.EXP_END);
                        $state.aimedAdd($state.raw(), State.AT);
                        result.append("((").append(token.toString()).append(")(");
                    }
                    default -> {
                        if (Character.isJavaIdentifierStart(i)) {
                            var fusDefinitionProcessor = new FusDefinitionProcessor(this);
                            fusDefinitionProcessor.getReady(token.toString());
                            subProcessor = fusDefinitionProcessor;
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.DEFINITION);
                            advance(i); return;
                        } else if(!Character.isWhitespace(i)) {
                            result.append(token.toString()).append(".class");
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.AFTER_BT_ID);
                            advance(i); return;
                        }
                    }
                }
            }
            case BT_HASH_STRING -> {
                if(Character.isJavaIdentifierPart(i)) {
                    token.appendCodePoint(i);
                } else if(i == '\\') {
                    $state.aimedAdd($state.raw(), State.BTHS_BACKSLASH);
                } else {
                    var str = token.toString();
                    result.append("\"").append(str).append("\"");
                    $state.unset($state.raw());
                    advance(i);
                    return;
                }
            }
            case BTHS_BACKSLASH -> {
                switch (i) {
                    case '[', ']', ',', ' ' -> {
                        token.appendCodePoint(i);
                        $state.unset($state.raw());
                    }
                    case 'd' -> {
                        token.append("\\\\");
                        $state.unset($state.raw());
                    }
                    case '\\', '\n' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.BACKSLASH);
                        advance(i);
                        return;
                    }
                    default -> {
                        token.append("\\").appendCodePoint(i);
                        $state.unset($state.raw());
                    }
                }
            }
            case AFTER_BTI -> {
                if(i == '\n' || i == ']' || i == ')' || i == '}' || i == ',' || i == ';') {
                    result.append(".get()");
                    $state.unset($state.raw());
                    advance(i);
                    return;
                } else if(Character.isWhitespace(i)) {
                    result.appendCodePoint(i);
                } else {
                    $state.unset($state.raw());
                    advance(i);
                    return;
                }
            }
            case ARRAY_INDEX -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '@' -> $state.aimedAdd($state.raw(), State.AT);
                    case '#' -> $state.aimedAdd($state.raw(), State.HASH);
                    case '[' -> {
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append("$uite.$(");
                    }
                    case '!' -> $state.aimedAdd($state.raw(), State.EXCLAMATION);
                    case '"' -> {
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.STRING);
                        result.appendCodePoint(i);
                    }
                    case '\'' -> {
                        $state.aimedAdd($state.raw(), State.CHARACTER);
                        result.appendCodePoint(i);
                    }
                    case '(' -> {
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.appendCodePoint(i);
                    }
                    case '}' -> $state.unset($state.raw());
                    case '{' -> {
                        subProcessor = new FusyFunProcessor(this);
                        subProcessor.getReady();
                        $state.aimedAdd($state.raw(), State.FUSY_FUN);
                    }
                    case ';' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.ARRAY_INIT);
                        result.append("]{");
                    }
                    default -> {
                        if (Character.isJavaIdentifierPart(i)) {
                            $state.aimedAdd($state.raw(), State.ID);
                            token = new StringBuilder();
                            advance(i); return;
                        } else {
                            result.appendCodePoint(i);
                        }
                    }
                }
            }
            case ARRAY_INIT -> {
                switch (i) {
                    case '"' -> {
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.STRING);
                        result.appendCodePoint(i);
                    }
                    case '\'' -> {
                        $state.aimedAdd($state.raw(), State.CHARACTER);
                        result.appendCodePoint(i);
                    }
                    case '(' -> {
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.appendCodePoint(i);
                    }
                    case '}' -> {
                        result.append("}");
                        $state.unset($state.raw());
                    }
                    case '#' -> $state.aimedAdd($state.raw(), State.HASH);
                    case '[' -> {
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append("$uite.$(");
                    }
                    case '@' -> $state.aimedAdd($state.raw(), State.AT);
                    case '!' -> $state.aimedAdd($state.raw(), State.EXCLAMATION);
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '<' -> $state.aimedAdd($state.raw(), State.BEAK);
                    case '>' -> $state.aimedAdd($state.raw(), State.BACKBEAK);
                    case '{' -> {
                        subProcessor = new FusyFunProcessor(this);
                        subProcessor.getReady();
                        $state.aimedAdd($state.raw(), State.FUSY_FUN);
                    }
                    default -> {
                        if (Character.isJavaIdentifierPart(i)) {
                            $state.aimedAdd($state.raw(), State.ID);
                            token = new StringBuilder();
                            advance(i); return;
                        } else {
                            result.appendCodePoint(i);
                        }
                    }
                }
            }
            case ARRAY_END -> {
                result.append("]");
                $state.unset($state.raw());
                advance(i);
                return;
            }
            case EXCLAMATION -> {
                if (i == '!') {
                    result.append(":");
                    $state.unset($state.raw());
                } else {
                    result.append("!");
                    $state.unset($state.raw());
                    advance(i);
                    return;
                }
            }
            case SCOPE_EXP -> {
                if (i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if (!Character.isWhitespace(i)) {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.NAKED_SCOPE_EXP_STAT);
                    $state.aimedAdd($state.raw(), State.LAMBDA_EXP);
                    result.append("(");
                    advance(i);
                    return;
                }
            }
            case SCOPE_EXP_STAT -> {
                $state.unset($state.raw());
                $state.aimedAdd($state.raw(), State.SCOPE_END);
                $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                result.append("{");
                advance(i); return;
            }
            case NAKED_SCOPE_EXP_STAT -> {
                $state.unset($state.raw());
                $state.aimedAdd($state.raw(), State.SCOPE_END);
                if(i == ',') {
                    $state.aimedAdd($state.raw(), State.STAT_END);
                    $state.aimedAdd($state.raw(), State.INLINE_STATEMENT);
                } else {
                    $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                }
                result.append("){");
            }
            case SCOPE_END -> {
                $state.unset($state.raw());
                result.append("}");
                advance(i);
                return;
            }
            case SCOPE -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '\n' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.SCOPE_END);
                        $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                        result.append("\n");
                    }
                    case ',' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.SCOPE_END);
                        $state.aimedAdd($state.raw(), State.STAT_END);
                        $state.aimedAdd($state.raw(), State.INLINE_STATEMENT);
                    }
                    default -> {
                        if (Character.isWhitespace(i)) {
                            result.appendCodePoint(i);
                        } else {
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.SCOPE_END);
                            $state.aimedAdd($state.raw(), State.STAT_END);
                            $state.aimedAdd($state.raw(), State.INLINE_STATEMENT);
                            advance(i); return;
                        }
                    }
                }
            }
            case FOR_SCOPE_EXP -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '#' -> {
                        result.append("(var ");
                        token = new StringBuilder();
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.FSE_ID);
                    }
                    default -> {
                        if (!Character.isWhitespace(i)) {
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.NAKED_SCOPE_EXP_STAT);
                            $state.aimedAdd($state.raw(), State.LAMBDA_EXP);
                            result.append("(");
                            advance(i);
                            return;
                        }
                    }
                }
            }
            case FSE_ID -> {
                if (Character.isJavaIdentifierPart(i)) {
                    token.appendCodePoint(i);
                } else {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.FSE_AFTER_ID);
                    advance(i);
                    return;
                }
            }
            case FSE_AFTER_ID -> {
                if (!Character.isWhitespace(i)) {
                    if (i == '=') {
                        result.append(token);
                    } else {
                        result.append(token).append(" : ");
                    }
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.NAKED_SCOPE_EXP_STAT);
                    $state.aimedAdd($state.raw(), State.LAMBDA_EXP);
                    advance(i);
                    return;
                }
            }
            case CATCH_SCOPE_EXP -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '#' -> {
                        result.append("(Throwable ");
                        token = new StringBuilder();
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.CSE_ID);
                    }
                    default -> {
                        if (!Character.isWhitespace(i)) {
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.NAKED_SCOPE_EXP_STAT);
                            $state.aimedAdd($state.raw(), State.LAMBDA_EXP);
                            result.append("(");
                            advance(i);
                            return;
                        }
                    }
                }
            }
            case CSE_ID -> {
                if (Character.isJavaIdentifierPart(i)) {
                    token.appendCodePoint(i);
                } else {
                    result.append(token);
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.NAKED_SCOPE_EXP_STAT);
                    $state.aimedAdd($state.raw(), State.LAMBDA_EXP);
                    advance(i);
                    return;
                }
            }
            case CASE_SCOPE -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '(' -> {
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.append("(");
                    }
                    case '"' -> {
                        $state.aimedAdd($state.raw(), State.STRING);
                        result.appendCodePoint(i);
                    }
                    case '\'' -> {
                        $state.aimedAdd($state.raw(), State.CHARACTER);
                        result.appendCodePoint(i);
                    }
                    case '\n', ',' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.CASE_SCOPE_STAT);
                        advance(i);
                        return;
                    }
                    case '|' -> result.append(",");
                    case '<' -> {
                        $state.unset($state.raw());
                        result.append("->{}");
                    }
                    default -> result.appendCodePoint(i);
                }
            }
            case CASE_SCOPE_STAT -> {
                if (i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                } else if (i == '\n') {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.SCOPE_END);
                    $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                    result.append("->{\n");
                } else if (i == ',') {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.SCOPE_END);
                    $state.aimedAdd($state.raw(), State.STAT_END);
                    $state.aimedAdd($state.raw(), State.INLINE_STATEMENT);
                    result.append("->{\n");
                } else if (Character.isWhitespace(i)) {
                    result.appendCodePoint(i);
                } else {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.SCOPE_END);
                    $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                    result.append("->{");
                    advance(i); return;
                }
            }
            case ELF_SCOPE -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '\n' -> {
                        result.append("else{");
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.SCOPE_END);
                        $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                    }
                    case ',' -> {
                        result.append("else{");
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.SCOPE_END);
                        $state.aimedAdd($state.raw(), State.STAT_END);
                        $state.aimedAdd($state.raw(), State.INLINE_STATEMENT);
                    }
                    default -> {
                        if (!Character.isWhitespace(i)) {
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.NAKED_SCOPE_EXP_STAT);
                            $state.aimedAdd($state.raw(), State.LAMBDA_EXP);
                            result.append("else if(");
                            advance(i); return;
                        }
                    }
                }
            }
        }
        if ($state.absent()) {
            parentProcessor.terminateSubProcess();
        }
    }

    public void terminateSubProcess() {
        switch ($state.in().as(State.class)) {
            case DEFINITION -> {
                var $ = subProcessor.finish();
                definitions.add($.in(FusDefinitionProcessor.Result.STATEMENTS).asString(""));
                imports.alter($.in(FusDefinitionProcessor.Result.IMPORTS));
                $state.unset($state.raw());
                if($state.in().as(State.class) == State.STATEMENT) {
                    $state.unset($state.raw());
                }
            }
            case INLINE_DEFINITION -> {
                var $ = subProcessor.finish();
                result.append($.in(FusDefinitionProcessor.Result.STATEMENTS).asString(""));
                imports.alter($.in(FusDefinitionProcessor.Result.IMPORTS));
                $state.unset($state.raw());
            }
            case FUSY_FUN -> {
                var $ = subProcessor.finish();
                String fun = $.in(FusyFunProcessor.Result.COMPLETE).asString();
                result.append(fun);
                $state.unset($state.raw());
            }
            case FUSY_TYPE -> {
                var $ = subProcessor.finish();
                String type = $.in(FusyTypeProcessor.Result.COMPLETE).asString();
                token = new StringBuilder(type);
                $state.unset($state.raw());
            }
        }
    }

    @Override
    public Subject finish() {
        if($state.present()) advance('\n');
        var defs = new StringBuilder();
        for(var def : definitions.eachIn().eachAs(String.class)) {
            defs.append(def);
        }
        var imps = new StringBuilder();
        for(var imp : imports.eachIn().eachAs(String.class)) {
            imps.append(imp);
        }
        return $(
                Result.STATEMENTS, $(result.toString()),
                Result.DEFINITIONS, $(defs.toString()),
                Result.IMPORTS, $(imps.toString())
        );
    }
}
