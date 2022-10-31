import java.io.*;
import java.time.LocalDate;

public class LETSREAD {
    public static void main(String[] args) throws IOException {
        // Example 1
        String dInStr = "2011-11-01";
        LocalDate d1 = LocalDate.parse(dInStr);
        String ans = String.valueOf(d1.getYear()) + d1.getMonth()
                + String.valueOf(d1.getDayOfMonth());
        System.out.println(ans);
        System.out.println("String to LocalDate : " + d1.getYear() + d1.getMonth() + d1.getDayOfMonth());
        // Example 2
        String dInStr2 = "2011-01-11";
        LocalDate d2 = LocalDate.parse(dInStr2);
        String ans1 = String.valueOf(d2.getYear()) + String.valueOf(d2.getMonthValue())
                + String.valueOf(d2.getDayOfMonth());
        System.out.println(ans1);
        System.out.println("String to LocalDate : " + d2);
    }
}
