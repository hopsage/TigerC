/*************************************************************************
 *  tigerc/src/semant/analysis/SemantV.java 
 *
 *  Author:  John Lasseter
 *
 *  This is the workhorse of the "middle end".  It is primarily responsible 
 *  for typechecking, and that is the version you see here.   The full version
 *  also serves as the glue between the front and back ends.  In particular,
 *  the code generation stage -- whether we translate to an internal 
 *  three-address code form or write raw assembler to the output -- requires 
 *  access to the computed type information of each expression and declaration.
 * 
 *  History: 11-21-2011 (jhel) created
 *           05/02/2014 (jhel) added ErrorMsg err field (refactoring from EnvTrans)
 *                             refactored "env.err."  accesses.
 *           01/19/2016 (jhel) refactored to use simpler Env interface
 *           
 *           
 ************************************************************************/

package tigerc.semant.analysis;

import tigerc.util.Symbol;
import tigerc.util.ErrorMsg;
import tigerc.syntax.absyn.*;
import tigerc.semant.Env;
import tigerc.semant.analysis.types.*;
import tigerc.util.Pair;

import java.util.List;
import java.util.Collection;

// (TODO) Prop 1:  \forall e \in (Exp - Decl*), if e contains an error, then e:ERROR

public class SemantV implements IAbsynVisitor {

    /*************************************************************************
     * private attributes (grouped/documented according to their purpose here)
     */
    private Type ty = null;
    // The "return value" of each visit() call is stored in ty

    private int loopNesting = 0;
    /*
     * Set to true only in the body of a while or for-loop Used to check that
     * "break" is properly nested Will break in the presence of function
     * definitions that are nested inside of loop bodies (hence a new instance
     * of SemantV should be created for typechecking each function definition).
     */

    private boolean declsOK = true;
    /*
     * A flag to indicate when a function declaration block contains an error.
     * It may be cleared in visit(DeclGroupFunction), and it is read in
     * visit(ExpLet). It is never set, except at construction time.
     */

    /*
     * Local environments: since Tiger semantics allows the same identifier to
     * be used for a type and a variable/procedure name, we keep separate name
     * spaces for types and var/procedure bindings
     */
    private Env<Entry> venv; // (variable / type identifier bindings)
    private Env<Type> tenv; // (type identifier / type bindings)

    /*
     * Components to support external bindings. Right now, that just means the
     * standard library, which in our case is implemented by the Java class
     * TigerStdLib
     */
    private static Env<Entry> extern_venv;
    private static Env<Type> extern_tenv;

    static {
        SemantV.extern_venv = Env.instance_noparent(); // new Env<Entry>();
        SemantV.extern_tenv = Env.instance_noparent(); // new Env<Type>();
        SemantV.setupStdLibrary();
    }

    // Finally, an error message reporting object:
    private ErrorMsg err;

    /*********************************************************************/

    public SemantV(ErrorMsg e) {
        this(new Env<Entry>(SemantV.extern_venv), new Env<Type>(SemantV.extern_tenv), e);
    }

    private SemantV(Env<Entry> ve, Env<Type> te, ErrorMsg err) {

        assert ve != null && te != null && err != null;

        this.venv = ve;
        this.tenv = te;
        this.err = err;

    }

    public Type getType() {
        return ty;
    }

    /*********************************************************************/

