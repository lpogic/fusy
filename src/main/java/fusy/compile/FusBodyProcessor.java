package fusy.compile;

import suite.suite.Subject;

import java.io.File;
import java.util.Stack;

import static suite.suite.$uite.$;

public class FusBodyProcessor extends FusProcessor {

    public enum State {
        DISCARD, TERMINATED, EMPTY_STATEMENT, STATEMENT, EXPRESSION, ID, AFTER_ID, STRING, STRING_END,
        RAW_STRING_IN, RAW_STRING, RAW_STRING_OUT, STRCB_END, CB_EXP, RB_EXP, STR_ENDLINE,
        CHARACTER, STR_BACKSLASH, SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT, BT, BT_LIST, BT_NEXT, AFTER_BT, STAT_END,
        ARRAY_INDEX, ARRAY_INIT, ARRAY_INIT_BETWEEN, ARRAY_END, COLON, DOUBLE_COLON, COLON_METHOD, CM_COLON, FOR_SCOPE_EXP, FSE_ID, EXP_END, FSE_AFTER_ID,
        SCOPE_EXP, SCOPE, SCOPE_END, CASE_SCOPE, NAKED_SCOPE_EXP_STAT, CASE_SCOPE_STAT, ELF_SCOPE, EMPTY_INLINE_STATEMENT, INLINE_STATEMENT,
        BACKSLASH, CLOSE_BRACE, DEFINITION, INLINE_DEFINITION, DOUBLE_BACKSLASH, MLC_BACKSLASH, MLC_DOUBLE_BACKSLASH,
        ES_AT, AT, AT_TYPE, AT_CAST, LAMBDA, LAMBDA_EXP, BEAK, FUSY_FUN, FUSY_TYPE, HASH, DOT_STRING,
        ES_PLUS, ES_MINUS, EXCLAMATION, CATCH, CSE_ID, AFTER_BTI, HASH_ID, STATEMENT_VARIABLE,
        TRY_ID, TRY_SCOPE_EXP, AID_DOT, EXTENDS_AFTER_TYPE, EXTENDS_AFTER_EXP,
        HASH_EXP, DOT, NEW, NEW_ARRAY, AFTER_NEW_ARRAY, NEW_AFTER_TYPE, INSTANCEOF, INSTANCEOF_ID,
        CATCH_VAR_ID, AFTER_TRY, AFTER_IF, AFTER_CASE, AFTER_SWITCH, BEFORE_PIN, PIN,
        BEFORE_ARG_TYPE, ARG_TYPE, AFTER_ARG_TYPE, BEFORE_ARG_NAME, ARG_NAME, IMPORT, IMPORT_BACKSLASH
    }

    enum Result {
        STATEMENTS, DEFINITIONS, IMPORTS, SETUP
    }

    public static final int MANUAL_LF = 0xFF0001;

