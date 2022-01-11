package fusy.compile;

import suite.suite.SolidSubject;
import suite.suite.Subject;
import suite.suite.Suite;
import suite.suite.action.Action;
import suite.suite.action.Statement;

import java.util.function.Supplier;

public class FusySubjectBuilder {

    public static class SubjectBlock {
        Subject subject;

        public SubjectBlock(Subject subject) {
            this.subject = subject;
        }
    }

    public static Subject $() {
        return new SolidSubject();
    }

    public static Subject $(Object o) {
        if(o instanceof Subject $o) {
            return $o;
        } else if(o instanceof SubjectBlock subjectBlock) {
            return $().inset(subjectBlock.subject);
        } else {
            if(o instanceof Suite.Mask m) o = m.object;
            return $().set(o);
        }
    }

    public static Subject $(Action a) {
        return $().set(a);
    }

    public static Subject $(Statement s) {
        return $().set(s);
    }

    public static Subject $(Supplier<?> e) {
        return $().set(e);
    }

    public static Subject $(Object o0, Object ... o) {
        boolean lastSubject = o0 instanceof SubjectBlock;
        Subject $;
        Object ol;
        if(lastSubject) {
            $ = $(o0);
            ol = null;
        } else {
            $ = $();
            ol = o0 instanceof Suite.Mask m ? m.object : o0;
        }
        for(var oi : o) {
            if (lastSubject) {
                if (oi instanceof SubjectBlock $i) {
                    $.inset($i.subject);
                } else {
                    ol = oi instanceof Suite.Mask m ? m.object : oi;
                    lastSubject = false;
                }
            } else {
                if (oi instanceof SubjectBlock $i) {
                    $.inset(ol, $i.subject);
                    lastSubject = true;
                } else {
                    $.set(ol);
                    ol = oi instanceof Suite.Mask m ? m.object : oi;
                }
            }
        }
        if(!lastSubject) $.alter(Suite.set(ol));
        return $;
    }

    public static SubjectBlock $$() {
        return new SubjectBlock($());
    }

    public static SubjectBlock $$(Object o) {
        return new SubjectBlock($(o));
    }

    public static SubjectBlock $$(Action a) {
        return new SubjectBlock($(a));
    }

    public static SubjectBlock $$(Statement s) {
        return new SubjectBlock($(s));
    }

    public static SubjectBlock $$(Supplier<?> e) {
        return new SubjectBlock($(e));
    }

    public static SubjectBlock $$(Object o0, Object ... o) {
        return new SubjectBlock($(o0, o));
    }
}
