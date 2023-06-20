import java.io.PrintStream;

public class TigerStdLibTest {

        public static void main(String[] args) {
	  testPrint();
          testPrinti();
          testFlush();
          testGetchar();
          testOrd();
          testChr();
          testSize();
          testSubstring();
        }


	public static void testPrint() {
		System.err.println("\nprint():  *******************************");
		TigerStdLib.print("This is the test string for print()");
	}

	public static void testPrinti() {
		System.err.println("\nprinti(42):  *******************************");
		TigerStdLib.printi(42);
	}

	public static void testFlush() {
		System.err.println("\nflush():  *******************************");
		TigerStdLib.flush();
		TigerStdLib.flush();
		TigerStdLib.flush();
		System.err.println("(3 blank lines should have been printed)");
	}

	public static void testGetchar() {
		System.err.println("getchar():  *******************************");
		String a = TigerStdLib.getchar();
		String b = TigerStdLib.getchar();
		String c = TigerStdLib.getchar();

		if ((a + b + c).equals("abc")) {
			System.err.println("PASS");
		} else {
			System.err.println("FAIL (expected \"abc\", received \"" + (a + b + c) + "\"");
		}
	}

	public static void testOrd() {
		System.err.println("ord():  *******************************");
		boolean pass = true;
		for (char c : "aB3#<,".toCharArray()) {
			int ordC = TigerStdLib.ord("" + c);
			int x = c;
			System.out.println("'" + c + "':  " + ordC + "(exp. " + x + ")");
			if (ordC != x)
				pass = false;
		}
		System.err.println(pass ? "PASS" : "FAIL");
	}

	public static void testChr() {
		System.err.println("ord():  *******************************");
		boolean pass = true;

		for (int i : new int[] { 0, 97, 66, 51, 35, 60, 44 }) {
			char c = (char) i;
			String iChr = TigerStdLib.chr(i);
			System.out.println(i + ":  '" + iChr + "' (exp. '" + c + "')");
			if (iChr.charAt(0) != c)
				pass = false;
		}
		System.err.println(pass ? "PASS" : "FAIL");
	}

	public static void testSize() {
		System.err.println("ord():  *******************************");
		boolean pass = true;

		String[] strs = { "", "a", "abcdefghijklmnopqrstuvwxyz" };
		for (String s : strs) {
			int len = TigerStdLib.size(s);
			System.out.println("\"" + s + "\":  " + len + " (exp. " + s.length() + ")");
			if (len != s.length())
				pass = false;
		}

		System.err.println(pass ? "PASS" : "FAIL");
	}

	public static void testSubstring() {
		fail("Not yet implemented");
	}

	public static void testConcat() {
		fail("Not yet implemented");
	}

	public static void testNot() {
		fail("Not yet implemented");
	}

	public static void testExit() {
		fail("Not yet implemented");
	}

        private static void fail(String s) {

            System.err.println(s);
        }
}
