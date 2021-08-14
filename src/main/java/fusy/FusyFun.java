package fusy;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class FusyFun {
    public interface FusyFun0<R> extends Supplier<R> {
        R apply();
    }
    public interface FusyFun1<A1, R> extends Function<A1, R> {
        R apply(A1 a1);
    }
    public interface FusyFun2<A1, A2, R> extends BiFunction<A1, A2, R> {
        R apply(A1 a1, A2 a2);
    }
    public interface FusyFun3<A1, A2, A3, R> {
        R apply(A1 a1, A2 a2, A3 a3);
    }
    public interface FusyFun4<A1, A2, A3, A4, R> {
        R apply(A1 a1, A2 a2, A3 a3, A4 a4);
    }
    public interface FusyFun5<A1, A2, A3, A4, A5, R> {
        R apply(A1 a1, A2 a2, A3 a3, A4 a4, A5 a5);
    }
    public interface FusyFun6<A1, A2, A3, A4, A5, A6, R> {
        R apply(A1 a1, A2 a2, A3 a3, A4 a4, A5 a5, A6 a6);
    }
    public interface FusyFun7<A1, A2, A3, A4, A5, A6, A7, R> {
        R apply(A1 a1, A2 a2, A3 a3, A4 a4, A5 a5, A6 a6, A7 a7);
    }
    public interface FusyFun8<A1, A2, A3, A4, A5, A6, A7, A8, R> {
        R apply(A1 a1, A2 a2, A3 a3, A4 a4, A5 a5, A6 a6, A7 a7, A8 a8);
    }
    public interface FusyFun9<A1, A2, A3, A4, A5, A6, A7, A8, A9, R> {
        R apply(A1 a1, A2 a2, A3 a3, A4 a4, A5 a5, A6 a6, A7 a7, A8 a8, A9 a9);
    }
}
