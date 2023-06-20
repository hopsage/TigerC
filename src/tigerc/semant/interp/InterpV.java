/*************************************************************************
 *  tigerc/src/semant/interp/InterpV.java 
 *
 *  Author:  John Lasseter
 *
 *  This is a bit of a digression from our ongoing task of language 
 *  translation.  In contrast to a compiler, which translates all the 
 *  instructions in a program at once, an interpreter *simulates* the 
 *  execution of instructions, one at a time.  The advantage of this
 *  approach is that an interpreter is typically much easier to build.  
 *  As such, it is an excellent choice for rapid prototype construction
 *  of new language constructs.  The disadvantages are all ones of 
 *  performance; interpreted code is usually significantly slower than 
 *  the same thing compiled to native machine code.
 *  
 *  For our class, the reason to study interpreters is that their structure 
 *  provides important insights into the structure of major stages in the 
 *  compiler pipeline, namely the tasks of semantic analysis and code 
 *  generation.  
 * 
 *  History: 10/01/2014 (jhel) created
 *           03/07/2018 (jhel) bug fixes
 *           04/22/2020 (jhel) fixed external bindings
 *                             improved literate programming documentation
 *           
 ************************************************************************/
package tigerc.semant.interp;

import tigerc.syntax.absyn.*;
import tigerc.util.*;
import tigerc.semant.interp.values.*;
import tigerc.semant.Env;

import java.util.List;

public class InterpV implements IAbsynVisitor {

    /*************************************************************************
     * private attributes (grouped/documented according to their purpose here)
     */

    private IValue result = null;
    /*
     * Each visit() call is stored a result here. Recall that a Visitor
     * represents the traversal function being applied to the AST. This field
     * therefore represents the return value of the traversal.
     * 
     */

    /*
     * Local environment. You'll note that, unlike the type checker (SemantV)
     * and code generator (JVMGeneratorV), there is no environment for type
     * bindings here that is because in this interpreter, static types are
     * ignored. Static typing can still be enforced by performing semantic
     * analysis before interpretation, but assuming the program is free of
     * errors, the type information itself is not relevant to execution of the
     * interpreter.
     */
    private Env<Entry> env; 

    /*
     * Components to support external bindings. Right now, that just means the
     * standard library, which in our case is implemented by the Java class
     * TigerStdLib
     */
    private static Env<Entry> extern_env;

    static {
        try {
            extern_env = Env.instance_noparent(); // new Env<Entry>();
            InterpV.setupStdLibrary();
        } catch (ClassNotFoundException e) {
            System.err.println("InterpV class loading failure");
            e.printStackTrace();
            throw new Error("Fatal error:  Cannot load standard library file TigerStdLib");
        }
    }

    // Error reporting object
    private ErrorMsg err;

    /*************************************************************************
     * constructors: one public, for top level interpretration of a program, the
     * other for interpretation of procedure bodies during interpretation of a
     * procedure call
     */

    public InterpV(ErrorMsg err) {
        this(err, new Env<Entry>(extern_env));
    }

    protected InterpV(ErrorMsg err, Env<Entry> e) {
        assert e != null && err != null;
        this.env = e;
        this.err = err;
    }

    /*************************************************************************
     * visit() method family: interpretation (and also pretty printing, type
     * checking, and code generation) is defined by induction on the structure
     * of the AST, or computationally, by a recursive traversal of the AST. The
     * visit methods define the behavior for the the base and inductive cases of
     * this traversal (the traversal itself -- i.e., the recursive calls to the
     * interpreter -- is realized through the accept() calls in the visit()
     * method bodies).
     */

