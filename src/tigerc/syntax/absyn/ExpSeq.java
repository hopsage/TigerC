package tigerc.syntax.absyn;

import java.util.List;

public class ExpSeq extends Exp {
	public List<Exp> list;

	public ExpSeq(int p, List<Exp>  l) {
		super(p);
		list = l;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}
