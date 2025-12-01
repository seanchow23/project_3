package dao;

import java.util.ArrayList;
import java.util.List;

import model.Flight;

public class FlightDao {
	
	public List<Flight> getAllFlights() {
		/* Get list of all flights, code goes here
		 */
		List<Flight> flights = new ArrayList<Flight>();
		
		for (int i = 0; i < 5; i++) {
			Flight flight = new Flight();
			flight.setAirlineID("AA");
			flight.setFlightNo(111);
			flight.setNumOfSeats(100);
			flight.setDaysOperating("1010100");
			flight.setMinLengthOfStay(1);
			flight.setMaxLengthOfStay(30);
			flights.add(flight);			
		}
		/*Sample data ends*/
		
		return flights;
	}
	public List<Flight> mostActiveFlights() {
		
		/* Get list of most active flights, code goes here
		 */
		
		List<Flight> flights = new ArrayList<Flight>();
		
		for (int i = 0; i < 5; i++) {
			Flight flight = new Flight();
			flight.setAirlineID("AA");
			flight.setFlightNo(111);
			flight.setNumReservations(30);
			flights.add(flight);			
		}
		/*Sample data ends*/
		
		return flights;
	}
	
	public List<Flight> getFlightsForAirport(String airport) {
		
		/*
		 * Code here to get flights given an airport
		 */
		
		List<Flight> flights = new ArrayList<Flight>();
		
		for (int i = 0; i < 5; i++) {
			Flight flight = new Flight();
			flight.setAirlineID("AA");
			flight.setFlightNo(111);
			flight.setNumOfSeats(100);
			flight.setDaysOperating("1010100");
			flight.setMinLengthOfStay(1);
			flight.setMaxLengthOfStay(30);
			flights.add(flight);			
		}
		/*Sample data ends*/
		
		return flights;
	}
	public List<Flight> getOnTimeFlights() {
		
		/*
		 * Code here to get on time flights
		 */
		
		List<Flight> flights = new ArrayList<Flight>();
		
		for (int i = 0; i < 5; i++) {
			Flight flight = new Flight();
			flight.setAirlineID("AA");
			flight.setFlightNo(111);
			flight.setNumOfSeats(100);
			flight.setDaysOperating("1010100");
			flight.setMinLengthOfStay(1);
			flight.setMaxLengthOfStay(30);
			flights.add(flight);			
		}
		/*Sample data ends*/
		
		return flights;
	}
	public List<Flight> getDelayedFlights() {
		
		/*
		 * Code here to get delayed flights
		 */
		
		List<Flight> flights = new ArrayList<Flight>();
		
		for (int i = 0; i < 5; i++) {
			Flight flight = new Flight();
			flight.setAirlineID("AA");
			flight.setFlightNo(111);
			flight.setNumOfSeats(100);
			flight.setDaysOperating("1010100");
			flight.setMinLengthOfStay(1);
			flight.setMaxLengthOfStay(30);
			flights.add(flight);			
		}
		/*Sample data ends*/
		
		return flights;
	}
	
	public List<Flight> getCustomerFlightSuggestions(int accountNo) {
		
		/* Get list of suggested flights depending on customer's accountNo passed
		 */
		
		List<Flight> flights = new ArrayList<Flight>();
		
		for (int i = 0; i < 5; i++) {
			Flight flight = new Flight();
			flight.setAirlineID("AA");
			flight.setFlightNo(111);
			flight.setNumReservations(30);
			flights.add(flight);			
		}
		/*Sample data ends*/
		
		return flights;
	}
	public List<Flight> getBestSellingFlights() {
		
		/* Get list of best selling flights 
		*/
		
		List<Flight> flights = new ArrayList<Flight>();
		
		for (int i = 0; i < 5; i++) {
			Flight flight = new Flight();
			flight.setAirlineID("AA");
			flight.setFlightNo(111);
			flight.setNumReservations(30);
			flights.add(flight);			
		}
		/*Sample data ends*/
		
		return flights;
	}
}