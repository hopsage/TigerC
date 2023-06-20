package tigerc.syntax.absyn;

import java.util.List; 

public class DeclGroupType extends Decl {
	public final List<DeclTy> decls;

	public DeclGroupType(int p, List<DeclTy> ds) {
		super(p);
		decls = ds;
	}
	
	public void accept(IAbsynVisitor v) {
		v.visit(this);
	}
}