    public void visit(DeclGroupFunction fun_decs) {
        /*
         * Declarations of function groups occur only in let expressions. A new
         * scope has already been declared there, and it will end at the end of
         * the let-body. Hence, we do not open a new scope here for the names.
         * However, a new scope _is_ opened for the body (since the formals must
         * be added to env).
         */

        /*
         * FIRST PASS: Enter the signature of each function in this declaration
         * block.
         */

        for (DeclFn dfn : fun_decs.fns) {
            // A. Check that the result type makes sense
            Type t_result;
            if (dfn.resultTy != null) {
                dfn.resultTy.accept(this);
                // Give this visitor to the resultTy object, in order to
                // correctly dispatch the visit method (one of the
                // visit(Ty*** ) methods)
                t_result = this.ty;
                this.declsOK = this.declsOK && (this.ty != ERROR.inst);
            } else {
                // dfn has no declared return type (i.e. a procedure).
                t_result = VOID.inst;
            }

            // B. Check the list of formals & build the FunEntry component
            Pair<List<Pair<Symbol, Type>>, Boolean> formals_construction = visitTypeFields(dfn, dfn.params);

            // C. Put the function in venv, but only if it's a new binding for
            // this scope
            boolean declWasAdded = this.venv.extend(dfn.name, new FunEntry(formals_construction.fst, t_result));
            if (!declWasAdded) {
                err.error(dfn.getPos(), "Symbol " + dfn.name + " is already defined in this scope.");
                this.declsOK = false;
            }
            this.declsOK = this.declsOK && formals_construction.snd;
        }

        /*
         * SECOND PASS: Check the body of each function in this declaration
         * block. For each of the parameters, we make a new VarEntry in venv,
         * for the duration of the time we spend checking that function's body.
         * The body's type much match the declared return type
         */
        for (DeclFn dfn : fun_decs.fns) {
            FunEntry curEntr = (FunEntry) this.venv.lookup(dfn.name);
            // Note: this cast is safe, since we have just made FunEntry
            // bindings for every element in d.fns

            // (1) Open a new scope, and enter the formals in venv. Note that
            // this is a shortcut from the "real" interpreter, where we would
            // have to use the environment stored with each function's closure,
            // in order to implement lexical scope.
            this.venv.beginScope();

            for (Pair<Symbol, Symbol> p : dfn.params) {
                this.venv.extend(p.fst, new VarEntry(this.tenv.lookup(p.snd)));
                // Note: if p.snd does not refer to a legitimate type
                // there will be a tenv binding to ERROR: see visitTypeFields
            }

            // (2) Now create a new Semant for the body of cur.
            // Why? Because certain matters of nesting and scope are "reset"
            // inside a new function body. In particular, you can use this to
            // leverage the checking of correct nesting of break statements.
            // You'll have to think about how. This is a hint here.

            SemantV newSem = new SemantV(this.venv, this.tenv, this.err);
            dfn.body.accept(newSem);
            Type bodyTy = newSem.ty;

            // Done with checking the current body. Close this scope.
            this.venv.endScope();

            // Did the type of the function body match what was declared?
            if (!bodyTy.coerceTo(curEntr.result)) {
                err.error(dfn.body.getPos(), "Return value of function body must be of type " + curEntr.result);
                declsOK = false;
            }
        } // for
    }

    public void visit(DeclGroupType dts) {
        /*
         * Declarations of types occur only in let expressions. A new scope has
         * already been declared there, and it will end at the end of the
         * let-body. Hence, we do not open a new scope here.
         */

        // PASS ONE: Add the names to tenv
        for (DeclTy dt : dts.decls) {
            this.tenv.extend(dt.name, new THUNK(dt.name));
        }

        // PASS TWO: Compute the actual bindings
        for (DeclTy dt : dts.decls) {
            dt.ty.accept(this);
            this.declsOK = this.declsOK && (this.ty != ERROR.inst);
            Type t_binding = ty;
            THUNK namet = (THUNK) this.tenv.lookup(dt.name);
            // Safe, since we just put these same symbols in env.
            namet.bind(t_binding);
            if (namet.isLoop()) {
                err.error(dts.getPos(), "Cycle detected in type declaration");
                this.declsOK = false;
            }
        }
    }

    public void visit(DeclVar d) {
        /*
         * Declarations of variables occur only in let expressions. A new scope
         * has already been declared there, and it will end at the end of the
         * let-body. Hence, we do not open a new scope here.
         */

        d.init.accept(this);
        Type t_init = this.ty;
        boolean sound = true;

        // All the ways a declaration can be unsound..
        if (t_init.coerceTo(VOID.inst)) {
            err.error(d.init.getPos(), "Cannot initialize with VOID type.");
            sound = false;
        }

        if (d.typ != null) {
            Type tid = tenv.lookup(d.typ);
            if (tid == null) {
                err.error(d.getPos(), "Type " + d.name + "is not defined in this scope.");
                sound = false;
            } else {
                assert !(tid.coerceTo(NIL.inst) || tid.coerceTo(VOID.inst));
                // Because such a binding should never be entered

                if (!t_init.coerceTo(tid)) {
                    err.error(d.getPos(), "Initializing expression is of " + "incompatible type for variable " + d.name
                            + " << expected: " + tid + ", found: " + t_init + " >>.");
                    sound = false;
                }
            }
        } else if (t_init.coerceTo(NIL.inst)) {
            err.error(d.getPos(), "Cannot determine type of variable " + d.name + " from nil initialization.");
            sound = false;
        }

        if (sound) {
            // OK: the type can be inferred from the d.init
            venv.extend(d.name, new VarEntry(t_init));
        }

        this.declsOK = this.declsOK && sound;
    }

