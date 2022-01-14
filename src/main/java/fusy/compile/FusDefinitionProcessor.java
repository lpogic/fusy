package fusy.compile;

import suite.suite.Subject;

import java.util.Stack;

import static suite.suite.$uite.$;

public class FusDefinitionProcessor extends FusProcessor {

    enum State {
        DISCARD, BEFORE_TYPE, TYPE, HEADER, BODY, ENUM_HEADER, ENUM_PENDING, ENUM_OPTION,
        ENUM_OPTION_CSTR, ENUM_BODY, BEAK, FUSY_FUN, INLINE_BODY,
        BACKSLASH, DOUBLE_BACKSLASH, SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT, MLC_BACKSLASH, MLC_DOUBLE_BACKSLASH,
        RESOURCE_HEADER, METHOD_ARGUMENTS, INSERT, INTERFACE_HEADER, INTERFACE_BODY, TERMINATED, METHOD_BODY,
        RECORD_HEADER, RECORD_COMPONENTS, BEFORE_SINGLETON, SINGLETON
    }

    enum Result {
        STATEMENTS, IMPORTS, SETUP, CATCH_VARS
    }

    Stack<State> state;
    StringBuilder result;
    StringBuilder token;
    FusBodyProcessor parentProcessor;
    FusProcessor subProcessor;
    boolean isImport;
    boolean isSetup;
    boolean isAbstract;
    boolean isPublic;
    boolean throwing;
    boolean isInner;
    Subject imports;
    Subject catchVars;

    public FusDefinitionProcessor(FusBodyProcessor parentProcessor) {
        this.parentProcessor = parentProcessor;
    }

    @Override
    public void getReady() {
        isImport = false;
        isSetup = false;
        isAbstract = false;
        isPublic = true;
        isInner = false;
        throwing = false;
        imports = $();
        catchVars = $();
        result = new StringBuilder();
        state = new Stack<>();
        state.push(State.BEFORE_TYPE);
    }

    public void getReady(String type) {
        isImport = false;
        isSetup = false;
        isAbstract = false;
        isPublic = true;
        isInner = false;
        throwing = false;
        imports = $();
        catchVars = $();
        result = new StringBuilder();
        state = new Stack<>();
        state.push(State.TYPE);
        typeComplete(type);
    }

    @Override
    public FusDebugger getDebugger() {
        return parentProcessor.getDebugger();
    }

    @Override
    public String getCatchVar(String symbol) {
        if(!isInner)return parentProcessor.getCatchVar(symbol);
        var cv = catchVars.in(symbol).set();
        if(cv.absent()) {
            var catchVar = getDebugger().getAutoVar();
            cv.set(catchVar);
            return catchVar;
        }
        return cv.asExpected();
    }

