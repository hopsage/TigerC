package tigerc.syntax.absyn;


public class VarSubscript extends Var {
	public Var var;
	public Exp index;

	public VarSubscript(int p, Var v, Exp i) {
		super(p);
		var = v;
		index = i;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}
