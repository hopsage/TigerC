package tigerc.syntax.absyn;

import tigerc.util.Symbol;

public class TyArray extends Ty {
	public Symbol typ; // element type

	public TyArray(int p, Symbol t) {
		super(p);
		typ = t;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}
