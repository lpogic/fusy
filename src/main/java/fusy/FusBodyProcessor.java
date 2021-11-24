package fusy;

import suite.suite.Subject;

import java.util.Stack;

import static suite.suite.$uite.$;

public class FusBodyProcessor extends FusProcessor {

    public enum State {
        DISCARD, TERMINATED, EMPTY_STATEMENT, STATEMENT, EXPRESSION, ID, AFTER_ID, STRING, STRING_HASH, STRHS_END, CB_EXP, RB_EXP, STR_ENDLINE,
        CHARACTER, STR_BACKSLASH, SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT, BT, BT_NEXT, AFTER_BT, BT_ID, BT_HASH, STAT_END,
        ARRAY_INDEX, ARRAY_INIT, ARRAY_INIT_BETWEEN, ARRAY_END, COLON, DOUBLE_COLON, COLON_METHOD, FOR_SCOPE_EXP, FSE_ID, EXP_END, FSE_AFTER_ID,
        SCOPE_EXP, SCOPE, SCOPE_END, CASE_SCOPE, NAKED_SCOPE_EXP_STAT, CASE_SCOPE_STAT, ELF_SCOPE, EMPTY_INLINE_STATEMENT, INLINE_STATEMENT,
        BACKSLASH, CLOSE_BRACE, DEFINITION, INLINE_DEFINITION, DOUBLE_BACKSLASH, MLC_BACKSLASH, MLC_DOUBLE_BACKSLASH,
        ES_AT, AT, AT_TYPE, AT_CAST, LAMBDA, LAMBDA_EXP, BEAK, BACKBEAK, FUSY_FUN, FUSY_TYPE, HASH, DOLLAR_STRING,
        ES_PLUS, ES_MINUS, EXCLAMATION, CATCH, CSE_ID, AFTER_BTI, HASH_ID, STATEMENT_VARIABLE,
        TRY_ID, TRY_SCOPE_EXP, DOLLAR, DOLLAR_BRACE_STRING, DBS_END, TYPED_VAR, EXTENDS_AFTER_TYPE, EXTENDS_AFTER_EXP,
        ASSIGN_EXP, DOT, NEW, NEW_ARRAY, AFTER_NEW_ARRAY, NEW_AFTER_TYPE, AT_QUESTION, AFTER_ATQ, AFTER_ATQ_ID, INSTANCEOF,
        CATCH_VAR_ID
    }

    enum Result {
        STATEMENTS, DEFINITIONS, IMPORTS, SETUP
    }

    final int DOLLAR_SUBSTITUTE = 0x20AC;

    Stack<State> state;
    StringBuilder result;
    StringBuilder token;
    int autoVarInsert;
    StringBuilder buffer;
    FusProcessor parentProcessor;
    FusProcessor subProcessor;
    Subject definitions;
    Subject imports;
    String setup;

    public FusBodyProcessor(FusProcessor parentProcessor) {
        this.parentProcessor = parentProcessor;
    }

    @Override
    public void getReady() {
        getReady(State.EMPTY_STATEMENT);
    }

