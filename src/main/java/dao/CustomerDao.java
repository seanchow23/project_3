package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Customer;

public class CustomerDao {
    
    /* Database Constants - UPDATE YOUR PASSWORD HERE */
    private static final String URL = "jdbc:mysql://localhost:3306/project_2";
    private static final String USER = "root";
    private static final String PASSWORD = "1234"; // 

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public List<Customer> getCustomers() {
        /*
         * Fetches all customers by joining Customer and Person tables.
         */
        List<Customer> customers = new ArrayList<Customer>();

        String sql = "SELECT C.AccountNo, C.CreditCardNo, C.Email, C.CreationDate, C.Rating, " +
                     "P.FirstName, P.LastName, P.Address, P.City, P.State, P.ZipCode, P.Phone " +
                     "FROM Customer C " +
                     "JOIN Person P ON C.Id = P.Id";

        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {

            while (rs.next()) {
                Customer customer = new Customer();
                
                // Map Person fields
                // REMOVED: customer.setId(...) as the method is undefined in your model
                customer.setFirstName(rs.getString("FirstName"));
                customer.setLastName(rs.getString("LastName"));
                customer.setAddress(rs.getString("Address"));
                customer.setCity(rs.getString("City"));
                customer.setState(rs.getString("State"));
                customer.setZipCode(rs.getInt("ZipCode"));
                // Note: If your Customer model has setPhone(), uncomment below:
                // customer.setPhone(rs.getString("Phone"));

                // Map Customer fields
                customer.setAccountNo(rs.getInt("AccountNo"));
                customer.setEmail(rs.getString("Email"));
                customer.setCreditCard(rs.getString("CreditCardNo"));
                customer.setRating(rs.getInt("Rating"));
                
                customers.add(customer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customers;
    }

    public Customer getHighestRevenueCustomer() {
        Customer customer = new Customer();
        
        String sql = "SELECT C.AccountNo, P.FirstName, P.LastName, C.Email, C.Rating, " +
                     "SUM(R.TotalFare) AS TotalRevenue " +
                     "FROM Customer C " +
                     "JOIN Person P ON C.Id = P.Id " +
                     "LEFT JOIN Reservation R ON C.AccountNo = R.AccountNo " +
                     "GROUP BY C.AccountNo, P.FirstName, P.LastName, C.Email, C.Rating " +
                     "ORDER BY TotalRevenue DESC " +
                     "LIMIT 1";

        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {

            if (rs.next()) {
                customer.setAccountNo(rs.getInt("AccountNo"));
                customer.setFirstName(rs.getString("FirstName"));
                customer.setLastName(rs.getString("LastName"));
                customer.setEmail(rs.getString("Email"));
                customer.setRating(rs.getInt("Rating"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customer;
    }

    public List<Customer> getCustomerMailingList() {
        List<Customer> customers = new ArrayList<Customer>();
        
        String sql = "SELECT P.FirstName, P.LastName, C.Email, P.Address, P.City, P.State, P.ZipCode " +
                     "FROM Customer C " +
                     "JOIN Person P ON P.Id = C.Id " +
                     "WHERE C.Email IS NOT NULL " +
                     "ORDER BY P.LastName, P.FirstName";

        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {

            while (rs.next()) {
                Customer customer = new Customer();
                customer.setFirstName(rs.getString("FirstName"));
                customer.setLastName(rs.getString("LastName"));
                customer.setEmail(rs.getString("Email"));
                customer.setAddress(rs.getString("Address"));
                customer.setCity(rs.getString("City"));
                customer.setState(rs.getString("State"));
                customer.setZipCode(rs.getInt("ZipCode"));
                
                customers.add(customer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customers;
    }

    public Customer getCustomer(int accountNo) {
        Customer customer = new Customer();
        
        String sql = "SELECT C.AccountNo, C.CreditCardNo, C.Email, C.Rating, " +
                     "P.FirstName, P.LastName, P.Address, P.City, P.State, P.ZipCode " +
                     "FROM Customer C " +
                     "JOIN Person P ON C.Id = P.Id " +
                     "WHERE C.AccountNo = ?";

        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {
            
            st.setInt(1, accountNo);
            ResultSet rs = st.executeQuery();

            if (rs.next()) {
                // REMOVED: customer.setId(...)
                customer.setAccountNo(rs.getInt("AccountNo"));
                customer.setFirstName(rs.getString("FirstName"));
                customer.setLastName(rs.getString("LastName"));
                customer.setAddress(rs.getString("Address"));
                customer.setCity(rs.getString("City"));
                customer.setState(rs.getString("State"));
                customer.setZipCode(rs.getInt("ZipCode"));
                customer.setEmail(rs.getString("Email"));
                customer.setCreditCard(rs.getString("CreditCardNo"));
                customer.setRating(rs.getInt("Rating"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customer;
    }
    
    public String deleteCustomer(int accountNo) {
        /*
         * Implementation of Transaction 3.2.4
         * Must delete dependencies in order: Preferences -> Passenger -> Customer -> Person
         */
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false); // Start Transaction

            // 1. Get the Person ID associated with this customer first
            // We use a local variable 'personId' instead of the Customer object
            int personId = 0;
            String getPersonIdSql = "SELECT Id FROM Customer WHERE AccountNo = ?";
            try (PreparedStatement psId = con.prepareStatement(getPersonIdSql)) {
                psId.setInt(1, accountNo);
                ResultSet rs = psId.executeQuery();
                if (rs.next()) {
                    personId = rs.getInt("Id");
                }
            }

            // 2. Delete Customer Preferences
            String deletePref = "DELETE FROM CustomerPreferences WHERE AccountNo = ?";
            try (PreparedStatement ps1 = con.prepareStatement(deletePref)) {
                ps1.setInt(1, accountNo);
                ps1.executeUpdate();
            }

            // 3. Delete from Passenger table
            String deletePass = "DELETE FROM Passenger WHERE AccountNo = ?";
            try (PreparedStatement ps2 = con.prepareStatement(deletePass)) {
                ps2.setInt(1, accountNo);
                ps2.executeUpdate();
            }

            // 4. Delete from Customer table
            String deleteCust = "DELETE FROM Customer WHERE AccountNo = ?";
            try (PreparedStatement ps3 = con.prepareStatement(deleteCust)) {
                ps3.setInt(1, accountNo);
                ps3.executeUpdate();
            }

            // 5. Delete from Person table
            if (personId > 0) {
                String deletePerson = "DELETE FROM Person WHERE Id = ?";
                try (PreparedStatement ps4 = con.prepareStatement(deletePerson)) {
                    ps4.setInt(1, personId);
                    ps4.executeUpdate();
                }
            }

            con.commit(); // Commit Transaction
            return "success";

        } catch (Exception e) {
            e.printStackTrace();
            if (con != null) {
                try {
                    con.rollback(); // Rollback on failure
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return "failure";
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public int getCustomerID(String emailAddress) {
        int id = -1;
        String sql = "SELECT AccountNo FROM Customer WHERE Email = ?";
        
        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {
             
            st.setString(1, emailAddress);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                id = rs.getInt("AccountNo");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    public String addCustomer(Customer customer) {
        /*
         * Implementation of Transaction 3.2.2
         */
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            // AUTO-GENERATE ID: Since Customer object doesn't have ID, we must generate one.
            // We get the maximum current Id in Person table and add 1.
            int newId = 1;
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT MAX(Id) FROM Person")) {
                if (rs.next()) {
                    newId = rs.getInt(1) + 1;
                }
            }

            // 1. Insert into Person
            String insertPerson = "INSERT INTO Person (Id, FirstName, LastName, Address, City, State, ZipCode, Phone) " +
                                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps1 = con.prepareStatement(insertPerson)) {
                ps1.setInt(1, newId); // Use the auto-generated ID
                ps1.setString(2, customer.getFirstName());
                ps1.setString(3, customer.getLastName());
                ps1.setString(4, customer.getAddress());
                ps1.setString(5, customer.getCity());
                ps1.setString(6, customer.getState());
                ps1.setInt(7, customer.getZipCode());
                // The model might not have getPhone(), so we default it or check if it exists
                // If your model has getTelephone(), change this line to customer.getTelephone()
                ps1.setString(8, "555-0000"); 
                ps1.executeUpdate();
            }

            // 2. Insert into Customer
            String insertCustomer = "INSERT INTO Customer (Id, AccountNo, Email, CreationDate, Rating, CreditCardNo) " +
                                    "VALUES (?, ?, ?, NOW(), 0, ?)";
            try (PreparedStatement ps2 = con.prepareStatement(insertCustomer)) {
                ps2.setInt(1, newId); // Use same ID
                ps2.setInt(2, customer.getAccountNo());
                ps2.setString(3, customer.getEmail());
                ps2.setString(4, customer.getCreditCard());
                ps2.executeUpdate();
            }

            // 3. Insert into Passenger (Required for booking)
            String insertPassenger = "INSERT INTO Passenger (Id, AccountNo) VALUES (?, ?)";
            try (PreparedStatement ps3 = con.prepareStatement(insertPassenger)) {
                ps3.setInt(1, newId); // Use same ID
                ps3.setInt(2, customer.getAccountNo());
                ps3.executeUpdate();
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

    public String editCustomer(Customer customer) {
        /*
         * Implementation of Transaction 3.2.3
         */
        String sql = "UPDATE Customer SET Email = ?, Rating = ?, CreditCardNo = ? WHERE AccountNo = ?";

        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {

            st.setString(1, customer.getEmail());
            st.setInt(2, customer.getRating());
            st.setString(3, customer.getCreditCard());
            st.setInt(4, customer.getAccountNo());

            int rowsUpdated = st.executeUpdate();
            return rowsUpdated > 0 ? "success" : "failure";

        } catch (Exception e) {
            e.printStackTrace();
            return "failure";
        }
    }
}