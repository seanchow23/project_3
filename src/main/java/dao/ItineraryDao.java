package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Itinerary;

public class ItineraryDao {

    /* Database Constants */
    private static final String URL = "jdbc:mysql://localhost:3306/project_2";
    private static final String USER = "root";
    private static final String PASSWORD = "Master442713"; // <--- UPDATE THIS

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    public List<Itinerary> getItineraryForReservation(int resrNo) {
        /*
         * Implements Transaction 3.3.5 (Simplified for Itinerary Model)
         * Fetches all flight legs associated with the given reservation number.
         */
        List<Itinerary> itineraryList = new ArrayList<Itinerary>();
        
        String sql = "SELECT I.ResrNo, I.AirlineID, I.FlightNo, " +
                     "L.DepAirportID, L.ArrAirportID, L.DepTime, L.ArrTime " +
                     "FROM Includes I " +
                     "JOIN Leg L ON I.AirlineID = L.AirlineID " +
                     "          AND I.FlightNo = L.FlightNo " +
                     "          AND I.LegNo = L.LegNo " +
                     "WHERE I.ResrNo = ? " +
                     "ORDER BY I.LegNo";
        
        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {
            
            st.setInt(1, resrNo);
            
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Itinerary it = new Itinerary();
                    
                    it.setResrNo(rs.getInt("ResrNo"));
                    it.setAirlineID(rs.getString("AirlineID"));
                    it.setFlightNo(rs.getInt("FlightNo"));
                    
                    // Map Airport Codes to Departure/Arrival
                    it.setDeparture(rs.getString("DepAirportID"));
                    it.setArrival(rs.getString("ArrAirportID"));
                    
                    // Map Times (SQL DateTime -> String)
                    // You can format this using SimpleDateFormat if needed, 
                    // but getString() usually provides a readable default "YYYY-MM-DD HH:MM:SS"
                    String depTime = rs.getString("DepTime");
                    if (depTime != null && depTime.length() > 2) {
                        it.setDepTime(depTime.substring(0, depTime.length() - 2)); // Trims .0 if present
                    } else {
                        it.setDepTime(depTime);
                    }

                    String arrTime = rs.getString("ArrTime");
                    if (arrTime != null && arrTime.length() > 2) {
                        it.setArrTime(arrTime.substring(0, arrTime.length() - 2));
                    } else {
                        it.setArrTime(arrTime);
                    }
                    
                    itineraryList.add(it);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return itineraryList;
    }
}