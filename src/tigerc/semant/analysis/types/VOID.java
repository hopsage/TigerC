package tigerc.semant.analysis.types;

public class VOID implements Type {
	private VOID() {
	}

	public static VOID inst = new VOID();

	public Type actual() {
		return this;
	}

	public boolean coerceTo(Type t) {
		return (t.actual() instanceof VOID);
	}

	public String toString() {
		return "void";
	}
}