    @Override
    public int advance(int i) {
        switch (state.peek()) {
            case DISCARD -> state.pop();
            case TERMINATED -> parentProcessor.advance(i);
            case BEFORE_TYPE -> {
                switch (i) {
                    case '\\' -> state.push(State.BACKSLASH);
                    case '>' -> {
                        isInner = true;
                        state.push(State.BODY);
                        result.append("{");
                        subProcessor = new FusBodyProcessor(this);
                        subProcessor.getReady();
                    }
                    case '-' -> {
                        result.append("private ");
                        isPublic = false;
                    }
                    case '~' -> {
                        result.append("protected ");
                        isPublic = false;
                    }
                    case '+' -> isPublic = true;
                    case '?' -> {
                        result.append("abstract ");
                        isAbstract = true;
                    }
                    case '*' -> result.append("static ");
                    case '.' -> result.append("final ");
                    case '!' -> throwing = true;
                    case '/' -> typeComplete("void");
                    default -> {
                        if(Character.isWhitespace(i)) {
                            result.appendCodePoint(i);
                        } else {
                            subProcessor = new FusyTypeProcessor(this);
                            subProcessor.getReady();
                            state.pop();
                            state.push(State.TYPE);
                            advance(i);
                        }
                    }
                }
            }
            case HEADER -> {
                if(i == '\\') {
                    state.push(State.BACKSLASH);
                } else if(i == '\n') {
                    state.push(State.BODY);
                    result.append("{\n");
                    subProcessor = new FusBodyProcessor(this);
                    subProcessor.getReady();
                } else if (i == '<') {
                    state.push(State.BEAK);
                } else if(i == '{') {
                    subProcessor = new FusyFunProcessor(this);
                    subProcessor.getReady();
                    state.push(State.FUSY_FUN);
                } else {
                    result.appendCodePoint(i);
                }
            }
            case INTERFACE_HEADER -> {
                if(i == '\\') {
                    state.push(State.BACKSLASH);
                } else if(i == '\n') {
                    state.push(State.INTERFACE_BODY);
                    result.append("{\n");
                    subProcessor = new FusBodyProcessor(this);
                    subProcessor.getReady();
                } else if (i == '<') {
                    state.push(State.BEAK);
                } else if(i == '{') {
                    subProcessor = new FusyFunProcessor(this);
                    subProcessor.getReady();
                    state.push(State.FUSY_FUN);
                } else {
                    result.appendCodePoint(i);
                }
            }
            case RECORD_HEADER -> {
                if(i == '\\') {
                    state.push(State.BACKSLASH);
                } else if(i == '\n') {
                    state.push(State.BODY);
                    result.append("{\n");
                    subProcessor = new FusBodyProcessor(this);
                    subProcessor.getReady();
                } else if (i == '<') {
                    state.push(State.BEAK);
                } else if(i == '(') {
                    result.append("(");
                    state.pop();
                    state.push(State.RECORD_COMPONENTS);
                    var fusBodyProcessor = new FusBodyProcessor(this);
                    fusBodyProcessor.getReady(FusBodyProcessor.State.ARG_TYPE);
                    subProcessor = fusBodyProcessor;
                } else {
                    result.appendCodePoint(i);
                }
            }
            case RESOURCE_HEADER -> {
                switch (i) {
                    case '\\' -> state.push(State.BACKSLASH);
                    case '\n' -> {
                        result.append(";\n");
                        parentProcessor.terminateSubProcess();
                        state.push(State.TERMINATED);
                    }
                    case '(' -> {
                        if(isAbstract) {
                            state.pop();
                            state.push(State.INLINE_BODY);
                            var fusBodyProcessor = new FusBodyProcessor(this);
                            fusBodyProcessor.getReady(FusBodyProcessor.State.STATEMENT);
                            subProcessor = fusBodyProcessor;
                            advance(i);
                        } else {
                            if(parentProcessor != null
                                    && parentProcessor.parentProcessor instanceof FusDefinitionProcessor fdp
                                    && fdp.state.peek() == State.INTERFACE_BODY) {
                                result = new StringBuilder("default ").append(result);
                            }
                            result.append("(");
                            state.pop();
                            state.push(State.METHOD_ARGUMENTS);
                            var fusBodyProcessor = new FusBodyProcessor(this);
                            fusBodyProcessor.getReady(FusBodyProcessor.State.ARG_TYPE);
                            subProcessor = fusBodyProcessor;
                        }
                    }
                    case '=' -> {
                        result.append("=");
                        state.pop();
                        state.push(State.INLINE_BODY);
                        var fusBodyProcessor = new FusBodyProcessor(this);
                        fusBodyProcessor.getReady(FusBodyProcessor.State.STATEMENT);
                        subProcessor = fusBodyProcessor;
                    }
                    case '<' -> state.push(State.BEAK);
                    case '{' -> {
                        subProcessor = new FusyFunProcessor(this);
                        subProcessor.getReady();
                        state.push(State.FUSY_FUN);
                    }
                    default -> result.appendCodePoint(i);
                }
            }
            case ENUM_HEADER -> {
                switch (i) {
                    case '\\' -> state.push(State.BACKSLASH);
                    case '(' -> {
                        state.push(State.ENUM_PENDING);
                        result.append("{");
                    }
                    default -> result.appendCodePoint(i);
                }
            }
            case BEAK -> {
                if (i == '\\') {
                    state.push(State.BACKSLASH);
                } else if(i == '\n') {
                    state.pop();
                    advance('\n');
                    advance('<');
                } else if(i == '<') {
                    state.push(State.BEAK);
                } else if(Character.isWhitespace(i)) {
                    result.appendCodePoint(i);
                } else {
                    result.append("<");
                    state.pop();
                    advance(i);
                }
            }
            case ENUM_PENDING -> {
                if(Character.isJavaIdentifierStart(i)) {
                    state.push(State.ENUM_OPTION);
                    advance(i);
                } else if (i == '\\') {
                    state.push(State.BACKSLASH);
                } else if(i == ')') {
                    result.append(";");
                    subProcessor = new FusBodyProcessor(this);
                    subProcessor.getReady();
                    state.pop();
                    state.push(State.ENUM_BODY);
                } else if(Character.isWhitespace(i)) {
                    result.appendCodePoint(i);
                }
            }
            case ENUM_OPTION -> {
                switch (i) {
                    case '\\' -> state.push(State.BACKSLASH);
                    case ',' -> {
                        result.appendCodePoint(i);
                        state.pop();
                    }
                    case '\n' -> {
                        result.append(",\n");
                        state.pop();
                    }
                    case '(' -> {
                        result.append("(");
                        var fusBodyProcessor = new FusBodyProcessor(this);
                        fusBodyProcessor.getReady(FusBodyProcessor.State.EXPRESSION);
                        subProcessor = fusBodyProcessor;
                        state.pop();
                        state.push(State.DISCARD);
                        state.push(State.ENUM_OPTION_CSTR);
                    }
                    case ')' -> {
                        state.pop();
                        return advance(i);
                    }
                    default -> {
                        if(Character.isJavaIdentifierPart(i)) {
                            result.appendCodePoint(i);
                        }
                    }
                }
            }
            case TYPE, BODY, INLINE_BODY, ENUM_OPTION_CSTR, ENUM_BODY, FUSY_FUN, METHOD_ARGUMENTS,
                    INTERFACE_BODY, METHOD_BODY, RECORD_COMPONENTS -> subProcessor.advance(i);
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
            case INSERT -> {
                if (i == '\\') {
                    state.push(State.BACKSLASH);
                } else if(i == '\n') {
                    getDebugger().pushSource(result.toString().trim());
                    result = new StringBuilder();
                    parentProcessor.terminateSubProcess();
                    state.push(State.TERMINATED);
                } else {
                    result.appendCodePoint(i);
                }
            }
            case BEFORE_SINGLETON -> {
                if(i == '\\') {
                    state.push(State.BACKSLASH);
                } else if(Character.isJavaIdentifierStart(i)) {
                    token = new StringBuilder();
                    state.pop();
                    state.push(State.SINGLETON);
                    return advance(i);
                }
            }
            case SINGLETON -> {
                if (Character.isJavaIdentifierPart(i)) {
                    token.appendCodePoint(i);
                } else {
                    result.append("public $")
                            .append(token)
                            .append(" ")
                            .append(token)
                            .append(" = new $")
                            .append(token)
                            .append("(); public class $")
                            .append(token)
                            .append("{\n");
                    subProcessor = new FusBodyProcessor(this);
                    subProcessor.getReady();
                    state.push(State.BODY);
                    return advance(i);
                }
            }
        }
        return 0;
    }

