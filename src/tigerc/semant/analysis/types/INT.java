package tigerc.semant.analysis.types;

public class INT implements Type {
    private INT() {
    }

    public static INT inst = new INT();

    public static INT instance() {
        return inst;
    }

    public Type actual() {
        return this;
    }

    public boolean coerceTo(Type t) {
        return (t.actual() instanceof INT);
    }

    public String toString() {
        return "int";
    }
}
