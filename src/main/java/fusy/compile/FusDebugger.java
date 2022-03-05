package fusy.compile;
import fusy.Fusy;
import suite.suite.Subject;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static suite.suite.$uite.$;

public class FusDebugger extends FusProcessor {

    public String defaultPath = Fusy.home;

    public enum Result {
        CODE, SETUP
    }

    StringBuilder line;
    int lineCounter;
    FusBodyProcessor processor;
    Subject sources;
    int autoVar;

    Subject classpath;
    boolean enableAssertions;

    @Override
    public void getReady() {
        processor = new FusBodyProcessor(this);
        processor.getReady();
        line = new StringBuilder();
        lineCounter = 1;
        sources = $();
        autoVar = 1;
        classpath = $();
        enableAssertions = false;
    }

    public void pushSource(String source) {
        var file = (File)null;
        if(!source.matches("^\\w+:.+") && sources.present()) {
            var currFile = sources.as(File.class);
            file = new File(currFile.getParentFile(), source);
        } else {
            file = new File(source);
        }
        if (sources.present(file))
            throw new DebuggerException(file + " EXCEPTION AT LINE " + lineCounter + ": cyclic insert " + file);
        try {
            var fis = new FileInputStream(file);
            var reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            sources.aimedPut(sources.raw(), file, reader);
        } catch (FileNotFoundException e) {
            throw new DebuggerException(file + " EXCEPTION AT LINE " + lineCounter + ": insert " + file + " not found");
        }
    }

    public String getAutoVar() {
        return "$" + autoVar++;
    }

    @Override
    public FusDebugger getDebugger() {
        return this;
    }

    @Override
    public String getCatchVar(String symbol) {
        return symbol;
    }

    @Override
    public Subject process(String str) {
        getReady();
        pushSource(str);
        while(sources.present()) {
            try {
                int cp = sources.in().as(BufferedReader.class).read();
                if (cp == -1) {
                    sources.unset(sources.raw());
                    advance('\n');
                } else if(cp != '\r') {
                    advance(cp);
                }
            } catch (IOException ioe) {
                throw new DebuggerException(sources.asString() + " EXCEPTION AT LINE " + lineCounter + ": io error");
            }
        }
        return finish();
    }

    @Override
    public int advance(int i) {
        if(i == '\n') {
            line = new StringBuilder();
            ++lineCounter;
        } else {
            line.appendCodePoint(i);
        }
        try {
            return advance(i, false);
        } catch (DebuggerException de) {
            throw de;
        } catch (Exception e) {
            throw new DebuggerException(sources.asString("") + " EXCEPTION AT LINE " + lineCounter + ": " + line.toString());
        }
    }

    public int advance(int i, boolean outputAppend) {
        return outputAppend ? advance(i) : processor.advance(i);
    }

    @Override
    public void terminateSubProcess() {
        throw new DebuggerException(sources.asString("") + " EXCEPTION AT LINE " + lineCounter + ": " + line.toString() +
                "\n Unexpected end of code");
    }

