package tigerc.syntax.absyn;

import java.util.List;

public class DeclGroupFunction extends Decl {
	public final List<DeclFn> fns;

	public DeclGroupFunction(int p, List<DeclFn> fs) {
		super(p);
		fns = fs;
	}
	
	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}
