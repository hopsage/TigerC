package tigerc.syntax.absyn;

public interface IAbsynVisitor {

    // NOTE: the visit methods are only defined on the concrete subclasses

    /**
     * procedure declarations
     * 
     * Note: procedure declarations that happen in a block are considered
     * mutually recursive. For example,
     * 
     * let function even(x:int):int = if x = 0 then 1 else not(odd(x-1))
     * function odd(x:int):int = if x = 1 then 1 else not(even(x-1)) var x:int
     * := 15 function f(n:int):int = if even(n) then n/2 else 3*n + 1 in f(x)
     * end
     * 
     * even() and odd() can reference each other, but neither one can call f()
     * 
     * We treat this block grouping as a syntactic construct.
     * 
     * @param d
     */
    void visit(DeclGroupFunction d);

    /**
     * type declarations
     * 
     * Note: type definitions can also be mutually recursive. As with functions,
     * the definitions must be in a contiguous block for this. Indeed, the
     * syntax of type declarations forms its own syntactic category, as the RHS
     * of the definition can specify type aliases, new record types, and new
     * array types.  See:  TyArray, TyName, TyRecord (below).
     * 
     * @param d
     */
    void visit(DeclGroupType d);

    /**
     * simple identifier declarations E.g., let var s:str := "hello, world." in
     * print(s) end
     * 
     * @param d
     */
    void visit(DeclVar d);

    /**
     * array construction expressions
     * 
     * @param e
     */
    void visit(ExpArray e);

    /**
     * assignment statements
     * 
     * @param e
     */
    void visit(ExpAssign e);

    /**
     * break, as in C/C++/C#/Java
     * 
     * @param e
     */
    void visit(ExpBreak e);

    /**
     * procedure calls
     * 
     * @param e
     */
    void visit(ExpCall e);

    /**
     * Bounded (definite) loop form
     * 
     * @param e
     */
    void visit(ExpFor e);

    /**
     * conditional statements (no else)
     * 
     * @param e
     */
    void visit(ExpIf e);

    /**
     * conditionals, with else
     * 
     * @param e
     */
    void visit(ExpIfElse e);

    /**
     * integer constants
     * 
     * @param e
     */
    void visit(ExpInt e);

    /**
     * local declarations (of variables, procedures, and types)
     * 
     * @param e
     */
    void visit(ExpLet e);

    /**
     * the constant record or array value, nil
     * 
     * @param e
     */
    void visit(ExpNil e);

    /**
     * binary operator application
     * 
     * Note that, although unary minus is part of Tiger syntax, our parser
     * builds "-x" as "0 - x".
     * 
     * @param e
     */
    void visit(ExpOp e);

    /**
     * record construction: roughly the same thing as a constructor call in an
     * OO language
     * 
     * @param e
     */
    void visit(ExpRecord e);

    /**
     * sequential execution of statements
     * 
     * @param e
     */
    void visit(ExpSeq e);

    /**
     * string constants
     * 
     * @param e
     */
    void visit(ExpString e);

    /**
     * variable access
     * 
     * Note that "variable" refers to any named memory location, as either an
     * lvalue or rvalue. This includes not only simple identifiers but also
     * record field access and array element access. Indeed, like type
     * definition expressions, "variable" is its own syntactic category (see:
     * VarSimple, VarField, VarSubscript).
     * 
     * @param e
     */
    void visit(ExpVar e);

    /**
     * general loop construct
     * 
     * @param e
     */
    void visit(ExpWhile e);

    /**
     * type definitions:  array types
     * @param t
     */
    void visit(TyArray t);

    /**
     * type definitions:  type aliases
     * @param t
     */
    void visit(TyName t);

    /**
     * type definitions:  record types
     * @param t
     */
    void visit(TyRecord t);

    /**
     * lvalues:  record field access
     * @param t
     */
    void visit(VarField v);

    /**
     * lvalues:  simple variable access
     * @param t
     */
    void visit(VarSimple v);

    /**
     * lvalues:  array element access
     * @param t
     */
    void visit(VarSubscript v);
}
