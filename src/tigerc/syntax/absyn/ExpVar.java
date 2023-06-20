package tigerc.syntax.absyn;


public class ExpVar extends Exp {
	public Var var;

	public ExpVar(int p, Var v) {
		super(p);
		var = v;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}