    public void terminateSubProcess() {
        switch (state.peek()) {
            case TYPE -> {
                var str = subProcessor.finish().in(FusyTypeProcessor.Result.COMPLETE).asString();
                typeComplete(str);
            }
            case BODY, ENUM_BODY, INTERFACE_BODY -> {
                var $ = subProcessor.finish();
                String stats = $.in(FusBodyProcessor.Result.STATEMENTS).asString();
                String defs = $.in(FusBodyProcessor.Result.DEFINITIONS).asString();
                if(!stats.isBlank()) {
                    result.append("{").append(stats).append("}");
                }
                result.append(defs).append("}");
                String imps = $.in(FusBodyProcessor.Result.IMPORTS).asString();
                imports.add(imps);
                parentProcessor.terminateSubProcess();
                state.push(State.TERMINATED);
            }
            case METHOD_BODY -> {
                var $ = subProcessor.finish();
                String stats = $.in(FusBodyProcessor.Result.STATEMENTS).asString();
                result.append("{").append(stats).append("}");
                parentProcessor.terminateSubProcess();
                state.push(State.TERMINATED);
            }
            case INLINE_BODY -> {
                var $ = subProcessor.finish();
                String stats = $.in(FusBodyProcessor.Result.STATEMENTS).asString();
                result.append(stats);
                String imps = $.in(FusBodyProcessor.Result.IMPORTS).asString();
                imports.add(imps);
                parentProcessor.terminateSubProcess();
                state.push(State.TERMINATED);
            }
            case ENUM_OPTION_CSTR -> {
                var $ = subProcessor.finish();
                String stats = $.in(FusBodyProcessor.Result.STATEMENTS).asString();
                result.append(stats).append("),");
                state.pop();
            }
            case FUSY_FUN -> {
                var $ = subProcessor.finish();
                String fun = $.in(FusyFunProcessor.Result.COMPLETE).asString();
                result.append(fun);
                state.pop();
            }
            case METHOD_ARGUMENTS -> {
                var $ = subProcessor.finish();
                String stats = $.in(FusBodyProcessor.Result.STATEMENTS).asString();
                result.append(stats).append(")");
                if(throwing) result.append("throws Throwable");
                subProcessor = new FusBodyProcessor(this);
                subProcessor.getReady();
                state.pop();
                state.push(State.METHOD_BODY);
                state.push(State.DISCARD);
            }
            case RECORD_COMPONENTS -> {
                var $ = subProcessor.finish();
                String stats = $.in(FusBodyProcessor.Result.STATEMENTS).asString();
                result.append(stats);
                state.pop();
                state.push(State.HEADER);
            }
        }
    }

