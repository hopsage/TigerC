package tigerc.semant.interp.values;

public class ValUnit implements IValue {
    public static final ValUnit inst = new ValUnit();
    
    private ValUnit() {}
    
    public String toString() { return "()"; }
}
