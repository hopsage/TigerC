package tigerc.translate;

import tigerc.syntax.absyn.IAbsyn;

public interface ICodegen {
    /**
     * There is almost always some amount of boilerplate that must begin an
     * executable file. See, for example "0xCAFEBABE".
     * 
     * @param initComment
     */
    void emitPrelude(String initComment);

    /**
     * Emit code for each defined function/method/procedure
     */
    void emitProcedures();

    /**
     * Code for the "main" procedure, if applicable (in Tiger, it always is)
     */
    void emitMain();

    /**
     * Set the AST from which we'll generate code and the name + extension of
     * the generated executable file
     * 
     * @param prog
     *            - The abstract syntax tree of the program we're translating
     * @param outFName
     *            - The name of the compiled executable
     * @param nameExtension
     *            - The extension to add to the compiled executable file name
     */
    void setProg(IAbsyn prog, String outFName, String nameExtension);
    
}
