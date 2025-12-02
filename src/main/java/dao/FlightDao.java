package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Flight;

public class FlightDao {
    
    /* Database Constants - UPDATE PASSWORD HERE */
    private static final String URL = "jdbc:mysql://localhost:3306/project_2";
    private static final String USER = "root";
    private static final String PASSWORD = "1234"; // <--- UPDATE THIS

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    public List<Flight> getAllFlights() {
        /* * Transaction 3.1.5: Comprehensive Listing of All Flights 
         */
        List<Flight> flights = new ArrayList<Flight>();
        
        String sql = "SELECT F.AirlineID, F.FlightNo, F.NoOfSeats, F.DaysOperating, " +
                     "F.MinLengthOfStay, F.MaxLengthOfStay " +
                     "FROM Flight F";
        
        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            
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

    public List<Flight> mostActiveFlights() {
        /* * Transaction 3.1.10: Produce List of Most Active Flights
         * Sorts flights by total number of reservations.
         */
        List<Flight> flights = new ArrayList<Flight>();
        
        String sql = "SELECT F.AirlineID, F.FlightNo, F.NoOfSeats, F.DaysOperating, " +
                     "F.MinLengthOfStay, F.MaxLengthOfStay, COUNT(DISTINCT I.ResrNo) AS TotalReservations " +
                     "FROM Flight F " +
                     "LEFT JOIN Includes I ON F.AirlineID = I.AirlineID AND F.FlightNo = I.FlightNo " +
                     "GROUP BY F.AirlineID, F.FlightNo, F.NoOfSeats, F.DaysOperating, F.MinLengthOfStay, F.MaxLengthOfStay " +
                     "ORDER BY TotalReservations DESC";
        
        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            
            while (rs.next()) {
                Flight flight = new Flight();
                flight.setAirlineID(rs.getString("AirlineID"));
                flight.setFlightNo(rs.getInt("FlightNo"));
                flight.setNumOfSeats(rs.getInt("NoOfSeats"));
                flight.setDaysOperating(rs.getString("DaysOperating"));
                flight.setMinLengthOfStay(rs.getInt("MinLengthOfStay"));
                flight.setMaxLengthOfStay(rs.getInt("MaxLengthOfStay"));
                // Map the calculated count to NumReservations
                flight.setNumReservations(rs.getInt("TotalReservations"));
                flights.add(flight);            
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return flights;
    }
    
    public List<Flight> getFlightsForAirport(String airport) {
        /*
         * Adapted from Transaction 3.1.12
         * Returns distinct Flight objects that have a leg arriving at OR departing from the given airport.
         */
        List<Flight> flights = new ArrayList<Flight>();
        
        String sql = "SELECT DISTINCT F.AirlineID, F.FlightNo, F.NoOfSeats, F.DaysOperating, " +
                     "F.MinLengthOfStay, F.MaxLengthOfStay " +
                     "FROM Flight F " +
                     "JOIN Leg L ON F.AirlineID = L.AirlineID AND F.FlightNo = L.FlightNo " +
                     "WHERE L.DepAirportID = ? OR L.ArrAirportID = ?";
        
        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {
            
            st.setString(1, airport);
            st.setString(2, airport);
            
            try (ResultSet rs = st.executeQuery()) {
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

    public List<Flight> getOnTimeFlights() {
        /*
         * Transaction 3.1.13
         * Note from SQL: "This query assumes all flights are on-time" due to schema limitations.
         */
        // Simply returns all flights, assuming they are on time.
        return getAllFlights();
    }

    public List<Flight> getDelayedFlights() {
        /*
         * Inverse of Transaction 3.1.13
         * Since the schema has no status column and defaults to On-Time, 
         * this returns an empty list or could implement specific logic if schema changes.
         */
        return new ArrayList<Flight>();
    }
    
    public List<Flight> getCustomerFlightSuggestions(int accountNo) {
        /* * Transaction 3.3.11: View Personalized Flight Suggestion List
         * Suggests flights based on routes the customer has flown before.
         */
        List<Flight> flights = new ArrayList<Flight>();
        
        // This query finds the most frequent routes for a customer and recommends flights on those routes
        String sql = "WITH CustomerRoutes AS ( " +
                     "    SELECT L.DepAirportID, L.ArrAirportID, COUNT(*) AS TimesFlown " +
                     "    FROM Reservation R " +
                     "    JOIN Includes I ON R.ResrNo = I.ResrNo " +
                     "    JOIN Leg L ON I.AirlineID = L.AirlineID AND I.FlightNo = L.FlightNo AND I.LegNo = L.LegNo " +
                     "    WHERE R.AccountNo = ? " +
                     "    GROUP BY L.DepAirportID, L.ArrAirportID " +
                     ") " +
                     "SELECT DISTINCT F.AirlineID, F.FlightNo, F.NoOfSeats, F.DaysOperating, " +
                     "F.MinLengthOfStay, F.MaxLengthOfStay, CR.TimesFlown " +
                     "FROM Flight F " +
                     "JOIN Leg L ON F.AirlineID = L.AirlineID AND F.FlightNo = L.FlightNo " +
                     "JOIN CustomerRoutes CR ON L.DepAirportID = CR.DepAirportID AND L.ArrAirportID = CR.ArrAirportID " +
                     "ORDER BY CR.TimesFlown DESC";
        
        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {
            
            st.setInt(1, accountNo);
            
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    Flight flight = new Flight();
                    flight.setAirlineID(rs.getString("AirlineID"));
                    flight.setFlightNo(rs.getInt("FlightNo"));
                    flight.setNumOfSeats(rs.getInt("NoOfSeats"));
                    flight.setDaysOperating(rs.getString("DaysOperating"));
                    flight.setMinLengthOfStay(rs.getInt("MinLengthOfStay"));
                    flight.setMaxLengthOfStay(rs.getInt("MaxLengthOfStay"));
                    // We can reuse NumReservations to store the relevance score (TimesFlown)
                    flight.setNumReservations(rs.getInt("TimesFlown"));
                    flights.add(flight);            
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return flights;
    }

    public List<Flight> getBestSellingFlights() {
        /* * Transaction 3.3.10: View Best-Seller List of Flights
         * Similar to mostActiveFlights but specifically for the customer view
         */
        List<Flight> flights = new ArrayList<Flight>();
        
        String sql = "SELECT F.AirlineID, F.FlightNo, F.NoOfSeats, F.DaysOperating, " +
                     "F.MinLengthOfStay, F.MaxLengthOfStay, COUNT(DISTINCT R.ResrNo) AS TotalBookings " +
                     "FROM Flight F " +
                     "JOIN Includes I ON F.AirlineID = I.AirlineID AND F.FlightNo = I.FlightNo " +
                     "JOIN Reservation R ON I.ResrNo = R.ResrNo " +
                     "GROUP BY F.AirlineID, F.FlightNo, F.NoOfSeats, F.DaysOperating, F.MinLengthOfStay, F.MaxLengthOfStay " +
                     "HAVING TotalBookings > 0 " +
                     "ORDER BY TotalBookings DESC";
        
        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            
            while (rs.next()) {
                Flight flight = new Flight();
                flight.setAirlineID(rs.getString("AirlineID"));
                flight.setFlightNo(rs.getInt("FlightNo"));
                flight.setNumOfSeats(rs.getInt("NoOfSeats"));
                flight.setDaysOperating(rs.getString("DaysOperating"));
                flight.setMinLengthOfStay(rs.getInt("MinLengthOfStay"));
                flight.setMaxLengthOfStay(rs.getInt("MaxLengthOfStay"));
                flight.setNumReservations(rs.getInt("TotalBookings"));
                flights.add(flight);            
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return flights;
    }
}