    @Override
    public void visit(DeclGroupFunction d) {
        /*
         * This one is subtle: a function declaration must result in a new
         * binding added to the environment, associating the function's
         * identifier with its value. The "value" of a function consists of
         * three parts: the parameters (a list of identifiers, the body (an
         * expression), and the environment in which the function was declared.
         * This is represented in TigerC by a tigerc.semant.interp.FunEntry
         * object.
         *
         * As with DeclVar, we can assume that a new Scope has already been
         * opened by the let that encloses this group of function declarations.
         * This is important for two reasons. First, like DeclVar, we don't want
         * to open/close another Scope inside of the declaration itself, since
         * that would render the declaration unusable in the body of the let
         * expression where we intend to use it. Second, lexical scope requires
         * that the closure (FunEntry) representing each function/procedure
         * definition stores the environment in which this group of declarations
         * occurs.
         * 
         * A subtlety arises in the choice of saved environment. Because
         * functions can be nested, they must be able to see every declaration
         * that is present in their enclosing scope. I.e., the current scope is
         * the one that must be saved. Because they can be declared in mutual
         * recursion with each other, however, the saved environment must be one
         * that includes bindings for every other function declared in the same
         * group (this is why the syntactic grouping of declarations is
         * considered significant).
         * 
         * We will see how to implement this in class, but the general idea is
         * that the environment saved in a FunEnv will need to be modified AFTER
         * the binding is added to the environment. If we were implementing
         * scopes entirely with linked lists, we would do this by creating each
         * FunEnv with a dummy environment (a "thunk"), then modifying this
         * value to point to the environment that encloses all of the present
         * function declarations, once we are done adding all of their bindings.
         * Because we are implementing a lexical scope with a hashtable,
         * however, we can accomplish this here by making sure that all
         * functions in this group of declarations store the same copy of the
         * current environment. The underlying hashtable will be modified
         * appropriately for each declaration, and the fact that every FunEntry
         * holds an alias to this table guarantees that all bindings within this
         * group will be visible to each other. However, we will need to make a
         * COPY, because we need to make sure that the Scope that currently
         * exists as the value of the field this.enclosing is the one that is
         * preserved by the closure. This prevents the problem of later
         * extensions interfering with the bindings saved at declaration time
         * (which can happen if the let body itself contains another let
         * expression).
         *
         * One more subtlety concerns multiple function declaration groups
         * within the same let, which are syntactically distinct (because a type
         * or var declaration separates them)? Functions in earlier groups
         * should NOT be able to see functions in later groups (though the
         * converse is fine). Otherwise, all functions within the same let could
         * be mutually recursive, regardless of syntactic grouping. The solution
         * implemented here is to ignore this! We'll count on the semantic
         * analysis phase to filter out such programs.
         * 
         */
        Env<Entry> savedEnv = this.env.copyOf();
        // NOTE: this will break if a deep copy of this.env is made. In
        // particular, we're counting on
        // ...... this.env != savedEnv && this.env.env == savedEnv.env
        //
        // assert this.env != savedEnv && (this.env.env == savedEnv.env);
        // (not really expressible, since env.env is protected)
        //
        // What we're saving here is a reference into the *linked list* of
        // Scope objects. Additional inner scopes will cause new Scope objects
        // to be pushed onto the head of the stack pointed to by this.env, yet
        // this will not interfere with the place we've saved in this stack for
        // a function's closure.

        for (DeclFn f : d.fns) {
            List<Symbol> paramIds = new java.util.ArrayList<>();
            for (Pair<Symbol, Symbol> p : f.params) {
                paramIds.add(p.fst);
            }
            env.extend(f.name, new FunEntry(paramIds, f.body, savedEnv));
            // Because savedEnv.env == this.env.env, this also extends this.env
            // Note, too, that we're already updating the environment that we're
            // saving for each closure (FunEntry).
        }
    }

    @Override
    public void visit(DeclGroupType d) {
        /*
         * From the perspective of an interpreter's dynamic execution model,
         * types are irrelevant. The does not mean that type errors are
         * impossible, but in the actual interpretation of a program, we
         * _assume_ they are, allowing a runtime failure when this assumption is
         * violated.
         */
    }

    @Override
    public void visit(DeclVar d) {
        /*
         * Declarations of variables occur only in let expressions. A new scope
         * has already been declared there, and it will end at the end of the
         * let-body. Hence, we do not open a new scope here.
         */

        d.init.accept(this);
        this.env.extend(d.name, new VarEntry(this.result));
    }

    @Override
    public void visit(ExpArray e) {
        // array creation: typ [ size ] of init
        // Java: tmp = new typ [ size ]; v = <init> for (int i = 0; i <
        // tmp.length; i++) { tmp[i] = v; }

        e.size.accept(this);
        ValInt vsz = (ValInt) this.result;
        e.init.accept(this); // initial expression only calculated once!!!

        ValArray arr = new ValArray(vsz.val, this.result);
        this.result = arr;
    }

