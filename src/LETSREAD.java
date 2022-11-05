import java.io.*;
import java.time.LocalDate;

public class LETSREAD {
    public static void main(String[] args) throws IOException {
        // Example 1
        String dInStr = "2011-11-01";
        LocalDate d1 = LocalDate.parse(dInStr);
        String ans = String.valueOf(d1);
        System.out.print(ans.substring(0, 4)+ans.substring(5, 7)+ans.substring(8, 10));
    }
}