    public void visit(ExpArray e) {
        assert e != null && e.typ != null && e.size != null && e.init != null;

        Type t = tenv.lookup(e.typ);
        if (t == null) {
            err.error(e.getPos(), "Undefined type.");
            this.ty = ERROR.inst;
        } else if (t.actual() instanceof ARRAY) {
            ARRAY a = (ARRAY) t.actual();

            e.size.accept(this);
            Type t_size = this.ty;

            e.init.accept(this);
            Type t_init = this.ty;

            if (!t_size.coerceTo(INT.inst)) {
                err.error(e.size.getPos(), "Array size must be of type INT.");
                this.ty = ERROR.inst;
            } else if (!t_init.coerceTo(a.element)) {
                err.error(e.init.getPos(), "Initial expression is of incompatible type " + "for array element"
                        + "  << expected: " + a.element + ", found: " + t_init + " >>");
                this.ty = ERROR.inst;
            } else
                this.ty = t;
        } else {
            err.error(e.getPos(), "Attempt to use non-ARRAY type " + t + " as if it were an ARRAY.");
            this.ty = ERROR.inst;
        }
    }

    public void visit(ExpAssign e) {
        assert e != null && e.lhs != null && e.rhs != null;

        // First make sure the LHS is assignable (i.e., not a for-loop index)

        if (e.lhs instanceof VarSimple) {
            // Because all for-loop index vars are VarSimple

            Entry en = venv.lookup(((VarSimple) e.lhs).name);
            if (en != null && en instanceof VarEntry) {
                if (!((VarEntry) en).assignable) {
                    err.error(e.lhs.getPos(), "Variable " + ((VarSimple) e.lhs).name + " is not assignable.");
                    this.ty = ERROR.inst;
                    return;
                }
            }
        }

        // Okay: now proceed with ordinary type-checking. If we have a VarSimple
        // on the LHS, this is a little wasteful, since we just looked that up.
        // Also,
        // the error reporting could be made a bit more accurate here, since the
        // potential for type errors on the RHS varies, depending on whether the
        // LHS
        // is an ERROR type.

        e.lhs.accept(this);
        Type t_lhs = this.ty;

        e.rhs.accept(this);
        Type t_rhs = this.ty;

        if (!t_rhs.coerceTo(t_lhs)) {
            err.error(e.getPos(),
                    "Assignment between incompatible types  " + "<< expected: " + t_lhs + ", found: " + t_rhs + " >>");
            this.ty = ERROR.inst;
        } else if (t_rhs.coerceTo(VOID.inst)) {
            err.error(e.getPos(), "Cannot make an assignment of VOID types.");
            this.ty = ERROR.inst;
        } else {
            this.ty = VOID.inst;
        }

    }

    public void visit(ExpBreak e) {
        assert e != null;

        if (loopNesting > 0) {
            this.ty = VOID.inst;
        } else {
            err.error(e.getPos(), "BREAK not properly nested.");
            this.ty = ERROR.inst;
        }
    }

