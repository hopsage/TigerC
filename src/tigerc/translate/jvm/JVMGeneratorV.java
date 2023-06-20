/*
    JVMGenerator.java
    
    Tiger code generator, targeting the JVM
    
    Author:  John Lasseter and <your name here>
    History: 
        04/13/2014 (jhel) created
        05/01/2016 (jhel) implemented all methods, except visit(DeclGroupFunctio)
        04/13/2018 (jhel) many bug fixes
        04/15/2018 (jhel) added emitPrintMainResultInstruction(); fixed visit(ExpArray)
 */

package tigerc.translate.jvm;

import tigerc.syntax.absyn.*;
import tigerc.semant.Env;

import tigerc.translate.*;
import tigerc.translate.access.IAccess;
import tigerc.semant.analysis.types.*;
import tigerc.util.*;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;

public class JVMGeneratorV implements tigerc.translate.ICodegen, tigerc.syntax.absyn.IAbsynVisitor {
    /**
     * There should be a new instance of JVMGeneratorV constructed for each
     * procedure definition. After completing code generation for that procedure
     * fragment, the resulting code can be retrieved from code, while the list
     * of accumulated procedure fragments is in procs.
     */

    private static int _serialNumGenerator = 0;

    private int serialNumber = JVMGeneratorV._serialNumGenerator++;

    private List<String> procs = new java.util.LinkedList<>();
    // List of procedure "fragments": in the JVM, this is just the strings
    // containing the complete definitions

    private java.io.PrintWriter tgtOut; // Target destination for generated code
    // (Analogous to the IValue field in InterpV or Type in SemantV)

    private IAbsyn prog; // The program we're translating
    private String classname; // The name of the class file we'll generate
    private java.io.PrintWriter code;
    // The code we generate for this program (without procedure fragments)

    // private int localsCt = 0;
    // private int maxLocals = 0; <---- moved to maxLocals() method in JVMFrame
    // The number of local variables allocated for this frame

    private int maxStack = 10;
    // TODO: this can be improved, along the same lines as maxLocals

    private JVMFrame frame;

    private java.util.Stack<Label> enclosingLoop;
    // Exit label of the lexically-closest enclosing loop. To support nested
    // loops, we use a stack. To ensure that function definitions nested
    // inside a loop do not retain the enclosingLoop stack, a new JVMGeneratoV
    // Should be created for each method body.

    private Env<Entry> venv;
    private Env<Type> tenv;

    private static Env<Entry> extern_venv;
    private static Env<Type> extern_tenv;

    static {
        try {
            JVMGeneratorV.extern_venv = Env.instance_noparent(); //new Env<Entry>();
            JVMGeneratorV.extern_tenv = Env.instance_noparent(); //new Env<Type>();
            JVMGeneratorV.setupStdLibrary();
        } catch (ClassNotFoundException e) {
            throw new Error("Fatal error:  Cannot load standard library file TigerStdLib");
        }
    }

    // To record variable allocations and procedure frames (and type
    // definitions)

    // "return values" of each visit call:
    private Type expType; // type of the most recently-visited expression
    private boolean rvalueMode = true;

    /**
     * Minimal code generator, for writing to stdout. Likely to be rarely, if
     * ever, used
     * 
     * @throws ClassNotFoundException
     */
    public JVMGeneratorV() throws ClassNotFoundException {
        this(System.out);
    }

    /**
     * Most commonly-used constructor.
     * 
     * @param o
     *            the text stream Writer where source code will be written
     * @throws ClassNotFoundException
     */
    public JVMGeneratorV(java.io.PrintWriter o) throws ClassNotFoundException {
        this(new Env<Entry>(JVMGeneratorV.extern_venv), new Env<Type>(JVMGeneratorV.extern_tenv), o, makePrintableWriter(), new java.util.LinkedList<String>());
    }

    /**
     * Private constructor, for use with the "basic", stdout form
     * 
     * @param o
     * @throws ClassNotFoundException
     */
    private JVMGeneratorV(java.io.PrintStream o) throws ClassNotFoundException {
        this(new java.io.PrintWriter(o));
    }

    /**
     * Actual workhorse of the constructors.
     * 
     * @param ve
     *            value environment
     * @param te
     *            type environment
     * @param tgt
     *            output writer
     * @param c
     *            procedure text writer (used internally)
     * @param ps
     *            list of procedure definitions
     * @throws ClassNotFoundException
     */
    private JVMGeneratorV(Env<Entry> ve, Env<Type> te, java.io.PrintWriter tgt, java.io.PrintWriter c, List<String> ps)
            throws ClassNotFoundException {

        assert JVMGeneratorV.extern_venv != null && JVMGeneratorV.extern_tenv != null;
        assert ve != null && te != null && tgt != null && c != null && ps != null;

        this.venv = ve;
        this.tenv = te;
        this.tgtOut = tgt;
        this.code = c;
        this.procs = ps;
        this.enclosingLoop = new java.util.Stack<Label>();
        this.frame = new JVMFrame();

    }

    /**
     * Constructor for code generation of a single procedure body (though that
     * may itself contain nested procedures)
     * 
     * @param classname
     *            the name of the class containing this procedure body; since
     *            this constructor is only called from the
     *            visit(DeclGroupFunction) method, the value of this.classname
     *            will not be set from the usual, top-level setProg call.
     * @param venv
     *            this procedure's enclosing "value" environment
     * @param tenv
     *            this procedure's enclosing type environment
     * @param procBodyWriter
     *            the target stream for writing the procedure body's code
     * @param formals
     *            the list of <ID,Type> pairs that are this procedures'
     *            parameters
     * @param procs
     *            the list of procedure definitions that have been competed so
     *            far; if there are nested definitions inside this body, we're
     *            going to need to add them.
     * @throws ClassNotFoundException
     * @pre this constructor is only called from the context of code generation
     *      for a procedure's body, i.e. from within a visit (DeclGroupFunction)
     *      call. That's impossible to formalize or easily, check, I think.
     */
    private JVMGeneratorV(String classname, Env<Entry> venv, Env<Type> tenv, List<Pair<Symbol, Type>> formals,
            List<String> procs) throws ClassNotFoundException {
        this(venv, tenv, null, makePrintableWriter(), procs);

        this.venv.beginScope();
        for (Pair<Symbol, Type> param : formals) {

            this.venv.extend(param.fst, new VarEntry(param.snd, this.frame.allocLocal(param.snd)));
        }

        this.classname = classname;
        // we won't bother to close the scope, since that will be done by the
        // enclosing
        // visit(DeclGroupFunction) method that created this generator function
        // object

    }

