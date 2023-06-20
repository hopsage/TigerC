package tigerc.syntax.absyn;

import tigerc.util.Symbol;
import tigerc.util.Pair;

import java.util.List;

public class DeclFn implements ISyntaxElt {
	public final int pos;
	public final Symbol name;
	public final List<Pair<Symbol, Symbol>> params;  // (id,type) pairs
	public final TyName resultTy; /* optional */
	public final Exp body;
	
	public DeclFn(int p, Symbol n, List<Pair<Symbol, Symbol>> a, TyName r, Exp b) {
		pos = p;
		name = n;
		params = a;
		resultTy = r;
		body = b;
	}
	
	@Override
	public int getPos() {
		return pos;
	}

}
