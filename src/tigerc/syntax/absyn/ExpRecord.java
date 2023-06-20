package tigerc.syntax.absyn;

import tigerc.util.Symbol;
import tigerc.util.Pair;

import java.util.List;

public class ExpRecord extends Exp {
	public final Symbol type;
	public final List<Pair<Symbol, Exp>> fields;

	public ExpRecord(int p, Symbol t, List<Pair<Symbol, Exp>> f) {
		super(p);
		type = t;
		fields = f;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}
