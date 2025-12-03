package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Auctions;

public class AuctionsDao {
    
    /* Database Constants */
    private static final String URL = "jdbc:mysql://localhost:3306/project_2";
    private static final String USER = "root";
    private static final String PASSWORD = "1234"; // <--- UPDATE THIS

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    public List<Auctions> getLatestBid(int AccountNo, String AirlineID, int FlightNo, String SeatClass) {
        /*
         * Fetches the single most recent bid for a specific user and flight.
         * Corresponds to Transaction 3.3.6 logic.
         */
        List<Auctions> auctions = new ArrayList<Auctions>();
        
        // Query to find the latest bid for this account/flight/class
        // Note: We ignore LegNo here because the model/input doesn't support it.
        String sql = "SELECT AccountNo, AirlineID, FlightNo, Class, Date, NYOP, Accepted " +
                     "FROM Auctions " +
                     "WHERE AccountNo = ? " +
                     "  AND AirlineID = ? " +
                     "  AND FlightNo = ? " +
                     "  AND Class = ? " +
                     "ORDER BY Date DESC " +
                     "LIMIT 1";
        
        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {
            
            st.setInt(1, AccountNo);
            st.setString(2, AirlineID);
            st.setInt(3, FlightNo);
            st.setString(4, SeatClass);
            
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    Auctions auction = new Auctions();
                    auction.setAccountNo(rs.getInt("AccountNo"));
                    auction.setAirlineID(rs.getString("AirlineID"));
                    auction.setFlightNo(rs.getInt("FlightNo"));
                    auction.setSeatClass(rs.getString("Class"));
                    
                    // Convert Timestamp to String
                    Timestamp ts = rs.getTimestamp("Date");
                    if (ts != null) {
                        auction.setDate(ts.toString());
                    }
                    
                    auction.setNYOP(rs.getDouble("NYOP"));
                    auction.setAccepted(rs.getBoolean("Accepted"));
                    
                    auctions.add(auction);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return auctions;
    }
    
    public List<Auctions> getAllBids(int AccountNo, String AirlineID, int FlightNo, String SeatClass) {
        /*
         * Fetches the complete bid history for a specific user and flight.
         * Corresponds to Transaction 3.3.7 logic (filtered by user).
         */
        List<Auctions> auctions = new ArrayList<Auctions>();
        
        String sql = "SELECT AccountNo, AirlineID, FlightNo, Class, Date, NYOP, Accepted " +
                     "FROM Auctions " +
                     "WHERE AccountNo = ? " +
                     "  AND AirlineID = ? " +
                     "  AND FlightNo = ? " +
                     "  AND Class = ? " +
                     "ORDER BY Date DESC";
        
        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {
            
            st.setInt(1, AccountNo);
            st.setString(2, AirlineID);
            st.setInt(3, FlightNo);
            st.setString(4, SeatClass);
            
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Auctions auction = new Auctions();
                    auction.setAccountNo(rs.getInt("AccountNo"));
                    auction.setAirlineID(rs.getString("AirlineID"));
                    auction.setFlightNo(rs.getInt("FlightNo"));
                    auction.setSeatClass(rs.getString("Class"));
                    
                    Timestamp ts = rs.getTimestamp("Date");
                    if (ts != null) {
                        auction.setDate(ts.toString());
                    }
                    
                    auction.setNYOP(rs.getDouble("NYOP"));
                    auction.setAccepted(rs.getBoolean("Accepted"));
                    
                    auctions.add(auction);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return auctions;
    }
}