    public void visit(ExpCall e) {
        // e = f(args)
        // rule: tenv,env |- f:'a -> 'b tenv,env |- args:'a
        // --------------------------------------------------
        // tenv,env |- f(args): 'b

        assert e != null && e.func != null;

        Entry en = venv.lookup(e.func);

        if (en == null) { // no entry for e.func
            err.error(e.getPos(), "Symbol " + e.func + " is undefined.");
            ty = ERROR.inst;

        } else if (en instanceof FunEntry) {
            FunEntry f = ((FunEntry) en);
            int numParms = f.formals.size();
            int numArgs = e.args.size();
            boolean sound = true;

            if (numParms != numArgs) {
                err.error(e.getPos(), "Wrong number of arguments for function " + e.func + ".");
                sound = false;
            }

            for (int i = 0; i < numParms && i < numArgs; i++) {
                Exp arg = e.args.get(i);
                Pair<Symbol, Type> param = f.formals.get(i);
                arg.accept(this); // typecheck the next argument
                if (!ty.coerceTo(param.snd)) {
                    // Is its type compatible with the parameter?
                    err.error(arg.getPos(), "Incompatible argument type for function " + e.func + "  << expected: "
                            + param.snd + ", found: " + ty + " >>");
                    sound = false;
                    // Mark the error, but keep going, in case we can report
                    // more.
                }
            }

            if (sound) {
                this.ty = f.result;
            } else {
                this.ty = ERROR.inst;
            }
            return;
        } else {// if (en instanceof VarEntry) {
            err.error(e.getPos(), "Variable " + e.func + " is not a function.");
            ty = ERROR.inst;

        }
    }

    public void visit(ExpFor e) {
        assert (e != null && e.lo != null && e.hi != null && e.var != null && e.body != null);

        venv.beginScope();
        // because we're declaring a new variable for the loop counter

        e.lo.accept(this);
        Type t_lo = this.ty;
        boolean sound = t_lo != ERROR.inst;

        e.hi.accept(this);
        Type t_hi = this.ty;
        sound = sound && (t_hi != ERROR.inst);

        if (!t_lo.coerceTo(INT.inst)) {
            err.error(e.lo.getPos(), "Initializing expression must be of type INT.");
            sound = false;
        } else if (!t_hi.coerceTo(INT.inst)) {
            err.error(e.hi.getPos(), "Counter limit expression must be " + "of type INT.");
            sound = false;
        }

        venv.extend(e.var, new VarEntry(INT.inst));
        loopNesting++; // BREAK legal in the following
        e.body.accept(this);
        Type t_body = this.ty;
        loopNesting--;

        if (!t_body.coerceTo(VOID.inst)) {
            err.error(e.body.getPos(), "Body of FOR must be of VOID type.");
            sound = false;
        }

        if (sound) { // Success
            this.ty = VOID.inst;
        } else {
            this.ty = ERROR.inst;
        }
    }

    public void visit(ExpIf e) {
        assert e != null && e.test != null && e.thenclause != null;

        e.test.accept(this);
        Type t_test = this.ty;

        if (!t_test.coerceTo(INT.inst)) {
            err.error(e.test.getPos(), "Test expression must be of type INT.");
        }

        e.thenclause.accept(this);
        Type t_then = this.ty;

        if (t_then.coerceTo(VOID.inst)) {
            // Success: all done
            this.ty = VOID.inst;
            return;
        } else {
            err.error(e.thenclause.getPos(), "Conditional clause must be of VOID type.");
        }

        // We can only reach the following if type checking of e fails:
        this.ty = ERROR.inst;
    }

    public void visit(ExpIfElse e) {
        assert (e != null && e.test != null && e.thenclause != null && e.elseclause != null);

        e.test.accept(this);
        Type t_test = this.ty;
        boolean goodTest = t_test.coerceTo(INT.inst);

        if (!goodTest) {
            err.error(e.test.getPos(), "Test expression must be of type INT.");
            // However, we also want to look for errors in the
            // clauses, so we keep going
        }

        e.thenclause.accept(this);
        Type t_then = this.ty;

        e.elseclause.accept(this);
        Type t_else = this.ty;

        if (!(t_else.coerceTo(t_then) || t_then.coerceTo(t_else))) {
            // We test both in case one clause is a RECORD, the other NIL
            err.error(e.test.getPos(), "then/else clauses must be of the same type.");

        } else if (goodTest) {
            if (t_then.coerceTo(NIL.inst)) {
                // If there's a choice between returning a RECORD and NIL,
                // we want to make sure we return the RECORD.
                this.ty = t_else;
            } else {
                this.ty = t_then;
            }
            // Either way, we now have a type for e, so we're done
            return;
        }

        // We can only reach the following if type checking of e fails:
        this.ty = ERROR.inst;
    }