    /**************** ICodegen implementation ************************/

    @Override
    public void emitMain() {
        assert this.prog != null;

        emitLn(this.tgtOut, ".method public static main([Ljava/lang/String;)V");

        this.prog.accept(this);
        // will calculate local variable allocation, add procedure fragments
        // to procs, and write JVM instructions to code

        emitLn(this.tgtOut, ".limit locals " + this.frame.maxLocals() + 1);
        emitLn(this.tgtOut, ".limit stack " + this.maxStack);
        // lazy stack limit guess: we could improve with a mechanism
        // similar to maxLocals
        emit(this.tgtOut, this.code.toString());

        if (!this.expType.coerceTo(VOID.inst)) {
            emitLn(this.tgtOut, "getstatic java/lang/System/out Ljava/io/PrintStream;");
            emitLn(this.tgtOut, "swap");

            printMainResult();
        }

        emitLn(this.tgtOut, "return");

        emitLn(this.tgtOut, ".end method", "    -- (main)");
    }

    @Override
    public void emitPrelude(String initComment) {
        emitComment(this.tgtOut, (initComment != null ? initComment : "class setup instructions"));
        emitLn(this.tgtOut, ".class " + this.classname);
        emitLn(this.tgtOut, ".super java/lang/Object");
        emitLn(this.tgtOut, ".method public <init>()V");
        emitLn(this.tgtOut, "aload_0");
        emitLn(this.tgtOut, "invokespecial java/lang/Object/<init>()V");
        emitLn(this.tgtOut, "return");
        emitLn(this.tgtOut, ".end method");
        emitComment(this.tgtOut, "end initial setup for class " + classname);
    }

    @Override
    public void emitProcedures() {
        assert this.prog != null;
        assert this.procs != null;

        for (String proc : this.procs) {
            emitLn(this.tgtOut, proc);
        }
    }

    /**
     * Sets the AST for which we'll generate code. Must be used before calling
     * <tt>emitPrelude</tt>, <tt>emitMain</tt>, or <tt>emitProcedures</tt> The
     * nameExtension parameter is unused in this implementation.
     */
    @Override
    public void setProg(IAbsyn program, String outFName, String nameExtension) {
        assert outFName != null && nameExtension != null;
        this.prog = program;
        this.classname = outFName;
    }

    /**************** IAbsynVisitor implementation **********************/

    @Override
    public void visit(DeclGroupFunction fun_decs) {
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
                t_result = this.expType;

            } else {
                // dfn has no declared return type (i.e. a procedure).
                t_result = VOID.inst;
            }

            // B. Check the list of formals & build the FunEntry component
            List<Pair<Symbol, Type>> formals_construction = visitTypeFields(dfn, dfn.params);

            // C. Make a new FunEntry for this procedure
            FunEntry dfnEntry = new FunEntry(formals_construction, t_result);

            // D. Make entry label for this procedure and set it:

            // TODO: scrapped unique method names choice: is that right? --
            // A: No: they must be unique, since we're flattening potentially
            // nested
            // declarations (which can reuse names in different scopes).
            String fname = new Label(this.serialNumber, dfn.name.toString()).toString();
            // This generates a unique method name for dfn, which will be used
            // by any code that calls this method, instead of the programmer-
            // selected name

            String paramsLabel = "";
            for (Pair<Symbol, Type> fml : formals_construction) {
                paramsLabel += jvmType(fml.snd);
            }
            Label fnameLabel = new Label(fname + "(" + paramsLabel + ")" + jvmType(t_result));
            dfnEntry.setLabel(fnameLabel);

