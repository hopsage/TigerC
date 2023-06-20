package tigerc.syntax.absyn;


public class ExpString extends Exp {
	public String value;

	public ExpString(int p, String v) {
		super(p);
		value = v;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}