    public void visit(ExpInt e) {
        ty = INT.inst;
    }

    public void visit(ExpLet e) {
        assert e != null && e.body != null;
        // Even empty bodies should be parsed as an ExpSeq(pos,new
        // ArrayList<Exp>())

        // save declsOK before checking nested let expressions
        boolean enclosingDeclsOK = this.declsOK;

        this.declsOK = !redeclaration(e.decls);

        venv.beginScope();
        tenv.beginScope();

        for (Decl dec : e.decls) {
            // check each declaration & add to env or tenv
            dec.accept(this);
            // Decls do not "evaluate" to types, so there is no point in
            // checking this.ty. However, a bad declaration will
            // invalidate the declsOK flag.
        }

        e.body.accept(this);

        venv.endScope();
        tenv.endScope();
        if (!declsOK) {
            // A bad declaration, in this or an enclosing block, should
            // invalidate the type computed for the body, if any.
            this.ty = ERROR.inst;
        }

        this.declsOK = enclosingDeclsOK;
    }

    public void visit(ExpNil e) {
        this.ty = NIL.inst;
    }

    public void visit(ExpOp e) {
        assert e != null && e.left != null && e.right != null;

        e.left.accept(this);
        Type t_left = this.ty;
        e.right.accept(this);
        Type t_right = this.ty;

        boolean sound = true;

        switch (e.oper) {
        case AND:
        case OR:
        case PLUS:
        case MIN:
        case MUL:
        case DIV: {
            sound = t_left.coerceTo(INT.inst) && t_right.coerceTo(INT.inst);
            break;
        }
        // Otherwise, we have a relational operator ...
        case EQ:
        case NE: {
            if (t_left.actual() instanceof VOID) {
                sound = false;
            } else {
                sound = t_right.coerceTo(t_left);
            }
            break;
        }
        default: { // LT, LE, GT, or GE: defined on two ints or strings
            sound = t_right.coerceTo(t_left) && (t_left.coerceTo(STRING.inst) || t_left.coerceTo(INT.inst));
            break;
        }
        }

        if (sound)
            this.ty = INT.inst;
        else {
            err.error(e.getPos(), "Cannot apply '" + op2String(e.oper) + "' to " + t_left + " and " + t_right + ".");
            this.ty = ERROR.inst;
        }
    }

    public void visit(ExpRecord e) {
        assert e != null & e.type != null;

        boolean sound = true;

        Type t_e = tenv.lookup(e.type);
        if (t_e == null) {
            err.error(e.getPos(), "Undefined type.");
            sound = false;
        } else if (t_e.actual() instanceof RECORD) {
            RECORD r = (RECORD) t_e.actual();

            if (e.fields == null && r.fields.size() != 0) {
                // Not really necessary, since this would be caught below.
                // However, it results in a somewhat more informative error
                // message.
                err.error(e.getPos(), "No field initializations specified.");
                sound = false;
            }

            // type check the field initialization
            // for (Pair<Symbol, Exp> fexp : e.fields) {
            if (e.fields.size() != r.fields.size()) {
                err.error(e.getPos(), "Wrong number of initializations in RECORD.");
                sound = false;

            } else {
                for (int i = 0; i < e.fields.size(); i++) {
                    // Record initialization enforces positions from the
                    // original declaration

                    Pair<Symbol, Exp> fexp = e.fields.get(i);
                    Pair<Symbol, Type> r_expected = r.fields.get(i);

                    fexp.snd.accept(this);
                    Type t_init = this.ty;

                    // Type t_expected = lookup(r.fields, fexp.fst);
                    if (r_expected.fst != fexp.fst) {
                        // Is the field name the expected one for this position?
                        err.error(e.getPos(), "Wrong field (expected " + r_expected.fst + ", found " + fexp.fst + ")");
                        sound = false;
                    } else if (!t_init.coerceTo(r_expected.snd)) {
                        // Is its initialization of the right type?
                        err.error(fexp.snd.getPos(), "Initial value of expression is of wrong type.");
                        sound = false;
                    }
                } // for
            }
        } else {
            err.error(e.getPos(), "Type " + t_e + " is not a RECORD.");
            sound = false;
        }

        if (sound) {
            this.ty = t_e; // .actual();
        } else {
            this.ty = ERROR.inst;
        }
    }

