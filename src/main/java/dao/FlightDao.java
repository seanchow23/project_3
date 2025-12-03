package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Flight;

public class FlightDao {
    
    /* Database Constants */
    private static final String URL = "jdbc:mysql://localhost:3306/project_2";
    private static final String USER = "root";
    private static final String PASSWORD = "1234"; // <--- UPDATE THIS

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    /**
     * Transaction 3.1.3: Get all flights (Manager)
     */
    public List<Flight> getAllFlights() {
        List<Flight> flights = new ArrayList<>();
        
        String sql = "SELECT AirlineID, FlightNo, NoOfSeats, DaysOperating, " +
                     "MinLengthOfStay, MaxLengthOfStay " +
                     "FROM Flight " +
                     "ORDER BY AirlineID, FlightNo";
        
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Flight flight = new Flight();
                flight.setAirlineID(rs.getString("AirlineID"));
                flight.setFlightNo(rs.getInt("FlightNo"));
                flight.setNumOfSeats(rs.getInt("NoOfSeats"));
                flight.setDaysOperating(rs.getString("DaysOperating"));
                flight.setMinLengthOfStay(rs.getInt("MinLengthOfStay"));
                flight.setMaxLengthOfStay(rs.getInt("MaxLengthOfStay"));
                flights.add(flight);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flights;
    }
    
    /**
     * Transaction 3.1.9: Most active flights (Manager)
     */
    public List<Flight> mostActiveFlights() {
        List<Flight> flights = new ArrayList<>();
        
        String sql = "SELECT F.AirlineID, F.FlightNo, F.NoOfSeats, F.DaysOperating, " +
                     "F.MinLengthOfStay, F.MaxLengthOfStay, " +
                     "COUNT(DISTINCT I.ResrNo) AS NumReservations " +
                     "FROM Flight F " +
                     "LEFT JOIN Includes I ON F.AirlineID = I.AirlineID AND F.FlightNo = I.FlightNo " +
                     "GROUP BY F.AirlineID, F.FlightNo, F.NoOfSeats, F.DaysOperating, " +
                     "F.MinLengthOfStay, F.MaxLengthOfStay " +
                     "ORDER BY NumReservations DESC " +
                     "LIMIT 10";
        
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Flight flight = new Flight();
                flight.setAirlineID(rs.getString("AirlineID"));
                flight.setFlightNo(rs.getInt("FlightNo"));
                flight.setNumOfSeats(rs.getInt("NoOfSeats"));
                flight.setDaysOperating(rs.getString("DaysOperating"));
                flight.setMinLengthOfStay(rs.getInt("MinLengthOfStay"));
                flight.setMaxLengthOfStay(rs.getInt("MaxLengthOfStay"));
                flight.setNumReservations(rs.getInt("NumReservations"));
                flights.add(flight);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flights;
    }
    