    public void getReady(State initialState) {
        result = new StringBuilder();
        definitions = $();
        imports = $();
        state = new Stack<>();
        state.push(initialState);
        autoVarInsert = 0;
        buffer = new StringBuilder();
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
        if(!state.isEmpty()) {
            switch (state.peek()) {
                case DISCARD -> state.pop();
                case TERMINATED -> parentProcessor.advance(i);
                case EMPTY_STATEMENT -> {
                    autoVarInsert = result.length();
                    switch (i) {
                        case '<' -> state.pop();
                        case '>' -> {
                            result.append("return ");
                            state.push(State.STATEMENT);
                        }
                        case '\\' -> state.push(State.BACKSLASH);
                        case '@' -> {
                            state.push(State.STATEMENT);
                            state.push(State.ES_AT);
                        }
                        case '~', '!', '*', '?', '.' -> {
                            state.push(State.STATEMENT);
                            state.push(State.ES_AT);
                            return advance(i);
                        }
                        case '+' -> state.push(State.ES_PLUS);
                        case '-' -> state.push(State.ES_MINUS);
                        case '(' -> {
                            result.append("idle");
                            state.push(State.STATEMENT);
                            return advance(i);
                        }
                        default -> {
                            if (Character.isWhitespace(i)) {
                                result.appendCodePoint(i);
                            } else {
                                state.push(State.STATEMENT);
                                return advance(i);
                            }
                        }
                    }
                }
                case ES_PLUS -> {
                    if (i == '+') {
                        state.pop();
                        state.push(State.STATEMENT);
                        advance(i);
                        return advance(i);
                    } else {
                        state.pop();
                        state.push(State.STATEMENT);
                        state.push(State.ES_AT);
                        advance('+');
                        return advance(i);
                    }
                }
                case ES_MINUS -> {
                    if (i == '-') {
                        state.pop();
                        state.push(State.STATEMENT);
                        advance('-');
                        return advance(i);
                    } else {
                        state.pop();
                        state.push(State.STATEMENT);
                        state.push(State.ES_AT);
                        advance('-');
                        return advance(i);
                    }
                }
                case DEFINITION, INLINE_DEFINITION, FUSY_FUN, FUSY_TYPE, STATEMENT_VARIABLE, NEW_ARRAY,
                        INSTANCEOF -> subProcessor.advance(i);
                case STATEMENT -> {
                    switch (i) {
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '\'' -> {
                            state.push(State.CHARACTER);
                            result.appendCodePoint(i);
                        }
                        case '(' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                            result.appendCodePoint(i);
                        }
                        case '@' -> state.push(State.AT);
                        case '#' -> state.push(State.HASH);
                        case '$' -> state.push(State.DOLLAR);
                        case '[' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.BT);
                            result.append("$uite.$(");
                        }
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\n' -> {
                            result.append(";\n");
                            state.pop();
                        }
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '>' -> state.push(State.BACKBEAK);
                        case '{' -> {
                            subProcessor = new FusyFunProcessor(this);
                            subProcessor.getReady();
                            state.push(State.FUSY_FUN);
                        }
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                state.push(State.ID);
                                token = new StringBuilder();
                                return advance(i);
                            } else {
                                result.appendCodePoint(i);
                            }
                        }
                    }
                }
                case EMPTY_INLINE_STATEMENT -> {
                    autoVarInsert = result.length();
                    switch (i) {
                        case '>' -> {
                            result.append("return ");
                            state.pop();
                            state.push(State.INLINE_STATEMENT);
                        }
                        case ';' -> result.appendCodePoint(i);
                        default -> {
                            state.pop();
                            state.push(State.INLINE_STATEMENT);
                            return advance(i);
                        }
                    }
                }
                case INLINE_STATEMENT -> {
                    switch (i) {
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '\'' -> {
                            state.push(State.CHARACTER);
                            result.appendCodePoint(i);
                        }
                        case '(' -> {
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                            result.appendCodePoint(i);
                        }
                        case '\n' -> {
                            state.pop();
                            return advance(i);
                        }
                        case ';' -> {
                            state.pop();
                            state.push(State.EMPTY_INLINE_STATEMENT);
                            return advance(i);
                        }
                        case '#' -> state.push(State.HASH);
                        case '$' -> state.push(State.DOLLAR);
                        case '[' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.BT);
                            result.append("$uite.$(");
                        }
                        case '@' -> state.push(State.AT);
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '>' -> state.push(State.BACKBEAK);
                        case '{' -> {
                            subProcessor = new FusyFunProcessor(this);
                            subProcessor.getReady();
                            state.push(State.FUSY_FUN);
                        }
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                state.push(State.ID);
                                token = new StringBuilder();
                                return advance(i);
                            } else {
                                result.appendCodePoint(i);
                            }
                        }
                    }
                }
                case STAT_END -> {
                    result.append(";");
                    state.pop();
                    return advance(i);
                }
                case BEAK -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '\n', ';' -> {
                            state.pop();
                            var debugger = getDebugger();
                            debugger.advance('\n', false);
                            debugger.advance('<', false);
                            debugger.advance('\n');
                        }
                        case ')', ']', '}', ',' -> {
                            state.pop();
                            var debugger = getDebugger();
                            debugger.advance('\n', false);
                            debugger.advance('<', false);
                            debugger.advance(i);
                        }
                        case '<' -> state.push(State.BEAK);
                        default -> {
                            if (Character.isWhitespace(i)) {
                                result.appendCodePoint(i);
                            } else {
                                result.append("<");
                                state.pop();
                                return advance(i);
                            }
                        }
                    }
                }
                case ES_AT -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '+', '-', '~', '!', '*', '?', '/' -> {
                            subProcessor = new FusDefinitionProcessor(this);
                            subProcessor.getReady();
                            state.pop();
                            state.push(State.DEFINITION);
                            return advance(i);
                        }
                        default -> {
                            if(i == '{' || Character.isJavaIdentifierStart(i)) {
                                subProcessor = new FusyTypeProcessor(this);
                                subProcessor.getReady();
                                state.pop();
                                state.push(State.AT_TYPE);
                                state.push(State.FUSY_TYPE);
                                return advance(i);
                            } else if(i == '\n' || !Character.isWhitespace(i)) {
                                result.append("fusy.this");
                                state.pop();
                                state.push(State.AFTER_ID);
                                return advance(i);
                            }
                        }
                    }
                }
                case AT -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '(' -> {
                            state.pop();
                            state.push(State.LAMBDA);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                            result.append("(");
                        }
                        case '?' -> {
                            subProcessor = new FusyTypeProcessor(this);
                            subProcessor.getReady();
                            state.pop();
                            state.push(State.AT_QUESTION);
                            state.push(State.FUSY_TYPE);
                        }
                        default -> {
                            if(i == '{' || Character.isJavaIdentifierStart(i)) {
                                subProcessor = new FusyTypeProcessor(this);
                                subProcessor.getReady();
                                state.pop();
                                state.push(State.AT_TYPE);
                                state.push(State.FUSY_TYPE);
                                return advance(i);
                            } else if(i == '\n' || !Character.isWhitespace(i)) {
                                result.append("fusy.this");
                                state.pop();
                                state.push(State.AFTER_ID);
                                return advance(i);
                            }
                        }
                    }
                }
                case AT_TYPE -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '(' -> {
                            result.append("((").append(token.toString()).append(")(");
                            state.pop();
                            state.push(State.AT_CAST);
                        }
                        case '$' -> {
                            state.pop();
                            state.push(State.AFTER_ID);
                            state.push(State.EXP_END);
                            state.push(State.EXP_END);
                            state.push(State.DOLLAR);
                            result.append("((").append(token.toString()).append(")(");
                        }
                        default -> {
                            if (Character.isJavaIdentifierStart(i)) {
                                var fusDefinitionProcessor = new FusDefinitionProcessor(this);
                                fusDefinitionProcessor.getReady(token.toString());
                                subProcessor = fusDefinitionProcessor;
                                state.pop();
                                state.push(State.DEFINITION);
                                return advance(i);
                            } else if (!Character.isWhitespace(i)) {
                                result.append(token.toString()).append(".class");
                                state.pop();
                                state.push(State.AFTER_ID);
                                return advance(i);
                            }
                        }
                    }
                }
                case AT_CAST -> {
                    if (i == ')') {
                        result.append("null))");
                        state.pop();
                        state.push(State.AFTER_ID);
                    } else if (!Character.isWhitespace(i)) {
                        state.pop();
                        state.push(State.AFTER_ID);
                        state.push(State.EXP_END);
                        state.push(State.DISCARD);
                        state.push(State.EXP_END);
                        state.push(State.EXPRESSION);
                        return advance(i);
                    }
                }
                case AT_QUESTION -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '(' -> {
                            var fusBodyProcessor = new FusBodyProcessor(this);
                            fusBodyProcessor.getReady(State.EXPRESSION);
                            subProcessor = fusBodyProcessor;
                            state.pop();
                            state.push(State.AFTER_ATQ);
                            state.push(State.DISCARD);
                            state.push(State.INSTANCEOF);
                        }
                        default -> {
                            if (!Character.isWhitespace(i)) {
                                state.pop();
                                return advance(i);
                            }
                        }
                    }
                }
                case AFTER_ATQ -> {
                    if(i == '#') {
                        state.pop();
                        state.push(State.AFTER_ATQ_ID);
                    } else if(!Character.isWhitespace(i) || i == '\n') {
                        state.pop();
                        return advance(i);
                    }
                }
                case AFTER_ATQ_ID -> {
                    if(Character.isJavaIdentifierPart(i)) {
                        result.appendCodePoint(i);
                    } else {
                        state.pop();
                        return advance(i);
                    }
                }
                case HASH -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '(' -> {
                            result.append("new Suite.Mask(");
                            state.pop();
                            state.push(State.AFTER_ID);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                        }
                        case ':' -> {
                            result.append("Suite.");
                            state.pop();
                        }
                        case '=' -> {
                            result.append(".alter(");
                            state.pop();
                            state.push(State.EXP_END);
                            state.push(State.ASSIGN_EXP);
                        }
                        default -> {
                            if (Character.isJavaIdentifierStart(i)) {
                                state.pop();
                                state.push(State.HASH_ID);
                                token = new StringBuilder();
                                return advance(i);
                            } else if (!Character.isWhitespace(i)) {
                                state.pop();
                                return advance(i);
                            }
                        }
                    }
                }
                case HASH_ID -> {
                    switch (i) {
                        case '{' -> {
                            result.append(token);
                            buffer.append("var ").append(token).append(" = (");
                            var fusBodyProcessor = new FusBodyProcessor(this);
                            fusBodyProcessor.getReady(State.CB_EXP);
                            subProcessor = fusBodyProcessor;
                            state.pop();
                            state.push(State.AFTER_ID);
                            state.push(State.STATEMENT_VARIABLE);
                        }
                        case '@' -> {
                            buffer = new StringBuilder(token);
                            subProcessor = new FusyTypeProcessor(this);
                            subProcessor.getReady();
                            state.pop();
                            state.push(State.TYPED_VAR);
                            state.push(State.FUSY_TYPE);
                        }
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                token.appendCodePoint(i);
                            } else if (!Character.isWhitespace(i)) {
                                result.append("var ").append(token);
                                state.pop();
                                return advance(i);
                            }
                        }
                    }
                }
                case TYPED_VAR -> {
                    result.append(token).append(" ").append(buffer);
                    state.pop();
                    return advance(i);
                }
                case DOLLAR -> {
                    switch (i) {
                        case '{' -> {
                            result.append("\"");
                            state.pop();
                            state.push(State.DBS_END);
                            state.push(State.DOLLAR_BRACE_STRING);
                        }
                        case '$' -> {
                            state.pop();
                            return advance(DOLLAR_SUBSTITUTE);
                        }
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                token = new StringBuilder();
                                state.pop();
                                state.push(State.DOLLAR_STRING);
                                return advance(i);
                            }
                        }
                    }
                }
                case DOLLAR_BRACE_STRING -> {
                    switch (i) {
                        case '{' -> state.push(State.DOLLAR_BRACE_STRING);
                        case '}' -> state.pop();
                        case '\\' -> result.append("\\\\");
                        default -> result.appendCodePoint(i);
                    }
                }
                case DBS_END -> {
                    result.append("\"");
                    state.pop();
                    return advance(i);
                }
                case LAMBDA -> {
                    result.append("->");
                    state.pop();
                    state.push(State.INLINE_DEFINITION);
                    subProcessor = new FusDefinitionProcessor(this);
                    subProcessor.getReady();
                    subProcessor.advance('>');
                    return advance(i);
//                    state.pop();
//                    state.push(State.SCOPE_END);
//                    state.push(State.EMPTY_STATEMENT);
//                    return advance(i);
                }
                case EXPRESSION -> {
                    switch (i) {
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '\'' -> {
                            state.push(State.CHARACTER);
                            result.appendCodePoint(i);
                        }
                        case '(' -> {
                            state.push(State.EXPRESSION);
                            result.appendCodePoint(i);
                        }
                        case ')' -> {
                            state.pop();
                            return advance(i);
                        }
                        case '@' -> state.push(State.AT);
                        case '#' -> state.push(State.HASH);
                        case '$' -> state.push(State.DOLLAR);
                        case '[' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.BT);
                            result.append("$uite.$(");
                        }
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '>' -> state.push(State.BACKBEAK);
                        case '{' -> {
                            subProcessor = new FusyFunProcessor(this);
                            subProcessor.getReady();
                            state.push(State.FUSY_FUN);
                        }
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                state.push(State.ID);
                                token = new StringBuilder();
                                return advance(i);
                            } else {
                                result.appendCodePoint(i);
                            }
                        }
                    }
                }
                case LAMBDA_EXP -> {
                    switch (i) {
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '\'' -> {
                            state.push(State.CHARACTER);
                            result.appendCodePoint(i);
                        }
                        case '(' -> {
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                            result.appendCodePoint(i);
                        }
                        case ')', ',', '\n', ']' -> {
                            state.pop();
                            return advance(i);
                        }
                        case '#' -> state.push(State.HASH);
                        case '$' -> state.push(State.DOLLAR);
                        case '[' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.BT);
                            result.append("$uite.$(");
                        }
                        case '@' -> state.push(State.AT);
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '>' -> state.push(State.BACKBEAK);
                        case '{' -> {
                            subProcessor = new FusyFunProcessor(this);
                            subProcessor.getReady();
                            state.push(State.FUSY_FUN);
                        }
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                state.push(State.ID);
                                token = new StringBuilder();
                                return advance(i);
                            } else {
                                result.appendCodePoint(i);
                            }
                        }
                    }
                }
                case ASSIGN_EXP -> {
                    switch (i) {
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '\'' -> {
                            state.push(State.CHARACTER);
                            result.appendCodePoint(i);
                        }
                        case '(' -> {
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                            result.appendCodePoint(i);
                        }
                        case ')', ',', '\n', ']', ';' -> {
                            state.pop();
                            return advance(i);
                        }
                        case '#' -> state.push(State.HASH);
                        case '$' -> state.push(State.DOLLAR);
                        case '[' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.BT);
                            result.append("$uite.$(");
                        }
                        case '@' -> state.push(State.AT);
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '>' -> state.push(State.BACKBEAK);
                        case '{' -> {
                            subProcessor = new FusyFunProcessor(this);
                            subProcessor.getReady();
                            state.push(State.FUSY_FUN);
                        }
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                state.push(State.ID);
                                token = new StringBuilder();
                                return advance(i);
                            } else {
                                result.appendCodePoint(i);
                            }
                        }
                    }
                }
                case CB_EXP -> {
                    switch (i) {
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '\'' -> {
                            state.push(State.CHARACTER);
                            result.appendCodePoint(i);
                        }
                        case '(' -> {
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                            result.appendCodePoint(i);
                        }
                        case '}' -> {
                            state.pop();
                            return advance(i);
                        }
                        case '#' -> state.push(State.HASH);
                        case '$' -> state.push(State.DOLLAR);
                        case '[' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.BT);
                            result.append("$uite.$(");
                        }
                        case '@' -> state.push(State.AT);
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '>' -> state.push(State.BACKBEAK);
                        case '{' -> {
                            subProcessor = new FusyFunProcessor(this);
                            subProcessor.getReady();
                            state.push(State.FUSY_FUN);
                        }
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                state.push(State.ID);
                                token = new StringBuilder();
                                return advance(i);
                            } else {
                                result.appendCodePoint(i);
                            }
                        }
                    }
                }
                case RB_EXP -> {
                    switch (i) {
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '\'' -> {
                            state.push(State.CHARACTER);
                            result.appendCodePoint(i);
                        }
                        case '(' -> {
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                            result.appendCodePoint(i);
                        }
                        case ']' -> {
                            state.pop();
                            return advance(i);
                        }
                        case '#' -> state.push(State.HASH);
                        case '$' -> state.push(State.DOLLAR);
                        case '[' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.BT);
                            result.append("$uite.$(");
                        }
                        case '@' -> state.push(State.AT);
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '>' -> state.push(State.BACKBEAK);
                        case '{' -> {
                            subProcessor = new FusyFunProcessor(this);
                            subProcessor.getReady();
                            state.push(State.FUSY_FUN);
                        }
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                state.push(State.ID);
                                token = new StringBuilder();
                                return advance(i);
                            } else {
                                result.appendCodePoint(i);
                            }
                        }
                    }
                }
                case EXP_END -> {
                    result.append(")");
                    state.pop();
                    return advance(i);
                }
                case ID -> {
                    if (Character.isJavaIdentifierPart(i) && i != '$') {
                        if(i == DOLLAR_SUBSTITUTE) {
                            token.append('$');
                        } else {
                            token.appendCodePoint(i);
                        }
                    } else {
                        var str = token.toString().toLowerCase();
                        switch (str) {
                            case "return" -> {
                                state.pop();
                                result.append(token);
                                return advance(i);
                            }
                            case "if", "switch", "while" -> {
                                state.pop();
                                state.pop();
                                state.push(State.SCOPE_EXP);
                                result.append(token);
                                return advance(i);
                            }
                            case "for" -> {
                                state.pop();
                                state.pop();
                                state.push(State.FOR_SCOPE_EXP);
                                result.append(token);
                                return advance(i);
                            }
                            case "catch" -> {
                                state.pop();
                                state.push(State.CATCH);
                                return advance(i);
                            }
                            case "sync" -> {
                                state.pop();
                                state.pop();
                                state.push(State.SCOPE_EXP);
                                result.append("synchronized");
                                return advance(i);
                            }
                            case "do", "else", "finally" -> {
                                state.pop();
                                state.pop();
                                state.push(State.SCOPE);
                                result.append(token).append("{");
                                return advance(i);
                            }
                            case "scope" -> {
                                state.pop();
                                state.pop();
                                state.push(State.SCOPE);
                                result.append("{");
                                return advance(i);
                            }
                            case "try" -> {
                                state.pop();
                                state.pop();
                                state.push(State.TRY_SCOPE_EXP);
                                result.append(token);
                                return advance(i);
                            }
                            case "new" -> {
                                result.append("new ");
                                state.pop();
                                state.push(State.AFTER_ID);
                                state.push(State.NEW);
				return advance(i);
                            }
                            case "extends" -> {
                                result.append("new ");
                                state.pop();
                                state.push(State.AFTER_ID);
                                state.push(State.EXTENDS_AFTER_TYPE);
                                state.push(State.FUSY_TYPE);
                                subProcessor = new FusyTypeProcessor(this);
                                subProcessor.getReady();
                            }
                            case "case" -> {
                                state.pop();
                                state.pop();
                                state.push(State.CASE_SCOPE);
                                result.append(token);
                                return advance(i);
                            }
                            case "rest" -> {
                                state.pop();
                                state.pop();
                                state.push(State.SCOPE);
                                result.append("default->{");
                                return advance(i);
                            }
                            case "elf" -> {
                                state.pop();
                                state.pop();
                                state.push(State.ELF_SCOPE);
                                return advance(i);
                            }
                            case "drop" -> {
                                result.append("throw new FusyDrop(");
                                state.pop();
                                state.push(State.EXP_END);
                                state.push(State.LAMBDA_EXP);
                                return advance(i);
                            }
                            default -> {
                                state.pop();
                                state.push(State.AFTER_ID);
                                result.append(token);
                                return advance(i);
                            }
                        }
                    }
                }
                case AFTER_ID -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '{' -> {
                            result.append("[");
                            state.push(State.ARRAY_END);
                            state.push(State.ARRAY_INDEX);
                        }
                        case '(' -> {
                            result.appendCodePoint(i);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                        }
                        case '[' -> {
                            result.append("($uite.$(");
                            state.push(State.EXP_END);
                            state.push(State.BT);
                        }
                        case ':' -> state.push(State.COLON);
                        case '.' -> state.push(State.DOT);
                        default -> {
                            if (Character.isWhitespace(i) && i != '\n') {
                                result.appendCodePoint(i);
                            } else {
                                state.pop();
                                return advance(i);
                            }
                        }
                    }
                }
                case NEW -> {
                    if (i == '(') {
                        subProcessor = new FusArrayProcessor(this);
                        subProcessor.getReady();
                        state.pop();
                        state.push(State.AFTER_NEW_ARRAY);
                        state.push(State.NEW_ARRAY);
                        return advance(i);
                    } else if (!Character.isWhitespace(i)) {
                        state.pop();
                        state.push(State.NEW_AFTER_TYPE);
                        state.push(State.FUSY_TYPE);
                        var fusyTypeProcessor = new FusyTypeProcessor(this);
                        fusyTypeProcessor.getReady();
                        fusyTypeProcessor.arrayTypeEnabled(false);
                        subProcessor = fusyTypeProcessor;
                        return advance(i);
                    }
                }
                case AFTER_NEW_ARRAY -> {
                    if (i == '{') {
                        result.append("{");
                        state.pop();
                        state.push(State.ARRAY_INIT);
                    } else if (!Character.isWhitespace(i) || i == '\n') {
                        state.pop();
                        return advance(i);
                    }
                }
                case NEW_AFTER_TYPE -> {
                    if (i == '{') {
                        result.append(token.toString()).append("[]{");
                        subProcessor = new FusArrayProcessor(this);
                        subProcessor.getReady();
                        state.pop();
                        state.push(State.DISCARD);
                        state.push(State.SCOPE_END);
                        state.push(State.CB_EXP);
                    } else if (!Character.isWhitespace(i) || i == '\n') {
                        result.append(token.toString());
                        state.pop();
                        return advance(i);
                    }
                }
                case EXTENDS_AFTER_TYPE -> {
                    if (i == '(') {
                        result.append(token).append("(");
                        state.pop();
                        state.push(State.EXTENDS_AFTER_EXP);
                        state.push(State.DISCARD);
                        state.push(State.EXP_END);
                        state.push(State.EXPRESSION);
                    }
                }
                case EXTENDS_AFTER_EXP -> {
                    state.pop();
                    state.push(State.INLINE_DEFINITION);
                    subProcessor = new FusDefinitionProcessor(this);
                    subProcessor.getReady();
                    subProcessor.advance('>');
                    return advance(i);
                }
                case COLON -> {
                    switch (i) {
                        case ':' -> {
                            state.pop();
                            state.push(State.DOUBLE_COLON);
                        }
                        case '(' -> {
                            result.append(".apply(");
                            state.pop();
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                        }
                        case '$' -> {
                            result.append(".in(");
                            token = new StringBuilder();
                            state.pop();
                            state.push(State.AFTER_BTI);
                            state.push(State.EXP_END);
                            state.push(State.DOLLAR_STRING);
                        }
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                result.append(".");
                                state.pop();
                                token = new StringBuilder();
                                state.push(State.COLON_METHOD);
                            } else {
                                state.pop();
                            }
                            return advance(i);
                        }
                    }
                }
                case DOUBLE_COLON -> {
                    if (Character.isJavaIdentifierStart(i)) {
                        result.append("::");
                        token = new StringBuilder();
                        state.pop();
                        state.push(State.ID);
                        return advance(i);
                    }
                }
                case COLON_METHOD -> {
                    if (Character.isJavaIdentifierPart(i)) {
                        token.appendCodePoint(i);
                    } else {
                        var str = token.toString();
                        result.append(str);
                        switch (str) {
                            case "int", "double", "float", "byte", "char", "short", "long" -> result.append("Value");
                        }
                        if (i == '(') {
                            result.appendCodePoint('(');
                            state.pop();
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                        } else {
                            result.append("()");
                            state.pop();
                            return advance(i);
                        }
                    }
                }
                case DOT -> {
                    switch (i) {
                        case '$' -> {
                            result.append(".in(");
                            token = new StringBuilder();
                            state.pop();
                            state.push(State.AFTER_BTI);
                            state.push(State.EXP_END);
                            state.push(State.DOLLAR_STRING);
                        }
                        default -> {
                            result.append(".");
                            state.pop();
                            return advance(i);
                        }
                    }
                }
                case CLOSE_BRACE -> {
                    result.appendCodePoint(')');
                    state.pop();
                    return advance(i);
                }
                case STRING -> {
                    switch (i) {
                        case '"' -> {
                            state.pop();
                            result.appendCodePoint(i);
                        }
                        case '\\' -> state.push(State.STR_BACKSLASH);
                        case '#' -> {
                            state.push(State.STRING_HASH);
                            result.append("\" + ");
                            token = new StringBuilder();
                        }
                        case '\n' -> state.push(State.STR_ENDLINE);
                        default -> result.appendCodePoint(i);
                    }
                }
                case STRING_HASH -> {
                    switch (i) {
                        case '{' -> {
                            result.append("(");
                            state.pop();
                            state.push(State.STRHS_END);
                            state.push(State.CB_EXP);
                        }
                        case '\\' -> {
                            state.pop();
                            result.append(token).append(" + \"");
                        }
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                token.appendCodePoint(i);
                            } else {
                                state.pop();
                                result.append(token).append(" + \"");
                                return advance(i);
                            }
                        }
                    }
                }
                case STRHS_END -> {
                    state.pop();
                    result.append(") + \"");
                }
                case CHARACTER -> {
                    if (i == '\'') {
                        state.pop();
                        result.appendCodePoint(i);
                    } else if (i == '\\') {
                        state.push(State.STR_BACKSLASH);
                        result.appendCodePoint(i);
                    } else {
                        result.appendCodePoint(i);
                    }
                }
                case STR_BACKSLASH -> {
                    state.pop();
                    if (i == '#' || i == ' ') {
                        result.appendCodePoint(i);
                    } else {
                        result.append("\\").appendCodePoint(i);
                    }
                }
                case STR_ENDLINE -> {
                    if (!Character.isWhitespace(i)) {
                        state.pop();
                        return advance(i);
                    }
                }
                case BACKSLASH -> {
                    switch (i) {
                        case '\\' -> {
                            state.pop();
                            state.push(State.DOUBLE_BACKSLASH);
                        }
                        case '\n' -> state.pop();
                        case '<' -> {
                            state.pop();
                            getDebugger().advance('\n', false);
                            getDebugger().advance('<', false);
                        }
                        default -> {
                            if (Character.isWhitespace(i)) {
                                result.appendCodePoint(i);
                            } else {
                                state.pop();
                                advance(';');
                                return advance(i);
                            }
                        }
                    }
                }
                case DOUBLE_BACKSLASH -> {
                    if (i == '\\') {
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
                    if (i == '\\') {
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
                        return advance(i);
                    }
                }
                case BT -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '[' -> {
                            result.append("$uite.$(");
                            state.pop();
                            state.push(State.AFTER_BT);
                            state.push(State.BT);
                        }
                        case '#' -> {
                            state.pop();
                            state.push(State.AFTER_BT);
                            state.push(State.BT_HASH);
                        }
                        case ';' -> {
                            advance('[');
                            advance(']');
                        }
                        case ',' -> {
                            advance(']');
                            advance('[');
                        }
                        default -> {
                            if (!Character.isWhitespace(i)) {
                                state.pop();
                                state.push(State.BT_NEXT);
                                return advance(i);
                            }
                        }
                    }
                }
                case BT_NEXT -> {
                    switch (i) {
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '\'' -> {
                            state.push(State.CHARACTER);
                            result.appendCodePoint(i);
                        }
                        case '(' -> {
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                            result.appendCodePoint(i);
                        }
                        case ']' -> {
                            state.pop();
                            result.append(")");
                        }
                        case '[' -> {
                            result.append(",$uite.$(");
                            state.pop();
                            state.push(State.AFTER_BT);
                            state.push(State.BT);
                        }
                        case '@' -> state.push(State.AT);
                        case '#' -> {
                            result.append(",");
                            state.pop();
                            state.push(State.AFTER_BT);
                            state.push(State.BT_HASH);
                        }
                        case '$' -> state.push(State.DOLLAR);
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '>' -> state.push(State.BACKBEAK);
                        case '{' -> {
                            subProcessor = new FusyFunProcessor(this);
                            subProcessor.getReady();
                            state.push(State.FUSY_FUN);
                        }
                        case ';' -> {
                            advance('[');
                            advance(']');
                        }
                        case ',' -> {
                            advance(']');
                            advance('[');
                        }
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                state.push(State.BT_ID);
                                token = new StringBuilder();
                                return advance(i);
                            } else if (!Character.isWhitespace(i)) {
                                result.appendCodePoint(i);
                            }
                        }
                    }
                }
                case AFTER_BT -> {
                    switch (i) {
                        case ']', '[', '#' -> {
                            state.pop();
                            state.push(State.BT_NEXT);
                            return advance(i);
                        }
                        case '\\' -> state.push(State.BACKSLASH);
                        case ';' -> {
                            advance('[');
                            advance(']');
                        }
                        case ',' -> {
                            advance(']');
                            advance('[');
                        }
                        default -> {
                            if (!Character.isWhitespace(i)) {
                                result.append(",");
                                state.pop();
                                state.push(State.BT_NEXT);
                                return advance(i);
                            }
                        }
                    }
                }
                case BT_ID -> {
                    if (Character.isJavaIdentifierPart(i) && i != '$') {
                        token.appendCodePoint(i);

                    } else {
                        var str = token.toString().toLowerCase();
                        switch (str) {
                            case "new" -> {
                                result.append("new ");
                                state.pop();
                                state.push(State.AFTER_ID);
                                state.push(State.NEW);
				return advance(i);
                            }
                            case "extends" -> {
                                result.append("new ");
                                state.pop();
                                state.push(State.AFTER_ID);
                                state.push(State.EXTENDS_AFTER_TYPE);
                                state.push(State.FUSY_TYPE);
                                subProcessor = new FusyTypeProcessor(this);
                                subProcessor.getReady();
                            }
                            default -> {
                                state.pop();
                                state.push(State.AFTER_ID);
                                result.append(token);
                                return advance(i);
                            }
                        }
                    }
                }
                case BT_HASH -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '[' -> {
                            state.pop();
                            state.push(State.DISCARD);
                            state.push(State.RB_EXP);
                        }
                        case '(' -> {
                            result.append("new Suite.Mask(");
                            state.pop();
                            state.push(State.AFTER_ID);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                        }
                        default -> {
                            state.pop();
                            state.push(State.HASH);
                            return advance(i);
                        }
                    }
                }
                case DOLLAR_STRING -> {
                    if (Character.isJavaIdentifierPart(i)) {
                        token.appendCodePoint(i);
                    } else {
                        var str = token.toString();
                        result.append("\"").append(str).append("\"");
                        state.pop();
                        return advance(i);
                    }
                }
                case AFTER_BTI -> {
                    if (i == '\n' || i == ']' || i == ')' || i == '}' || i == ',' || i == ';') {
                        result.append(".get()");
                        state.pop();
                        return advance(i);
                    } else if (Character.isWhitespace(i)) {
                        result.appendCodePoint(i);
                    } else {
                        state.pop();
                        return advance(i);
                    }
                }
                case ARRAY_INDEX -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '@' -> state.push(State.AT);
                        case '#' -> state.push(State.HASH);
                        case '$' -> state.push(State.DOLLAR);
                        case '[' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.BT);
                            result.append("$uite.$(");
                        }
                        case '!' -> state.push(State.EXCLAMATION);
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '\'' -> {
                            state.push(State.CHARACTER);
                            result.appendCodePoint(i);
                        }
                        case '(' -> {
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                            result.appendCodePoint(i);
                        }
                        case '}' -> state.pop();
                        case '{' -> {
                            subProcessor = new FusyFunProcessor(this);
                            subProcessor.getReady();
                            state.push(State.FUSY_FUN);
                        }
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                state.push(State.ID);
                                token = new StringBuilder();
                                return advance(i);
                            } else {
                                result.appendCodePoint(i);
                            }
                        }
                    }
                }
                case ARRAY_INIT -> {
                    switch (i) {
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '\'' -> {
                            state.push(State.CHARACTER);
                            result.appendCodePoint(i);
                        }
                        case '(' -> {
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                            result.appendCodePoint(i);
                        }
                        case '}' -> {
                            result.append("}");
                            state.pop();
                        }
                        case '{' -> {
                            result.append("{");
                            state.push(State.ARRAY_INIT_BETWEEN);
                            state.push(State.ARRAY_INIT);
                        }
                        case '@' -> state.push(State.AT);
                        case '$' -> state.push(State.DOLLAR);
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '>' -> state.push(State.BACKBEAK);
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                state.push(State.BT_ID);
                                token = new StringBuilder();
                                return advance(i);
                            } else if (!Character.isWhitespace(i)) {
                                result.appendCodePoint(i);
                            }
                        }
                    }
                }
                case ARRAY_INIT_BETWEEN -> {
                    if(i == '{') {
                        state.pop();
                        result.append(",");
                        return advance(i);
                    } else if(!Character.isWhitespace(i)) {
                        state.pop();
                        return advance(i);
                    }
                }
                case ARRAY_END -> {
                    result.append("]");
                    state.pop();
                    return advance(i);
                }
                case EXCLAMATION -> {
                    if (i == '!') {
                        result.append(":");
                        state.pop();
                    } else {
                        result.append("!");
                        state.pop();
                        return advance(i);
                    }
                }
                case SCOPE_EXP -> {
                    if (i == '\\') {
                        state.push(State.BACKSLASH);
                    } else if (!Character.isWhitespace(i)) {
                        state.pop();
                        state.push(State.NAKED_SCOPE_EXP_STAT);
                        state.push(State.LAMBDA_EXP);
                        result.append("(");
                        return advance(i);
                    }
                }
                case NAKED_SCOPE_EXP_STAT -> {
                    state.pop();
                    state.push(State.SCOPE_END);
                    if (i == ',') {
                        state.push(State.STAT_END);
                        state.push(State.EMPTY_INLINE_STATEMENT);
                    } else {
                        state.push(State.EMPTY_STATEMENT);
                    }
                    result.append("){");
                }
                case SCOPE_END -> {
                    state.pop();
                    result.append("}");
                    return advance(i);
                }
                case SCOPE -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '\n', ';' -> {
                            state.pop();
                            state.push(State.SCOPE_END);
                            state.push(State.EMPTY_STATEMENT);
                            result.append("\n");
                        }
                        case ',' -> {
                            state.pop();
                            state.push(State.SCOPE_END);
                            state.push(State.STAT_END);
                            state.push(State.EMPTY_INLINE_STATEMENT);
                        }
                        default -> {
                            if (Character.isWhitespace(i)) {
                                result.appendCodePoint(i);
                            } else {
                                state.pop();
                                state.push(State.SCOPE_END);
                                state.push(State.STAT_END);
                                state.push(State.EMPTY_INLINE_STATEMENT);
                                return advance(i);
                            }
                        }
                    }
                }
                case FOR_SCOPE_EXP -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '#' -> {
                            result.append("(var ");
                            token = new StringBuilder();
                            state.pop();
                            state.push(State.FSE_ID);
                        }
                        default -> {
                            if (!Character.isWhitespace(i)) {
                                state.pop();
                                state.push(State.NAKED_SCOPE_EXP_STAT);
                                state.push(State.LAMBDA_EXP);
                                result.append("(");
                                return advance(i);
                            }
                        }
                    }
                }
                case FSE_ID -> {
                    if (Character.isJavaIdentifierPart(i)) {
                        token.appendCodePoint(i);
                    } else {
                        state.pop();
                        state.push(State.FSE_AFTER_ID);
                        return advance(i);
                    }
                }
                case FSE_AFTER_ID -> {
                    if (!Character.isWhitespace(i)) {
                        if (i == '=') {
                            result.append(token);
                        } else {
                            result.append(token).append(" : ");
                        }
                        state.pop();
                        state.push(State.NAKED_SCOPE_EXP_STAT);
                        state.push(State.LAMBDA_EXP);
                        return advance(i);
                    }
                }
                case TRY_SCOPE_EXP -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '#' -> {
                            result.append("(var ");
                            token = new StringBuilder();
                            state.pop();
                            state.push(State.TRY_ID);
                        }
                        case '\n', ';', ',' -> {
                            result.append("{");
                            state.pop();
                            state.push(State.SCOPE);
                            return advance(i);
                        }
                        default -> {
                            if (!Character.isWhitespace(i)) {
                                state.pop();
                                state.push(State.NAKED_SCOPE_EXP_STAT);
                                state.push(State.LAMBDA_EXP);
                                result.append("(");
                                return advance(i);
                            }
                        }
                    }
                }
                case TRY_ID -> {
                    if (Character.isJavaIdentifierPart(i)) {
                        token.appendCodePoint(i);
                    } else {
                        result.append(token).append("=");
                        state.pop();
                        state.push(State.NAKED_SCOPE_EXP_STAT);
                        state.push(State.LAMBDA_EXP);
                        return advance(i);
                    }
                }
                case CATCH -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '#' -> {
                            result.append("catch(Throwable ");
                            token = new StringBuilder();
                            state.pop();
                            state.pop();
                            state.push(State.CSE_ID);
                        }
                        case '\n', ';', ',' -> {
                            result.append("catch(Throwable ignored){");
                            state.pop();
                            state.pop();
                            state.push(State.SCOPE);
                            return advance(i);
                        }
                        case '.' -> {
                            token = new StringBuilder();
                            state.pop();
                            state.push(State.CATCH_VAR_ID);
                        }
                        default -> {
                            if (!Character.isWhitespace(i)) {
                                result.append("catch(");
                                state.pop();
                                state.pop();
                                state.push(State.NAKED_SCOPE_EXP_STAT);
                                state.push(State.LAMBDA_EXP);
                                return advance(i);
                            }
                        }
                    }
                }
                case CSE_ID -> {
                    if (Character.isJavaIdentifierPart(i)) {
                        token.appendCodePoint(i);
                    } else {
                        result.append(token);
                        state.pop();
                        state.push(State.NAKED_SCOPE_EXP_STAT);
                        state.push(State.LAMBDA_EXP);
                        return advance(i);
                    }
                }
                case CATCH_VAR_ID -> {
                    if (Character.isJavaIdentifierPart(i)) {
                        token.appendCodePoint(i);
                    } else {
                        result.append(getCatchVar(token.toString()));
                        state.pop();
                        state.push(State.AFTER_ID);
                        return advance(i);
                    }
                }
                case CASE_SCOPE -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '(' -> {
                            result.append("(");
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                        }
                        case '"' -> {
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '\'' -> {
                            state.push(State.CHARACTER);
                            result.appendCodePoint(i);
                        }
                        case '\n', ',' -> {
                            state.pop();
                            state.push(State.CASE_SCOPE_STAT);
                            return advance(i);
                        }
                        case '|' -> result.append(",");
                        case '<' -> {
                            state.pop();
                            result.append("->{}");
                        }
                        default -> result.appendCodePoint(i);
                    }
                }
                case CASE_SCOPE_STAT -> {
                    if (i == '\\') {
                        state.push(State.BACKSLASH);
                    } else if (i == '\n') {
                        state.pop();
                        state.push(State.SCOPE_END);
                        state.push(State.EMPTY_STATEMENT);
                        result.append("->{\n");
                    } else if (i == ',') {
                        state.pop();
                        state.push(State.SCOPE_END);
                        state.push(State.STAT_END);
                        state.push(State.EMPTY_INLINE_STATEMENT);
                        result.append("->{\n");
                    } else if (Character.isWhitespace(i)) {
                        result.appendCodePoint(i);
                    } else {
                        state.pop();
                        state.push(State.SCOPE_END);
                        state.push(State.EMPTY_STATEMENT);
                        result.append("->{");
                        return advance(i);
                    }
                }
                case ELF_SCOPE -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '\n' -> {
                            result.append("else{");
                            state.pop();
                            state.push(State.SCOPE_END);
                            state.push(State.EMPTY_STATEMENT);
                        }
                        case ',' -> {
                            result.append("else{");
                            state.pop();
                            state.push(State.SCOPE_END);
                            state.push(State.STAT_END);
                            state.push(State.EMPTY_INLINE_STATEMENT);
                        }
                        default -> {
                            if (!Character.isWhitespace(i)) {
                                state.pop();
                                state.push(State.NAKED_SCOPE_EXP_STAT);
                                state.push(State.LAMBDA_EXP);
                                result.append("else if(");
                                return advance(i);
                            }
                        }
                    }
                }
            }
        } else {
            parentProcessor.terminateSubProcess();
            parentProcessor.advance(i);
        }
        return 0;
    }

    public void terminateSubProcess() {
        switch (state.peek()) {
            case DEFINITION -> {
                var $ = subProcessor.finish();
                if($.present(FusDefinitionProcessor.Result.SETUP)) {
                    setup = $.in(FusDefinitionProcessor.Result.SETUP).asString();
                } else {
                    definitions.add($.in(FusDefinitionProcessor.Result.STATEMENTS).asString(""));
                    imports.alter($.in(FusDefinitionProcessor.Result.IMPORTS));
                }
                state.pop();
                if (state.peek() == State.STATEMENT) {
                    state.pop();
                }
            }
            case INLINE_DEFINITION -> {
                var $ = subProcessor.finish();
                var catchVars = new StringBuilder();
                for(var $cv : $.in(FusDefinitionProcessor.Result.CATCH_VARS)) {
                    catchVars.append("var ").append($cv.in().asString()).append(" = ").append($cv.asString()).append(";");
                }
                result.insert(autoVarInsert, catchVars);
                result.append($.in(FusDefinitionProcessor.Result.STATEMENTS).asString(""));
                imports.alter($.in(FusDefinitionProcessor.Result.IMPORTS));
                state.pop();
            }
            case FUSY_FUN -> {
                var $ = subProcessor.finish();
                String fun = $.in(FusyFunProcessor.Result.COMPLETE).asString();
                result.append(fun);
                state.pop();
            }
            case FUSY_TYPE -> {
                var $ = subProcessor.finish();
                String type = $.in(FusyTypeProcessor.Result.COMPLETE).asString();
                token = new StringBuilder(type);
                state.pop();
                var i = new int[][]{{1}, {2}, {3}};
            }
            case STATEMENT_VARIABLE -> {
                var $ = subProcessor.finish();
                String varDefinition = $.in(Result.STATEMENTS).asString();
                buffer.append(varDefinition).append(";");
                state.pop();
            }
            case NEW_ARRAY -> {
                var $ = subProcessor.finish();
                String arrayInit = $.in(FusArrayProcessor.Result.COMPLETE).asString();
                result.append(arrayInit);
                state.pop();
            }
            case INSTANCEOF -> {
                var $ = subProcessor.finish();
                String exp = $.in(Result.STATEMENTS).asString();
                result.append("(").append(exp).append(") instanceof ").append(token.toString()).append(" ");
                state.pop();
            }
        }
    }

    @Override
    public Subject finish() {
        if(!state.isEmpty()) advance('\n');
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
                Result.IMPORTS, $(imps.toString()),
                Result.SETUP, $(setup)
        );
    }
}
