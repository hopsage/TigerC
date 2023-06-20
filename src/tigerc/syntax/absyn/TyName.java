package tigerc.syntax.absyn;

import tigerc.util.Symbol;

public class TyName extends Ty {
	public final Symbol name;

	public TyName(int p, Symbol n) {
		super(p);
		name = n;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}
