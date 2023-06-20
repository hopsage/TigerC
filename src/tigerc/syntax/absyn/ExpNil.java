package tigerc.syntax.absyn;


public class ExpNil extends Exp {
	public ExpNil(int p) {
		super(p);
	}

	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}
