package dao;

import java.sql.*;
import model.BookReservation;

public class BookReservationDao {

    /* Database Constants */
    private static final String URL = "jdbc:mysql://localhost:3306/project_2";
    private static final String USER = "root";
    private static final String PASSWORD = "1234"; // <--- CHECK THIS

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // --- HELPER METHODS WITH DEBUGGING ---

    private int[] getCustomerIds(Connection con, String email) throws SQLException {
        System.out.println("[DEBUG] Looking up customer email: " + email);
        String sql = "SELECT AccountNo, Id FROM Customer WHERE Email = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int acct = rs.getInt("AccountNo");
                    int id = rs.getInt("Id");
                    System.out.println("[DEBUG] Found Customer - Account: " + acct + ", PersonID: " + id);
                    return new int[] { acct, id };
                }
            }
        }
        System.out.println("[DEBUG] Customer NOT FOUND for email: " + email);
        return null;
    }

    private int generateResrNo(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(ResrNo) FROM Reservation")) {
            if (rs.next()) {
                int next = rs.getInt(1) + 1;
                System.out.println("[DEBUG] Generated New ResrNo: " + next);
                return next;
            }
        }
        return 1;
    }

    private double getFlightFare(Connection con, String airline, int flightNo, String seatClass) throws SQLException {
        System.out.println("[DEBUG] Calculating Fare for: " + airline + " #" + flightNo + " (" + seatClass + ")");
        
        // Note: Using LOWER() to make the check case-insensitive (Economy == economy)
        String sql = "SELECT FareAmount FROM Fare " +
                     "WHERE AirlineID = ? AND FlightNo = ? " +
                     "AND LOWER(Class) = LOWER(?) AND FareType = 'OneWay'";
        
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, airline);
            ps.setInt(2, flightNo);
            ps.setString(3, seatClass);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double amt = rs.getDouble("FareAmount");
                    System.out.println("[DEBUG] Fare Found: $" + amt);
                    return amt;
                }
            }
        }
        System.out.println("[DEBUG] Fare NOT FOUND (Returned 0.0). Check Fare table for this combo.");
        return 0.0;
    }

    // --- MAIN BOOKING METHOD ---

    public String bookOneWayRoundTripReservation(BookReservation bookRes) {
        System.out.println("=== STARTING BOOKING TRANSACTION ===");
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false); 

            // 1. Validate Customer
            int[] ids = getCustomerIds(con, bookRes.getPassEmail());
            if (ids == null) return "failure: Customer email not found in database.";
            int accountNo = ids[0];
            int personId = ids[1];

            // 2. Calculate Fare & Check Flight Existence
            double leg1Fare = getFlightFare(con, bookRes.getAirlineID(), bookRes.getFlightNum1(), bookRes.getSeatClass());
            
            if (leg1Fare == 0.0) {
                con.rollback();
                System.out.println("[ERROR] Flight 1 Fare is 0.0. Transaction Aborted.");
                return "failure: Flight " + bookRes.getAirlineID() + " #" + bookRes.getFlightNum1() + " with class '" + bookRes.getSeatClass() + "' not found.";
            }

            double leg2Fare = 0.0;
            if (bookRes.getFlightNum2() > 0) {
                 leg2Fare = getFlightFare(con, bookRes.getAirlineID(), bookRes.getFlightNum2(), bookRes.getSeatClass());
                 if (leg2Fare == 0.0) {
                    con.rollback();
                    System.out.println("[ERROR] Flight 2 Fare is 0.0. Transaction Aborted.");
                    return "failure: Return Flight " + bookRes.getAirlineID() + " #" + bookRes.getFlightNum2() + " not found.";
                 }
            }
            
            double totalFare = leg1Fare + leg2Fare;
            double bookingFee = totalFare * 0.10;
            
            System.out.println("[DEBUG] Total Fare: " + totalFare + ", Fee: " + bookingFee);

            // 3. Insert Reservation
            int resrNo = generateResrNo(con);
            String resSql = "INSERT INTO Reservation (ResrNo, ResrDate, BookingFee, TotalFare, RepSSN, AccountNo) VALUES (?, NOW(), ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(resSql)) {
                ps.setInt(1, resrNo);
                ps.setDouble(2, bookingFee);
                ps.setDouble(3, totalFare);
                
                // Safe Integer Parsing for RepSSN
                if (bookRes.getRepSSN() != null && !bookRes.getRepSSN().trim().isEmpty()) {
                    try {
                        ps.setInt(4, Integer.parseInt(bookRes.getRepSSN()));
                    } catch (NumberFormatException nfe) {
                        System.out.println("[WARN] Invalid RepSSN format: " + bookRes.getRepSSN() + ". Setting to NULL.");
                        ps.setNull(4, Types.INTEGER);
                    }
                } else {
                    ps.setNull(4, Types.INTEGER);
                }
                ps.setInt(5, accountNo);
                ps.executeUpdate();
                System.out.println("[DEBUG] Reservation Inserted. ID: " + resrNo);
            }

            // 4. Insert Includes (Outbound)
            String incSql = "INSERT INTO Includes (ResrNo, AirlineID, FlightNo, LegNo, Date) " +
                            "SELECT ?, L.AirlineID, L.FlightNo, L.LegNo, ? FROM Leg L " +
                            "WHERE L.AirlineID = ? AND L.FlightNo = ? ORDER BY L.LegNo";

            try (PreparedStatement ps = con.prepareStatement(incSql)) {
                ps.setInt(1, resrNo);
                ps.setString(2, bookRes.getDepartureDate());
                ps.setString(3, bookRes.getAirlineID());
                ps.setInt(4, bookRes.getFlightNum1());
                int legs = ps.executeUpdate();
                System.out.println("[DEBUG] Outbound Legs Inserted: " + legs);
                
                if (legs == 0) {
                    con.rollback(); 
                    System.out.println("[ERROR] No legs found in Leg table for Flight #" + bookRes.getFlightNum1());
                    return "failure: Flight exists in Fare table but has no Legs defined in Database.";
                }
            }

            // 5. Insert Includes (Return)
            if (bookRes.getFlightNum2() > 0) {
                try (PreparedStatement ps = con.prepareStatement(incSql)) {
                    ps.setInt(1, resrNo);
                    ps.setString(2, bookRes.getReturnDate());
                    ps.setString(3, bookRes.getAirlineID());
                    ps.setInt(4, bookRes.getFlightNum2());
                    int legs = ps.executeUpdate();
                    System.out.println("[DEBUG] Return Legs Inserted: " + legs);
                }
            }

            // 6. Insert Passenger
            String passSql = "INSERT INTO ReservationPassenger (ResrNo, Id, AccountNo, SeatNo, Class, Meal) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(passSql)) {
                ps.setInt(1, resrNo);
                ps.setInt(2, personId);
                ps.setInt(3, accountNo);
                ps.setString(4, bookRes.getSeatNum());
                ps.setString(5, bookRes.getSeatClass());
                ps.setString(6, bookRes.getMealPref());
                ps.executeUpdate();
                System.out.println("[DEBUG] Passenger Inserted.");
            }

            con.commit();
            System.out.println("=== TRANSACTION SUCCESSFUL ===");
            return "success";

        } catch (Exception e) {
            e.printStackTrace(); // LOOK AT ECLIPSE CONSOLE FOR THIS
            if (con != null) try { con.rollback(); } catch (SQLException ex) {}
            return "failure: " + e.getMessage();
        } finally {
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException ex) {}
        }
    }

    public String bookMultiCityReservation(BookReservation bookRes) {
        // ... (Logic is effectively the same as above, just repeated for multiple trips)
        // For debugging purposes, stick to OneWay/RoundTrip testing first using the method above.
        return "failure: Feature not implemented for debugging yet";
    }
}