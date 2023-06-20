/*************************************************************************
 **  ErrorMsg.java
 **  tigerc
 ** 
 **  Created by John Lasseter
 **  for the class CS 353 (Principles of Compiler Construction), Fall 2011
 **  with acknowledgement of a heavy debt to Andrew Appel's version, as
 **  presented in _Modern Compiler Implementation in Java (1e)_ .
 **
 **  This is a very bare-bones version of an error message handler.  Its 
 **  error method does nothing other than display a single message and set
 **  a flag that can be used by the caller.
 **
 **  For use with the lexer generator.
 **
 **************************************************************************/

package tigerc.util;  

public class ErrorMsg {

	private int lineNum = 1;
	private String filename;
	public boolean anyErrors;

	public ErrorMsg(String f) {
		filename = f;
	}

	public void newline() {
		lineNum++;
	}

	public void error(int col, String msg) {
		anyErrors = true;
		System.err.println(filename + ":" + lineNum + "." + col + ": " + msg);
	}
}
