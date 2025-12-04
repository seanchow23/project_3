package dao;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import model.Login;
public class LoginDao {
	/*
	 * This class handles all the database operations related to login functionality
	 */
		private static final String URL = "jdbc:mysql://localhost:3306/project_2";
	    private static final String USER = "root";
	    private static final String PASSWORD = "Master442713"; //change as necessary 
	
	   
	    private Connection getConnection() throws SQLException, ClassNotFoundException {
	        Class.forName("com.mysql.cj.jdbc.Driver");
	        return DriverManager.getConnection(URL, USER, PASSWORD);
	    }
	   
	   
	    public Login login(String username, String password) {
	        Login login = null;
	       
	        // Input validation
	        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
	            System.err.println("Login attempt with null/empty credentials");
	            return null;
	        }
	       
	        System.out.println("=== LOGIN ATTEMPT ===");
	        System.out.println("Username: " + username);
	        System.out.println("====================");
	       
	        try (Connection con = getConnection();
	             PreparedStatement ps = con.prepareStatement(
	                 "SELECT Username, Role FROM Login WHERE Username = ? AND Password = ?")) {
	           
	            System.out.println("Database connection established");
	           
	            // Set parameters
	            ps.setString(1, username);
	            ps.setString(2, password);
	           
	            // Execute query
	            ResultSet rs = ps.executeQuery();
	           
	            // Process result
	            if (rs.next()) {
	                // Valid credentials found
	                login = new Login();
	                login.setUsername(rs.getString("Username"));
	                login.setRole(rs.getString("Role"));
	               
	                System.out.println(" Login SUCCESSFUL");
	                System.out.println("Role: " + login.getRole());
	            } else {
	                // Invalid credentials
	                System.out.println(" Login FAILED - Invalid credentials");
	            }
	           
	            rs.close();
	           
	        } catch (ClassNotFoundException e) {
	            System.err.println(" ERROR: MySQL JDBC Driver not found!");
	            e.printStackTrace();
	        } catch (SQLException e) {
	            System.err.println(" ERROR: Database connection/query failed!");
	            System.err.println("Check your database name, username, and password");
	            e.printStackTrace();
	        }
	       
	        return login; // Returns null if login failed, Login object if successful
	    }
	   
	    /**
	     * Adds a new user to the Login table.
	     * Called when a new customer is registered.
	     */
	    public String addUser(Login login) {
	        // Input validation
	        if (login == null || login.getUsername() == null ||
	            login.getPassword() == null || login.getRole() == null) {
	            System.err.println(" Cannot add user: Login object or fields are null");
	            return "failure";
	        }
	       
	        System.out.println("=== ADD LOGIN USER ===");
	        System.out.println("Username: " + login.getUsername());
	        System.out.println("Role: " + login.getRole());
	        System.out.println("=====================");
	       
	        try (Connection con = getConnection();
	             PreparedStatement ps = con.prepareStatement(
	                 "INSERT INTO Login (Username, Password, Role) VALUES (?, ?, ?)")) {
	           
	            // Set parameters
	            ps.setString(1, login.getUsername());
	            ps.setString(2, login.getPassword());
	            ps.setString(3, login.getRole());
	           
	            // Execute INSERT
	            int rowsInserted = ps.executeUpdate();
	           
	            if (rowsInserted > 0) {
	                System.out.println("Login account created successfully");
	                return "success";
	            } else {
	                System.out.println(" Failed to create login account");
	                return "failure";
	            }
	           
	        } catch (ClassNotFoundException e) {
	            System.err.println(" ERROR: MySQL JDBC Driver not found!");
	            e.printStackTrace();
	            return "failure";
	        } catch (SQLException e) {
	            System.err.println(" ERROR: Failed to insert login user");
	           
	            // Check for duplicate username
	            if (e.getMessage().contains("Duplicate entry")) {
	                System.err.println("Username already exists: " + login.getUsername());
	            }
	           
	            e.printStackTrace();
	            return "failure";
	        }
	    }
	}
