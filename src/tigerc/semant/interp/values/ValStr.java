package tigerc.semant.interp.values;


public class ValStr implements IValue {
    public final String val;

    public ValStr(String s) {
        val = s;
    }

    public String toString() {
        return val;
    }

}
