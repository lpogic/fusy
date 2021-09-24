package fusy;

import java.util.Iterator;

public interface Repeater<T> extends Iterator<T> {
    @Override
    default boolean hasNext() {
        return true;
    }
}
