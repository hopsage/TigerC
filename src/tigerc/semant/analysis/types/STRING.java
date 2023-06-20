package tigerc.semant.analysis.types;

public class STRING implements Type {
	private STRING() {
	}

	public static STRING inst = new STRING();

	public Type actual() {
		return this;
	}

	public boolean coerceTo(Type t) {
		return (t.actual() instanceof STRING);
	}

	public String toString() {
		return "string";
	}
}