    @Override
    public void visit(ExpAssign e) {
        /*
         * This is one of the places where the elegance afforded by polymorphism
         * breaks down, or at least I can't find a way to use it. The difficulty
         * is that the way we update a value depends heavily on the actual type
         * of the LHS in this assignment. Record fields and array cells are
         * updated directly. Simple variables, however, must have their bindings
         * changed in the environment.
         */

        if (e.lhs instanceof VarSimple) {
            VarSimple x = (VarSimple) e.lhs;

            e.rhs.accept(this);
            boolean newx = this.env.update(x.name, new VarEntry(this.result));
            assert newx;
        } else if (e.lhs instanceof VarSubscript) {
            /*
             * A subtle point here, and one that is not resolved in the language
             * reference manual, is whether the LHS or RHS should be evaluated
             * first. This matters, because both the address of the LHS array
             * and the index into that array can be calculated by arbitrary
             * expressions, including those that produce side effects. Executing
             * the LHS first can therefore affect the value of the RHS, and
             * vice-versa.
             * 
             * Here we'll adopt the convention that the LHS should be evaluated
             * first, so that the location into which we write the RHS value is
             * predictable. Too, this is consistent with the semantics adopted
             * in the Java/C family of languages. For example,
             * 
             * int[] A = {0,0,0,0}; int i = 1; A[i++] = i; A[++i] = i;
             * 
             * will cause the contentws of A to become [0,2,0,3], rather than
             * [0,1,0,2].
             */
            VarSubscript lhs = (VarSubscript) e.lhs;
            lhs.var.accept(this);
            assert this.result instanceof ValArray;

            ValArray a = (ValArray) this.result;

            lhs.index.accept(this);
            assert this.result instanceof ValInt;
            ValInt i = (ValInt) this.result;

            // evaluate the RHS and store the result in a[i]
            e.rhs.accept(this);
            a.set(i.val, this.result);

        } else { // e.lhs is a VarField
            /*
             * As with array subscripts, the expresssions used to calculate the
             * record whose field we'll assign can involve side effects. Again,
             * we'll adopt the semantics of the LHS before the RHS.
             */

            VarField lhs = (VarField) e.lhs;
            lhs.var.accept(this);
            assert this.result instanceof ValRecord;

            ValRecord r = (ValRecord) this.result;
            e.rhs.accept(this);
            r.set(lhs.field, this.result);
            // throws IllegalArgumentException, if field is not defined already
            // in r
        }
        this.result = ValUnit.inst;
    }

    @Override
    public void visit(ExpBreak e) {
        // Handlers for BreakE are installed int he interpretation of both
        // ExpFor and ExoWhile, ensuring that "jumps" from inside those loops
        // will land at the end of the loop.
        this.result = ValUnit.inst;
        throw new BreakE("break");
    }

    @Override
    public void visit(ExpCall e) {
        /*
         * There are several steps involved here. First, the binding for the
         * function (e.func) must be retrieved from the current environment.
         * Then, each argument expression must be evaluated in turn, and the
         * resulting bindings must be added to the environment. The question is,
         * which environment, the saved one or the current one?
         * 
         */

        Entry fe = env.lookup(e.func);
        assert fe != null && fe instanceof FunEntry;

        FunEntry f = (FunEntry) fe;
        assert f.parameters.size() == e.args.size();

        List<IValue> values = new java.util.ArrayList<>();
        for (int i = 0; i < e.args.size(); i++) {
            Exp argE = e.args.get(i);

            // Evaluate each argument for this call
            argE.accept(this);
            values.add(this.result);
        }
        this.result = f.apply(this.err, values);

    }

    @Override
    public void visit(ExpFor e) {
        // See the Tiger Reference Manual for the necessary behavior

        e.lo.accept(this);
        assert this.result instanceof ValInt;
        ValInt v_lo = (ValInt) this.result;

        e.hi.accept(this);
        int hi = ((ValInt) this.result).val;

        this.env.beginScope();
        this.env.extend(e.var, new VarEntry(v_lo));

        try {
            for (int i = v_lo.val; i <= hi; i++) {
                e.body.accept(this);
                this.env.update(e.var, new VarEntry(new ValInt(i + 1)));
            }
        } catch (BreakE exc) {
        }

        this.env.endScope();

        assert this.result == ValUnit.inst;
    }

    @Override
    public void visit(ExpIf e) {
        e.test.accept(this);
        ValInt tv = (ValInt) this.result;

        if (tv.val != 0) {
            e.thenclause.accept(this);
        }

        // As with loops and assignment, the "value" here is merely void
        assert this.result == ValUnit.inst;
    }

    @Override
    public void visit(ExpIfElse e) {
        /*
         * e.test e.thenclause e.elseclause
         */

        // 1. interpret the test "interpret(e.test)"
        e.test.accept(this);

        // this.result is now either "true" or "false" (0)
        ValInt tv = (ValInt) this.result;

        if (tv.val != 0) { // "true"
            e.thenclause.accept(this);
        } else {
            e.elseclause.accept(this);
        }
    }

