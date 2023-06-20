/*************************************************************************
 **  tigerc/TigerC.java 
 **
 **  Author:  John Lasseter and Max Barsh
 **  Created:  04/11/2016
 **  Last Modified: 04/21/2018
 **
 **  Driver for the tigerc compiler. 
 ** 
 ************************************************************************/

package tigerc;

import tigerc.syntax.parse.*;
import tigerc.syntax.absyn.*;
import tigerc.util.AbsynPrintVisitor;
import tigerc.util.ErrorMsg;
import tigerc.semant.analysis.SemantV;
import tigerc.translate.*;
import tigerc.translate.jvm.JVMGeneratorV;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.PrintWriter;

public class TigerC {
    private static boolean _DEBUG = false;

    public static void main(String[] args) {
        File srcFile = null;
        String fname = null;
        InputStream inp = null;
        PrintWriter outp = null;
        ErrorMsg errorMsg = null;
        String tgtClassName = "A_out";
        String outFileName = "A_out.j";
        // String outFileNameFull = "A_out.j";

        try {
            if (args.length >= 1) {
                srcFile = (new File(args[0])).getCanonicalFile();
                fname = srcFile.getName();

                if (fname.endsWith(".tig")) {
                    tgtClassName = Character.toUpperCase(srcFile.getName().charAt(0))
                            + fname.substring(1, fname.length() - 4);
                    System.out.println("target class is " + tgtClassName);

                    outFileName = tgtClassName + ".j";
                    errorMsg = new ErrorMsg(fname);
                    inp = new FileInputStream(srcFile);

                    File f = new File(srcFile.getParent(), outFileName);
                    System.out.println("Opening " + f.getCanonicalPath() + "(" + f.getCanonicalFile() + ")");
                    outp = new PrintWriter(f);
                } else {
                    System.err.println(
                            "error: source format " + fname.substring(fname.length() - 4) + " not recognized.");
                    System.exit(1);
                }
            } else {
                errorMsg = new ErrorMsg(null);
                inp = System.in;
                outp = new PrintWriter(System.out);
            }

            TigerParse parser = new TigerParse(new TigerLex(inp, errorMsg), errorMsg);

            IAbsyn prog;
           
            if (TigerC._DEBUG ) {
                prog = (IAbsyn) (parser.debug_parse().value);
                AbsynPrintVisitor prettyprint = new AbsynPrintVisitor(System.out);
                prog.accept(prettyprint);
            } else {
                prog = (IAbsyn) (parser.parse().value);
            }
            
            inp.close();


            System.out.println();
            SemantV typechecker = new SemantV(errorMsg);
            prog.accept(typechecker);

            if (errorMsg.anyErrors) {
                System.err.println("Error - no code was generated.");
                System.exit(1);
            } else {
                ICodegen jvm = new JVMGeneratorV(outp);
                // jvm.setupStdLibrary();
                jvm.setProg(prog, tgtClassName, "j");
                if (fname != null) {
                    jvm.emitPrelude("generated from " + fname + " on " + new java.util.Date());
                } else {
                    jvm.emitPrelude("generated from standard input on " + new java.util.Date());
                }
                jvm.emitMain();
                jvm.emitProcedures();

                outp.flush();
                outp.close();

                System.err.println("Code written to " + outFileName);
                System.err.println("JVM byte code can be produced using Jasmin.");
                System.exit(0);
            }
        } catch (java.io.FileNotFoundException e) {
            System.err.println("Cannot open input file " + args[0] + "");
            e.printStackTrace();
            System.exit(1);
        } catch (Throwable e) {
            System.err.println("Error: " + e);
            e.printStackTrace();
        }
    } // main
}