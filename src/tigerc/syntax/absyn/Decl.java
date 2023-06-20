package tigerc.syntax.absyn;

abstract public class Decl implements IAbsyn {
	protected final int pos;
	
	protected Decl(int p) { pos =p; }
	
	public int getPos() { return pos; }
}
