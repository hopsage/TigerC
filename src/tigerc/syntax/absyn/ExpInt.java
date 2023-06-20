package tigerc.syntax.absyn;


public class ExpInt extends Exp {
	public int value;

	public ExpInt(int p, int v) {
		super(p);
		value = v;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}
