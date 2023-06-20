/*************************************************************************
 **  tigerc/test/TigerInterpreter.java 
 **
 **  Author:  John Lasseter
 **  Created:  03/09/2016
 **
 **  This driver file is suitable for running the interpeter for Tiger.
 **  It's very no-frills.  If you don't provide input from a file opened 
 **  with IO redirection, you'll need to close the input stream manually, 
 **  using ^D
 ** 
 ************************************************************************/
package test;

import tigerc.syntax.parse.*;
import tigerc.syntax.absyn.*;

import tigerc.util.ErrorMsg;
import tigerc.util.AbsynPrintVisitor;
import tigerc.semant.interp.InterpV;
import tigerc.semant.interp.values.IValue;

public class TigerInterpreter {
    public static void main(String[] args) {
        java.io.InputStream inp;
        ErrorMsg errorMsg;
        try {
            errorMsg = new ErrorMsg(null);
            inp = System.in;

            while (true) {
                System.out.print("-->  ");
                TigerParse parser = new TigerParse(new TigerLex(inp, errorMsg),
                        errorMsg);
                IAbsyn prog = (IAbsyn) parser.parse().value;
            
                AbsynPrintVisitor prettyprint = new AbsynPrintVisitor(System.out);
                prog.accept(prettyprint); // "call prettyprint(prog)"
                System.out.println();

                InterpV interpreter = new InterpV(errorMsg);
                prog.accept(interpreter);
                IValue val = interpreter.getResult();
                System.out.println(val);
            }
        } catch (Throwable e) {
            System.out.println("Error: " + e);
            e.printStackTrace();
        }
    }
}
