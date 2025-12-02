package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Customer;

public class CustomerDao {
    
    /* Database Constants - UPDATE PASSWORD HERE */
    private static final String URL = "jdbc:mysql://localhost:3306/project_2";
    private static final String USER = "root";
    private static final String PASSWORD = "Master442713"; // <--- UPDATE THIS

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public List<Customer> getCustomers() {
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
                customer.setFirstName(rs.getString("FirstName"));
                customer.setLastName(rs.getString("LastName"));
                customer.setAddress(rs.getString("Address"));
                customer.setCity(rs.getString("City"));
                customer.setState(rs.getString("State"));
                customer.setZipCode(rs.getInt("ZipCode"));
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
         * UPDATED LOGIC FOR SAMPLE DATA:
         * We MUST delete from Auctions, Includes, and ReservationPassenger before deleting the Customer.
         */
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false); // Start Transaction

            // 1. Get Person ID
            int personId = 0;
            String getPersonIdSql = "SELECT Id FROM Customer WHERE AccountNo = ?";
            try (PreparedStatement psId = con.prepareStatement(getPersonIdSql)) {
                psId.setInt(1, accountNo);
                ResultSet rs = psId.executeQuery();
                if (rs.next()) {
                    personId = rs.getInt("Id");
                }
            }
            
            // 2. DELETE FROM AUCTIONS (Crucial for Sample Data)
            try (PreparedStatement psAuc = con.prepareStatement("DELETE FROM Auctions WHERE AccountNo = ?")) {
                psAuc.setInt(1, accountNo);
                psAuc.executeUpdate();
            }

            // 3. DELETE OWNED RESERVATIONS (Crucial for Sample Data)
            // Get list of reservations owned by this customer
            String getReservationsSql = "SELECT ResrNo FROM Reservation WHERE AccountNo = ?";
            List<Integer> reservationIds = new ArrayList<>();
            try (PreparedStatement psGetRes = con.prepareStatement(getReservationsSql)) {
                psGetRes.setInt(1, accountNo);
                ResultSet rsRes = psGetRes.executeQuery();
                while (rsRes.next()) {
                    reservationIds.add(rsRes.getInt("ResrNo"));
                }
            }

            // Delete dependencies for each reservation
            String delIncludes = "DELETE FROM Includes WHERE ResrNo = ?";
            String delResPass = "DELETE FROM ReservationPassenger WHERE ResrNo = ?";
            String delRes = "DELETE FROM Reservation WHERE ResrNo = ?";
            
            for (int resrNo : reservationIds) {
                try (PreparedStatement psInc = con.prepareStatement(delIncludes);
                     PreparedStatement psRP = con.prepareStatement(delResPass);
                     PreparedStatement psR = con.prepareStatement(delRes)) {
                    
                    psInc.setInt(1, resrNo); psInc.executeUpdate(); 
                    psRP.setInt(1, resrNo);  psRP.executeUpdate();  
                    psR.setInt(1, resrNo);   psR.executeUpdate();   
                }
            }

            // 4. DELETE CUSTOMER DEPENDENCIES
            try (PreparedStatement ps1 = con.prepareStatement("DELETE FROM ReservationPassenger WHERE AccountNo = ?")) {
                ps1.setInt(1, accountNo); ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = con.prepareStatement("DELETE FROM CustomerPreferences WHERE AccountNo = ?")) {
                ps2.setInt(1, accountNo); ps2.executeUpdate();
            }
            try (PreparedStatement ps3 = con.prepareStatement("DELETE FROM Passenger WHERE AccountNo = ?")) {
                ps3.setInt(1, accountNo); ps3.executeUpdate();
            }
            try (PreparedStatement ps4 = con.prepareStatement("DELETE FROM Customer WHERE AccountNo = ?")) {
                ps4.setInt(1, accountNo); ps4.executeUpdate();
            }

            // 5. DELETE PERSON
            if (personId > 0) {
                try (PreparedStatement ps5 = con.prepareStatement("DELETE FROM Person WHERE Id = ?")) {
                    ps5.setInt(1, personId); ps5.executeUpdate();
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

    public int getCustomerID(String emailAddress) {
        int accountNo = -1;
        String sql = "SELECT AccountNo FROM Customer WHERE Email = ?";
        
        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {
             
            st.setString(1, emailAddress);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                accountNo = rs.getInt("AccountNo");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return accountNo;
    }

    public String addCustomer(Customer customer) {
        /*
         * UPDATED LOGIC FOR UI:
         * The UI does NOT send AccountNo. We must generate it manually.
         */
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            // 1. AUTO-GENERATE Person ID (Id)
            int newPersonId = 1;
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT MAX(Id) FROM Person")) {
                if (rs.next()) {
                    newPersonId = rs.getInt(1) + 1;
                }
            }
            
            // 2. AUTO-GENERATE Account Number (Since UI form does not provide it)
            int newAccountNo = 1;
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT MAX(AccountNo) FROM Customer")) {
                if (rs.next()) {
                    // Check if table was empty (MAX returns 0 if empty)
                    int max = rs.getInt(1);
                    newAccountNo = (max == 0) ? 1 : max + 1;
                }
            }
            
            // 3. Insert into Person table
            String insertPerson = "INSERT INTO Person (Id, FirstName, LastName, Address, City, State, ZipCode, Phone) " +
                                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps1 = con.prepareStatement(insertPerson)) {
                ps1.setInt(1, newPersonId);
                ps1.setString(2, customer.getFirstName());
                ps1.setString(3, customer.getLastName());
                ps1.setString(4, customer.getAddress());
                ps1.setString(5, customer.getCity());
                ps1.setString(6, customer.getState());
                ps1.setInt(7, customer.getZipCode());
                ps1.setString(8, "555-0000"); // Default phone
                ps1.executeUpdate();
            }

            // 4. Insert into Customer table using GENERATED AccountNo
            String insertCustomer = "INSERT INTO Customer (Id, AccountNo, Email, CreationDate, Rating, CreditCardNo) " +
                                    "VALUES (?, ?, ?, NOW(), 0, ?)";
            try (PreparedStatement ps2 = con.prepareStatement(insertCustomer)) {
                ps2.setInt(1, newPersonId);
                ps2.setInt(2, newAccountNo); // <--- USING GENERATED ID
                ps2.setString(3, customer.getEmail());
                ps2.setString(4, customer.getCreditCard());
                ps2.executeUpdate();
            }

            // 5. Insert into Passenger
            String insertPassenger = "INSERT INTO Passenger (Id, AccountNo) VALUES (?, ?)";
            try (PreparedStatement ps3 = con.prepareStatement(insertPassenger)) {
                ps3.setInt(1, newPersonId);
                ps3.setInt(2, newAccountNo); // <--- USING GENERATED ID
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