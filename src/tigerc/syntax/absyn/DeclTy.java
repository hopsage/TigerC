package tigerc.syntax.absyn;

import tigerc.util.Symbol;


public class DeclTy implements ISyntaxElt {
	public final Symbol name;
	public final Ty ty;
	public final int pos;
	
	public DeclTy(int p, Symbol id, Ty t) {
		pos = p;
		name = id;
		ty = t;
	}
	
	public int getPos() { return pos; }
}
