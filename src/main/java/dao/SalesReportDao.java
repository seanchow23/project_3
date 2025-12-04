package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.SalesReport;

public class SalesReportDao {

    /* Database Constants - UPDATE PASSWORD HERE */
    private static final String URL = "jdbc:mysql://localhost:3306/project_2";
    private static final String USER = "root";
    private static final String PASSWORD = "Master442713"; // <--- change as necessary 

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    public List<SalesReport> getSalesReport(String month, String year) {
        
        List<SalesReport> sales = new ArrayList<SalesReport>();
        
        /* * query joins Reservation -> customer -> person to get the Customer Name 
         * alongside the financial details for the specific Month/Year.
         */
        String sql = "SELECT R.ResrNo, R.ResrDate, R.TotalFare, R.BookingFee, R.RepSSN, P.FirstName, P.LastName " +
                     "FROM Reservation R " +
                     "JOIN Customer C ON R.AccountNo = C.AccountNo " +
                     "JOIN Person P ON C.Id = P.Id " +
                     "WHERE MONTH(R.ResrDate) = ? AND YEAR(R.ResrDate) = ? " +
                     "ORDER BY R.ResrDate DESC";
            
        try (Connection con = getConnection();
             PreparedStatement st = con.prepareStatement(sql)) {
            
            // Convert String inputs to Integer for the SQL functions
            st.setInt(1, Integer.parseInt(month));
            st.setInt(2, Integer.parseInt(year));
            
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    SalesReport sale = new SalesReport();
                    
                    sale.setResrNo(rs.getInt("ResrNo"));
                    sale.setResrDate(rs.getString("ResrDate"));
                    sale.setTotalFare(rs.getDouble("TotalFare"));
                    sale.setBookingFee(rs.getDouble("BookingFee"));
                    
                    // Handle RepSSN (might be NULL for online bookings)
                    int repSSN = rs.getInt("RepSSN");
                    if (rs.wasNull()) {
                        sale.setRepSSN("Online");
                    } else {
                        sale.setRepSSN(String.valueOf(repSSN));
                    }
                    
                    sale.setFirstName(rs.getString("FirstName"));
                    sale.setLastName(rs.getString("LastName"));
                        
                    sales.add(sale);
                }
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid Month/Year format.");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
                        
        return sales;
    }
}