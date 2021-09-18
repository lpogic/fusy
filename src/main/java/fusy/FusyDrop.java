package fusy;

import java.util.Objects;

public class FusyDrop extends RuntimeException {

    Object drop;

    public FusyDrop() {
    }

    public FusyDrop(Object drop) {
        this.drop = drop;
    }

    public Object drop() {
        return drop;
    }

    @Override
    public String toString() {
        return Objects.toString(drop);
    }
}
