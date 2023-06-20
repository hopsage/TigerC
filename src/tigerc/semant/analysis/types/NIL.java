package tigerc.semant.analysis.types;

public class NIL implements Type {
	private NIL() {
	}

	public static NIL inst = new NIL();

	public Type actual() {
		return this;
	}

	public boolean coerceTo(Type t) {
		Type a = t.actual();
		return (a instanceof RECORD) || (a instanceof NIL);
	}

	public String toString() {
		return "nil";
	}
}
