package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Employee;

public class EmployeeDao {
	
    /* Database Constants - UPDATE PASSWORD HERE */
    private static final String URL = "jdbc:mysql://localhost:3306/project_2";
    private static final String USER = "root";
    private static final String PASSWORD = "Master442713"; // <--- change as necessary

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

	public String addEmployee(Employee employee) {
        /*
         * Implementation of Transaction 3.1.1: Add Employee
         * handles the dependencies between Person and Employee tables.
         */
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false); // Start Transaction

            // 1. GENERATE Person ID
            int newPersonId = 1;
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT MAX(Id) FROM Person")) {
                if (rs.next()) {
                    newPersonId = rs.getInt(1) + 1;
                }
            }
            
            // 2. Insert into Person table
            String insertPerson = "INSERT INTO Person (Id, FirstName, LastName, Address, City, State, ZipCode, Phone) " +
                                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps1 = con.prepareStatement(insertPerson)) {
                ps1.setInt(1, newPersonId);
                ps1.setString(2, employee.getFirstName());
                ps1.setString(3, employee.getLastName());
                ps1.setString(4, employee.getAddress());
                ps1.setString(5, employee.getCity());
                ps1.setString(6, employee.getState());
                ps1.setInt(7, employee.getZipCode());
                // Phone is not in Employee model, using default placeholder
                ps1.setString(8, "555-0000"); 
                ps1.executeUpdate();
            }

            // 3. insert into Employee table
            // SSN is String in Model, but INT in Database
            String insertEmployee = "INSERT INTO Employee (Id, SSN, IsManager, StartDate, HourlyRate) " +
                                    "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps2 = con.prepareStatement(insertEmployee)) {
                ps2.setInt(1, newPersonId);
                
                // Parse SSN string to int for DB
                try {
                    ps2.setInt(2, Integer.parseInt(employee.getSSN())); 
                } catch (NumberFormatException e) {
                    con.rollback();
                    return "failure: Invalid SSN format (must be numeric)";
                }
                
                ps2.setBoolean(3, employee.getIsManager());
                ps2.setString(4, employee.getStartDate()); // DB expects 'YYYY-MM-DD' format
                ps2.setFloat(5, employee.getHourlyRate());
                ps2.executeUpdate();
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

	public String editEmployee(Employee employee) {
        /*
         * Implementation of Transaction 3.1.2: Edit Employee
         * Updates both Employee and Person tables.
         */
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false); // Start Transaction
            
            int ssnInt = 0;
            try {
                ssnInt = Integer.parseInt(employee.getSSN());
            } catch (NumberFormatException e) {
                return "failure: Invalid SSN";
            }

            // 1. Get Person ID using SSN
            int personId = 0;
            String getIdSql = "SELECT Id FROM Employee WHERE SSN = ?";
            try (PreparedStatement psId = con.prepareStatement(getIdSql)) {
                psId.setInt(1, ssnInt);
                ResultSet rs = psId.executeQuery();
                if (rs.next()) {
                    personId = rs.getInt("Id");
                }
            }
            
            if (personId == 0) {
                con.rollback();
                return "failure: Employee not found";
            }

            // 2. Update Employee table details
            String updateEmpSql = "UPDATE Employee SET HourlyRate = ?, IsManager = ? WHERE SSN = ?";
            try (PreparedStatement ps1 = con.prepareStatement(updateEmpSql)) {
                ps1.setFloat(1, employee.getHourlyRate());
                ps1.setBoolean(2, employee.getIsManager());
                ps1.setInt(3, ssnInt);
                ps1.executeUpdate();
            }

            // 3. Update Person table details
            String updatePersonSql = "UPDATE Person SET Address = ?, City = ?, State = ?, ZipCode = ?, FirstName = ?, LastName = ? WHERE Id = ?";
            try (PreparedStatement ps2 = con.prepareStatement(updatePersonSql)) {
                ps2.setString(1, employee.getAddress());
                ps2.setString(2, employee.getCity());
                ps2.setString(3, employee.getState());
                ps2.setInt(4, employee.getZipCode());
                ps2.setString(5, employee.getFirstName());
                ps2.setString(6, employee.getLastName());
                ps2.setInt(7, personId);
                ps2.executeUpdate();
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

	public String deleteEmployee(String SSN) {
        /*
         * Implementation of Transaction 3.1.3: Delete Employee
         * Handles Foreign Key constraints on Reservation.RepSSN by setting them to NULL first.
         */
        Connection con = null;
        try {
            con = getConnection();
            con.setAutoCommit(false);

            int ssnInt = Integer.parseInt(SSN);

            // 1. Get Person ID
            int personId = 0;
            String getIdSql = "SELECT Id FROM Employee WHERE SSN = ?";
            try (PreparedStatement psId = con.prepareStatement(getIdSql)) {
                psId.setInt(1, ssnInt);
                ResultSet rs = psId.executeQuery();
                if (rs.next()) {
                    personId = rs.getInt("Id");
                }
            }

            // 2. UPDATE Reservations handled by this employee to NULL (Safe Delete)
            // This prevents Foreign Key constraint failure if the employee has history.
            String unlinkReservations = "UPDATE Reservation SET RepSSN = NULL WHERE RepSSN = ?";
            try (PreparedStatement psRes = con.prepareStatement(unlinkReservations)) {
                psRes.setInt(1, ssnInt);
                psRes.executeUpdate();
            }
            
            // 3. Delete from Employee table
            String deleteEmp = "DELETE FROM Employee WHERE SSN = ?";
            try (PreparedStatement ps1 = con.prepareStatement(deleteEmp)) {
                ps1.setInt(1, ssnInt);
                ps1.executeUpdate();
            }

            // 4. Delete from Person table
            //  only delete from Person if they are NOT also a Customer (check dependencies)
            // for this project scope, we assume straightforward deletion is requested.
            if (personId > 0) {
                 // check if this person is also a customer
                boolean isCustomer = false;
                try (PreparedStatement psCheck = con.prepareStatement("SELECT AccountNo FROM Customer WHERE Id = ?")) {
                    psCheck.setInt(1, personId);
                    if (psCheck.executeQuery().next()) isCustomer = true;
                }

                if (!isCustomer) {
                    String deletePerson = "DELETE FROM Person WHERE Id = ?";
                    try (PreparedStatement ps2 = con.prepareStatement(deletePerson)) {
                        ps2.setInt(1, personId);
                        ps2.executeUpdate();
                    }
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

	public List<Employee> getEmployees() {
        /*
         * fetches all employees by joining Person and Employee tables.
         */
		List<Employee> employees = new ArrayList<Employee>();
        String sql = "SELECT P.FirstName, P.LastName, P.Address, P.City, P.State, P.ZipCode, P.Phone, " + 
                     "E.SSN, E.IsManager, E.StartDate, E.HourlyRate, C.Email " + 
                     "FROM Employee E " +
                     "JOIN Person P ON E.Id = P.Id " +
                     "LEFT JOIN Customer C ON P.Id = C.Id"; // left Join to get Email if they are also a customer

		try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {

			while (rs.next()) {
				Employee employee = new Employee();
				employee.setFirstName(rs.getString("FirstName"));
				employee.setLastName(rs.getString("LastName"));
				employee.setAddress(rs.getString("Address"));
				employee.setCity(rs.getString("City"));
				employee.setState(rs.getString("State"));
				employee.setZipCode(rs.getInt("ZipCode"));
				employee.setEmail(rs.getString("Email")); // will be null if not a customer
				employee.setSSN(String.valueOf(rs.getInt("SSN"))); // Convert DB Int to Model String
				employee.setStartDate(rs.getString("StartDate"));
				employee.setHourlyRate(rs.getFloat("HourlyRate"));
				employee.setIsManager(rs.getBoolean("IsManager"));
				
				employees.add(employee);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return employees;
	}

	public Employee getEmployee(String SSN) {
        /*
         * fetches a single employee by SSN.
         */
		Employee employee = new Employee();
        String sql = "SELECT P.FirstName, P.LastName, P.Address, P.City, P.State, P.ZipCode, P.Phone, " + 
                     "E.SSN, E.IsManager, E.StartDate, E.HourlyRate, C.Email " + 
                     "FROM Employee E " +
                     "JOIN Person P ON E.Id = P.Id " + 
                     "LEFT JOIN Customer C ON P.Id = C.Id " + 
                     "WHERE E.SSN = ?";

		try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {

            st.setInt(1, Integer.parseInt(SSN));
            ResultSet rs = st.executeQuery();

			if (rs.next()) {
				employee.setFirstName(rs.getString("FirstName"));
				employee.setLastName(rs.getString("LastName"));
				employee.setAddress(rs.getString("Address"));
				employee.setCity(rs.getString("City"));
				employee.setState(rs.getString("State"));
				employee.setZipCode(rs.getInt("ZipCode"));
				employee.setEmail(rs.getString("Email"));
				employee.setSSN(String.valueOf(rs.getInt("SSN")));
				employee.setStartDate(rs.getString("StartDate"));
				employee.setHourlyRate(rs.getFloat("HourlyRate"));
				employee.setIsManager(rs.getBoolean("IsManager"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return employee;
	}
	
	public Employee getHighestRevenueEmployee() {
        /*
         * implementation of Transaction 3.1.8: Highest Revenue Customer Rep
         */
		Employee employee = new Employee();
        String sql = "SELECT E.SSN, P.FirstName, P.LastName, E.HourlyRate, " + 
                     "SUM(R.TotalFare) AS TotalRevenue " +
                     "FROM Employee E " +
                     "JOIN Person P ON E.Id = P.Id " +
                     "LEFT JOIN Reservation R ON E.SSN = R.RepSSN " +
                     "WHERE E.IsManager = FALSE " + // Filter: Customer Reps only
                     "GROUP BY E.SSN, P.FirstName, P.LastName, E.HourlyRate " +
                     "ORDER BY TotalRevenue DESC " +
                     "LIMIT 1";

		try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
			
			if (rs.next()) {
				employee.setSSN(String.valueOf(rs.getInt("SSN")));
				employee.setFirstName(rs.getString("FirstName"));
				employee.setLastName(rs.getString("LastName"));
				employee.setHourlyRate(rs.getFloat("HourlyRate"));
                // Revenue is not in the Employee model, so we only return the ID/Name
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return employee;
	}

	public String getEmployeeID(String username) {
        /*
         * Fetches Employee's SSN based on email 
         */
		String ssn = null;
        String sql = "SELECT E.SSN FROM Employee E " +
                     "JOIN Person P ON E.Id = P.Id " +
                     "JOIN Customer C ON P.Id = C.Id " + 
                     "WHERE C.Email = ?";

		try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {
            
            st.setString(1, username);
            ResultSet rs = st.executeQuery();

			if (rs.next()) {
				ssn = String.valueOf(rs.getInt("SSN"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ssn;
	}
}