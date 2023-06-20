public class tiglib {

  public static String chr(int i) {
    char c = (char) i;
    return String.valueOf(c);
  }
  
  public static int ord(String s) {
    if (s.length() == 0)
      return -1;
    else
      return s.charAt(0);
  }
  
  public static String concat(String s1, String s2) {
    return s1.concat(s2);
  }
  
  public static String getchar() throws java.io.IOException {
    int i = System.in.read();
    if(i < 0) {
      return new String();
    } else {
      char c = (char) i;
      return String.valueOf(c);
    }
  }
  
  public static int not(int i) {
    if (i == 0) return 1;
    else return 0;
  }
  
  public static String substring(String s, int f, int n) {
    return s.substring(f, f+n);
  }
}
