
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentDAO {

    // Register a new student
    public boolean addStudent(String rollNo, String name,
                              String department, int semester) {

        String sql = "INSERT INTO students " +
                "(roll_no, student_name, department, current_semester) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, rollNo);
            ps.setString(2, name);
            ps.setString(3, department);
            ps.setInt(4, semester);

            int rows = ps.executeUpdate();

            return rows > 0;

        } catch (SQLException e) {
            System.out.println("Student registration failed: "
                    + e.getMessage());
            return false;
        }
    }

    // Display all registered students
    public void displayStudents() {

        String sql = "SELECT roll_no, student_name, department, " +
                "current_semester FROM students ORDER BY roll_no";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n========== STUDENT RECORDS ==========");

            boolean found = false;

            while (rs.next()) {
                found = true;

                System.out.println("Roll No: "
                        + rs.getString("roll_no"));
                System.out.println("Name: "
                        + rs.getString("student_name"));
                System.out.println("Department: "
                        + rs.getString("department"));
                System.out.println("Semester: "
                        + rs.getInt("current_semester"));
                System.out.println("-------------------------------------");
            }

            if (!found) {
                System.out.println("No student records found.");
            }

        } catch (SQLException e) {
            System.out.println("Unable to display students: "
                    + e.getMessage());
        }
    }
}
