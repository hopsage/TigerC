package tigerc.util;

public class Pair<A, B> {
    public final A fst;
    public final B snd;

    public Pair(A a, B b) {
        fst = a;
        snd = b;
    }

    /**
     * Static factory method, supporting construction with type inference
     * 
     * @param left
     * @param right
     * @return a new Pair object on each call
     */
    public static <C, D> Pair<C, D> of(C left, D right) {
        return new Pair<C, D>(left, right);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Pair) && ((Pair) o).fst.equals(fst)
                && ((Pair) o).snd.equals(snd);
    }

    @Override
    public String toString() {
        return "(" + fst + "," + snd + ")";
    }
}
