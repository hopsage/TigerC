package tigerc.semant.analysis.types;

public class ERROR implements Type {
    private ERROR() {
    }

    public static ERROR inst = new ERROR();

    public Type actual() {
        return this;
    }

    public boolean coerceTo(Type t) {
        return false;
    }

    public String toString() {
        return "<< error >>";
    }
}