    public void visit(ExpSeq es) {
        assert es != null;

        boolean errors = false;
        this.ty = VOID.inst;

        for (Exp e : es.list) {
            e.accept(this);
            if (this.ty == ERROR.inst) {
                errors = true;
            }
        }
        if (errors)
            this.ty = ERROR.inst;
        // This strategy -- declare the type of the whole sequence an error if
        // any errors are found is a subjective choice. The alternative is
        // simply to let the type of e be the type of the last expression,
        // regardless of any errors earlier in the sequence. They'll be reported
        // no matter what, and code generation will be aborted regardless, so
        // it's really a question of which strategy gives the most informative
        // messages. Here, I've adopted the "fail fast" philosophy, in which any
        // errors means that nothing meaningful can be determined from the
        // entire sequence.
    }

    public void visit(ExpString e) {
        this.ty = STRING.inst;
    }

    public void visit(ExpVar e) {
        e.var.accept(this);
    }

    public void visit(ExpWhile e) {
        assert e != null && e.test != null && e.body != null;

        e.test.accept(this);
        Type t_test = this.ty;

        if (!t_test.coerceTo(INT.inst)) {
            err.error(e.test.getPos(), "Test expression must be of type INT.");
        } else {
            loopNesting++; // BREAK legal in the following
            e.body.accept(this);
            Type t_body = this.ty;
            loopNesting--;

            if (!t_body.coerceTo(VOID.inst)) {
                err.error(e.body.getPos(), "Body of WHILE must be of VOID type.");
            } else {
                // Success
                this.ty = VOID.inst;
                return;
            }
        }
        // We can only reach the following if type checking of e fails:
        this.ty = ERROR.inst;
    }

    /*
     * The visit(Ty*) methods are encountered while processing the RHS of a type
     * declaration. The resulting "return value" (assignment to this.ty) will be
     * the env.tenv binding for the identifier on the LHS of this declaration.
     */
    public void visit(TyArray ta) {
        assert ta != null;

        Type t = tenv.lookup(ta.typ);
        this.ty = new ARRAY(t);

        if (t == null) {
            err.error(ta.getPos(), "Undefined type.");
            this.ty = ERROR.inst;
        }

    }

    public void visit(TyName tn) {
        assert tn != null;

        Type t = tenv.lookup(tn.name);
        this.ty = t;

        if (t == null) {
            err.error(tn.getPos(), "Undefined type.");
            this.ty = ERROR.inst;
        }
    }

    public void visit(TyRecord tr) {
        Pair<List<Pair<Symbol, Type>>, Boolean> field_construction = visitTypeFields(tr, tr.fields);
        if (field_construction.snd) {
            this.ty = new RECORD(field_construction.fst);
        } else {
            this.ty = ERROR.inst;
        }
    }

    public void visit(VarField v) {
        v.var.accept(this);
        Type var = this.ty;
        if (var.actual() instanceof RECORD) {
            List<Pair<Symbol, Type>> r = ((RECORD) var.actual()).fields;
            Type t = lookup(r, v.field);
            if (t != null) {
                this.ty = t;
                return;
            } else {
                err.error(v.getPos(), "Undefined field for type " + var);
            }
        } else {
            err.error(v.getPos(), "Attempt to access non-existent field from " + "non-RECORD variable.");
        }
        // If we reach this, we know an error has been signaled
        this.ty = ERROR.inst;
        return;
    }

    public void visit(VarSimple v) {
        Entry en = venv.lookup(v.name);
        if (en == null) {
            err.error(v.getPos(), "Symbol " + v.name + " is undefined.");
            this.ty = ERROR.inst;
        } else if (en instanceof FunEntry) {
            err.error(v.getPos(), "Cannot reference function " + v.name + "() as if it were a variable.");
            this.ty = ERROR.inst;
        } else {
            VarEntry var = (VarEntry) en;
            this.ty = var.ty; // .actual();
        }
    }

