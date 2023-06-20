package tigerc.syntax.absyn;

import tigerc.util.Symbol;

public class VarSimple extends Var {
	public Symbol name;

	public VarSimple(int p, Symbol n) {
		super(p);
		name = n;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}
