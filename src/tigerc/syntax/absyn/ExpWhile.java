package tigerc.syntax.absyn;


public class ExpWhile extends Exp {
	public Exp test, body;

	public ExpWhile(int p, Exp t, Exp b) {
		super(p);
		test = t;
		body = b;
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}
