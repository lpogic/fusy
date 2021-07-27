package fusy;

import suite.suite.Subject;

import static suite.suite.$uite.$;

public class FusDefinitionProcessor implements FusProcessor {

    enum State {
        HEADER, BODY, BACKSLASH
    }

    enum Result {
        COMPLETE
    }

    Subject $state;
    StringBuilder result;
    FusBodyProcessor parentProcess;
    FusBodyProcessor subProcess;

    public FusDefinitionProcessor(FusBodyProcessor parentProcess) {
        this.parentProcess = parentProcess;
    }

    @Override
    public void getReady() {
        result = new StringBuilder();
        $state = $($(State.HEADER));
    }

    @Override
    public void advance(int i) {
        switch ($state.in().as(State.class)) {
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
            case BODY -> subProcess.advance(i);
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
        }
    }

    public void terminateSubProcess() {
        if($state.in().raw() != State.BODY) return;
        var $ = subProcess.finish();
        String stats = $.in(FusBodyProcessor.Result.STATEMENTS).asString();
        String defs = $.in(FusBodyProcessor.Result.DEFINITIONS).asString();
        result.append(stats).append(defs).append("}");
        parentProcess.terminateSubProcess();
    }

    @Override
    public Subject finish() {
        return $(Result.COMPLETE, $(result.toString()));
    }
}
