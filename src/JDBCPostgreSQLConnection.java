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
        System.out.println("You were here!");
        try {
            String dt = String.valueOf(doj.getYear()) + doj.getMonth()
                    + String.valueOf(doj.getDayOfMonth());

            String table1Name = "AC_" + String.valueOf(trainno) + "_" + dt;
            String table2Name = "SL_" + String.valueOf(trainno) + "_" + dt;

            // PreparedStatement pstmt = connection.prepareStatement("CALL
            // table_create(?)");
            // pstmt.setString(1, table1Name);
            // pstmt.execute();
            // pstmt.close();
            // Statement stmt = connection.createStatement();
            // stmt.addBatch(String.format("call table_create('%s');", table1Name));
            // stmt.addBatch(String.format("call table_create('%s');", table2Name));
            // stmt.executeBatch();
            // stmt.close();
            // CallableStatement upperFunc = connection.prepareCall("{? = call table_create(
            // ? ) }");
            // upperFunc.registerOutParameter(1, Types.VARCHAR);
            // upperFunc.setString(2, "lowercase to uppercase");
            // upperFunc.execute();
            // String upperCased = upperFunc.getString(1);
            // upperFunc.close();

            PreparedStatement pstmt = connection.prepareStatement("CALL fill_table(?, ?, ?, ?, ?, ?)");
            pstmt.setString(1, table1Name);
            pstmt.setString(2, table2Name);
            pstmt.setInt(3, trainno);
            pstmt.setDate(4, Date.valueOf(doj));
            pstmt.setInt(5, ac);
            pstmt.setInt(6, sl);
            pstmt.execute();
            pstmt.close();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        JDBCPostgreSQLConnection app = new JDBCPostgreSQLConnection();
        Connection connection = null;

        try {
            connection = app.connect();
            connection.setAutoCommit(false);
            connection.beginRequest();
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

            Release_Train(connection, 12345, LocalDate.parse("2022-11-20"), 2, 3);
            Release_Train(connection, 12345, LocalDate.parse("2022-11-21"), 10, 30);

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