    @Override
    public void visit(ExpInt e) {
        result = new ValInt(e.value);
    }

    @Override
    public void visit(ExpLet e) {
        // The behavior of Tiger's let is more along the lines of Lisp/Scheme
        // let* (scope for each declaration includes the previous binding).

        env.beginScope();

        for (Decl dec : e.decls) {
            dec.accept(this);
        }

        e.body.accept(this);
        env.endScope();
    }

    @Override
    public void visit(ExpNil e) {
        this.result = ValUnit.inst;
    }

    @Override
    public void visit(ExpOp e) {
        e.left.accept(this);
        IValue v1 = this.result; // int v1 = interp(e.left)
        IValue v2 = null; // don't evaluate the right operand yet, in case we
                          // need to short circuit

        if (e.oper == ExpOp.Op.AND) {
            if (((ValInt) v1).val == 0) {
                this.result = ValInt.ZERO;
            } else {
                e.right.accept(this);
                v2 = this.result; // int v2 = interp(right)

                this.result = (((ValInt) v2).val != 0 ? ValInt.ONE : ValInt.ZERO);
            }
        } else if (e.oper == ExpOp.Op.OR) {
            if (((ValInt) v1).val != 0) {
                this.result = ValInt.ONE;
            } else {
                e.right.accept(this);
                v2 = this.result; // int v2 = interp(right)

                this.result = (((ValInt) v2).val != 0) ? ValInt.ONE : ValInt.ZERO;
            }
        } else {
            e.right.accept(this);
            v2 = this.result; // int v2 = interp(right)

            switch (e.oper)

            {
            case PLUS: {
                this.result = new ValInt(((ValInt) v1).val + ((ValInt) v2).val);
                break;
            }
            case MIN: {
                this.result = new ValInt(((ValInt) v1).val - ((ValInt) v2).val);
                break;
            }
            case MUL: {

                this.result = new ValInt(((ValInt) v1).val * ((ValInt) v2).val);
                break;
            }
            case DIV: {
                this.result = new ValInt(((ValInt) v1).val / ((ValInt) v2).val);
                break;
            }
            case LT: {
                this.result = (((ValInt) v1).val < ((ValInt) v2).val) ? ValInt.ONE : ValInt.ZERO;
                break;
            }
            case LE: {
                this.result = (((ValInt) v1).val <= ((ValInt) v2).val) ? ValInt.ONE : ValInt.ZERO;
                break;
            }
            case GT: {
                this.result = (((ValInt) v1).val > ((ValInt) v2).val) ? ValInt.ONE : ValInt.ZERO;
                break;
            }
            case GE: {
                this.result = (((ValInt) v1).val >= ((ValInt) v2).val) ? ValInt.ONE : ValInt.ZERO;
                break;
            }

            default: { // either EQ or NE: these tests are dual to each other
                ValInt yes, no;
                if (e.oper == ExpOp.Op.EQ) {
                    yes = ValInt.ONE;
                    no = ValInt.ZERO;
                } else {
                    assert e.oper == ExpOp.Op.NE;
                    yes = ValInt.ZERO;
                    no = ValInt.ONE;
                }
                if (v1 instanceof ValInt) {
                    this.result = (((ValInt) v1).val == ((ValInt) v2).val) ? yes : no;
                    break;
                } else if (v1 instanceof ValStr) {
                    this.result = (((ValStr) v1).val.equals(((ValStr) v2).val)) ? yes : no;
                    break;
                } else if (v1 instanceof ValRecord) {
                    this.result = (v1 == v2) ? yes : no;
                    break;
                } else if (v1 instanceof ValArray) {
                    this.result = (v1 == v2) ? yes : no;
                    break;
                } else if (v1 == ValNil.inst && v2 == ValNil.inst) {
                    this.result = yes;
                    break;
                } else {
                    this.result = no;
                    break;
                }
            } // case EQ, NE
            } // switch
        }
    }

    @Override
    public void visit(ExpRecord e) {
        // public final Symbol type;
        // public final List<Pair<Symbol, Exp>>fields;

        List<Pair<Symbol, IValue>> inits = new java.util.LinkedList<>();

        for (Pair<Symbol, Exp> field : e.fields) {
            // Evaluate each Exp in the fields
            field.snd.accept(this);

            // Make corresponding <Symbol, IValue> pair
            // Add that to a list
            inits.add(new Pair<>(field.fst, this.result));
        } // for

        // When you're done, pass that list to a constructor call for a new
        // ValRecord
        this.result = new ValRecord(inits);
    }

