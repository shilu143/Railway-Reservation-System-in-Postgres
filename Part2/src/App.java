import java.sql.*;
import java.io.*;
import java.util.*;

public class App {
    private final String url = "jdbc:postgresql://localhost:5432/search";
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

    public static void train_insert(Connection connection) throws SQLException {
        String query = "";
        try {
            File file = new File("./src/trainInput.txt");
            Scanner scan = new Scanner(file);
            while (scan.hasNextLine()) {
                query = scan.nextLine();
                if (query.equals("\\s+")) {
                    continue;
                }
                int trainno = Integer.valueOf(query.replaceAll(" ", ""));
                PreparedStatement pstmt = connection.prepareStatement("CALL insert_train( ? )");
                pstmt.setInt(1, trainno);
                pstmt.execute();
            }
            scan.close();
        } catch (IOException e) {
            System.out.print(e.getLocalizedMessage());
        }
    }

    public static void schedule_insert(Connection connection) throws SQLException {
        String query = "";
        try {
            File file = new File("./src/schedule.txt");
            Scanner scan = new Scanner(file);
            while (scan.hasNextLine()) {
                query = scan.nextLine();
                if (query.trim().isEmpty()) {
                    continue;
                }
                String token[] = query.split("\\s+");
                int n = token.length;
                // System.out.println("NUM " + n);
                String trainno = "T" + token[0];
                String code = token[1];
                String stationName = "";
                for (int i = 0; i < (n - 5); i++) {
                    if (i == 0)
                        stationName = token[2 + i];
                    else
                        stationName = stationName + " " + token[2 + i];
                }
                String arrivalTime = token[n - 3];
                String departureTime = token[n - 2];
                int dayinfo = Integer.valueOf(token[n - 1]);
                query = "insert into " + trainno + " values(?, ?, ?, ?, ?)";
                PreparedStatement pstmt = connection.prepareStatement(query);
                pstmt.setString(1, code);
                pstmt.setString(2, stationName);
                pstmt.setString(3, arrivalTime);
                pstmt.setString(4, departureTime);
                pstmt.setInt(5, dayinfo);
                pstmt.execute();
                pstmt = connection.prepareStatement("call create_station(?)");
                pstmt.setString(1, "S_" + code);
                pstmt.execute();
                query = "INSERT INTO S_" + code + " values(?, ?, ?, ?)";
                pstmt = connection.prepareStatement(query);
                pstmt.setInt(1, Integer.parseInt(trainno.substring(1, trainno.length())));
                pstmt.setString(2, arrivalTime);
                pstmt.setString(3, departureTime);
                pstmt.setInt(4, dayinfo);
                pstmt.execute();
            }
            scan.close();
        } catch (IOException e) {
            System.out.print(e.getLocalizedMessage());
        }
    }

    public static void searchPath(Connection connection, String src_code, String dest_code) {
        try {
            PreparedStatement pstmt = connection.prepareStatement("SELECT search(?, ?)");
            pstmt.setString(1, src_code);
            pstmt.setString(2, dest_code);
            //pstmt.execute();
            ResultSet rs = pstmt.executeQuery();
            // boolean didweenter = false;
            
            while(rs.next()){
                System.out.println("-------------------------------------------------------------------------------------------------------------");
                // System.out.println(rs.getString(1));
                String[] temp = rs.getString(1).split(",");

                for(int i=0; i<temp.length; i++){
                    temp[i] = temp[i].replace("\"","");
                    temp[i] = temp[i].replace("{","");
                    temp[i] = temp[i].replace("}","");
                }

                if(temp.length ==1){
                    System.out.println("No paths available");
                }

                for(int i=0; i<temp.length; i++){
                    // didweenter = true;
                    String[] why = temp[i].split("\\s+");
                    if(why.length==3){
                        System.out.println("Train "+ why[2]+ " departs at "+ why[0]+ " and reaches destination by "+ why[1]);
                        System.out.println("------------------------------------------------------------------------------------------------------------");
                    }

                    else if(why.length==15){
                        System.out.println("Take train "+ why[0]+ " departs at "+ why[2]+
                        "|| VIA " + why[7]+
                        " take train " + why[11] + " departs by " + why[9]+
                         "||  reaches destination by "+ why[12]);
                         System.out.println("-------------------------------------------------------------------------------------------------------------");
                    }

                }        
                
                // if(didweenter = false){
                //     System.out.println("No paths were found.");
                // }

            }
            rs.close();
            pstmt.close();
        } catch (SQLException exception) {
            System.out.print(exception.getMessage());
        }

    }
    //NEED TO ADD NO PATH DELETE AFTER THAT
    public static void main(String[] args) throws SQLException {
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

            // insert trains
            train_insert(connection);

            // schedule insert
            schedule_insert(connection);

            // boolean flag = false;
            // search
            Scanner mscan = new Scanner(System.in);
            while (true) {
                System.out.print("Enter Source and Destination Station names or q(exit) : ");
                
                String inp = mscan.nextLine();
                if (inp.replaceAll(" ", "").equals("q")) {
                    break;
                }
                String[] station = inp.split("\\s+");
                searchPath(connection, station[0], station[1]);
            }
            mscan.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            connection.close();
        }
    }
}