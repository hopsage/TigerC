*********************************************
# TigerC - A compiler and interpreter for Tiger

Author: John H. E. Lasseter
Last Modified:  04/23/2020

## OVERVIEW

This is a complete interpreter and nearly complete compiler for the Tiger
programming language, as described by Andrew Appel in his textbook trio *Modern
Compiler Implementation in Java [/ML/C]*.  See also, Stephen Edwards'
documentation of the language, available (as of 04/23/2020) at 

http://www.cs.columbia.edu/~sedwards/classes/2002/w4115/tiger.pdf

This is the reference implementation for the semester-long project I built for
CPSC 433 (Compilers), as taught in the Spring semesters of 2014, 2016, 2018, and
2020.  Some parts of the source code are a fair bit older than that, with the
oldest source files dating back to my time as a grad student, in 1998!  With the
exception of some code templates in the JFlex file, and the skeletons of a
couple of targets in the `build.xml` file, all of it was designed and written by
me.

### Background

In my Winter 2014 Compilers class, I began introducing the idea of a language 
interpreter to explain the major front end translation tasks for a statically-typed
language, in particular semantic analysis and intermediate code generation.  The 
simplicity of a basic interpreter's structure seemed to help many students to more 
rapidly build the necessry conceptual models for understanding both type checking
and some of the stickier parts of language translation, such as procedure/function 
setup, execution, and tear down (in Appel's text, the `procEntryExit*()` methods).

This structure also allows for some slick code reuse, using Java's generics mechanism, 
a fact that helpos to further drive home the connections between the interpreter and
these stages of translation.  Finally, it also let me work in brief discussions of 
two of my favorite topics abstract interpretation (the theoretical basis for type
checking) and partial evaluation (the theoretical guarantee that code generation is
possible).  This is explained in more detail in a SIGCSE paper I wrote, "The Interpreter
In An Undergraduate Compilers Course" *(SIGCSE 2015)*.  A PDF copy of that paper is
incuded in the `doc` directory, though some of the specifics of the design choices
have changed since that time.

### Support For The Tiger Language

The compiler implements the core Tiger language semantics completely (that is, without 
the later book chapters that add in first class functions, class definitions, and so on), 
with two exceptions:

1. Escape characters of the form `\^*` are not supported.
2. Lexical closures -- i.e., nested procedure definitions whose bodies reference
   variables with scope defined in an enclosing definition -- are not correctly
   implemented at this time.  For example, the `queens.tig` example from Appel's 
   original distribution contains a nested procedure, 
   `printboard()`, which uses variables defined in the enclosing method 
   body.  Although `tigerc` will compile these to JVM assembly, it does not do so 
   correctly, and the resulting executable will throw a `VerifyError` exception.
   
   This is an artifact of the JVM giving no access to a method's stack frame,
   which makes the construction of static links impossible.  Other techniques 
   for implementing lexical closures do exist (and I have given this as a final 
   project in the past), but so far, I haven't finished a complete implementation
   that I'm happy with.

## BUILDING

The project contains an Ant build file, with the following main targets (also 
visible by invoking `ant -projecthelp`):

* `clean`        - Remove build directory (opt: generated src files and dist).  
                 NOTE: If you want it to also delete the generated lexer and 
                 parser source code files and/or the distribution JAR files 
                 (built by dist and dist_interp), you can uncommment 
                 a couple of lines in this target.
 * `dist`        - Build executable jar for the compiler, dist/tigerc.jar
 * `dist_interp` - Build executable jar for the interpreter, dist/tigeri.jar
 * `genLex`      - Generate the lexer from JFlex specification, 
                 tigerc/syntax/parse/TigerLex.java
 * `genParse`    - Generate the parser from CUP specification,
                 tigerc/syntax/parse/TigerParse.java and TigerSyms.java
 * `stdlib`      - Make jar file of Tiger standard library, lib/tiger_stdlib.jar
 * `testLexer`   - Compile and run test file for TigerLex:  this was part of what I gave
                   my students in the lexical analysis assignments
 * `testParser`  - Compile and run test file for TigerParse:  another student distribution
 * `testSemant`  - Compile and run test file for semantic analysis:  likewise
 
The default target is `dist`.

If you are interested in seeing some of the internal execution of the common
compiler/interpreter front end, there is a static DEBUG field in both `TigerI` and
`TigerC`.  Modifying the source code to set this field to `true` turns on the debug
mode for CUP's LALR(1) parser, which will show all the shift/reduce steps taken
during parsing.  It will also display the abstract syntax tree produced by the
parser.

## REQUIRED JAR FILES

The following archives are included in the lib directory.  All are necessary for
building the various components of the compiler and rendering Java byte code
from the result.

* `jasmin.jar`                - A tool for turning JVM assembler into byte code
* `java-cup-11a.jar`          - The CUP parser generator.  Yes, I know that CUP is
                                basically an abandoned project, but until recently,
                                it was the best of the yacc-like alternatives.  Now 
                                that yacc supports Java code more easily, I should
                                probably get around to adopting it for parser generation.
* `java-cup-11a-runtime.jar`  - Runtime libraries used by the generated parser
* `jflex-1.6.1.jar`           - The JFlex lexical analyzer generator
* `tiger_stdlib.jar`          - The standard library of procedures for Tiger 
                              programs.  See A. Appel, _Modern Compiler 
                              Implementation in [C/Java/ML]_.

## RUNNING 

* To run the interactive interpreter:
   
   ```
    java - jar tigeri.jar
    ```
    
* To compile Tiger source code to JVM byte code:

   ```
    java -jar tigerc.jar  myprogram.tig
   ```
    
    (This produces the file Myprogram.j, in the same folder as myprogram.tig.)
    
   ```
    java -jar jasmin.jar Myprogram.j
   ```
    
    (This will produce an executable class file, Myprogram.class, in the current
     folder.)
    
* Running compiled binaries requires the Tiger Standard Library be added to the 
  JRE classpath:
  
   ```
    java -cp .:$CLASSPATH:./lib/tiger_stdlib.jar  Myprogram
   ```

   
As of July 1st, 2020, this project will be archived long term on GitHub, at 

   ```
    https://github.com/hopsage/TigerC-Compiler
   ```
    
For the foreseeable future, you can reach me either through that page or by 
email at jhelasseter@gmail.com

## KNOWN BUGS, LIMITATIONS

* `tigeri`
    - You have to type an extra blank line following an expression to begin 
      parsing.  This is an easy but tedious fix.  At some point, I'll get around 
      to improving it.
    - The Tiger standard library is fully supported, but some of the assumptions 
      that are made in doing so will make it challenging to support 
      general-purpose importing of resources from other namespaces.  In particular, the source code in tigerc.semant.interp.ExternFunEntry only supports translation between strings and integer values, along with methods of void return type.   As of this writing (04/23/2020), external methods that interact with Tiger arrays and records are not supported.
* `tigerc`
    - Because this is designed to be a teaching compiler project rather than a 
      production-grade effort, the code generator produces Jasmin rather than 
      actual JVM byte code.  This is a textual representation of the byte code, 
      such as you would see by using "javap -v" to disassemble the actual .class 
      file.  Hence an additional invocation of Jasmin is required to produce 
      actual, running code.
    - In many places, the quality of the generated code may be terrible.  There 
      is no effort at code-improving transformations, and there is only 
      rudimentary made to control local variable allocation and to limit the 
      size of a method's runtim stack.  In every case, I opted for clarity of 
      exposition over concerns of performance or memory.
    - Although they are correctly handled during semantic analysis, the JVM code 
      generator does not correctly support lexical closures, so any nested 
      procedure definitions that reference variables defined in an enclosing 
      scope will fail.
  
* both `tigerc` and `tigeri`:
    - Support for the `\^*` family of control characters is not implemented.
    - The lexer and parser produce very low-quality error reporting and make no 
      effort at repairing syntax errors.
