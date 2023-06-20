package tigerc.syntax.absyn;


public interface IAbsyn extends ISyntaxElt, IVisitable {
	// int getPos();  
	// Returns position in the source file beginning the 
	// construct to which this IAbsyn object corresponds
	
	// void accept(IAbsynVisitor v);
	// To support various behavior foldings on abstract syntax trees
}
