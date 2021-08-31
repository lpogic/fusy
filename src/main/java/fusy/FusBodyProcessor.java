package fusy;

import suite.suite.Subject;

import static suite.suite.$uite.$;

public class FusBodyProcessor extends FusProcessor {

    public enum State {
        EMPTY_STATEMENT, STATEMENT, EXPRESSION, ID, AFTER_ID, STRING, STRING_HASH, STRHS_END, STRHS_EXP, STR_ENDLINE,
        CHARACTER, STR_BACKSLASH, SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT, BT, BT_NEXT, AFTER_BT, BT_HASH,
        ARRAY_INDEX, ARRAY_INIT, COLON, COLON_METHOD, FOR_SCOPE_EXP, FSE_ID, EXP_END, EXCLAMATION, FSE_AFTER_ID,
        SCOPE_EXP, SCOPE, SCOPE_END, CASE_SCOPE, SCOPE_EXP_STAT, NAKED_SCOPE_EXP_STAT, CASE_SCOPE_STAT, ELF_SCOPE, NAKED_EXP,
        BACKSLASH, CLOSE_BRACE, DEFINITION, INLINE_DEFINITION, DOUBLE_BACKSLASH, MLC_BACKSLASH, MLC_DOUBLE_BACKSLASH,
        AT, AT_TYPE, LAMBDA, LAMBDA_EXP, BEAK, BACKBEAK, FUSY_FUN, FUSY_TYPE, HASH, BT_HASH_STRING, BTHS_BACKSLASH
    }

    enum Result {
        STATEMENTS, DEFINITIONS
    }

