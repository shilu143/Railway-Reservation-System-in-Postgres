import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.io.*;
import java.util.*;

public class App {
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
                System.out.println("Failed to make connection!!!");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return cnct;
    }

    static void Release_Train(Connection connection, int trainno, LocalDate doj, int ac, int sl) {
        try {
            String tmp = String.valueOf(doj);
            String dt = tmp.substring(0, 4) + tmp.substring(5, 7) + tmp.substring(8, 10);

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
            System.out.println("Trained released in the Booking System");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    static void BookTicket(Connection connection, int n, String[] names, int trainno,
            LocalDate doj, String cls) {
        String tmp = String.valueOf(doj);
        String dt = tmp.substring(0, 4) + tmp.substring(5, 7) + tmp.substring(8, 10);

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
            CallableStatement pstmt = connection.prepareCall(" CALL book_ticket(?, ?, ?, ?, ?, ?, ?) ");
            pstmt.setString(1, tabname);
            pstmt.setInt(2, n);
            Array nameArray = connection.createArrayOf("varchar", names);
            pstmt.setArray(3, nameArray);
            pstmt.setInt(4, trainno);
            pstmt.setDate(5, Date.valueOf(doj));
            pstmt.setString(6, cls);
            pstmt.registerOutParameter(7, Types.INTEGER);
            pstmt.execute();
            if (pstmt.getInt(7) == 1) {
                System.out.println("Ticket successfully Booked");
            } else {
                System.out.println("Unable to book Ticket(there is not enough seat available)");
            }

        } catch (SQLException exception) {
            System.out.println(exception.getMessage());
        }
    }

    static void bookingSystemInput(Connection connection) {
        String query = "";
        try {
            File file = new File("./input/Trainschedule.txt");
            Scanner scan = new Scanner(file);
            while (scan.hasNextLine()) {
                query = scan.nextLine();
                if (query.replaceAll("\\s+", "").equals("#")) {
                    break;
                }

                String[] token = query.split("\\s+");
                int trainno = Integer.valueOf(token[0]);
                LocalDate doj = LocalDate.parse(token[1]);
                int ac = Integer.valueOf(token[2]);
                int sl = Integer.valueOf(token[3]);

                Release_Train(connection, trainno, doj, ac, sl);
            }
            scan.close();
        } catch (IOException e) {
            System.out.print(e.getLocalizedMessage());
        }
    }

    static void ticketBookingInput(Connection connection) {
        String query = "";
        try {
            File file = new File("./input/bookings.txt");
            Scanner scan = new Scanner(file);
            while (scan.hasNextLine()) {
                query = scan.nextLine();
                if (query.replaceAll("\\s", "").equals("#")) {
                    scan.close();
                    return;
                }
                query = query.replaceAll(",", "");
                String[] token = query.split("\\s+");
                int n = Integer.valueOf(token[0]);
                String[] names = new String[n];
                for (int i = 0; i < n; i++) {
                    names[i] = token[i + 1];
                }

                int trainno = Integer.valueOf(token[n + 1]);
                LocalDate doj = LocalDate.parse(token[n + 2]);
                String cls = token[n + 3];

                BookTicket(connection, n, names, trainno, doj, cls);
            }
            scan.close();
        } catch (IOException e) {
            System.out.print(e.getLocalizedMessage());
        }
    }

    static void insertTrainInput(Connection connection) throws SQLException {
        String query = "";
        try {
            File file = new File("./input/trains.txt");
            Scanner scan = new Scanner(file);
            while (scan.hasNextLine()) {
                query = scan.nextLine();
                query = query.replaceAll("\\s+", "");
                if (query.equals("#")) {
                    break;
                }
                int trainno = Integer.valueOf(query);
                PreparedStatement pstmt = connection.prepareStatement("CALL Insert_Train(?)");
                pstmt.setInt(1, trainno);
                pstmt.execute();
                pstmt.close();
                System.out.println("Train inserted in Train table");
            }
            scan.close();
        } catch (IOException e) {
            System.out.print(e.getLocalizedMessage());
        }
    }

    public static void main(String[] args) {
        App app = new App();
        Connection connection = null;

        try {
            connection = app.connect();

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

            // Insert Train
            insertTrainInput(connection);

            // Release Train in booking system
            bookingSystemInput(connection);

            // Booking ticket
            ticketBookingInput(connection);

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