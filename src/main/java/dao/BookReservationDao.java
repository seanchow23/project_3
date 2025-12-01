package dao;

import java.sql.*;
import model.BookReservation;

public class BookReservationDao {

    /* Database Constants */
    private static final String URL = "jdbc:mysql://localhost:3306/project_2";
    private static final String USER = "root";
    private static final String PASSWORD = "your_password"; // <--- UPDATE THIS

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // =========================================================================
    // HELPER METHODS (Required because BookReservation model lacks this data)
    // =========================================================================

    /**
     * Translates the user's Email (from BookReservation) into AccountNo and Person ID (needed for DB tables).
     */
    private int[] getCustomerIds(Connection con, String email) throws SQLException {
        String sql = "SELECT AccountNo, Id FROM Customer WHERE Email = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new int[] { rs.getInt("AccountNo"), rs.getInt("Id") };
            }
        }
        return null; // Customer not found
    }

    /**
     * Calculates the next ID manually since the table is not Auto-Increment.
     */
    private int generateResrNo(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(ResrNo) FROM Reservation")) {
            if (rs.next()) return rs.getInt(1) + 1;
        }
        return 1; // Fallback if table is empty
    }

    /**
     * Look up the official price from the Fare table. 
     * BookReservation object does not hold price data.
     */
    private double getFlightFare(Connection con, String airline, int flightNo, String seatClass) throws SQLException {
        // Try to get OneWay fare
        String sql = "SELECT FareAmount FROM Fare WHERE AirlineID = ? AND FlightNo = ? AND Class = ? AND FareType = 'OneWay'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, airline);
            ps.setInt(2, flightNo);
            ps.setString(3, seatClass);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("FareAmount");
            }
        }
        return 0.0; // Price not found
    }

    // =========================================================================
    // MAIN BOOKING TRANSACTIONS
    // =========================================================================

    public String bookOneWayRoundTripReservation(BookReservation bookRes) {
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false); // Start Transaction

            // 1. Get Customer IDs (Model has Email, DB needs IDs)
            int[] ids = getCustomerIds(con, bookRes.getPassEmail());
            if (ids == null) return "failure: Customer not found";
            int accountNo = ids[0];
            int personId = ids[1];

            // 2. Generate new Reservation ID
            int resrNo = generateResrNo(con);

            // 3. Calculate Financials (Fare lookup + 10% Fee)
            double leg1Fare = getFlightFare(con, bookRes.getAirlineID(), bookRes.getFlightNum1(), bookRes.getSeatClass());
            double leg2Fare = 0.0;
            
            // Check if there is a return flight
            if (bookRes.getFlightNum2() > 0) {
                 leg2Fare = getFlightFare(con, bookRes.getAirlineID(), bookRes.getFlightNum2(), bookRes.getSeatClass());
            }
            
            double totalFare = leg1Fare + leg2Fare;
            double bookingFee = totalFare * 0.10;

            // 4. Insert RESERVATION
            String resSql = "INSERT INTO Reservation (ResrNo, ResrDate, BookingFee, TotalFare, RepSSN, AccountNo) VALUES (?, NOW(), ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(resSql)) {
                ps.setInt(1, resrNo);
                ps.setDouble(2, bookingFee);
                ps.setDouble(3, totalFare);
                
                // Handle optional RepSSN
                if (bookRes.getRepSSN() != null && !bookRes.getRepSSN().isEmpty()) {
                    ps.setInt(4, Integer.parseInt(bookRes.getRepSSN()));
                } else {
                    ps.setNull(4, Types.INTEGER);
                }
                ps.setInt(5, accountNo);
                ps.executeUpdate();
            }

            // 5. Insert INCLUDES (Links Flight Legs to Reservation)
            String incSql = "INSERT INTO Includes (ResrNo, AirlineID, FlightNo, LegNo, Date) " +
                            "SELECT ?, L.AirlineID, L.FlightNo, L.LegNo, ? FROM Leg L " +
                            "WHERE L.AirlineID = ? AND L.FlightNo = ? ORDER BY L.LegNo";

            // Trip 1 (Outbound)
            try (PreparedStatement ps = con.prepareStatement(incSql)) {
                ps.setInt(1, resrNo);
                ps.setString(2, bookRes.getDepartureDate());
                ps.setString(3, bookRes.getAirlineID());
                ps.setInt(4, bookRes.getFlightNum1());
                ps.executeUpdate();
            }

            // Trip 2 (Return - only if FlightNum2 is set)
            if (bookRes.getFlightNum2() > 0) {
                try (PreparedStatement ps = con.prepareStatement(incSql)) {
                    ps.setInt(1, resrNo);
                    ps.setString(2, bookRes.getReturnDate());
                    ps.setString(3, bookRes.getAirlineID());
                    ps.setInt(4, bookRes.getFlightNum2());
                    ps.executeUpdate();
                }
            }

            // 6. Insert PASSENGER (Links Person to Reservation)
            String passSql = "INSERT INTO ReservationPassenger (ResrNo, Id, AccountNo, SeatNo, Class, Meal) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(passSql)) {
                ps.setInt(1, resrNo);
                ps.setInt(2, personId);
                ps.setInt(3, accountNo);
                ps.setString(4, bookRes.getSeatNum());
                ps.setString(5, bookRes.getSeatClass());
                ps.setString(6, bookRes.getMealPref());
                ps.executeUpdate();
            }

            con.commit();
            return "success";

        } catch (Exception e) {
            e.printStackTrace();
            if (con != null) try { con.rollback(); } catch (SQLException ex) {}
            return "failure";
        } finally {
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException ex) {}
        }
    }

    public String bookMultiCityReservation(BookReservation bookRes) {
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false); // Start Transaction

            // 1. Get Customer IDs
            int[] ids = getCustomerIds(con, bookRes.getPassEmail());
            if (ids == null) return "failure: Customer not found";
            int accountNo = ids[0];
            int personId = ids[1];

            // 2. Generate ID
            int resrNo = generateResrNo(con);

            // 3. Calculate Fare (Sum of Trip 1 and Trip 2)
            double fare1 = getFlightFare(con, bookRes.getAirlineID(), bookRes.getFlightNum1(), bookRes.getSeatClass());
            double fare2 = 0.0;
            if (bookRes.getFlightNum2() > 0) {
                fare2 = getFlightFare(con, bookRes.getAirlineID(), bookRes.getFlightNum2(), bookRes.getSeatClass());
            }
            
            double totalFare = fare1 + fare2;
            double bookingFee = totalFare * 0.10;

            // 4. Insert RESERVATION
            String resSql = "INSERT INTO Reservation (ResrNo, ResrDate, BookingFee, TotalFare, RepSSN, AccountNo) VALUES (?, NOW(), ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(resSql)) {
                ps.setInt(1, resrNo);
                ps.setDouble(2, bookingFee);
                ps.setDouble(3, totalFare);
                if (bookRes.getRepSSN() != null && !bookRes.getRepSSN().isEmpty()) {
                    ps.setInt(4, Integer.parseInt(bookRes.getRepSSN()));
                } else {
                    ps.setNull(4, Types.INTEGER);
                }
                ps.setInt(5, accountNo);
                ps.executeUpdate();
            }

            // 5. Insert INCLUDES (For Trip 1 and Trip 2)
            String incSql = "INSERT INTO Includes (ResrNo, AirlineID, FlightNo, LegNo, Date) " +
                            "SELECT ?, L.AirlineID, L.FlightNo, L.LegNo, ? FROM Leg L " +
                            "WHERE L.AirlineID = ? AND L.FlightNo = ? ORDER BY L.LegNo";

            // Trip 1
            try (PreparedStatement ps = con.prepareStatement(incSql)) {
                ps.setInt(1, resrNo);
                ps.setString(2, bookRes.getTrip1Date());
                ps.setString(3, bookRes.getAirlineID());
                ps.setInt(4, bookRes.getFlightNum1());
                ps.executeUpdate();
            }

            // Trip 2
            if (bookRes.getFlightNum2() > 0) {
                try (PreparedStatement ps = con.prepareStatement(incSql)) {
                    ps.setInt(1, resrNo);
                    ps.setString(2, bookRes.getTrip2Date());
                    ps.setString(3, bookRes.getAirlineID()); 
                    ps.setInt(4, bookRes.getFlightNum2());
                    ps.executeUpdate();
                }
            }

            // 6. Insert PASSENGER
            String passSql = "INSERT INTO ReservationPassenger (ResrNo, Id, AccountNo, SeatNo, Class, Meal) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(passSql)) {
                ps.setInt(1, resrNo);
                ps.setInt(2, personId); // Person ID
                ps.setInt(3, accountNo); // Account ID
                ps.setString(4, bookRes.getSeatNum());
                ps.setString(5, bookRes.getSeatClass());
                ps.setString(6, bookRes.getMealPref());
                ps.executeUpdate();
            }

            con.commit();
            return "success";

        } catch (Exception e) {
            e.printStackTrace();
            if (con != null) try { con.rollback(); } catch (SQLException ex) {}
            return "failure";
        } finally {
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException ex) {}
        }
    }
}