import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.io.*;
import java.util.*;

public class JDBCPostgreSQLConnection {
    private final String url = "jdbc:postgresql://localhost:5432/rr_sys";
    private final String user = "postgres";
    private final String password = "root";

    public Connection connect() {
        Connection cnct = null;
        try {
            cnct = DriverManager.getConnection(url, user, password);

            if (cnct != null) {
                System.out.println("Connected to the PostgreSQL server successfully.");
            } else {
                System.out.println("Failed to make connection!");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return cnct;
    }

    static void Release_Train(Connection connection, int trainno, LocalDate doj, int ac, int sl) {
        try {
            String tmp = String.valueOf(doj.getMonth());
            tmp = tmp.substring(0, 1) + tmp.substring(tmp.length() - 1);
            String dt = String.valueOf(doj.getYear()) + tmp +
                    String.valueOf(doj.getDayOfMonth());

            String table1Name = "A" + String.valueOf(trainno) + "D" + dt;
            String table2Name = "S" + String.valueOf(trainno) + "D" + dt;

            PreparedStatement pstmt = connection.prepareStatement("CALL fill_table(?, ?, ?, ?, ?, ?)");
            pstmt.setString(1, table1Name);
            pstmt.setString(2, table2Name);
            pstmt.setInt(3, trainno);
            pstmt.setDate(4, Date.valueOf(doj));
            pstmt.setInt(5, ac);
            pstmt.setInt(6, sl);
            pstmt.execute();
            pstmt.close();
            System.out.println("Trained released in the Booking System!");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    static void BookTicket(Connection connection, int n, String[] names, int trainno, LocalDate doj, String cls) {
        String tmp = String.valueOf(doj.getMonth());
        tmp = tmp.substring(0, 1) + tmp.substring(tmp.length() - 1);
        String dt = String.valueOf(doj.getYear()) + tmp +
                String.valueOf(doj.getDayOfMonth());
        String tabname = "";
        if (cls.equals("AC")) {
            tabname = "A" + String.valueOf(trainno) + "D" + dt;
        } else if (cls.equals("SL")) {
            tabname = "S" + String.valueOf(trainno) + "D" + dt;
        } else {
            System.out.println("Give Proper Class choice (AC/SL)");
            return;
        }

        try {
            PreparedStatement pstmt = connection.prepareStatement("CALL book_ticket(?, ?, ?, ?, ?, ?)");
            pstmt.setString(1, tabname);
            pstmt.setInt(2, n);
            Array stringsArray = connection.createArrayOf("varchar", names);
            pstmt.setArray(3, stringsArray);
            pstmt.setInt(4, trainno);
            pstmt.setDate(5, Date.valueOf(doj));
            pstmt.setString(6, cls);
            pstmt.execute();
            System.out.println("Ticket has been booked!");
        } catch (SQLException exception) {
            System.out.println(exception.getMessage());
        }
    }

    public static void main(String[] args) {
        JDBCPostgreSQLConnection app = new JDBCPostgreSQLConnection();
        Connection connection = null;

        try {
            connection = app.connect();
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(2); // READ-COMMITED

            String query = "";
            try {
                File file = new File("./src/rrsys.sql");
                Scanner scan = new Scanner(file);
                while (scan.hasNextLine()) {
                    query = query.concat(scan.nextLine() + " ");
                }
                scan.close();
            } catch (IOException e) {
                System.out.print(e.getLocalizedMessage());
            }

            Statement stmt = connection.createStatement();
            stmt.execute(query);
            stmt.close();

            Release_Train(connection, 12345, LocalDate.parse("2022-11-20"), 1, 1);
            Release_Train(connection, 12345, LocalDate.parse("2022-11-21"), 1, 1);
            connection.commit();

            // Booking ticket
            String[] pss = { "SHILU", "ANITA", "SUMITA" };
            BookTicket(connection, 3, pss, 12345, LocalDate.parse("2022-11-20"), "AC");
            BookTicket(connection, 3, pss, 12345, LocalDate.parse("2022-11-20"), "SL");
            connection.commit();
            connection.close();
        } catch (SQLException e) {

            System.out.println(e.getMessage());
            try {
                connection.rollback();
            } catch (SQLException exception) {
                System.out.println(exception.getMessage());
            }
        }
    }
}