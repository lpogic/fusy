package fusy.compile;

import java.io.Serializable;
import java.util.function.*;

public class FusyFun {
    public interface FusyFun0<R> extends Supplier<R>, Serializable {
        R apply();

        @Override
        default R get() {
            return apply();
        }
    }
    public interface FusyFun1<A1, R> extends Function<A1, R>, Serializable {
        R apply(A1 a1);
    }
    public interface FusyFun2<A1, A2, R> extends BiFunction<A1, A2, R>, Serializable {
        R apply(A1 a1, A2 a2);
    }
    public interface FusyFun3<A1, A2, A3, R> extends Serializable {
        R apply(A1 a1, A2 a2, A3 a3);
    }
    public interface FusyFun4<A1, A2, A3, A4, R> extends Serializable {
        R apply(A1 a1, A2 a2, A3 a3, A4 a4);
    }
    public interface FusyFun5<A1, A2, A3, A4, A5, R> extends Serializable {
        R apply(A1 a1, A2 a2, A3 a3, A4 a4, A5 a5);
    }
    public interface FusyFun6<A1, A2, A3, A4, A5, A6, R> extends Serializable {
        R apply(A1 a1, A2 a2, A3 a3, A4 a4, A5 a5, A6 a6);
    }
    public interface FusyFun7<A1, A2, A3, A4, A5, A6, A7, R> extends Serializable {
        R apply(A1 a1, A2 a2, A3 a3, A4 a4, A5 a5, A6 a6, A7 a7);
    }
    public interface FusyFun8<A1, A2, A3, A4, A5, A6, A7, A8, R> extends Serializable {
        R apply(A1 a1, A2 a2, A3 a3, A4 a4, A5 a5, A6 a6, A7 a7, A8 a8);
    }
    public interface FusyFun9<A1, A2, A3, A4, A5, A6, A7, A8, A9, R> extends Serializable {
        R apply(A1 a1, A2 a2, A3 a3, A4 a4, A5 a5, A6 a6, A7 a7, A8 a8, A9 a9);
    }

    public interface FusyFun0V extends Runnable, Serializable {
        void apply();

        @Override
        default void run() {
            apply();
        }
    }
    public interface FusyFun1V<A1> extends Consumer<A1>, Serializable {
        void apply(A1 a1);

        @Override
        default void accept(A1 a1) {
            apply(a1);
        }
    }
    public interface FusyFun2V<A1, A2> extends BiConsumer<A1, A2>, Serializable {
        void apply(A1 a1, A2 a2);

        @Override
        default void accept(A1 a1, A2 a2) {
            apply(a1, a2);
        }
    }
    public interface FusyFun3V<A1, A2, A3> extends Serializable {
        void apply(A1 a1, A2 a2, A3 a3);
    }
    public interface FusyFun4V<A1, A2, A3, A4> extends Serializable {
        void apply(A1 a1, A2 a2, A3 a3, A4 a4);
    }
    public interface FusyFun5V<A1, A2, A3, A4, A5> extends Serializable {
        void apply(A1 a1, A2 a2, A3 a3, A4 a4, A5 a5);
    }
    public interface FusyFun6V<A1, A2, A3, A4, A5, A6> extends Serializable {
        void apply(A1 a1, A2 a2, A3 a3, A4 a4, A5 a5, A6 a6);
    }
    public interface FusyFun7V<A1, A2, A3, A4, A5, A6, A7> extends Serializable {
        void apply(A1 a1, A2 a2, A3 a3, A4 a4, A5 a5, A6 a6, A7 a7);
    }
    public interface FusyFun8V<A1, A2, A3, A4, A5, A6, A7, A8> extends Serializable {
        void apply(A1 a1, A2 a2, A3 a3, A4 a4, A5 a5, A6 a6, A7 a7, A8 a8);
    }
    public interface FusyFun9V<A1, A2, A3, A4, A5, A6, A7, A8, A9> extends Serializable {
        void apply(A1 a1, A2 a2, A3 a3, A4 a4, A5 a5, A6 a6, A7 a7, A8 a8, A9 a9);
    }
}