    /**
     * Transaction 3.1.10: Flights for a given airport (Manager)
     */
    public List<Flight> getFlightsForAirport(String airport) {
        List<Flight> flights = new ArrayList<>();
        
        String sql = "SELECT DISTINCT F.AirlineID, F.FlightNo, F.NoOfSeats, F.DaysOperating, " +
                     "F.MinLengthOfStay, F.MaxLengthOfStay " +
                     "FROM Flight F " +
                     "JOIN Leg L ON F.AirlineID = L.AirlineID AND F.FlightNo = L.FlightNo " +
                     "WHERE L.DepAirportID = ? OR L.ArrAirportID = ? " +
                     "ORDER BY F.AirlineID, F.FlightNo";
        
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, airport);
            ps.setString(2, airport);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Flight flight = new Flight();
                    flight.setAirlineID(rs.getString("AirlineID"));
                    flight.setFlightNo(rs.getInt("FlightNo"));
                    flight.setNumOfSeats(rs.getInt("NoOfSeats"));
                    flight.setDaysOperating(rs.getString("DaysOperating"));
                    flight.setMinLengthOfStay(rs.getInt("MinLengthOfStay"));
                    flight.setMaxLengthOfStay(rs.getInt("MaxLengthOfStay"));
                    flights.add(flight);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flights;
    }
    
    /**
     * Transaction 3.1.12: Get on-time flights
     * Note: This requires a comparison between scheduled and actual times
     * For now, returning all flights (implement logic based on your schema)
     */
    public List<Flight> getOnTimeFlights() {
        List<Flight> flights = new ArrayList<>();
        
        // TODO: Implement logic to determine on-time flights
        // This would require comparing Leg.DepTime with actual departure times
        // For now, returning all flights as placeholder
        
        String sql = "SELECT AirlineID, FlightNo, NoOfSeats, DaysOperating, " +
                     "MinLengthOfStay, MaxLengthOfStay " +
                     "FROM Flight " +
                     "ORDER BY AirlineID, FlightNo";
        
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Flight flight = new Flight();
                flight.setAirlineID(rs.getString("AirlineID"));
                flight.setFlightNo(rs.getInt("FlightNo"));
                flight.setNumOfSeats(rs.getInt("NoOfSeats"));
                flight.setDaysOperating(rs.getString("DaysOperating"));
                flight.setMinLengthOfStay(rs.getInt("MinLengthOfStay"));
                flight.setMaxLengthOfStay(rs.getInt("MaxLengthOfStay"));
                flights.add(flight);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flights;
    }
    
    /**
     * Transaction 3.1.12: Get delayed flights
     * Note: This requires a comparison between scheduled and actual times
     * For now, returning empty list (implement logic based on your schema)
     */
    public List<Flight> getDelayedFlights() {
        List<Flight> flights = new ArrayList<>();
        
        // TODO: Implement logic to determine delayed flights
        // This would require comparing Leg.DepTime with actual departure times
        // Your schema may not have actual departure time data
        
        String sql = "SELECT AirlineID, FlightNo, NoOfSeats, DaysOperating, " +
                     "MinLengthOfStay, MaxLengthOfStay " +
                     "FROM Flight " +
                     "WHERE 1=0"; // Returns empty for now
        
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Flight flight = new Flight();
                flight.setAirlineID(rs.getString("AirlineID"));
                flight.setFlightNo(rs.getInt("FlightNo"));
                flight.setNumOfSeats(rs.getInt("NoOfSeats"));
                flight.setDaysOperating(rs.getString("DaysOperating"));
                flight.setMinLengthOfStay(rs.getInt("MinLengthOfStay"));
                flight.setMaxLengthOfStay(rs.getInt("MaxLengthOfStay"));
                flights.add(flight);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flights;
    }
    
    /**
     * Transaction 3.2.4: Personalized flight suggestions based on customer's past reservations
     */
    public List<Flight> getCustomerFlightSuggestions(int accountNo) {
        List<Flight> flights = new ArrayList<>();
        
        // Get flights to airports the customer has previously visited
        String sql = "SELECT DISTINCT F.AirlineID, F.FlightNo, F.NoOfSeats, F.DaysOperating, " +
                     "F.MinLengthOfStay, F.MaxLengthOfStay, " +
                     "COUNT(DISTINCT I2.ResrNo) AS NumReservations " +
                     "FROM Flight F " +
                     "JOIN Leg L ON F.AirlineID = L.AirlineID AND F.FlightNo = L.FlightNo " +
                     "LEFT JOIN Includes I2 ON F.AirlineID = I2.AirlineID AND F.FlightNo = I2.FlightNo " +
                     "WHERE L.ArrAirportID IN ( " +
                     "    SELECT DISTINCT L2.ArrAirportID " +
                     "    FROM Reservation R " +
                     "    JOIN Includes I ON R.ResrNo = I.ResrNo " +
                     "    JOIN Leg L2 ON I.AirlineID = L2.AirlineID AND I.FlightNo = L2.FlightNo " +
                     "    WHERE R.AccountNo = ? " +
                     ") " +
                     "AND F.FlightNo NOT IN ( " +
                     "    SELECT I3.FlightNo FROM Includes I3 " +
                     "    JOIN Reservation R2 ON I3.ResrNo = R2.ResrNo " +
                     "    WHERE R2.AccountNo = ? " +
                     ") " +
                     "GROUP BY F.AirlineID, F.FlightNo, F.NoOfSeats, F.DaysOperating, " +
                     "F.MinLengthOfStay, F.MaxLengthOfStay " +
                     "ORDER BY NumReservations DESC " +
                     "LIMIT 10";
        
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setInt(1, accountNo);
            ps.setInt(2, accountNo);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Flight flight = new Flight();
                    flight.setAirlineID(rs.getString("AirlineID"));
                    flight.setFlightNo(rs.getInt("FlightNo"));
                    flight.setNumOfSeats(rs.getInt("NoOfSeats"));
                    flight.setDaysOperating(rs.getString("DaysOperating"));
                    flight.setMinLengthOfStay(rs.getInt("MinLengthOfStay"));
                    flight.setMaxLengthOfStay(rs.getInt("MaxLengthOfStay"));
                    flight.setNumReservations(rs.getInt("NumReservations"));
                    flights.add(flight);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flights;
    }
    
    /**
     * Transaction 3.3.7: Best-seller flights (most booked)
     */
    public List<Flight> getBestSellingFlights() {
        List<Flight> flights = new ArrayList<>();
        
        String sql = "SELECT F.AirlineID, F.FlightNo, F.NoOfSeats, F.DaysOperating, " +
                     "F.MinLengthOfStay, F.MaxLengthOfStay, " +
                     "COUNT(DISTINCT I.ResrNo) AS NumReservations " +
                     "FROM Flight F " +
                     "LEFT JOIN Includes I ON F.AirlineID = I.AirlineID AND F.FlightNo = I.FlightNo " +
                     "GROUP BY F.AirlineID, F.FlightNo, F.NoOfSeats, F.DaysOperating, " +
                     "F.MinLengthOfStay, F.MaxLengthOfStay " +
                     "ORDER BY NumReservations DESC " +
                     "LIMIT 10";
        
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Flight flight = new Flight();
                flight.setAirlineID(rs.getString("AirlineID"));
                flight.setFlightNo(rs.getInt("FlightNo"));
                flight.setNumOfSeats(rs.getInt("NoOfSeats"));
                flight.setDaysOperating(rs.getString("DaysOperating"));
                flight.setMinLengthOfStay(rs.getInt("MinLengthOfStay"));
                flight.setMaxLengthOfStay(rs.getInt("MaxLengthOfStay"));
                flight.setNumReservations(rs.getInt("NumReservations"));
                flights.add(flight);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flights;
    }
}