            venv.extend(dfn.name, dfnEntry);
        } // (FIRST PASS)

        /*
         * SECOND PASS: Generate the body of each function in this declaration
         * block. Since a procedure invocation represents another stack frame
         * (and since certain things like the break scope are reset), we do this
         * with a new code generator. For each of the parameters, we make a new
         * VarEntry in venv, for the duration of the time we spend checking that
         * function's body. For each of these, we also allocate another local in
         * the frame.
         * 
         * Finally, we're going to generate code to a temporary target, rather
         * than this.code. This allows us to assemble the three parts of the
         * procedure when we're done (prelude, body, and postlude), then add the
         * resulting procedure definition to a list of procedures that can be
         * emitted to our target object file at a later time (i.e. in the
         * emitProcedures() call).
         */
        for (DeclFn dfn : fun_decs.fns) {
            FunEntry fentry = (FunEntry) this.venv.lookup(dfn.name);
            // Note: this cast is safe, since we have just made FunEntry
            // bindings for every element in d.fns

            // (1) Create a new generator for the body of cur.
            // Why? Because certain matters of nesting and scope are "reset"
            // inside a new function body. In particular, you can use this to
            // leverage the checking of correct nesting of break statements.
            // You'll have to think about how. This is a hint here.

            JVMGeneratorV bodyCodeGen;
            try {
                // (2) This constructor call will open a new scope, and enter
                // the formals in
                // venv. This corresponds to the requirement in the interpreter
                // that the
                // present at the point of a function's definition must be saved
                // with the
                // function's "value" (closure), in order to implement lexical
                // scope.

                bodyCodeGen = new JVMGeneratorV(this.classname, this.venv, this.tenv, fentry.formals, this.procs);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new Error("INTERNAL BUG");
            }

            dfn.body.accept(bodyCodeGen);

            // Done with checking the current body. Close this scope.
            bodyCodeGen.venv.endScope();

            // write code for handling the prelude
            String procDefnCode = ";\n.method public static " + fentry.getLabel().toString() + "\n"; // "prelude"
            procDefnCode += (".limit locals " + bodyCodeGen.frame.maxLocals() + "\n");// bodyCodeGen.frame.maxLocals()
                                                                                      // +
                                                                                      // "\n");
            procDefnCode += ".limit stack " + bodyCodeGen.maxStack + "\n";

            // the body:
            procDefnCode += bodyCodeGen.code.toString();

            // the postlude (not much to do, in the JVM case)
            if (bodyCodeGen.expType.coerceTo(INT.inst))
                procDefnCode += "ireturn\n";
            else if (bodyCodeGen.expType.coerceTo(VOID.inst))
                procDefnCode += "return\n";
            else
                procDefnCode += "areturn\n";
            procDefnCode += ".end method ;     < " + fentry.getLabel() + " >";

            this.procs.add(procDefnCode);
        } // (SECOND PASS)
    }

    @Override
    public void visit(DeclGroupType dts) {
        assert dts != null;

        /*
         * The body of this visit method is virtually identical to the one in
         * SemantV, with the exception of no error checking (since it's
         * unnecessary). Declarations of types occur only in let expressions. A
         * new scope has already been declared there, and it will end at the end
         * of the let-body. Hence, we do not open a new scope here.
         */

        // PASS ONE: Add the names to tenv
        for (DeclTy dt : dts.decls) {
            tenv.extend(dt.name, new THUNK(dt.name));
        }

        // PASS TWO: Compute the actual bindings
        for (DeclTy dt : dts.decls) {
            dt.ty.accept(this);
            Type t_binding = this.expType;
            THUNK namet = (THUNK) tenv.lookup(dt.name);
            // Safe, since we just put these same symbols in env.
            namet.bind(t_binding);
        }
    }

    @Override
    public void visit(DeclVar d) {
        /*
         * <i>Effects:</i> a new IAccess is allocated and added to this.frame;
         * code is emitted for istore/astore instruction; a new binding is added
         * to this.venv;
         */
        assert d != null;

        d.init.accept(this);
        Type t = this.expType;

        IAccess acc = this.frame.allocLocal(t.actual());

        emitLn(this.code, ((t.coerceTo(INT.inst) ? "istore " : "astore ") + acc.offset()));

        venv.extend(d.name, new VarEntry(t, true, acc));
    }

    @Override
    public void visit(ExpArray e) {
        // PRE: Stack is [STK]
        // POST: Stack is [arr_ref::STK ] && expType is arrayResultType
        assert e != null;

        // 1. push array size onto the stack
        e.size.accept(this);
        assert this.expType.coerceTo(INT.inst) : "e.size is not an int";
        emitLn(this.code, "dup");
        // Stack is: [arr_length::arr_length::STK]

        ARRAY arrayRefType = (ARRAY) tenv.lookup(e.typ).actual(); // This will
                                                                  // be expType
                                                                  // when we're
                                                                  // done
        Type arrayEltType = arrayRefType.element.actual();

        /*
         * We're going to need two temporaries to handle setup and
         * initialization: one to store the array, another to walk through each
         * index in the created array
         */

        int frameSz = this.frame.frameEnd();
        /*
         * This is a safety check for debugging: after we're all done setting up
         * and deallocating the temporaries, the value of this.frame.frameEnd()
         * should be the same.
         */

        char jvmArrRefType = 0;

        if (arrayEltType.coerceTo(INT.inst)) {
            emitLn(this.code, "newarray int");
            jvmArrRefType = 'i';
        } else if (arrayEltType.coerceTo(STRING.inst) || arrayEltType.actual() instanceof RECORD
                || arrayEltType.actual() instanceof ARRAY) {
            emitLn(this.code, "anewarray " + jvmType(arrayRefType));
            /*
             * TODO For multidimensional arrays, using multianewarray would
             * produce faster code. We could even calculate every dimension up
             * front here. But I'm lazy.
             */
            jvmArrRefType = 'a';
        } else {
            throw new Error("visit(ExpArray): internal bug [found array element type " + arrayEltType.toString() + "]");
        }
        // Stack: [ arr_address::arr_length::STK ]

        IAccess arrRefTmp = this.frame.allocLocal(arrayEltType);
        // maxLocals += 1

        emitLn(this.code, "astore " + arrRefTmp.offset(), "store array reference");
        // Stack: [ arr_length::STK ]

        // 3. Evaluate initial value expression

        /*
         * Now for the initialization of every cell (a syntactic requirement in
         * Tiger): calculate the value of e.init, and write this value into each
         * cell of the newly-created array.
         * 
         * One subtlety: do we evaluate e.init once, then store the value in
         * each cell, or do we evaluate it once for each cell? This becomes
         * important when the array cells are of mutable reference type
         * (records, other arrays), since the evaluate-once approach will
         * introduce aliasing.
         * 
         * The original language standard is unclear on this matter. However,
         * the EPITA Tiger Project (the only active project dedicated to Tiger
         * that I know of) clearly states that the init expression should be
         * evaluated exactly once:
         * 
         * https://www.lrde.epita.fr/~tiger/tiger.html#Type-Declarations
         * 
         * We will adopt their convention here.
         */

        IAccess initTmp = this.frame.allocLocal(arrayEltType);
        char init_jvmT = (arrayEltType.coerceTo(INT.inst) ? 'i' : 'a');
        // maxLocals += 1;
        e.init.accept(this);
        // Stack: [ v_init::arr_length::STK ]
        emitLn(this.code, init_jvmT + "store " + initTmp.offset(), "initial value for array cells");

        // 4. Make another integer temporary, i, initialized to 0
        IAccess idx = this.frame.allocLocal(INT.inst);
        // maxLocals += 1;

        emitLn(this.code, "iconst_0");
        emitLn(this.code, "istore " + idx.offset(), "index variable, for initialization");
        // (INV) Stack is [ arr_length::STK ] -- beginning of loop

        // 5. Walk through each cell, i, calculate the value of e.init,
        // store in cell i.
        Label initLoopBody = new Label(this.serialNumber, "L");
        Label initLoopTest = new Label(this.serialNumber, "Test");

        emitLn(this.code, "goto " + initLoopTest);
        emitLn(this.code, initLoopBody + ":");
        // (INV) Stack is [ arr_length::STK ]
        emitLn(this.code, "aload " + arrRefTmp.offset());
        emitLn(this.code, "iload " + idx.offset());
        // Stack is [ i::arr_address::arr_length::STK ]

        emitLn(this.code, init_jvmT + "load " + initTmp.offset());
        // Stack is [ v_init::i::arr_address::arr_length::STK ]

        emitLn(this.code, jvmArrRefType + "astore");
        // (INV) Stack is [ arr_length::STK ]
        emitLn(this.code, "iinc " + idx.offset() + " 1", "end of init loop body"); // i++;
        // (INV) Stack is [ arr_length::STK ]

        emitLn(this.code, initLoopTest + ":");
        // (INV) Stack is [ arr_length::STK ]

        emitLn(this.code, "dup", "init loop entry");
        emitLn(this.code, "iload " + idx.offset());
        // Stack: [ i::arr_length::arr_length::STK]

        emitLn(this.code, "if_icmpgt " + initLoopBody);
        // (INV) Stack is [ arr_length::STK ] -- end of iteration
        emitLn(this.code, "pop");
        // Stack: (after loop cleanup): [ STK ]

        // 6. All done. Time for frame clean-up:

        this.frame.popLocal(); // deallocate idx
        this.frame.popLocal(); // deallocate arrTmp
        this.frame.popLocal(); // deallocate initTmp
        assert this.frame.frameEnd() == frameSz;

        emitLn(this.code, "aload " + arrRefTmp.offset(), "reference to created array");
        // Stack (done): [ arr_address::STK ]
        this.expType = arrayRefType;
    }

    @Override
    public void visit(ExpAssign e) {
        assert e != null && this.rvalueMode;
        // (since := cannot be chained together)

        /*
         * The evaluation order is not uniform across the three possibilities
         * for the LHS. If e.lhs is a simple identifier, then calling its
         * accept() method (in !rvalueMode) will generate code for the
         * associated store. This means the RHS must be executed first, and
         * hence we generate its code first.
         * 
         * In the case of either VarField, however, we must first calculate the
         * address of the record itself, then the value of the field where we're
         * storing the RHS, then generate the call to the record's (i.e.
         * java.util.Hashtable's) put() method. This means that the *LHS* must
         * be generated first. A similar constraint arises with VarSubscript.
         * First, we must put the address of the array on top of the stack, then
         * the value of the index, and finally, the value to be written. Again,
         * this requires that the LHS be executed first.
         */

        if (e.lhs instanceof VarSimple) {
            // For simple identifiers, we don't need to put the LHS value on the
            // stack. It suffices to compute the RHS, then store the value.

            e.rhs.accept(this);

            this.rvalueMode = false;
            e.lhs.accept(this);
            this.rvalueMode = true;

        } else {
            // For array subscripts and record fields, we need to do a little
            // more work in computing the LHS. Too, the address on the
            // record/array must be put on the stack, just below the top (which
            // will contain the value of the RHS).

            this.rvalueMode = false;
            e.lhs.accept(this);
            this.rvalueMode = true;

            // STACK: [ lhs_ addr, ... ]
            e.rhs.accept(this);
            // STACK: [ rhs_val, lhs_addr, ... ]

            if (e.lhs instanceof VarField) {
                emitLn(this.code,
                        "invokevirtual java/util/Hashtable/put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
                emitLn(this.code, "pop");
                // We don't care about the return value of put(), so throw it
                // away

            } else { // array store
                String tid = (this.expType.coerceTo(INT.inst) ? "i" : "a");
                emitLn(this.code, tid + "astore");
            }
            // STACK: [ ... ]
        }
        this.expType = VOID.inst;
    }

    @Override
    public void visit(ExpBreak e) {
        assert e != null && this.enclosingLoop != null && !this.enclosingLoop.empty();

        emitLn(this.code, "goto " + this.enclosingLoop.peek());
        this.expType = VOID.inst;
    }

    @Override
    public void visit(ExpCall e) {
        assert e != null;

        FunEntry f = (FunEntry) venv.lookup(e.func);

        // evaluation and passing of arguments
        for (Exp arg : e.args) {
            arg.accept(this);
        }

        // control transfer to procedure entry point
        if (f.isExternal()) {
            Class<?> extern = f.getEnclosingClass();
            emitLn(this.code, "invokestatic " + extern.getCanonicalName().replace(".", "/") + "/" + f.getLabel());
        } else {
            emitLn(this.code, "invokestatic " + classname + "/" + f.getLabel());
        }

        this.expType = f.result;
        // INVARIANT: Stack is [result::STK]
    }

    @Override
    public void visit(ExpFor e) {
        /*
         * Semantic analysis has already assured us of the basic type
         * requirements, so we're not going to be producing any error messages
         * here. Since we've already got code generation for ExpWhile, ExpSeq,
         * and ExpAssign, there's no reason not to treat this as a rewriting
         * problem, rather than reinventing the wheel, except for one: it takes
         * more effort to build the corresponding AST manually than it does to
         * just generate JVM code.
         */

        assert e != null;

        IAccess idx = this.frame.allocLocal(INT.inst); // for e.var
        // this.localsCt += 1;
        // this.maxLocals = (maxLocals < localsCt ? localsCt : maxLocals);

        e.lo.accept(this);
        // Stack: [ vlo, ... ]

        emitLn(this.code, "istore " + idx.offset());
        // Stack: [ ... ]

        e.hi.accept(this);
        // Stack: [ vhi, ... ]
        // Since this value won't change, we can just keep it on the stack.

        this.venv.beginScope();
        this.venv.extend(e.var, new VarEntry(INT.inst, false, idx));

        // Now for the actual loop:
        Label test = new Label(this.serialNumber, "test");
        Label body = new Label(this.serialNumber, "body");

        // Stack: [ vhi, ... ]
        emitLn(this.code, "goto " + test);

        // Stack (invariant): [ vhi, ... ]
        emitLn(this.code, body + ":");
        e.body.accept(this);
        assert this.expType.coerceTo(VOID.inst);

        // Stack (invariant): [ vhi, ... ] (since body produces no value)
        emitLn(this.code, "iinc " + idx.offset() + " 1");

        emitLn(this.code, test + ":");
        // Stack: [ vhi, ... ]
        emitLn(this.code, "dup");
        emitLn(this.code, "iload " + idx.offset());
        // Stack: [ idx, vhi, vhi, ... ]
        emitLn(this.code, "if_icmpge " + body);
        // Stack (invariant): [ vhi, ... ]

        emitLn(this.code, "pop");
        // remove the remaining vhi value from the stack, which gives us
        // Stack: [ ... ] (since body produces no value)

        // remove the loop counter variable
        this.venv.endScope();
        this.frame.popLocal(); // idx
        // this.localsCt -= 1;
    }

    @Override
    public void visit(ExpIf e) {
        assert e != null;

        Label skip = new Label(this.serialNumber, "endif");

        e.test.accept(this);
        emitLn(this.code, "ifeq " + skip);
        e.thenclause.accept(this);
        emitLn(this.code, skip + ":");

        // Assuming a correct SemantV, expType will be VOID
    }

    @Override
    public void visit(ExpIfElse e) {
        assert e != null;

        Label falseBranch = new Label(this.serialNumber, "false");
        Label joinPoint = new Label(this.serialNumber, "endif");

        e.test.accept(this);
        emitLn(this.code, "ifeq " + falseBranch);

        e.thenclause.accept(this);
        emitLn(this.code, "goto " + joinPoint);

        emitLn(this.code, falseBranch + ":");
        e.elseclause.accept(this);

        emitLn(this.code, joinPoint + ":");

        // Assuming a correct SemantV, expType will always be the type of the
        // then/else clauses
    }

    @Override
    public void visit(ExpInt e) {
        assert e != null;

        if (e.value == -1)
            emitLn(this.code, "iconst_m1");
        else if (0 <= e.value && e.value <= 5)
            emitLn(this.code, "iconst_" + e.value);
        else if (-128 <= e.value && e.value <= 127)
            emitLn(this.code, "bipush " + e.value);
        else if (Short.MIN_VALUE <= e.value && e.value <= Short.MAX_VALUE)
            emitLn(this.code, "sipush " + e.value);
        else
            emitLn(this.code, "ldc " + e.value);

        this.expType = INT.inst;
    }

    @Override
    public void visit(ExpLet e) {
        assert e != null;

        this.venv.beginScope();
        this.tenv.beginScope();
        int savedCount = this.frame.frameEnd();

        for (Decl d : e.decls) {
            d.accept(this);
        }

        e.body.accept(this);

        // Now remove the local variables from this frame
        // (since they won't be used again):
        for (int ct = this.frame.frameEnd() - savedCount; ct > 0; ct--) {
            this.frame.popLocal();
        }

        // this.localsCt = savedCount;

        this.venv.endScope();
        this.tenv.endScope();

    }

    @Override
    public void visit(ExpNil e) {
        emitLn(this.code, "aconst_null");
        this.expType = NIL.inst;
    }

    @Override
    public void visit(ExpOp e) {
        assert e != null;

        switch (e.oper) {
        case PLUS:
        case MIN:
        case MUL:
        case DIV: {
            genopArith(e);
            break;
        }
        case AND:
        case OR: {
            genopLogical(e);
            break;
        }
        default:
            genopCompare(e);
            break;
        }

        this.expType = INT.inst;
    }

    @Override
    public void visit(ExpRecord e) {
        /*
         * There are a few choices for implementing record types, but perhaps
         * the most straightforward in the JVM is to use java.util.Hashtable.
         * This offers the advantage of great simplicity, since reading and
         * writing from fields becomes a matter of using the hashtable's get()
         * and put() methods. The disadvantages are the impossibility of
         * checking at run time whether the field referenced is valid for this
         * record type, the commitment to implementing records on the heap, and
         * the requirement that all stored field values be of reference type
         * (with the associated performance hit).
         */
        assert e != null;

        emitLn(this.code, "new java/util/Hashtable");
        emitLn(this.code, "dup");
        emitLn(this.code, "invokespecial java/util/Hashtable/<init>()V");

        tigerc.semant.analysis.types.RECORD r = (RECORD) tenv.lookup(e.type);
        assert r.fields.size() == e.fields.size();

        // INV:TOS is reference to the Hashtable we're setting up
        for (int i = 0; i < e.fields.size(); i++) {
            Pair<Symbol, Type> fld = r.fields.get(i);
            Pair<Symbol, Exp> init = e.fields.get(i);

            // 0. duplicate hashtable reference (since we'll pop the currently
            // sole copy with put)
            emitLn(this.code, "dup");

            // 1. code for loading key value: always a String ldc "val"
            assert fld.fst == init.fst;
            emitLn(this.code, "ldc \"" + fld.fst + "\"");

            // 2.code for loading associated value: any type, and we may need to
            // convert (if it's an INT)

            init.snd.accept(this);
            assert this.expType.coerceTo(fld.snd);

            if (this.expType.coerceTo(INT.inst)) {
                emitLn(this.code, "invokestatic java/lang/Integer/valueOf(I)Ljava/lang/Integer; ");
            }

            // 3. code to store field & its value
            emitLn(this.code, "invokevirtual " + "java/util/Hashtable/put(Ljava/lang/Object;"
                    + "Ljava/lang/Object;)Ljava/lang/Object;");

            // 4. remove reference returned by put()
            emitLn(this.code, "pop");

            // INV:TOS is reference to the Hashtable we're setting up
        }
    }

    @Override
    public void visit(ExpSeq es) {
        assert es != null;

        for (Exp e : es.list) {
            e.accept(this);
        }
        // expTy will be the type of the final Exp in e.list
    }

    @Override
    public void visit(ExpString e) {
        assert e != null;

        emitLn(this.code, "ldc \"" + e.value + "\"");
        this.expType = STRING.inst;
    }

    @Override
    public void visit(ExpVar e) {
        assert e != null;

        e.var.accept(this);
    }

    @Override
    public void visit(ExpWhile e) {
        assert e != null;

        Label test = new Label(this.serialNumber, "test");
        Label loop = new Label(this.serialNumber, "loop");
        Label endWhile = new Label(this.serialNumber, "endwhile");
        this.enclosingLoop.push(endWhile);
        // record loop nesting, for potential break statements

        emitLn(this.code, "goto " + test);
        emitLn(this.code, loop + ":");

        e.body.accept(this);

        emitLn(this.code, test + ":");
        e.test.accept(this);
        emitLn(this.code, "ifne " + loop);

        emitLn(this.code, endWhile + ":");

        this.enclosingLoop.pop();
        // This nesting must exclude the test expression,
        // which may need to use the exit label of an enclosing loop

        this.expType = VOID.inst;
        // necessary, since we generated the test code last
    }

    /**
     * The visit(Ty*) methods are encountered while processing the RHS of a type
     * declaration. The resulting "return value" (assignment to this.ty) will be
     * the tenv binding for the identifier on the LHS of this declaration.
     * 
     * NOTE: Except for the lack of error checking, the bodies are identical to
     * those in SemantV.
     */
    public void visit(TyArray ta) {
        assert ta != null;

        Type t = tenv.lookup(ta.typ);
        this.expType = new ARRAY(t);

    }

    @Override
    public void visit(TyName tn) {
        assert tn != null;

        Type t = tenv.lookup(tn.name);
        this.expType = t;

    }

    @Override
    public void visit(TyRecord tr) {
        assert tr != null;

        List<Pair<Symbol, Type>> field_construction = visitTypeFields(tr, tr.fields);

        this.expType = new RECORD(field_construction);
    }

    @Override
    public void visit(VarField v) {
        assert v != null;

        // STACK: [ S ]
        // 0. Load the record value pointer:

        boolean savedMode = this.rvalueMode;
        this.rvalueMode = true;
        v.var.accept(this);
        this.rvalueMode = savedMode;

        // STACK: [ record_addr, S ]

        // 1. compute the key:
        emitLn(this.code, "ldc " + v.field.toString());

        // STACK: [ key, record_addr, S ]

        // 2. Set the type to be the one associated with v.field. Note that
        // this.expType is currently the type associated with v.var, i.e.
        // a RECORD. Hence, the resulting value of this.expType should be
        // the result of looking up in this record the associated Type for
        // v.field

        this.expType = lookup(((RECORD) this.expType.actual()).fields, v.field).actual();
        assert this.expType != null;

        // 3a. If this is part of an r-value, invoke the associated get()
        if (this.rvalueMode) {

            emitLn(this.code, "invokevirtual java/util/Hashtable/get(Ljava/lang/Object;)Ljava/lang/Object;");

            // Fine point: Hashtables only store object references as values, so
            // if the type of this field is INT, we'll need to convert it to its
            // primitive equivalent:
            if (this.expType.coerceTo(tigerc.semant.analysis.types.INT.inst)) {
                emitLn(this.code, "invokestatic java/lang/Integer/intValue()I");
            }

            // STACK: [ key_binding, S ]
        } else {

            // 3b. On the other hand, a "put" corresponds to the LHS of an
            // ExpAssign. We need the RHS for that, we'll defer it to the rest
            // of the visit(ExpAssign) call in which this visit(VarField) method
            // must have been nested.

            // STACK: [ key, record_addr, S ]
        }
    }

    @Override
    public void visit(VarSimple x) {
        assert x != null;

        VarEntry v = (VarEntry) venv.lookup(x.name);
        String loadstore = (this.rvalueMode ? "load" : "store");

        if (v.ty.coerceTo(INT.inst)) {
            emitLn(this.code, "i" + loadstore + " " + v.access.offset());
        } else { // strings, arrays, and records are all reference types
            emitLn(this.code, "a" + loadstore + " " + v.access.offset());
        }

        this.expType = v.ty.actual();
    }

    @Override
    public void visit(VarSubscript v) {

        /*
         * As in visit(VarField), the final instruction we generate here depends
         * on whether call is part of a left hand side or right hand side. If
         * it's a LHS, we *can't* generate code for it yet, because we don't
         * have access to the RHS.
         * 
         * Actually, the structure of this method is outrageously similar to
         * that of visit(VarField).
         */
        assert v != null;

        // 0a.Hang on to the read/write mode at entry to this call:

        boolean savedMode = this.rvalueMode;
        this.rvalueMode = true;

        // 0b. Get the array part:
        v.var.accept(this);
        // System.out.println("ExpArray: found v.var type " + expType);
        assert (this.expType instanceof ARRAY);
        Type te = ((ARRAY) this.expType).element.actual();

        this.rvalueMode = savedMode;

        // Stack: [ arr_addr, ... ]

        // 0c. Set the appropriate JVM type indicator:
        char jvmArrType = 0;

        if (te instanceof INT) {
            jvmArrType = 'i';
        } else if (te instanceof STRING || te instanceof RECORD || te instanceof ARRAY) {
            jvmArrType = 'a';
        } else {
            throw new Error("visit(ExpArray): internal bug");
        }

        // 1. compute the index:

        v.index.accept(this);
        // Stack: [ idx, arr_addr, ... ]

        // 2a. If this is part of an r-value, invoke the associated get()
        if (this.rvalueMode) {
            emitLn(this.code, jvmArrType + "aload");
        }
        // 2b. On the other hand, a "*astore" corresponds to the LHS of an
        // ExpAssign. We need the RHS for that, so we'll defer it to the rest
        // of the visit(ExpAssign) call in which this visit(VarSubscript) method
        // must be nested.

        this.expType = te;
        // If subscript expression is part of an assignment, the value of
        // this.expType communicates what we know about the value of the RHS.
    } // visit(VarSubscript)

    
    
    /************************************************************************* 
     ***** private utility methods (instance)
     ****************************************/
    private void emitLn(PrintWriter out, String s, String comment) {
        emit(out, s + " ");
        emitComment(out, comment);
    }

    private void printMainResult() {
        // println isn't defined on that
        // PRE: Stack is [mainRes::System_out_ref::'()]

        if (this.expType instanceof ARRAY) {
            ARRAY arrExpTy = (ARRAY) this.expType;
            if (arrExpTy.element.coerceTo((INT.inst))) {
                // Use java.util.Arrays.toString(intArray);
                emitLn(this.tgtOut, "invokestatic java/util/Arrays/toString([I)Ljava/lang/String;");
                // Stack is [str(mainRes)::System_out_ref::'()]
            } else {
                // Use java.util.Arrays.toString(intArray);
                emitLn(this.tgtOut, "invokestatic java/util/Arrays/toString([java/lang/Object;)Ljava/lang/String;");
                // Stack is [str(mainRes)::System_out_ref::'()]
            }
            emitLn(this.tgtOut, "invokevirtual java/io/PrintStream/println(Ljava/lang/String;)V");
            // Stack is ['()]
        } else if (!this.expType.coerceTo(VOID.inst)) {
            emitLn(this.tgtOut, "invokevirtual java/io/PrintStream/println(" + jvmType(this.expType) + ")V");
            // Stack is ['()]
        }
    }

    private void genopArith(ExpOp e) {

        e.left.accept(this);
        e.right.accept(this);
        switch (e.oper) {
        case PLUS: {
            emitLn(this.code, "iadd");
            break;
        }
        case MIN: {
            emitLn(this.code, "isub");
            break;
        }
        case MUL: {
            emitLn(this.code, "imul");
            break;
        }
        case DIV: {
            emitLn(this.code, "idiv");
            break;
        }
        default:
            throw new Error("JVMGeneratorV.genopArith() -- internal bug");
        }
    }

    private void genopCompare(ExpOp e) {
        final Label labelT = new Label(this.serialNumber, "true");
        final Label join = new Label(this.serialNumber, "end");
        final java.util.Hashtable<ExpOp.Op, String> intCmds = intCmds();
        final java.util.Hashtable<ExpOp.Op, String> strCmds = strCmds();

        e.left.accept(this);
        e.right.accept(this);

        // First, we generate the appropriate comparison instruction(s)
        if (this.expType.coerceTo(INT.inst)) {
            emitLn(this.code, intCmds.get(e.oper) + " " + labelT);
        } else if (this.expType.coerceTo(STRING.inst)) {
            emitLn(this.code, "invokevirtual Ljava/lang/String/compareTo(Ljava/lang/String;)Z");
            emitLn(this.code, strCmds.get(e.oper) + " " + labelT);
        } else
            throw new Error("JVMGeneratorV::genopCompare() -- internal bug");

        // the "false" branch of the comparison begins here
        emitLn(this.code, "iconst_0");
        emitLn(this.code, "goto " + join);
        // the "true" branch"
        emitLn(this.code, labelT + ":");
        emitLn(this.code, "iconst_1");
        // and, done.
        emitLn(this.code, join + ":");
    }

    private void genopLogical(ExpOp e) {
        Label labRight = new Label(this.serialNumber, "right");
        Label join = new Label(this.serialNumber, "join");

        e.left.accept(this);

        if (e.oper == ExpOp.Op.AND) {
            emitLn(this.code, "ifne " + labRight);
            emitLn(this.code, "iconst_0");
            emitLn(this.code, "goto " + join);
        } else if (e.oper == ExpOp.Op.OR) {
            emitLn(this.code, "ifeq " + labRight);
            emitLn(this.code, "iconst_1");
            emitLn(this.code, "goto " + join);
        } else
            throw new Error("JVMGeneratorV.genopLogical() -- internal bug");

        emitLn(this.code, labRight + ":");
        e.right.accept(this);
        emitLn(this.code, join + ":");
    }

    /**
     * From a collection of (id,type-id) pairs, this produces a corresponding
     * list of (id,type) pairs. Except for the error checking and the additional
     * sentinel boolean value in the result, this function is identical to the
     * one in SemantV.
     * 
     * @param a
     *            The syntax element where the access list occurs used for error
     *            reporting
     * @param access
     * @return
     */
    private List<Pair<Symbol, Type>> visitTypeFields(ISyntaxElt a, Collection<Pair<Symbol, Symbol>> access) {

        List<Pair<Symbol, Type>> container = new java.util.ArrayList<Pair<Symbol, Type>>();
        java.util.Set<Symbol> defined = new java.util.HashSet<Symbol>();

        for (Pair<Symbol, Symbol> p : access) {
            Type t = tenv.lookup(p.snd);
            assert t != null;

            container.add(new Pair<Symbol, Type>(p.fst, t));
            defined.add(p.fst);
        }

        return container;
    }

    
    /************************************************************************* 
     ***** private utility methods (static)
     ****************************/

    private static void setupStdLibrary() throws ClassNotFoundException {

        final Class<?> STDLIB_CLASS = Class.forName("TigerStdLib");

        /****************
         * The two "built-in" types:
         */

        extern_tenv.extend(Symbol.sym("int"), INT.inst);
        extern_tenv.extend(Symbol.sym("string"), STRING.inst);

        /****************
         * Labels for the library procedures:
         */
        List<Pair<Symbol, Type>> fmls;
        FunEntry fe;

        // print
        fmls = new java.util.ArrayList<>();
        fmls.add(new Pair<Symbol, Type>(Symbol.sym("s"), STRING.inst));
        fe = new FunEntry(fmls, VOID.inst);
        fe.setEnclosingClass(STDLIB_CLASS);
        fe.setLabel(new Label("print(Ljava/lang/String;)V"));
        extern_venv.extend(Symbol.sym("print"), fe);

        // printi
        fmls.clear();
        fmls.add(new Pair<Symbol, Type>(Symbol.sym("s"), INT.inst));
        fe = new FunEntry(fmls, VOID.inst);
        fe.setEnclosingClass(STDLIB_CLASS);
        fe.setLabel(new Label("printi(I)V"));
        extern_venv.extend(Symbol.sym("printi"), fe);

        // flush
        fe = new FunEntry(null, VOID.inst);
        fe.setEnclosingClass(STDLIB_CLASS);
        fe.setLabel(new Label("flush()V"));
        extern_venv.extend(Symbol.sym("flush"), fe);

        // getchar
        fe = new FunEntry(null, STRING.inst);
        fe.setEnclosingClass(STDLIB_CLASS);
        fe.setLabel(new Label("getchar()Ljava/lang/String;"));
        extern_venv.extend(Symbol.sym("getchar"), fe);

        // ord
        fmls.clear();
        fmls.add(new Pair<Symbol, Type>(Symbol.sym("s"), STRING.inst));
        fe = new FunEntry(fmls, INT.inst);
        fe.setEnclosingClass(STDLIB_CLASS);
        fe.setLabel(new Label("ord(Ljava/lang/String;)I"));
        extern_venv.extend(Symbol.sym("ord"), fe);

        // chr
        fmls.clear();
        fmls.add(new Pair<Symbol, Type>(Symbol.sym("s"), INT.inst));
        fe = new FunEntry(fmls, STRING.inst);
        fe.setEnclosingClass(STDLIB_CLASS);
        fe.setLabel(new Label("chr(I)Ljava/lang/String;"));
        extern_venv.extend(Symbol.sym("chr"), fe);

        // size
        fmls.clear();
        fmls.add(new Pair<Symbol, Type>(Symbol.sym("s"), STRING.inst));
        fe = new FunEntry(fmls, INT.inst);
        fe.setEnclosingClass(STDLIB_CLASS);
        fe.setLabel(new Label("size(Ljava/lang/String;)I"));
        extern_venv.extend(Symbol.sym("size"), fe);

        // substring
        fmls.clear();
        fmls.add(new Pair<Symbol, Type>(Symbol.sym("s"), STRING.inst));
        fmls.add(new Pair<Symbol, Type>(Symbol.sym("first"), INT.inst));
        fmls.add(new Pair<Symbol, Type>(Symbol.sym("n"), INT.inst));
        fe = new FunEntry(fmls, STRING.inst);
        fe.setEnclosingClass(STDLIB_CLASS);
        fe.setLabel(new Label("substring(Ljava/lang/String;II)Ljava/lang/String;"));
        extern_venv.extend(Symbol.sym("substring"), fe);

        // concat
        fmls.clear();
        fmls.add(new Pair<Symbol, Type>(Symbol.sym("s1"), STRING.inst));
        fmls.add(new Pair<Symbol, Type>(Symbol.sym("s2"), STRING.inst));
        fe = new FunEntry(fmls, STRING.inst);
        fe.setEnclosingClass(STDLIB_CLASS);
        fe.setLabel(new Label("concat(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"));
        extern_venv.extend(Symbol.sym("concat"), fe);

        // not
        fmls.clear();
        fmls.add(new Pair<Symbol, Type>(Symbol.sym("i"), INT.inst));
        fe = new FunEntry(fmls, INT.inst);
        fe.setEnclosingClass(STDLIB_CLASS);
        fe.setLabel(new Label("not(I)I"));
        extern_venv.extend(Symbol.sym("not"), fe);

        // exit
        fmls.clear();
        fmls.add(new Pair<Symbol, Type>(Symbol.sym("i"), INT.inst));
        fe = new FunEntry(fmls, VOID.inst);
        fe.setEnclosingClass(STDLIB_CLASS);
        fe.setLabel(new Label("exit(I)V"));
        extern_venv.extend(Symbol.sym("exit"), fe);
    }

    // /////////////////////////////////////////////////////////////////////

    private static void emit(java.io.PrintWriter out, String s) {
        out.print(s);
    }

    private static void emitComment(java.io.PrintWriter out, String comment) {
        emitLn(out, "; " + comment);
    }

    private static void emitLn(java.io.PrintWriter out, String s) {
        emit(out, s + "\n");
    }

    private static java.util.Hashtable<ExpOp.Op, String> intCmds() {
        java.util.Hashtable<ExpOp.Op, String> cmds = new java.util.Hashtable<ExpOp.Op, String>();

        cmds.put(ExpOp.Op.EQ, "if_icmpeq");
        cmds.put(ExpOp.Op.NE, "if_icmpne");
        cmds.put(ExpOp.Op.GT, "if_icmpgt");
        cmds.put(ExpOp.Op.GE, "if_icmpge");
        cmds.put(ExpOp.Op.LT, "if_icmplt");
        cmds.put(ExpOp.Op.LE, "if_icmple");

        return cmds;
    }

    private static String jvmType(Type t) {
        assert t != null;

        t = t.actual();

        if (t instanceof INT) {
            return "I";
        } else if (t instanceof VOID) {
            return "V";
        } else if (t instanceof STRING) {
            return "Ljava/lang/String;";
        } else if (t instanceof RECORD) {
            return "Ljava/util/Hashtable;";
        } else if (t instanceof ARRAY) {
            return "[" + jvmType(((ARRAY) t).element);
        } else
            throw new Error("JVMGenerator.jvmType(): internal bug");
    }

    /*
     * For a given list of Pairs and left element x, returns the corresponding
     * right element in the Pair with the lowest index
     * 
     * @param ls The list of Pairs
     * 
     * @param x The left element we're looking for
     * 
     * @return y such that (x,y) is the first Pair in ls containing x. Returns
     * <tt>null</tt>, if <em>x</em> is not found
     */
    private static <A, B> B lookup(List<Pair<A, B>> ls, A x) {
        for (Pair<A, B> p : ls) {
            if (p.fst.equals(x))
                return p.snd;
        }

        return null;
    }

    /**
     * This is used in the constructor and visit(DecGroupFunction) methods to
     * build a PrintWriter from which we can print contents of the underlying
     * character output stream. The underlying stream is a StringWriter.
     * 
     * @return The resulting PrintWriter is behaviorally indistinguishable from
     *         java.util.PrintWriter, except that the toString() method is that
     *         of the underlying output stream.
     */
    private static PrintWriter makePrintableWriter() {
        return new java.io.PrintWriter(new java.io.StringWriter()) {
            public String toString() {
                return out.toString();
            }
        };
    }

    private static java.util.Hashtable<ExpOp.Op, String> strCmds() {
        java.util.Hashtable<ExpOp.Op, String> cmds = new java.util.Hashtable<ExpOp.Op, String>();

        cmds.put(ExpOp.Op.EQ, "ifeq");
        cmds.put(ExpOp.Op.NE, "ifne");
        cmds.put(ExpOp.Op.GT, "ifgt");
        cmds.put(ExpOp.Op.GE, "ifge");
        cmds.put(ExpOp.Op.LT, "iflt");
        cmds.put(ExpOp.Op.LE, "ifle");

        return cmds;
    }
}
