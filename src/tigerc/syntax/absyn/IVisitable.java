package tigerc.syntax.absyn;

public interface IVisitable {
	void accept(IAbsynVisitor v);
}