    public void visit(VarSubscript v) {
        v.var.accept(this);
        Type var = this.ty;
        if (var.actual() instanceof ARRAY) {
            v.index.accept(this);
            Type idx = this.ty;
            if (!idx.coerceTo(INT.inst)) {
                err.error(v.index.getPos(), "Index must be of type INT");
                this.ty = ERROR.inst;
            } else {
                this.ty = ((ARRAY) var.actual()).element;
            }
        } else {
            err.error(v.getPos(), "Attempt to index non-ARRAY type variable.");
            this.ty = ERROR.inst;
        }

    }

    // //////////////////////// private utility methods ////////////////////////

    /**
     * Sets up appropriate bindings for the two "primitive" types, int and
     * string, as well as the functions defined as part of the Tiger standard
     * library: print, flush, getchar, ord, chr, size, substring, concat, not,
     * exit.
     * 
     */
    private static void setupStdLibrary() {
        // Standard library...
        // Java generics do offer type-safe reusability, but at the cost of
        // god-awful verbosity. I've kept the formals list constructions
        // separate from the env bindings, which I hope enhances clarity.

        extern_tenv.extend(Symbol.sym("int"), INT.inst);
        extern_tenv.extend(Symbol.sym("string"), STRING.inst);

        // print
        List<Pair<Symbol, Type>> printFmls = new java.util.ArrayList<>();
        printFmls.add(new Pair<Symbol, Type>(Symbol.sym("s"), STRING.inst));
        extern_venv.extend(Symbol.sym("print"), new FunEntry(printFmls, VOID.inst));

        // printi
        printFmls = new java.util.ArrayList<>();
        printFmls.add(new Pair<Symbol, Type>(Symbol.sym("x"), INT.inst));
        extern_venv.extend(Symbol.sym("printi"), new FunEntry(printFmls, VOID.inst));

        // flush
        extern_venv.extend(Symbol.sym("flush"), new FunEntry(null, VOID.inst));

        // getchar
        extern_venv.extend(Symbol.sym("getchar"), new FunEntry(null, STRING.inst));

        // ord
        List<Pair<Symbol, Type>> ordFmls = new java.util.ArrayList<>();
        ordFmls.add(new Pair<Symbol, Type>(Symbol.sym("s"), STRING.inst));
        extern_venv.extend(Symbol.sym("ord"), new FunEntry(ordFmls, INT.inst));

        // chr
        List<Pair<Symbol, Type>> chrFmls = new java.util.ArrayList<>();
        chrFmls.add(new Pair<Symbol, Type>(Symbol.sym("s"), INT.inst));
        extern_venv.extend(Symbol.sym("chr"), new FunEntry(chrFmls, STRING.inst));

        // size
        List<Pair<Symbol, Type>> sizeFmls = new java.util.ArrayList<>();
        sizeFmls.add(new Pair<Symbol, Type>(Symbol.sym("s"), STRING.inst));
        extern_venv.extend(Symbol.sym("size"), new FunEntry(sizeFmls, INT.inst));

        // substring
        List<Pair<Symbol, Type>> substringFmls = new java.util.ArrayList<>();
        substringFmls.add(new Pair<Symbol, Type>(Symbol.sym("s"), STRING.inst));
        substringFmls.add(new Pair<Symbol, Type>(Symbol.sym("first"), INT.inst));
        substringFmls.add(new Pair<Symbol, Type>(Symbol.sym("n"), INT.inst));
        extern_venv.extend(Symbol.sym("substring"), new FunEntry(substringFmls, STRING.inst));

        // concat
        List<Pair<Symbol, Type>> concatFmls = new java.util.ArrayList<>();
        concatFmls.add(new Pair<Symbol, Type>(Symbol.sym("s1"), STRING.inst));
        concatFmls.add(new Pair<Symbol, Type>(Symbol.sym("s2"), STRING.inst));
        extern_venv.extend(Symbol.sym("concat"), new FunEntry(concatFmls, STRING.inst));

        // not
        List<Pair<Symbol, Type>> notFmls = new java.util.ArrayList<>();
        notFmls.add(new Pair<Symbol, Type>(Symbol.sym("i"), INT.inst));
        extern_venv.extend(Symbol.sym("not"), new FunEntry(notFmls, INT.inst));

        // exit
        List<Pair<Symbol, Type>> exitFmls = new java.util.ArrayList<>();
        exitFmls.add(new Pair<Symbol, Type>(Symbol.sym("i"), INT.inst));
        extern_venv.extend(Symbol.sym("exit"), new FunEntry(exitFmls, VOID.inst));
    }

