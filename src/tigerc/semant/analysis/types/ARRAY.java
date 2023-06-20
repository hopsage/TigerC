package tigerc.semant.analysis.types;

public class ARRAY implements Type {
    public final Type element;

    public ARRAY(Type e) {
        element = e;
    }

    public Type actual() {
        return this;
    }

    public boolean coerceTo(Type t) {
        // Note: array types are nominal, not structural
       // structural version:  return (t instanceof ARRAY) && ((ARRAY) t).element.coerceTo(this.element));
        return this == t.actual();
    }

    public String toString() {
        return "array of " + element.toString();
    }
}
