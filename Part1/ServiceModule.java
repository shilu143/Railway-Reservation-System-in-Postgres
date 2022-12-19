import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.io.*;
import java.util.*;

class QueryRunner implements Runnable {
    // Declare socket for client access
    protected Socket socketConnection;

    public QueryRunner(Socket clientSocket) {
        this.socketConnection = clientSocket;
    }

    public void run() {
        try {
            // Reading data from client
            InputStreamReader inputStream = new InputStreamReader(socketConnection
                    .getInputStream());
            BufferedReader bufferedInput = new BufferedReader(inputStream);
            OutputStreamWriter outputStream = new OutputStreamWriter(socketConnection
                    .getOutputStream());
            BufferedWriter bufferedOutput = new BufferedWriter(outputStream);
            PrintWriter printWriter = new PrintWriter(bufferedOutput, true);
            String clientCommand = "";
            String responseQuery = "";
            // Read client query from the socket endpoint
            clientCommand = bufferedInput.readLine();
            while (!clientCommand.replaceAll(" ", "").equals("#")) {
                if (clientCommand.trim().isEmpty()) {
                    continue;
                }
                // System.out.println("Recieved data <" + clientCommand + "> from client : "
                // + socketConnection.getRemoteSocketAddress().toString());

                responseQuery = "******* EMPTY ******";

                /*******************************************
                 * Your DB code goes here
                 */
                // ServiceModule App = new ServiceModule();
                try {
                    responseQuery = ServiceModule.ticketBookingInput(clientCommand);
                } catch (SQLException exception) {
                    exception.getMessage();
                }
                /********************************************/

                // Dummy response send to client

                // Sending data back to the client
                printWriter.println(responseQuery);
                // Read next client query
                clientCommand = bufferedInput.readLine();
            }
            inputStream.close();
            bufferedInput.close();
            outputStream.close();
            bufferedOutput.close();
            printWriter.close();
            socketConnection.close();
        } catch (IOException e) {
            return;
        }
    }
}

/**
 * Main Class to controll the program flow
 */
public class ServiceModule {
    private final String url = "jdbc:postgresql://localhost:5432/rr_sys";
    private final String user = "postgres";
    private final String password = "12345";

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

    static void Release_Train(Connection connection, String trainno, LocalDate doj, int ac, int sl) {
        try {
            String tmp = String.valueOf(doj);
            String dt = tmp.substring(0, 4) + tmp.substring(5, 7) + tmp.substring(8, 10);

            String table1Name = "A" + trainno + "D" + dt;
            String table2Name = "S" + trainno + "D" + dt;

            PreparedStatement pstmt = connection.prepareStatement("CALL fill_table(?, ?, ?, ?, ?, ?)");
            pstmt.setString(1, table1Name);
            pstmt.setString(2, table2Name);
            pstmt.setString(3, trainno);
            pstmt.setDate(4, Date.valueOf(doj));
            pstmt.setInt(5, ac);
            pstmt.setInt(6, sl);
            pstmt.execute();
            pstmt.close();
            System.out.println("Train released in the Booking System");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    static String BookTicket(int n, String[] names, String trainno,
            LocalDate doj, String cls) throws SQLException {
        String tmp = String.valueOf(doj);
        String dt = tmp.substring(0, 4) + tmp.substring(5, 7) + tmp.substring(8, 10);

        String tabname = "";
        if (cls.equals("AC")) {
            tabname = "A" + trainno + "D" + dt;
        } else if (cls.equals("SL")) {
            tabname = "S" + trainno + "D" + dt;
        } else {
            System.out.println("Give Proper Class choice (AC/SL)");
            return " ";
        }
        ServiceModule App = new ServiceModule();
        Connection connection = App.connect();
        //
        // connection.setTransactionIsolation(8); // serializable
        // connection.setAutoCommit(false);
        //

        try {
            CallableStatement pstmt = connection.prepareCall(" CALL book_ticket(?, ?, ?, ?, ?, ?, ?) ");
            pstmt.setString(1, tabname);
            pstmt.setInt(2, n);
            Array nameArray = connection.createArrayOf("varchar", names);
            pstmt.setArray(3, nameArray);
            pstmt.setString(4, trainno);
            pstmt.setDate(5, Date.valueOf(doj));
            pstmt.setString(6, cls);
            pstmt.setInt(7,0);
            pstmt.registerOutParameter(7, Types.INTEGER);
            pstmt.execute();
            //
            // connection.commit();
            connection.close();
            //
            if (pstmt.getInt(7) == 1) {
                return "Ticket successfully Booked";
            } else if (pstmt.getInt(7) == 0) {
                return "Unable to book Ticket(there is not enough seat available)";
            }
        } catch (SQLException exception) {
            String state = exception.getMessage();
            connection.close();
            System.out.println(state);
            // return state;
            // exception.getLocalizedMessage();
            // if (state.equals("40001")) {
            // return BookTicket(n, names, trainno, doj, cls);
            // }
        }

        return "ERROR";
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
                } else if (query.replaceAll(" ", "").equals("")) {
                    continue;
                }

                String[] token = query.split("\\s+");
                String trainno = token[0];
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

    static String ticketBookingInput(String query) throws SQLException {

        query = query.replaceAll(",", "");
        String[] token = query.split("\\s+");
        int n = Integer.valueOf(token[0]);
        String[] names = new String[n];
        for (int i = 0; i < n; i++) {
            names[i] = token[i + 1];
        }

        String trainno = token[n + 1];
        LocalDate doj = LocalDate.parse(token[n + 2]);
        String cls = token[n + 3];

        return BookTicket(n, names, trainno, doj, cls);

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
                } else if (query.replaceAll(" ", "").equals("")) {
                    continue;
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

    public void generateSchema(Connection connection) {
        try {
            String query = "";
            try {
                File file = new File("./SQL/rrsys.sql");
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
            // insertTrainInput(connection);

            // Release Train in booking system
            bookingSystemInput(connection);

            // Booking ticket
            // ticketBookingInput(connection);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // Server listens to port
    static int serverPort = 7008;
    // Max no of parallel requests the server can process
    static int numServerCores = 40;

    // ------------ Main----------------------
    public static void main(String[] args) throws IOException {
        // Creating a thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(numServerCores);

        try (// Creating a server socket to listen for clients
                ServerSocket serverSocket = new ServerSocket(serverPort)) {
            Socket socketConnection = null;

            ServiceModule App = new ServiceModule();
            Connection connection = App.connect();
            App.generateSchema(connection);

            // Always-ON server
            while (true) {
                System.out.println("Listening port : " + serverPort
                        + "\nWaiting for clients...");
                socketConnection = serverSocket.accept(); // Accept a connection from a client
                System.out.println("Accepted client :"
                        + socketConnection.getRemoteSocketAddress().toString()
                        + "\n");
                // Create a runnable task
                Runnable runnableTask = new QueryRunner(socketConnection);
                // Submit task for execution
                executorService.submit(runnableTask);
            }
        }
    }
}
