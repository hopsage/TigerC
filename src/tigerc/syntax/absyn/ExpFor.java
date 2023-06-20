package tigerc.syntax.absyn;

import tigerc.util.Symbol;

public class ExpFor extends Exp {
	public final Symbol var;
	public final Exp lo, hi, body;

	public ExpFor(int p, Symbol i, Exp l, Exp h, Exp b) {
		super(p);
		var = i;
		lo = l;
		hi = h;
		body = b;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}