    Stack<State> state;
    StringBuilder result;
    StringBuilder token;
    int counter;
    int autoVarInsert;
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
                        case ',', ')' -> {
                            state.pop();
                            return advance(i);
                        }
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
                case DEFINITION,  FUSY_FUN, FUSY_TYPE, STATEMENT_VARIABLE, NEW_ARRAY,
                        INLINE_DEFINITION -> subProcessor.advance(i);
                case STATEMENT -> {
                    switch (i) {
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING_END);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '`' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.RAW_STRING_IN);
                            counter = 1;
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
                        case '.' -> state.push(State.DOT);
                        case '[' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.BT);
                            result.append("FusySubjectBuilder.$(");
                        }
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\n', MANUAL_LF -> {
                            result.append(";\n");
                            state.pop();
                        }
                        case ',', ')' -> {
                            result.append(";\n");
                            state.pop();
                            return advance(i);
                        }
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '{' -> {
                            result.append("(");
                            state.push(State.LAMBDA);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.CB_EXP);
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
                        case MANUAL_LF -> result.append(";");
                        default -> {
                            if(!Character.isWhitespace(i) || i == '\n') {
                                state.pop();
                                state.push(State.INLINE_STATEMENT);
                                return advance(i);
                            }
                        }
                    }
                }
                case INLINE_STATEMENT -> {
                    switch (i) {
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING_END);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '`' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.RAW_STRING_IN);
                            counter = 1;
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
                        case '\n', ',', ')' -> {
                            state.pop();
                            return advance(i);
                        }
                        case MANUAL_LF -> {
                            state.pop();
                            state.push(State.EMPTY_INLINE_STATEMENT);
                            return advance(i);
                        }
                        case '#' -> state.push(State.HASH);
                        case '.' -> state.push(State.DOT);
                        case '[' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.BT);
                            result.append("FusySubjectBuilder.$(");
                        }
                        case '@' -> state.push(State.AT);
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '{' -> {
                            result.append("(");
                            state.push(State.LAMBDA);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.CB_EXP);
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
                        case '\n', MANUAL_LF -> {
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
                        case '+', '-', '~', '!', '*', '?', '/', '<' -> {
                            subProcessor = new FusDefinitionProcessor(this);
                            subProcessor.getReady();
                            state.pop();
                            state.push(State.DEFINITION);
                            return advance(i);
                        }
                        case '@' -> {
                            result.append("Fus.this");
                            state.pop();
                            state.push(State.STATEMENT);
                            state.push(State.AFTER_ID);
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
                                result.append("this");
                                state.pop();
                                state.push(State.STATEMENT);
                                state.push(State.AFTER_ID);
                                return advance(i);
                            }
                        }
                    }
                }
                case AT -> {
                    switch (i) {
                        case '?' -> {
                            result.append(" instanceof ");
                            subProcessor = new FusyTypeProcessor(this);
                            subProcessor.getReady();
                            state.pop();
                            state.push(State.INSTANCEOF);
                            state.push(State.FUSY_TYPE);
                        }
                        case '!' -> {
                            result.append(" != null ");
                            state.pop();
                        }
                        case '/' -> {
                            result.append(" == null ");
                            state.pop();
                        }
                        case '@' -> {
                            result.append("Fus.this");
                            state.pop();
                            state.push(State.AFTER_ID);
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
                                result.append("this");
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
                        default -> {
                            var str = token.toString();
                            if (Character.isJavaIdentifierStart(i)) {
                                var fusDefinitionProcessor = new FusDefinitionProcessor(this);
                                fusDefinitionProcessor.getReady(str);
                                subProcessor = fusDefinitionProcessor;
                                state.pop();
                                if("language".equals(str)) {
                                    state.push(State.INLINE_DEFINITION);
                                } else {
                                    state.push(State.DEFINITION);
                                }
                                return advance(i);
                            } else if (!Character.isWhitespace(i)) {
                                result.append(str).append(".class");
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
                case INSTANCEOF -> {
                    if(i == '#') {
                        result.append(token.toString()).append(" ");
                        state.pop();
                        state.push(State.INSTANCEOF_ID);
                    } else if(!Character.isWhitespace(i) || i == '\n') {
                        result.append(token.toString());
                        state.pop();
                        return advance(i);
                    }
                }
                case INSTANCEOF_ID -> {
                    if(Character.isJavaIdentifierPart(i)) {
                        result.appendCodePoint(i);
                    } else {
                        state.pop();
                        return advance(i);
                    }
                }
                case HASH -> {
                    switch (i) {
                        case '+' -> {
                            result.append(".fusyAdd(");
                            state.pop();
                            state.push(State.EXP_END);
                            state.push(State.HASH_EXP);
                        }
                        case '#' -> {
                            result.append(".fusySet(");
                            state.pop();
                            state.push(State.EXP_END);
                            state.push(State.HASH_EXP);
                        }
                        case '=' -> {
                            result.append(".fusyReset(");
                            state.pop();
                            state.push(State.EXP_END);
                            state.push(State.LAMBDA_EXP);
                        }
                        default -> {
                            if (Character.isJavaIdentifierStart(i)) {
                                state.pop();
                                state.push(State.HASH_ID);
                                token = new StringBuilder();
                            } else {
                                result.append("this");
                                state.pop();
                                state.push(State.AFTER_ID);
                            }
                            return advance(i);
                        }
                    }
                }
                case HASH_ID -> {
                    switch (i) {
                        case '(' -> {
                            result.append(token);
                            var fusBodyProcessor = new FusBodyProcessor(this);
                            fusBodyProcessor.getReady(State.EXPRESSION);
                            subProcessor = fusBodyProcessor;
                            state.pop();
                            state.push(State.AFTER_ID);
                            state.push(State.DISCARD);
                            state.push(State.STATEMENT_VARIABLE);
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
                case DOT -> {
                    if (Character.isJavaIdentifierStart(i)) {
                        token = new StringBuilder();
                        state.pop();
                        state.push(State.AFTER_ID);
                        state.push(State.DOT_STRING);
                    } else {
                        result.append(".");
                        state.pop();
                    }
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
                }
                case EXPRESSION -> {
                    switch (i) {
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING_END);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '`' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.RAW_STRING_IN);
                            counter = 1;
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
                        case ')' -> {
                            state.pop();
                            return advance(i);
                        }
                        case '@' -> state.push(State.AT);
                        case '#' -> state.push(State.HASH);
                        case '.' -> state.push(State.DOT);
                        case '[' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.BT);
                            result.append("FusySubjectBuilder.$(");
                        }
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '{' -> {
                            result.append("(");
                            state.push(State.LAMBDA);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.CB_EXP);
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
                case BEFORE_ARG_TYPE -> {
                    switch (i) {
                        case ')' -> {
                            state.pop();
                            return advance(i);
                        }
                        case '\\' -> state.push(State.BACKSLASH);
                        default -> {
                            if(Character.isWhitespace(i)) {
                                result.appendCodePoint(i);
                            } else if(Character.isJavaIdentifierStart(i) || i == '{') {
                                result.append(",");
                                state.pop();
                                return advance(i);
                            }
                        }
                    }
                }
                case ARG_TYPE -> {
                    switch (i) {
                        case ')' -> {
                            state.pop();
                            return advance(i);
                        }
                        case '\\' -> state.push(State.BACKSLASH);
                        case ',' -> {}
                        default -> {
                            if(Character.isWhitespace(i)) {
                                result.appendCodePoint(i);
                            } else {
                                subProcessor = new FusyTypeProcessor(this);
                                subProcessor.getReady();
                                state.push(State.BEFORE_ARG_NAME);
                                state.push(State.AFTER_ARG_TYPE);
                                state.push(State.FUSY_TYPE);
                                return advance(i);
                            }
                        }
                    }
                }
                case AFTER_ARG_TYPE -> {
                    if(i == '.') {
                        token.append(".");
                    } else if(!Character.isWhitespace(i)) {
                        state.pop();
                        return advance(i);
                    }
                }
                case BEFORE_ARG_NAME -> {
                    switch (i) {
                        case ')' -> {
                            state.pop();
                            return advance(i);
                        }
                        case '\\' -> state.push(State.BACKSLASH);
                        default -> {
                            if(Character.isJavaIdentifierStart(i)) {
                                result.append(token).append(" ");
                                state.pop();
                                state.push(State.ARG_NAME);
                                return advance(i);
                            }
                        }
                    }
                }
                case ARG_NAME -> {
                    if(Character.isJavaIdentifierPart(i) || i == '.') {
                        result.appendCodePoint(i);
                    } else {
                        state.pop();
                        state.push(State.BEFORE_ARG_TYPE);
                        return advance(i);
                    }
                }
                case LAMBDA_EXP -> {
                    switch (i) {
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING_END);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '`' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.RAW_STRING_IN);
                            counter = 1;
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
                        case ')', ',', '\n', ']', MANUAL_LF -> {
                            state.pop();
                            return advance(i);
                        }
                        case '#' -> state.push(State.HASH);
                        case '.' -> state.push(State.DOT);
                        case '[' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.BT);
                            result.append("FusySubjectBuilder.$(");
                        }
                        case '@' -> state.push(State.AT);
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '{' -> {
                            result.append("(");
                            state.push(State.LAMBDA);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.CB_EXP);
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
                case HASH_EXP -> {
                    switch (i) {
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING_END);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '`' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.RAW_STRING_IN);
                            counter = 1;
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
                        case ')', ',', '\n', ']', MANUAL_LF, '#' -> {
                            state.pop();
                            return advance(i);
                        }
                        case '.' -> state.push(State.DOT);
                        case '[' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.BT);
                            result.append("FusySubjectBuilder.$(");
                        }
                        case '@' -> state.push(State.AT);
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '{' -> {
                            result.append("(");
                            state.push(State.LAMBDA);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.CB_EXP);
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
                            state.push(State.STRING_END);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '`' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.RAW_STRING_IN);
                            counter = 1;
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
                        case '}' -> {
                            state.pop();
                            return advance(i);
                        }
                        case '#' -> state.push(State.HASH);
                        case '.' -> state.push(State.DOT);
                        case '[' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.BT);
                            result.append("FusySubjectBuilder.$(");
                        }
                        case '@' -> state.push(State.AT);
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '{' -> {
                            result.append("(");
                            state.push(State.LAMBDA);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.CB_EXP);
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
                            state.push(State.STRING_END);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '`' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.RAW_STRING_IN);
                            counter = 1;
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
                        case ']' -> {
                            state.pop();
                            return advance(i);
                        }
                        case '#' -> state.push(State.HASH);
                        case '.' -> state.push(State.DOT);
                        case '[' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.BT);
                            result.append("FusySubjectBuilder.$(");
                        }
                        case '@' -> state.push(State.AT);
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '{' -> {
                            result.append("(");
                            state.push(State.LAMBDA);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.CB_EXP);
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
                    if (Character.isJavaIdentifierPart(i)) {
                        token.appendCodePoint(i);
                    } else {
                        var str = token.toString().toLowerCase();
                        switch (str) {
                            case "return" -> {
                                state.pop();
                                result.append(token);
                                return advance(i);
                            }
                            case "if" -> {
                                state.pop();
                                state.pop();
                                state.push(State.AFTER_IF);
                                state.push(State.SCOPE_EXP);
                                result.append(token);
                                return advance(i);
                            }
                            case "switch" -> {
                                state.pop();
                                state.pop();
                                state.push(State.AFTER_SWITCH);
                                state.push(State.SCOPE_EXP);
                                result.append(token);
                                return advance(i);
                            }
                            case "while" -> {
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
                            case "else" -> {
                                state.pop();
                                state.pop();
                                var ifIndex = state.lastIndexOf(State.AFTER_IF);
                                if(ifIndex >= 0) {
                                    while (state.size() > ifIndex) {
                                        if (state.peek() != State.STAT_END) {
                                            advance('<');
                                        }
                                        advance('\n');
                                    }
                                }
                                state.push(State.SCOPE);
                                result.append(token).append("{");
                                return advance(i);
                            }
                            case "do", "finally" -> {
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
                            case "pin" -> {
                                state.pop();
                                state.pop();
                                state.push(State.BEFORE_PIN);
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
                                var caseIndex = state.lastIndexOf(State.AFTER_CASE);
                                var switchIndex = state.lastIndexOf(State.AFTER_SWITCH);
                                if(caseIndex >= 0 && caseIndex > switchIndex) {
                                    while (state.size() > caseIndex) {
                                        if (state.peek() != State.STAT_END) {
                                            advance('<');
                                        }
                                        advance('\n');
                                    }
                                }
                                state.push(State.CASE_SCOPE);
                                result.append(token);
                                return advance(i);
                            }
                            case "rest" -> {
                                state.pop();
                                state.pop();
                                var caseIndex = state.lastIndexOf(State.AFTER_CASE);
                                var switchIndex = state.lastIndexOf(State.AFTER_SWITCH);
                                if(caseIndex >= 0 && caseIndex > switchIndex) {
                                    while (state.size() > caseIndex) {
                                        if (state.peek() != State.STAT_END) {
                                            advance('<');
                                        }
                                        advance('\n');
                                    }
                                }
                                state.push(State.SCOPE);
                                result.append("default->{");
                                return advance(i);
                            }
                            case "elf" -> {
                                state.pop();
                                state.pop();
                                var ifIndex = state.lastIndexOf(State.AFTER_IF);
                                if(ifIndex >= 0) {
                                    while (state.size() > ifIndex) {
                                        if (state.peek() != State.STAT_END) {
                                            advance('<');
                                        }
                                        advance('\n');
                                    }
                                }
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
                            case "import" -> {
                                token = new StringBuilder();
                                state.pop();
                                state.push(State.IMPORT);
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
                        case ':' -> state.push(State.COLON);
                        case '.' -> state.push(State.AID_DOT);
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
                    switch(i) {
                        case '(' -> {
                            subProcessor = new FusArrayProcessor(this);
                            subProcessor.getReady();
                            state.pop();
                            state.push(State.AFTER_NEW_ARRAY);
                            state.push(State.NEW_ARRAY);
                            return advance(i);
                        }
                        case ':' -> {
                            state.pop();
                            state.push(State.NEW_AFTER_TYPE);
                            state.push(State.FUSY_TYPE);
                            var fusyTypeProcessor = new FusyTypeProcessor(this);
                            fusyTypeProcessor.getReady();
                            fusyTypeProcessor.arrayTypeEnabled(false);
                            subProcessor = fusyTypeProcessor;
                        }
                        default -> {
                            if (!Character.isWhitespace(i)) {
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
                    } else if(i == '(') {
                        result.append(token.toString());
                        state.pop();
                        return advance(i);
                    } else if (!Character.isWhitespace(i) || i == '\n') {
                        result.append(token.toString()).append("()");
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
                        case '[' -> {
                            result.append("(FusySubjectBuilder.$(");
                            state.pop();
                            state.push(State.EXP_END);
                            state.push(State.BT);
                        }
                        case '.' -> {
                            result.append(".in(");
                            token = new StringBuilder();
                            state.pop();
                            state.push(State.AFTER_BTI);
                            state.push(State.EXP_END);
                            state.push(State.DOT_STRING);
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
                    } else if(i == '[') {
                        result.append(".apply(FusySubjectBuilder.$(");
                        state.pop();
                        state.push(State.EXP_END);
                        state.push(State.BT);
                    }
                }
                case COLON_METHOD -> {
                    if (Character.isJavaIdentifierPart(i)) {
                        token.appendCodePoint(i);
                    } else {
                        var str = token.toString();
                        result.append(str);
                        switch (str) {
                            case "int", "double", "float", "byte", "char", "short", "long", "boolean" -> result.append("Value");
                        }
                        switch (i) {
                            case '(' -> {
                                result.appendCodePoint('(');
                                state.pop();
                                state.push(State.DISCARD);
                                state.push(State.EXP_END);
                                state.push(State.EXPRESSION);
                            }
                            case ':' -> {
                                state.pop();
                                state.push(State.CM_COLON);
                            }
                            default -> {
                                result.append("()");
                                state.pop();
                                return advance(i);
                            }
                        }
                    }
                }
                case CM_COLON -> {
                    if(i == '[') {
                        result.append("(FusySubjectBuilder.$(");
                        state.pop();
                        state.push(State.EXP_END);
                        state.push(State.BT);
                    } else {
                        result.append("()");
                        state.pop();
                        advance(':');
                        return advance(i);
                    }
                }
                case IMPORT -> {
                    switch (i) {
                        case '\\' -> state.push(State.IMPORT_BACKSLASH);
                        case '}' -> {
                            var str = token.toString().trim().replace((char)0x7F, ' ');
                            if(str.contains(".")) {
                                getDebugger().pushSource(str);
                            } else {
                                getDebugger().pushSource(getDebugger().defaultPath + File.separator +
                                        "rsc" + File.separator + "fusy" + File.separator + str + ".fus");
                            }
                            state.pop();
                        }
                        case '%' -> token.append(getDebugger().defaultPath).append(File.separator);
                        case '/' -> token.append(File.separator);
                        default -> token.appendCodePoint(i);
                    }
                }
                case IMPORT_BACKSLASH -> {
                    if (i == '}' || i == '\\' || i == '/' || i == '%') {
                        token.append(i);
                    } else if(i == ' ') {
                        token.appendCodePoint(0x7F);
                    } else {
                        token.append('\\').appendCodePoint(i);
                    }
                    state.pop();
                }
                case AFTER_IF, AFTER_CASE, AFTER_SWITCH -> {
                        state.pop();
                        if(i != '\n' && i != MANUAL_LF) return advance(i);
                }
                case AFTER_TRY -> {
                    state.pop();
                    if(i != MANUAL_LF) {
                        result.append("catch(Exception e){hitException(e);}");
                        if(i != '\n') return advance(i);
                    }
                }
                case AID_DOT -> {
                    result.append(".");
                    state.pop();
                    return advance(i);
                }
                case CLOSE_BRACE -> {
                    result.appendCodePoint(')');
                    state.pop();
                    return advance(i);
                }
                case STRING -> {
                    switch (i) {
                        case '"' -> state.pop();
                        case '\\' -> state.push(State.STR_BACKSLASH);
                        case '{' -> {
                            result.append("\"+(");
                            state.push(State.STRCB_END);
                            state.push(State.CB_EXP);
                        }
                        case '\n' -> state.push(State.STR_ENDLINE);
                        default -> {
                            if(i < 128) {
                                result.appendCodePoint(i);
                            } else {
                                result.append(String.format("\\u%04x", i));
                            }
                        }
                    }
                }
                case STRING_END -> {
                    result.append("\"");
                    state.pop();
                    return advance(i);
                }
                case STRCB_END -> {
                    state.pop();
                    result.append(")+\"");
                }
                case RAW_STRING_IN -> {
                    if(i == '`') ++counter;
                    else {
                        result.append("\"");
                        state.pop();
                        state.push(State.RAW_STRING);
                        if(i != '>') return advance(i);
                    }
                }
                case RAW_STRING -> {
                    switch (i) {
                        case '`' -> {
                            token = new StringBuilder("`");
                            state.pop();
                            state.push(State.RAW_STRING_OUT);
                        }
                        case '\\' -> result.append("\\\\");
                        case '"' -> result.append("\\\"");
                        case '\n' -> result.append("\\n");
                        default -> {
                            if(i < 128 && i > 31) {
                                result.appendCodePoint(i);
                            } else {
                                result.append(String.format("\\u%04x", i));
                            }
                        }
                    }
                }
                case RAW_STRING_OUT -> {
                    if(i == '`') token.appendCodePoint(i);
                    else {
                        var tokenLength = token.length();
                        state.pop();
                        if (counter < tokenLength) {
                            result.append("`".repeat(tokenLength - counter)).append("\"");
                            return advance(i);
                        } else if(counter == tokenLength) {
                            result.append("\"");
                            return advance(i);
                        } else {
                            result.append(token);
                            state.push(State.RAW_STRING);
                            return advance(i);
                        }
                    }
                }
                case CHARACTER -> {
                    switch (i) {
                        case '\'' -> {
                            result.appendCodePoint(i);
                            state.pop();
                        }
                        case '\\' -> state.push(State.STR_BACKSLASH);
                        default -> {
                            if(i < 128) {
                                result.appendCodePoint(i);
                            } else {
                                result.append(String.format("\\u%04x", i));
                            }
                        }
                    }
                }
                case STR_BACKSLASH -> {
                    state.pop();
                    if (i == '{' || Character.isWhitespace(i)) {
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
                                advance(MANUAL_LF);
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
                            result.append("FusySubjectBuilder.$$(");
                            state.pop();
                            state.push(State.AFTER_BT);
                            state.push(State.BT);
                        }
                        case '(' -> {
                            result.append("new SuiteMask");
                            state.pop();
                            state.push(State.BT_NEXT);
                            return advance(i);
                        }
                        case '#' -> state.push(State.HASH);
                        case ',' -> {
                            advance('[');
                            advance(']');
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
                case BT_LIST -> {
                    switch (i) {
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING_END);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '`' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.RAW_STRING_IN);
                            counter = 1;
                        }
                        case '\'' -> {
                            state.push(State.CHARACTER);
                            result.appendCodePoint(i);
                        }
                        case '(' -> {
                            result.append("(");
                            state.push(State.AFTER_ID);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                        }
                        case ']' -> {
                            state.pop();
                            result.append(")");
                        }
                        case '[' -> {
                            result.append(",FusySubjectBuilder.$$(");
                            state.pop();
                            state.push(State.AFTER_BT);
                            state.push(State.BT);
                        }
                        case ',' -> result.append("),FusySubjectBuilder.$$(");
                        case '@' -> state.push(State.AT);
                        case '#' -> state.push(State.HASH);
                        case '.' -> state.push(State.DOT);
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '{' -> {
                            result.append("(");
                            state.push(State.LAMBDA);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.CB_EXP);
                        }
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                state.push(State.ID);
                                token = new StringBuilder();
                                return advance(i);
                            } else if (!Character.isWhitespace(i)) {
                                result.appendCodePoint(i);
                            }
                        }
                    }
                }
                case BT_NEXT -> {
                    switch (i) {
                        case '"' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.STRING_END);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '`' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.RAW_STRING_IN);
                            counter = 1;
                        }
                        case '\'' -> {
                            state.push(State.CHARACTER);
                            result.appendCodePoint(i);
                        }
                        case '(' -> {
                            result.append("(");
                            state.push(State.AFTER_ID);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                        }
                        case ']' -> {
                            state.pop();
                            result.append(")");
                        }
                        case '[' -> {
                            result.append(",FusySubjectBuilder.$$(");
                            state.pop();
                            state.push(State.AFTER_BT);
                            state.push(State.BT);
                        }
                        case ',' -> {
                            advance('[');
                            advance(']');
                        }
                        case '@' -> state.push(State.AT);
                        case '#' -> state.push(State.HASH);
                        case '.' -> state.push(State.DOT);
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        case '{' -> {
                            result.append("(");
                            state.push(State.LAMBDA);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.CB_EXP);
                        }
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                state.push(State.ID);
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
                        case ']', '[' -> {
                            state.pop();
                            state.push(State.BT_NEXT);
                            return advance(i);
                        }
                        case '(' -> {
                            result.append(",new SuiteMask");
                            state.pop();
                            state.push(State.BT_NEXT);
                            return advance(i);
                        }
                        case '\\' -> state.push(State.BACKSLASH);
                        case ',' -> {
                            advance('[');
                            advance(']');
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
                case DOT_STRING -> {
                    if (Character.isJavaIdentifierPart(i)) {
                        if(i < 128) {
                            token.appendCodePoint(i);
                        } else {
                            token.append(String.format("\\u%04x", i));
                        }
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
                        case '.' -> state.push(State.DOT);
                        case '[' -> {
                            result.append("FusySubjectBuilder.$(");
                            state.push(State.AFTER_ID);
                            state.push(State.BT);
                        }
                        case '!' -> state.push(State.EXCLAMATION);
                        case '"' -> {
                            result.appendCodePoint(i);
                            state.push(State.AFTER_ID);
                            state.push(State.STRING_END);
                            state.push(State.STRING);
                        }
                        case '`' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.RAW_STRING_IN);
                            counter = 1;
                        }
                        case '\'' -> {
                            result.appendCodePoint(i);
                            state.push(State.CHARACTER);
                        }
                        case '(' -> {
                            result.appendCodePoint(i);
                            state.push(State.AFTER_ID);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.EXPRESSION);
                        }
                        case '}' -> state.pop();
                        case ',' -> {
                            advance('}');
                            return advance('{');
                        }
                        case '{' -> {
                            result.append("(");
                            state.push(State.LAMBDA);
                            state.push(State.DISCARD);
                            state.push(State.EXP_END);
                            state.push(State.CB_EXP);
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
                            state.push(State.STRING_END);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '`' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.RAW_STRING_IN);
                            counter = 1;
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
                        case '.' -> state.push(State.DOT);
                        case '!' -> state.push(State.EXCLAMATION);
                        case '\\' -> state.push(State.BACKSLASH);
                        case '<' -> state.push(State.BEAK);
                        default -> {
                            if (Character.isJavaIdentifierPart(i)) {
                                state.push(State.ID);
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
                        case '\n', MANUAL_LF -> {
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
                        case ';' -> {
                            result.append(token);
                            state.pop();
                            state.push(State.NAKED_SCOPE_EXP_STAT);
                            state.push(State.LAMBDA_EXP);
                        }
                        default -> {
                            if (!Character.isWhitespace(i)) {
                                result.append("(var ignored : ");
                                state.pop();
                                state.push(State.NAKED_SCOPE_EXP_STAT);
                                state.push(State.LAMBDA_EXP);
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
                        case '\n', MANUAL_LF, ',' -> {
                            result.append("{");
                            state.pop();
                            state.push(State.AFTER_TRY);
                            state.push(State.SCOPE);
                            return advance(i);
                        }
                        default -> {
                            if (!Character.isWhitespace(i)) {
                                state.pop();
                                state.push(State.AFTER_TRY);
                                state.push(State.NAKED_SCOPE_EXP_STAT);
                                state.push(State.LAMBDA_EXP);
                                result.append("(var ").append(getDebugger().getAutoVar()).append(" = ");
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
                        state.push(State.AFTER_TRY);
                        state.push(State.NAKED_SCOPE_EXP_STAT);
                        state.push(State.LAMBDA_EXP);
                        return advance(i);
                    }
                }
                case CATCH -> {
                    switch (i) {
                        case '\\' -> state.push(State.BACKSLASH);
                        case '#' -> {
                            state.pop();
                            state.pop();
                            var tryIndex = state.lastIndexOf(State.AFTER_TRY);
                            if(tryIndex >= 0) {
                                while (state.size() > tryIndex) {
                                    if (state.peek() != State.STAT_END) {
                                        advance('<');
                                    }
                                    advance(MANUAL_LF);
                                }
                            }
                            result.append("catch(Throwable ");
                            token = new StringBuilder();
                            state.push(State.CSE_ID);
                        }
                        case '\n', MANUAL_LF, ',' -> {
                            state.pop();
                            state.pop();
                            var tryIndex = state.lastIndexOf(State.AFTER_TRY);
                            if(tryIndex >= 0) {
                                while (state.size() > tryIndex) {
                                    if (state.peek() != State.STAT_END) {
                                        advance('<');
                                    }
                                    advance(MANUAL_LF);
                                }
                            }
                            result.append("catch(Throwable $ignored");
                            state.push(State.NAKED_SCOPE_EXP_STAT);
                            return advance(i);
                        }
                        case '.' -> {
                            token = new StringBuilder();
                            state.pop();
                            state.push(State.CATCH_VAR_ID);
                        }
                        default -> {
                            if (!Character.isWhitespace(i)) {
                                state.pop();
                                state.pop();
                                var tryIndex = state.lastIndexOf(State.AFTER_TRY);
                                if(tryIndex >= 0) {
                                    while (state.size() > tryIndex) {
                                        if (state.peek() != State.STAT_END) {
                                            advance('<');
                                        }
                                        advance(MANUAL_LF);
                                    }
                                }
                                result.append("catch(");
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
                            state.push(State.STRING_END);
                            state.push(State.STRING);
                            result.appendCodePoint(i);
                        }
                        case '`' -> {
                            state.push(State.AFTER_ID);
                            state.push(State.RAW_STRING_IN);
                            counter = 1;
                        }
                        case '.' -> state.push(State.DOT);
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
                        state.push(State.AFTER_CASE);
                        state.push(State.SCOPE_END);
                        state.push(State.EMPTY_STATEMENT);
                        result.append("->{\n");
                    } else if (i == ',') {
                        state.pop();
                        state.push(State.AFTER_CASE);
                        state.push(State.SCOPE_END);
                        state.push(State.STAT_END);
                        state.push(State.EMPTY_INLINE_STATEMENT);
                        result.append("->{\n");
                    } else if (Character.isWhitespace(i)) {
                        result.appendCodePoint(i);
                    } else {
                        state.pop();
                        state.push(State.AFTER_CASE);
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
                                state.push(State.AFTER_IF);
                                state.push(State.NAKED_SCOPE_EXP_STAT);
                                state.push(State.LAMBDA_EXP);
                                result.append("else if(");
                                return advance(i);
                            }
                        }
                    }
                }
                case BEFORE_PIN -> {
                    if(Character.isJavaIdentifierStart(i)) {
                        token = new StringBuilder();
                        state.pop();
                        state.push(State.PIN);
                        return advance(i);
                    }
                }
                case PIN -> {
                    if(Character.isJavaIdentifierPart(i)) {
                        token.appendCodePoint(i);
                    } else {
                        result.append(token).append(":");
                        state.pop();
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
            }
            case STATEMENT_VARIABLE -> {
                var $ = subProcessor.finish();
                String exp = $.in(Result.STATEMENTS).asString();
                result.insert(autoVarInsert, "var " + token.toString() + " = " + exp + ";");
                state.pop();
            }
            case NEW_ARRAY -> {
                var $ = subProcessor.finish();
                String arrayInit = $.in(FusArrayProcessor.Result.COMPLETE).asString();
                result.append(arrayInit);
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