    private void typeComplete(String complete) {
        switch (complete) {
            case "enum" -> {
                if(isPublic) result.append("public ");
                result.append(complete).append(" ");
                state.pop();
                state.push(State.ENUM_HEADER);
            }
            case "import" -> {
                result = new StringBuilder("import ").append(result.toString());
                isImport =  true;
                state.pop();
                state.push(State.INLINE_BODY);
                var fusBodyProcessor = new FusBodyProcessor(this);
                fusBodyProcessor.getReady(FusBodyProcessor.State.STATEMENT);
                subProcessor = fusBodyProcessor;
            }
            case "extends" -> {
                isSetup =  true;
                state.pop();
                state.push(State.INLINE_BODY);
                var fusBodyProcessor = new FusBodyProcessor(this);
                fusBodyProcessor.getReady(FusBodyProcessor.State.STATEMENT);
                subProcessor = fusBodyProcessor;
            }
            case "insert" -> state.push(State.INSERT);
            case "singleton" -> state.push(State.BEFORE_SINGLETON);
            case "throws" -> throwing = true;
            case "default", "final", "native", "static", "strictfp", "transient", "volatile" -> {
                result.append(complete).append(" ");
                state.pop();
                state.push(State.BEFORE_TYPE);
            }
            case "private", "protected" -> {
                isPublic = false;
                result.append(complete).append(" ");
                state.pop();
                state.push(State.BEFORE_TYPE);
            }
            case "abstract" -> {
                isAbstract = true;
                result.append(complete).append(" ");
                state.pop();
                state.push(State.BEFORE_TYPE);
            }
            case "class" -> {
                if(isPublic) result.append("public ");
                result.append(complete).append(" ");
                state.pop();
                state.push(State.HEADER);
            }
            case "record" -> {
                if(isPublic) result.append("public ");
                result.append(complete).append(" ");
                state.pop();
                state.push(State.RECORD_HEADER);
            }
            case "interface" -> {
                if(isPublic) result.append("public ");
                result.append(complete).append(" ");
                state.pop();
                state.push(State.INTERFACE_HEADER);
            }
            case "new" -> {
                if(!(parentProcessor != null
                        && parentProcessor.parentProcessor instanceof FusDefinitionProcessor fdp
                        && fdp.state.peek() == State.ENUM_BODY)
                        && isPublic) {
                    result.append("public ");
                }
                state.pop();
                state.push(State.RESOURCE_HEADER);
            }
            default -> {
                if(isPublic) result.append("public ");
                result.append(complete).append(" ");
                state.pop();
                state.push(State.RESOURCE_HEADER);
            }
        }
    }

    @Override
    public Subject finish() {
        if(isSetup) {
            return $(Result.SETUP, $(result.delete(result.length() - 2, result.length()).toString()));
        } if(isImport) {
            return $(Result.IMPORTS, $($(result.toString())));
        } else {
            return $(
                    Result.STATEMENTS, $(result.toString()),
                    Result.IMPORTS, imports,
                    Result.CATCH_VARS, catchVars
            );
        }
    }
}
