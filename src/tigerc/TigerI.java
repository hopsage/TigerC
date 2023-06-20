/*************************************************************************
 **  tigerc/TigerI.java 
 **
 **  Author:  John Lasseter
 **  Created:  04/11/2014
 **  Last Modified: 03/05/2018
 **
 **  
 **  Tiger Interpreter. 
 ** 
 ************************************************************************/

package tigerc;

import tigerc.syntax.parse.*;
import tigerc.syntax.absyn.*;
import tigerc.util.AbsynPrintVisitor;
import tigerc.util.ErrorMsg;
import tigerc.semant.analysis.SemantV;
import tigerc.semant.interp.InterpV;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;

public class TigerI {
    private static final double version = 0.1;
    private static String lastModified = "April 22, 2020";
    private static boolean _DEBUG = false;
    
    public static void main(String[] args) throws IOException {

        System.out.println(
                "************ TigerI (interactive Tiger) *******************");
        System.out.printf("version %.1f -- %s)%n", version, lastModified);
        System.out.println("John Lasseter (Hobart & William Smith Colleges)");


        BufferedInputStream b_in = new BufferedInputStream(System.in);
        BufferedReader in = new BufferedReader(new InputStreamReader(b_in));
        PrintStream out = new PrintStream(System.out, true);

        String input = "";

        out.println();
        out.print("--> ");
        out.flush();

        do {
            String line = in.readLine();
            if (line != null) {
                // blank line triggers evaluation
                if (line.trim().equals("")) {
                    try {
                        ErrorMsg errorMsg = new ErrorMsg(null);
                        TigerParse parser = new TigerParse(
                                new TigerLex(new StringReader(input), errorMsg),
                                errorMsg);
                        IAbsyn prog = null;
                        
                        if (TigerI._DEBUG  ) {
                            prog = (IAbsyn) (parser.debug_parse().value);
                            AbsynPrintVisitor prettyprint = new AbsynPrintVisitor(System.out);
                            prog.accept(prettyprint);
                            // "call prettyprint(prog)"
                        } else {
                            prog = (IAbsyn) (parser.parse().value);
                        }

                        SemantV typechecker = new SemantV(errorMsg);
                        prog.accept(typechecker);

                        if (!errorMsg.anyErrors) {
                            InterpV interp = new InterpV(errorMsg);
                            prog.accept(interp);

                            out.println(
                                    "\nRESULT = " + interp.getResult() + "\n");

                        }
                    } catch (Exception e) {
                        System.err.println("\nError: " + e);
                        e.printStackTrace();
                        // in.skip(b_in.available()); // clear anything still on the input
                    }

                    input = "";
                    out.println();
                    out.print("--> ");
                    out.flush();
                } else {
                    input = input + line + "\n";
                    out.print(">   ");
                    out.flush();
                }
            } // if line != null
        } while (input != null);
    } // main
}