    /**
     * For a given list of Pairs and left element x, returns the corresponding
     * right element in the Pair with the lowest index
     * 
     * @param ls
     *            The list of Pairs
     * @param x
     *            The left element we're looking for
     * @return y such that (x,y) is the first Pair in ls containing x. Returns
     *         <tt>null</tt>, if <em>x</em> is not found
     */
    private static <A, B> B lookup(List<Pair<A, B>> ls, A x) {
        for (Pair<A, B> p : ls) {
            if (p.fst.equals(x))
                return p.snd;
        }
        return null;
    }

    private static String op2String(ExpOp.Op op) {
        switch (op) {
        case AND:
            return "&";
        case OR:
            return "|";
        case PLUS:
            return "+";
        case MIN:
            return "-";
        case MUL:
            return "*";
        case DIV:
            return "/";
        case EQ:
            return "=";
        case NE:
            return "<>";
        case LT:
            return "<";
        case LE:
            return "<=";
        case GT:
            return ">";
        default: // case GE:
            return ">=";
        }
    }

    /**
     * Returns true if and only if the list of declarations contains duplicate
     * occurences of the same type identifier or the same variable/function
     * identifier
     * 
     * @param decls
     *            the list of declarations
     * @return
     */
    private boolean redeclaration(List<Decl> decls) {

        // Pre-filter: check for duplicate identifier and type declarations
        java.util.Set<Symbol> vnames = new java.util.HashSet<Symbol>();
        java.util.Set<Symbol> tnames = new java.util.HashSet<Symbol>();

        boolean duplicates = false;

        for (Decl d : decls) {
            if (d instanceof DeclVar) {
                if (!vnames.add(((DeclVar) d).name)) {
                    err.error(d.getPos(), "Symbol " + ((DeclVar) d).name + " is already declared in this scope.");
                    duplicates = true;
                }
            } else if (d instanceof DeclGroupFunction) {
                for (DeclFn dec_f : ((DeclGroupFunction) d).fns) {
                    if (!vnames.add(dec_f.name)) {
                        err.error(dec_f.getPos(), "Symbol " + dec_f.name + " is already declared in this scope.");
                        duplicates = true;
                    }
                }
            } else { // (d instanceof DeclGroupType)
                for (DeclTy dec_t : ((DeclGroupType) d).decls) {
                    if (!tnames.add(dec_t.name)) {
                        err.error(dec_t.pos, "Type symbol " + dec_t.name + " is already declared in this scope.");
                        duplicates = true;
                    }
                }
            }
        }
        return duplicates;
    }

    /**
     * From a collection of (id,type-id) pairs, this produces a corresponding
     * list of (id,type) pairs. The additional Boolean value in the result
     * indicates whether the construction was sound or built while encountering
     * errors
     * 
     * @param a
     *            The syntax element where the access list occurs (used for
     *            error reporting)
     * @param access
     * @return
     */
    private Pair<List<Pair<Symbol, Type>>, Boolean> visitTypeFields(ISyntaxElt a,
            Collection<Pair<Symbol, Symbol>> access) {

        List<Pair<Symbol, Type>> container = new java.util.ArrayList<Pair<Symbol, Type>>();
        java.util.Set<Symbol> defined = new java.util.HashSet<Symbol>();

        Boolean sound = true;

        for (Pair<Symbol, Symbol> p : access) {
            // Does this type exist?
            Type t = tenv.lookup(p.snd);
            if (t == null) {
                err.error(a.getPos(), "Undefined type:  " + p.snd);
                t = ERROR.inst;
                sound = false;
            }
            // Have we already defined a field with this name?
            if (defined.contains(p.fst)) {
                err.error(a.getPos(), "Field " + p.fst + " is already defined in this scope.");
                sound = false;
            } else {
                container.add(new Pair<Symbol, Type>(p.fst, t));
                defined.add(p.fst);
            }
        }

        return Pair.of(container, sound);
    }
}
