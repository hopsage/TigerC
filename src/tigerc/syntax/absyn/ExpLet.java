package tigerc.syntax.absyn;

import java.util.List;

public class ExpLet extends Exp {
	public List<Decl> decls;
	public Exp body;

	public ExpLet(int p, List<Decl> d, Exp b) {
		super(p);
		decls = d;
		body = b;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}
