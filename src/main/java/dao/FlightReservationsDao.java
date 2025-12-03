package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.FlightReservations;

public class FlightReservationsDao {
    
    /* Database Constants - UPDATE PASSWORD HERE */
    private static final String URL = "jdbc:mysql://localhost:3306/project_2";
    private static final String USER = "root";
    private static final String PASSWORD = "1234"; // <--- UPDATE THIS

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    public List<FlightReservations> getReservations(int FlightNum, String airlineID, String CustomerName) {
        /*
         * Implements Transaction 3.1.6a (By Flight) and 3.1.6b (By Customer Name)
         */
        List<FlightReservations> reservations = new ArrayList<>();
        String sql = "";
        boolean isByCustomer = (CustomerName != null && !CustomerName.isEmpty());

        if (isByCustomer) {
            // Transaction 3.1.6b: List Reservations by Customer Name
            sql = "SELECT R.ResrNo, R.ResrDate, R.TotalFare, R.BookingFee, P.FirstName, P.LastName, C.AccountNo " +
                  "FROM Reservation R " +
                  "JOIN Customer C ON R.AccountNo = C.AccountNo " +
                  "JOIN Person P ON C.Id = P.Id " +
                  "WHERE P.LastName = ? OR P.FirstName = ? " +
                  "ORDER BY R.ResrDate DESC";
        } else {
            // Transaction 3.1.6a: List Reservations by Flight Number
            sql = "SELECT DISTINCT R.ResrNo, R.ResrDate, R.TotalFare, R.BookingFee, P.FirstName, P.LastName, R.AccountNo " +
                  "FROM Reservation R " +
                  "JOIN Includes I ON R.ResrNo = I.ResrNo " +
                  "JOIN Customer C ON R.AccountNo = C.AccountNo " +
                  "JOIN Person P ON C.Id = P.Id " +
                  "WHERE I.AirlineID = ? AND I.FlightNo = ? " +
                  "ORDER BY R.ResrNo";
        }

        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {

            if (isByCustomer) {
                st.setString(1, CustomerName);
                st.setString(2, CustomerName); // Check both first and last name
            } else {
                st.setString(1, airlineID);
                st.setInt(2, FlightNum);
            }

            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    FlightReservations res = new FlightReservations();
                    res.setResrNo(rs.getInt("ResrNo"));
                    res.setResrDate(rs.getString("ResrDate"));
                    res.setTotalFare(rs.getDouble("TotalFare"));
                    res.setBookingFee(rs.getDouble("BookingFee"));
                    res.setFirstName(rs.getString("FirstName"));
                    res.setLastName(rs.getString("LastName"));
                    res.setAccountNo(rs.getInt("AccountNo"));
                    reservations.add(res);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return reservations;
    }
    
    public List<FlightReservations> getRevenueSummary(int FlightNum, String airlineID, String CustomerName, String destCity) {
        /*
         * Implements Transactions 3.1.7a (Flight), 3.1.7b (City), 3.1.7c (Customer)
         */
        List<FlightReservations> revenueList = new ArrayList<>();
        String sql = "";
        int queryType = 0; // 1=Flight, 2=Customer, 3=City

        if (FlightNum > 0 && airlineID != null && !airlineID.isEmpty()) {
            // 3.1.7a: Revenue by Flight
            sql = "SELECT SUM(R.TotalFare) AS TotalRevenue " +
                  "FROM Includes I " +
                  "JOIN Reservation R ON I.ResrNo = R.ResrNo " +
                  "WHERE I.AirlineID = ? AND I.FlightNo = ?";
            queryType = 1;
        } else if (CustomerName != null && !CustomerName.isEmpty()) {
            // 3.1.7c: Revenue by Customer (Approximate using Name match)
            sql = "SELECT SUM(R.TotalFare) AS TotalRevenue " +
                  "FROM Reservation R " +
                  "JOIN Customer C ON R.AccountNo = C.AccountNo " +
                  "JOIN Person P ON C.Id = P.Id " +
                  "WHERE P.LastName = ? OR P.FirstName = ?";
            queryType = 2;
        } else if (destCity != null && !destCity.isEmpty()) {
            // 3.1.7b: Revenue by Destination City
            sql = "SELECT SUM(R.TotalFare) AS TotalRevenue " +
                  "FROM Reservation R " +
                  "JOIN Includes I ON R.ResrNo = I.ResrNo " +
                  "JOIN Leg L ON I.AirlineID = L.AirlineID AND I.FlightNo = L.FlightNo AND I.LegNo = L.LegNo " +
                  "JOIN Airport AP ON L.ArrAirportID = AP.Id " +
                  "WHERE AP.City = ?";
            queryType = 3;
        }

        if (sql.isEmpty()) return revenueList;

        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {

            if (queryType == 1) {
                st.setString(1, airlineID);
                st.setInt(2, FlightNum);
            } else if (queryType == 2) {
                st.setString(1, CustomerName);
                st.setString(2, CustomerName);
            } else if (queryType == 3) {
                st.setString(1, destCity);
            }

            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    FlightReservations res = new FlightReservations();
                    res.setResrNo(1); // Placeholder
                    double rev = rs.getDouble("TotalRevenue");
                    res.setRevenue(rev);
                    revenueList.add(res);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return revenueList;
    }
    
    public List<FlightReservations> getPassengerList(int FlightNum, String AirlineID) {
        /*
         * Implements Transaction 3.1.11: List of customers on a flight
         */
        List<FlightReservations> passengers = new ArrayList<>();
        
        String sql = "SELECT DISTINCT P.Id AS PassengerID, P.FirstName, P.LastName " +
                     "FROM Reservation R " +
                     "JOIN Includes I ON R.ResrNo = I.ResrNo " +
                     "JOIN ReservationPassenger RP ON R.ResrNo = RP.ResrNo " +
                     "JOIN Passenger Pass ON RP.Id = Pass.Id AND RP.AccountNo = Pass.AccountNo " +
                     "JOIN Person P ON Pass.Id = P.Id " +
                     "WHERE I.AirlineID = ? AND I.FlightNo = ?";
        
        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {
            
            st.setString(1, AirlineID);
            st.setInt(2, FlightNum);
            
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    FlightReservations res = new FlightReservations();
                    res.setPassengerID(rs.getInt("PassengerID"));
                    res.setFirstName(rs.getString("FirstName"));
                    res.setLastName(rs.getString("LastName"));
                    passengers.add(res);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return passengers;
    }
    
    public List<FlightReservations> getCurrentReservations(int accountNo) {
        /*
         * Implements Transaction 3.3.4: View Customer's Current (Future) Reservations
         */
        List<FlightReservations> reservations = new ArrayList<>();
        
        String sql = "SELECT R.ResrNo, R.ResrDate, R.TotalFare, R.BookingFee, R.RepSSN, R.AccountNo " +
                     "FROM Reservation R " +
                     "JOIN Includes I ON R.ResrNo = I.ResrNo " +
                     "WHERE R.AccountNo = ? " +
                     "GROUP BY R.ResrNo, R.ResrDate, R.TotalFare, R.BookingFee, R.RepSSN, R.AccountNo " +
                     "HAVING MIN(I.Date) >= CURDATE() " +
                     "ORDER BY MIN(I.Date)";

        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {
            
            st.setInt(1, accountNo);
            
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    FlightReservations res = new FlightReservations();
                    res.setResrNo(rs.getInt("ResrNo"));
                    res.setResrDate(rs.getString("ResrDate"));
                    res.setTotalFare(rs.getDouble("TotalFare"));
                    res.setBookingFee(rs.getDouble("BookingFee"));
                    res.setRepSSN(rs.getString("RepSSN"));
                    res.setAccountNo(rs.getInt("AccountNo"));
                    reservations.add(res);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return reservations;
    }

    public List<FlightReservations> getAllReservations(int accountNo) {
        /*
         * Implements Transaction 3.3.9: View Reservation History
         */
        List<FlightReservations> reservations = new ArrayList<>();
        
        String sql = "SELECT ResrNo, ResrDate, TotalFare, BookingFee, RepSSN, AccountNo " +
                     "FROM Reservation " +
                     "WHERE AccountNo = ? " +
                     "ORDER BY ResrDate DESC";
        
        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {
            
            st.setInt(1, accountNo);
            
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    FlightReservations res = new FlightReservations();
                    res.setResrNo(rs.getInt("ResrNo"));
                    res.setResrDate(rs.getString("ResrDate"));
                    res.setTotalFare(rs.getDouble("TotalFare"));
                    res.setBookingFee(rs.getDouble("BookingFee"));
                    res.setRepSSN(rs.getString("RepSSN"));
                    res.setAccountNo(rs.getInt("AccountNo"));
                    reservations.add(res);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return reservations;
    }
    
    public String cancelReservation(int resrNo) {
        /*
         * Implements Transaction 3.3.3: Cancel Reservation
         * Must delete children (ReservationPassenger, Includes) before parent (Reservation).
         */
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false); // Start Transaction

            // 1. Delete Passengers
            String delResPass = "DELETE FROM ReservationPassenger WHERE ResrNo = ?";
            try (PreparedStatement ps1 = con.prepareStatement(delResPass)) {
                ps1.setInt(1, resrNo);
                ps1.executeUpdate();
            }

            // 2. Delete Included Legs
            String delIncludes = "DELETE FROM Includes WHERE ResrNo = ?";
            try (PreparedStatement ps2 = con.prepareStatement(delIncludes)) {
                ps2.setInt(1, resrNo);
                ps2.executeUpdate();
            }

            // 3. Delete Reservation
            String delRes = "DELETE FROM Reservation WHERE ResrNo = ?";
            try (PreparedStatement ps3 = con.prepareStatement(delRes)) {
                ps3.setInt(1, resrNo);
                int rows = ps3.executeUpdate();
                if (rows == 0) {
                    con.rollback();
                    return "failure: Reservation not found";
                }
            }

            con.commit();
            return "success";
            
        } catch (Exception e) {
            e.printStackTrace();
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return "failure";
        } finally {
            if (con != null) {
                try { con.setAutoCommit(true); con.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
} 