    @Override
    public void visit(ExpSeq e) {
        // for every expression in e.list: (1) interpret the expression (2)
        // return the value of the final expression

        if (e.list.size() == 0) {
            this.result = ValUnit.inst;
        } else {
            for (Exp exp : e.list) {
                exp.accept(this);
            }
            // evaluation of the last expression has already set this.result
        }
    }

    @Override
    public void visit(ExpString e) {
        ValStr sv = new ValStr(e.value);
        this.result = sv;
    }

    @Override
    public void visit(ExpVar e) {
        // Expressions that consist of a single variable, array cell access, or
        // a record field access

        e.var.accept(this);
    }

    @Override
    public void visit(ExpWhile e) {
        /*
         * e.test, e.body
         */

        e.test.accept(this);
        ValInt tv = (ValInt) this.result;

        try {
            while (tv.val != 0) {
                e.body.accept(this);
                e.test.accept(this);
                tv = (ValInt) this.result;
            }
        } catch (BreakE breakException) {
            // ...
        }

        assert this.result == ValUnit.inst;
        // The "value" of a loop is the "unit" type, i.e. void
    }

    @Override
    public void visit(TyArray t) {
        // As with DeclGroupTy, types are irrelevant here, so this is ignored
    }

    @Override
    public void visit(TyName t) {
        // As with DeclGroupTy, types are irrelevant here, so this is ignored
    }

    @Override
    public void visit(TyRecord t) {
        // As with DeclGroupTy, types are irrelevant here, so this is ignored
    }

    @Override
    public void visit(VarField v) {
        v.var.accept(this);
        assert this.result instanceof ValRecord;

        this.result = ((ValRecord) this.result).get(v.field);

    }

    @Override
    public void visit(VarSimple v) {
        Entry ve = this.env.lookup(v.name);
        assert ve instanceof VarEntry;

        this.result = ((VarEntry) ve).val;
    }

    @Override
    public void visit(VarSubscript v) {
        v.var.accept(this);
        assert this.result instanceof ValArray;
        ValArray a = (ValArray) this.result;

        v.index.accept(this);
        assert this.result instanceof ValInt;

        this.result = a.get(((ValInt) this.result).val);
    }

    public IValue getResult() {
        return result;
    }

    /*************************************************************************
     * utility methods: Unlike type checking and code generation, we don't need
     * much utility support for the interpreter.  Here, we just have a procedure for 
     * setting up external bindings with the standard library.
     */
    private static void setupStdLibrary() throws ClassNotFoundException {
        
        final Class<?> std_lib = Class.forName("TigerStdLib");
        final Class<?> str_class = String.class; 
        final Class<?> int_class = int.class;
        
        Class<?>[] _void_ = new Class[0];
        Class<?>[] _string_ = { str_class };
        Class<?>[] oneIntParam = { int_class };
        Class<?>[] strstr = {str_class, str_class };
        Class<?>[] strstrint = {str_class,int_class,int_class};
        
        extern_env.extend(Symbol.sym("print"), new ExternFunEntry(std_lib,"print",_string_));
        extern_env.extend(Symbol.sym("printi"), new ExternFunEntry(std_lib,"printi",oneIntParam));
        extern_env.extend(Symbol.sym("flush"), new ExternFunEntry(std_lib,"flush",_void_));
        extern_env.extend(Symbol.sym("getchar"), new ExternFunEntry(std_lib,"getchar",_void_));
        extern_env.extend(Symbol.sym("ord"), new ExternFunEntry(std_lib,"ord",_string_));
        extern_env.extend(Symbol.sym("chr"), new ExternFunEntry(std_lib,"chr",oneIntParam));
        extern_env.extend(Symbol.sym("size"), new ExternFunEntry(std_lib,"size",_string_));
        extern_env.extend(Symbol.sym("substring"), new ExternFunEntry(std_lib,"substring",strstrint));
        extern_env.extend(Symbol.sym("concat"), new ExternFunEntry(std_lib,"concat",strstr));
        extern_env.extend(Symbol.sym("not"), new ExternFunEntry(std_lib,"not",oneIntParam));
        extern_env.extend(Symbol.sym("exit"), new ExternFunEntry(std_lib,"exit",oneIntParam));
    }

}
