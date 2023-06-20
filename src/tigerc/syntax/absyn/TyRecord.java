package tigerc.syntax.absyn;

import tigerc.util.Symbol;
import tigerc.util.Pair;

import java.util.List;

public class TyRecord extends Ty {
	public List<Pair<Symbol, Symbol>> fields;

	public TyRecord(int p, List<Pair<Symbol, Symbol>> f) {
		super(p);
		fields = f;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}