    @Override
    public Subject finish() {
        var $program = processor.finish();
        var setup = $program.in(FusBodyProcessor.Result.SETUP).asString("");
        var program = switch (setup) {
            case "graphic" -> """
                    import static fusy.setup.Common.*;
                    import fusy.Fusy;
                    import static fusy.compile.FusyFun.*;
                    import fusy.compile.FusyDrop;
                    import fusy.compile.FusySubjectBuilder;
                    import suite.suite.Suite;
                    import suite.suite.Subject;
                    import suite.suite.util.Series;
                    import suite.suite.util.Sequence;
                    import java.util.Objects;
                    import java.util.Arrays;
                    import airbricks.Wall;
                    import fusy.setup.Graphic;
                    """ + $program.in(FusBodyProcessor.Result.IMPORTS).asString() + """
                                    
                    @SuppressWarnings("unchecked")
                    class Fus extends Graphic {
                        public static void main(String[] args) throws Exception {
                            Fus.args = Fusy.parseArgs(args);
                            var wallCnf = Suite.set();
                            wallCnf.put(Wall.class, new Fus());
                            wallCnf.put("w", 800);
                            wallCnf.put("h", 800);
                            wallCnf.put("title", "Fusy window");
                            
                            Wall.play(wallCnf);
                            try{System.in.close();}catch(Exception $e){}
                        }
                        
                        static Subject args;
                        
                        public void setup() {
                        """ + $program.in(FusBodyProcessor.Result.STATEMENTS).asString()
                    + "}\n" +
                    $program.in(FusBodyProcessor.Result.DEFINITIONS).asString() + """
                }
                """;
            case "robot" -> """
                import static fusy.setup.Common.*;
                import fusy.Fusy;
                import static fusy.compile.FusyFun.*;
                import fusy.compile.FusyDrop;
                import fusy.compile.FusySubjectBuilder;
                import suite.suite.Suite;
                import suite.suite.Subject;
                import suite.suite.util.Series;
                import suite.suite.util.Sequence;
                import java.util.Objects;
                import java.util.Arrays;
                import fusy.setup.Daemon;
                import static fusy.setup.Daemon.*;
                """ + $program.in(FusBodyProcessor.Result.IMPORTS).asString() + """
                
                @SuppressWarnings("unchecked")
                class Fus extends Daemon {
                    public static void main(String[] args) throws Exception {
                        Fus.args = Fusy.parseArgs(args);
                        new Fus();
                        try{System.in.close();}catch(Exception $e){}
                    }
                    
                    static Subject args;
                    
                    Fus() throws Exception {
                    """ + $program.in(FusBodyProcessor.Result.STATEMENTS).asString()
                    + "}\n" +
                    $program.in(FusBodyProcessor.Result.DEFINITIONS).asString() + """
                }
                """;
            case "console", "" -> """
                import static fusy.setup.Common.*;
                import fusy.Fusy;
                import static fusy.compile.FusyFun.*;
                import fusy.compile.FusyDrop;
                import fusy.compile.FusySubjectBuilder;
                import suite.suite.Suite;
                import suite.suite.Subject;
                import suite.suite.util.Series;
                import suite.suite.util.Sequence;
                import java.util.Objects;
                import java.util.Arrays;
                import fusy.setup.Console;
                import static fusy.setup.Console.*;
                """ + $program.in(FusBodyProcessor.Result.IMPORTS).asString() + """
                
                @SuppressWarnings("unchecked")
                class Fus extends Console {
                    public static void main(String[] args) throws Exception {
                        Fus.args = Fusy.parseArgs(args);
                        new Fus();
                        try{System.in.close();}catch(Exception $e){}
                    }
                    
                    static Subject args;
                    
                    Fus() throws Exception {
                    """ + $program.in(FusBodyProcessor.Result.STATEMENTS).asString()
                    + "}\n" +
                    $program.in(FusBodyProcessor.Result.DEFINITIONS).asString() + """
                }
                """;

            default -> throw new DebuggerException("Unsupported setup " + setup);
        };

        for(var fis : sources.eachIn().each(BufferedReader.class)) {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return $(Result.CODE, $(program),
                Result.SETUP, $(setup));
    }

    public void buildAction(String str) {
        var spl = str.split("\\s");
        if(spl.length > 0) {
            switch(spl[0]) {
                case "classpath" -> {
                    for(int i = 1;i < spl.length; ++i) {
                        if(!spl[i].isBlank()) classpath.set(spl[i]);
                    }
                }
                case "assert" -> {
                    enableAssertions = true;
                }
                default -> {
                    throw new DebuggerException("Undefined compiler action '" + str + "'.");
                }
            }
        }
    }

    public String getClasspath() {
        var sb = new StringBuilder();
        var c = classpath.eachString().cascade();
        if(c.hasNext()) sb.append(c.next());
        for(var s : c) sb.append(";").append(s);
        return sb.toString();
    }

    public boolean assertionsEnabled() {
        return enableAssertions;
    }
}