    StringBuilder result;
    StringBuilder token;
    FusProcessor parentProcessor;
    FusProcessor subProcessor;
    Subject definitions;

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
        $state = $($(initialState));
    }

    @Override
    public void advance(int i) {
        switch ($state.in().as(State.class)) {
            case EMPTY_STATEMENT -> {
                switch (i) {
                    case '<' -> $state.unset($state.raw());
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    default -> {
                        if (Character.isWhitespace(i)) {
                            result.appendCodePoint(i);
                        } else {
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.STATEMENT);
                            advance(i);
                        }
                    }
                }
            }
            case DEFINITION, INLINE_DEFINITION, FUSY_FUN, FUSY_TYPE -> subProcessor.advance(i);
            case STATEMENT -> {
                switch (i) {
                    case '"' -> {
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
                    case '@' -> $state.aimedAdd($state.raw(), State.AT);
                    case '#' -> $state.aimedAdd($state.raw(), State.HASH);
                    case '!' -> $state.aimedAdd($state.raw(), State.EXCLAMATION);
                    case '\n' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                        result.append(";\n");
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
            case BEAK -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '\n' -> {
                        $state.unset($state.raw());
                        advance('\n');
                        advance('<');
                    }
                    case '<' -> $state.aimedAdd($state.raw(), State.BEAK);
                    default -> {
                        if (Character.isWhitespace(i)) {
                            result.appendCodePoint(i);
                        } else {
                            result.append("<");
                            $state.unset($state.raw());
                            advance(i);
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
                        advance('>');
                    }
                    case '<' -> $state.aimedAdd($state.raw(), State.BEAK);
                    default -> {
                        if (!Character.isWhitespace(i)) {
                            result.append(">");
                            $state.unset($state.raw());
                            advance(i);
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
                        advance(i);
                    }
                    case '.', ':', '!', '^' -> {
                        subProcessor = new FusDefinitionProcessor(this);
                        subProcessor.getReady();
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.DEFINITION);
                        advance(i);
                    }
                    default -> {
                        subProcessor = new FusyTypeProcessor(this);
                        subProcessor.getReady();
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.AT_TYPE);
                        $state.aimedAdd($state.raw(), State.FUSY_TYPE);
                        advance(i);
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
                    case '#' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.AFTER_ID);
                        $state.aimedAdd($state.raw(), State.EXP_END);
                        $state.aimedAdd($state.raw(), State.EXP_END);
                        $state.aimedAdd($state.raw(), State.HASH);
                        result.append("((").append(token.toString()).append(")(");
                    }
                    default -> {
                        if (!Character.isWhitespace(i)) {
                            var fusDefinitionProcessor = new FusDefinitionProcessor(this);
                            fusDefinitionProcessor.getReady(token.toString());
                            subProcessor = fusDefinitionProcessor;
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.DEFINITION);
                            advance(i);
                        }
                    }
                }
            }
            case HASH -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '[' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append("$uite.$(");
                    }
                    case '(' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.LAMBDA);
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.append("(");
                    }
                    case '.' -> {
                        result.append("Suite.");
                        $state.unset($state.raw());
                    }
                    default -> {
                        if (Character.isJavaIdentifierStart(i)) {
                            result.append("var ");
                            $state.unset($state.raw());
                            advance(i);
                        } else if (!Character.isWhitespace(i)) {
                            $state.unset($state.raw());
                            advance(i);
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
                            advance(i);
                        }
                    }
                }
            }
            case EXPRESSION -> {
                switch (i) {
                    case '"' -> {
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
                        $state.unset($state.raw());
                        result.appendCodePoint(i);
                    }
                    case '@' -> $state.aimedAdd($state.raw(), State.AT);
                    case '#' -> $state.aimedAdd($state.raw(), State.HASH);
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
                            advance(i);
                        } else {
                            result.appendCodePoint(i);
                        }
                    }
                }
            }
            case NAKED_EXP -> {
                switch (i) {
                    case '"' -> {
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
                    case ')' -> $state.unset($state.raw());
                    case '#' -> $state.aimedAdd($state.raw(), State.HASH);
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
                            advance(i);
                        } else {
                            result.appendCodePoint(i);
                        }
                    }
                }
            }
            case LAMBDA_EXP -> {
                switch (i) {
                    case '"' -> {
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
                    case ')', ',', '\n', '[', ']' -> {
                        $state.unset($state.raw());
                        advance(i);
                    }
                    case '#' -> $state.aimedAdd($state.raw(), State.HASH);
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
                            advance(i);
                        } else {
                            result.appendCodePoint(i);
                        }
                    }
                }
            }
            case STRHS_EXP -> {
                switch (i) {
                    case '"' -> {
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
                    }
                    case '#' -> $state.aimedAdd($state.raw(), State.HASH);
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
                            advance(i);
                        } else {
                            result.appendCodePoint(i);
                        }
                    }
                }
            }
            case EXP_END -> {
                $state.unset($state.raw());
                result.append(")");
                advance(i);
            }
            case ID -> {
                if (Character.isJavaIdentifierPart(i)) {
                    token.appendCodePoint(i);

                } else {
                    var str = token.toString().toLowerCase();
                    switch (str) {
                        case "return" -> {
                            $state.unset($state.raw());
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.STATEMENT);
                            result.append(token);
                            advance(i);
                        }
                        case "if", "switch", "catch", "while" -> {
                            $state.unset($state.raw());
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                            $state.aimedAdd($state.raw(), State.SCOPE_EXP);
                            result.append(token);
                            advance(i);
                        }
                        case "for" -> {
                            $state.unset($state.raw());
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                            $state.aimedAdd($state.raw(), State.FOR_SCOPE_EXP);
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
                        case "rest" -> {
                            $state.unset($state.raw());
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                            $state.aimedAdd($state.raw(), State.SCOPE);
                            result.append("default->{");
                            advance(i);
                        }
                        case "elf" -> {
                            $state.unset($state.raw());
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                            $state.aimedAdd($state.raw(), State.ELF_SCOPE);
                            advance(i);
                        }
                        case "final", "static", "public", "private", "protected" -> {
                            $state.unset($state.raw());
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
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '{' -> {
                        $state.aimedAdd($state.raw(), State.ARRAY_INDEX);
                        result.append("[");
                    }
                    case '(' -> {
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.appendCodePoint(i);
                    }
                    case '@' -> result.append(".class");
                    case '!' -> $state.aimedAdd($state.raw(), State.EXCLAMATION);
                    case ':' -> $state.aimedAdd($state.raw(), State.COLON);
                    default -> {
                        if (Character.isWhitespace(i) && i != '\n') {
                            result.appendCodePoint(i);
                        } else {
                            $state.unset($state.raw());
                            advance(i);
                        }
                    }
                }
            }
            case COLON -> {
                switch (i) {
                    case ':' -> {
                        result.append("::");
                        $state.unset($state.raw());
                    }
                    case '!' -> {
                        result.append(".apply()");
                        $state.unset($state.raw());
                    }
                    default -> {
                        if (Character.isJavaIdentifierPart(i)) {
                            result.append(".");
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.COLON_METHOD);
                        } else {
                            $state.unset($state.raw());
                        }
                        advance(i);
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
                    advance(i);
                }
            }
            case CLOSE_BRACE -> {
                result.appendCodePoint(')');
                $state.unset($state.raw());
                advance(i);
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
                    advance(i);
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
                    advance(i);
                }
            }
            case BACKSLASH -> {
                if (i == '\\') {
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
            case BT -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '[' -> {
                        $state.in($state.raw()).reset(State.AFTER_BT);
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append("$uite.$(");
                    }
                    case ',' -> {
                        advance('[');
                        advance(']');
                    }
                    default -> {
                        if (!Character.isWhitespace(i)) {
                            $state.in($state.raw()).reset(State.BT_NEXT);
                            advance(i);
                        }
                    }
                }
            }
            case BT_NEXT -> {
                switch (i) {
                    case '"' -> {
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
                        $state.in($state.raw()).reset(State.AFTER_BT);
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append(",$uite.$(");
                    }
                    case '@' -> $state.aimedAdd($state.raw(), State.AT);
                    case '#' -> $state.aimedAdd($state.raw(), State.BT_HASH);
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
                    default -> {
                        if (Character.isJavaIdentifierPart(i)) {
                            $state.aimedAdd($state.raw(), State.ID);
                            token = new StringBuilder();
                            advance(i);
                        } else if (!Character.isWhitespace(i)) {
                            result.appendCodePoint(i);
                        }
                    }
                }
            }
            case AFTER_BT -> {
                switch (i) {
                    case ']', '[' -> {
                        $state.in($state.raw()).reset(State.BT_NEXT);
                        advance(i);
                    }
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case ',' -> {
                        advance('[');
                        advance(']');
                    }
                    default -> {
                        if (!Character.isWhitespace(i)) {
                            result.append(",");
                            $state.in($state.raw()).reset(State.BT_NEXT);
                            advance(i);
                        }
                    }
                }
            }
            case BT_HASH -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '[' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.BT);
                        result.append("$uite.$(");
                    }
                    default -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.BT_HASH_STRING);
                        token = new StringBuilder();
                        advance(i);
                    }
                }
            }
            case BT_HASH_STRING -> {
                switch (i) {
                    case '[', ']' -> {
                        var str = token.toString().replaceFirst("$\s+\n", "").stripTrailing();
                        result.append("\"\"\"\n").append(str).append("\"\"\"");
                        $state.unset($state.raw());
                        advance(i);
                    }
                    case '\\' -> $state.aimedAdd($state.raw(), State.BTHS_BACKSLASH);
                    case ',' -> {
                        advance('[');
                        advance(']');
                    }
                    default -> token.appendCodePoint(i);
                }
            }
            case BTHS_BACKSLASH -> {
                switch (i) {
                    case '[', ']', ',' -> {
                        token.appendCodePoint(i);
                        $state.unset($state.raw());
                    }
                    case '\\' -> {
                        token.append("\\\\\\\\");
                        $state.unset($state.raw());
                    }
                    default -> {
                        token.append("\\\\").appendCodePoint(i);
                        $state.unset($state.raw());
                    }
                }
            }
            case ARRAY_INDEX -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '@' -> $state.aimedAdd($state.raw(), State.AT);
                    case '#' -> $state.aimedAdd($state.raw(), State.BT_HASH);
                    case '!' -> $state.aimedAdd($state.raw(), State.EXCLAMATION);
                    case '"' -> {
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
                        $state.unset($state.raw());
                        result.append("]");
                    }
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
                            advance(i);
                        } else {
                            result.appendCodePoint(i);
                        }
                    }
                }
            }
            case ARRAY_INIT -> {
                switch (i) {
                    case '"' -> {
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
                            advance(i);
                        } else {
                            result.appendCodePoint(i);
                        }
                    }
                }
            }
            case EXCLAMATION -> {
                switch (i) {
                    case '(' -> {
                        result.append(".apply(");
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                    }
                    case '!' -> {
                        result.append(":");
                        $state.unset($state.raw());
                    }
                    default -> {
                        result.append("!");
                        $state.unset($state.raw());
                        advance(i);
                    }
                }
            }
            case SCOPE_EXP -> {
                if (i == '\\') {
                    $state.aimedAdd($state.raw(), State.BACKSLASH);
                }
                if (i == '(') {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.SCOPE_EXP_STAT);
                    $state.aimedAdd($state.raw(), State.EXPRESSION);
                    result.append("(");
                } else if (!Character.isWhitespace(i)) {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.NAKED_SCOPE_EXP_STAT);
                    $state.aimedAdd($state.raw(), State.LAMBDA_EXP);
                    result.append("(");
                    advance(i);
                }
            }
            case SCOPE_EXP_STAT -> {
                $state.unset($state.raw());
                $state.aimedAdd($state.raw(), State.SCOPE_END);
                $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                result.append("{");
                advance(i);
            }
            case NAKED_SCOPE_EXP_STAT -> {
                $state.unset($state.raw());
                $state.aimedAdd($state.raw(), State.SCOPE_END);
                $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                result.append("){");
            }
            case SCOPE_END -> {
                $state.unset($state.raw());
                result.append("}");
                advance(i);
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
                        $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                    }
                    default -> {
                        if (Character.isWhitespace(i)) {
                            result.appendCodePoint(i);
                        } else {
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.SCOPE_END);
                            $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                            advance(i);
                        }
                    }
                }
            }
            case FOR_SCOPE_EXP -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '(' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.SCOPE_EXP_STAT);
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                        result.append("(");
                    }
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
                }
            }
            case FSE_AFTER_ID -> {
                if (!Character.isWhitespace(i)) {
                    if (Character.isJavaIdentifierStart(i)) {
                        result.append(token).append(" : ");
                    } else {
                        result.append(token);
                    }
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.NAKED_SCOPE_EXP_STAT);
                    $state.aimedAdd($state.raw(), State.LAMBDA_EXP);
                    advance(i);
                }
            }
            case CASE_SCOPE -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '(' -> {
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.CASE_SCOPE_STAT);
                        $state.aimedAdd($state.raw(), State.NAKED_EXP);
                        result.append(" ");
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
                        advance('\n');
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
                } else if (Character.isWhitespace(i)) {
                    result.appendCodePoint(i);
                } else {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.SCOPE_END);
                    $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                    result.append("->{");
                    advance(i);
                }
            }
            case ELF_SCOPE -> {
                switch (i) {
                    case '\\' -> $state.aimedAdd($state.raw(), State.BACKSLASH);
                    case '(' -> {
                        result.append("else if(");
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.SCOPE_EXP_STAT);
                        $state.aimedAdd($state.raw(), State.EXPRESSION);
                    }
                    case ',', '\n' -> {
                        result.append("else{");
                        $state.unset($state.raw());
                        $state.aimedAdd($state.raw(), State.SCOPE_END);
                        $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                    }
                    default -> {
                        if (!Character.isWhitespace(i)) {
                            $state.unset($state.raw());
                            $state.aimedAdd($state.raw(), State.NAKED_SCOPE_EXP_STAT);
                            $state.aimedAdd($state.raw(), State.LAMBDA_EXP);
                            result.append("else if(");
                            advance(i);
                        }
                    }
                }
            }
        }
        if ($state.absent() && parentProcessor != null) {
            parentProcessor.terminateSubProcess();
        }
    }

    public void terminateSubProcess() {
        switch ($state.in().as(State.class)) {
            case DEFINITION -> {
                var $ = subProcessor.finish();
                String def = $.in(FusDefinitionProcessor.Result.COMPLETE).asString();
                definitions.add(def);
                $state.unset($state.raw());
                if($state.in().as(State.class) == State.STATEMENT) {
                    $state.unset($state.raw());
                    $state.aimedAdd($state.raw(), State.EMPTY_STATEMENT);
                }
            }
            case INLINE_DEFINITION -> {
                var $ = subProcessor.finish();
                String def = $.in(FusDefinitionProcessor.Result.COMPLETE).asString();
                result.append(def);
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
