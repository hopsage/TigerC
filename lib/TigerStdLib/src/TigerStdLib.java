
public class TigerStdLib {
	public static void print(String s) {
		System.out.print(s);
	}

	public static void printi(int i) {
		System.out.print(i);
	}

	public static void flush() {
		System.out.flush();
	}

	public static java.lang.String getchar() {
		try {
			char c = (char) System.in.read();
			return "" + c;
		} catch (java.io.IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			throw new Error();
		}
	}

	public static int ord(String c) {
		if (c.length() < 1) {
			return 0;
		} else {
			return c.charAt(0);
		}
	}

	public static java.lang.String chr(int x) {
		String res = "" + (char) x;
		return res;
	}

	public static int size(String s) {
		return s.length();
	}

	public static java.lang.String substring(String s, int i, int j) {
		return s.substring(i,j);
	}

	public static java.lang.String concat(String r, String s) {
		return r.concat(s);
	}

	public static int not(int b) {
		return (b==0?1:0);
	}

	public static void exit(int code) {
		System.exit(code);
	}
}
