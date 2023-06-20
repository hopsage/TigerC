package tigerc.semant.interp.values;

public class ValNil implements IValue {
    public String toString() {  return "nil"; }
    
    public static final ValNil inst = new ValNil();
    
    private ValNil() {}
}
