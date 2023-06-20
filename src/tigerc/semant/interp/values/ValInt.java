package tigerc.semant.interp.values;


public class ValInt implements IValue {
    public static final ValInt ZERO = new ValInt(0), 
            ONE = new ValInt(1), TWO = new ValInt(2), 
            THREE = new ValInt(3), FOUR = new ValInt(4);
    
    public final int val;

    public ValInt(int v) {
        val = v;
    }

    public String toString() {
        return "" + val;
    }
}
