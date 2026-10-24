
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AcademicDAO {

    // Helper to ensure an assessment_marks row exists for an enrollment
    private void ensureAssessmentRecord(Connection con, int enrollmentId) {
        String sql = "INSERT INTO assessment_marks (enrollment_id) VALUES (?) " +
                "ON DUPLICATE KEY UPDATE enrollment_id = enrollment_id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            ps.executeUpdate();
        } catch (SQLException ignored) {
            // Ignore if row already exists or handled by trigger
        }
    }

    // Enroll a student in a course and initialize assessment record
    public boolean enrollStudent(String rollNo, String courseCode,
                                 int semester) {

        String sql = "INSERT INTO enrollments " +
                "(roll_no, course_code, semester) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, rollNo);
            ps.setString(2, courseCode);
            ps.setInt(3, semester);

            int affected = ps.executeUpdate();
            if (affected > 0) {
                int enrollmentId = -1;
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        enrollmentId = rs.getInt(1);
                    }
                }

                if (enrollmentId > 0) {
                    ensureAssessmentRecord(con, enrollmentId);
                    System.out.println("Enrolled successfully with Enrollment ID: " + enrollmentId);
                } else {
                    System.out.println("Enrolled successfully!");
                }
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.out.println("Enrollment failed: "
                    + e.getMessage());
            return false;
        }
    }

    // Update attendance for a student enrolled in a course
    public boolean updateAttendance(String rollNo, String courseCode,
                                    int semester, int held,
                                    int attended) {

        if (held < 0 || attended < 0 || attended > held) {
            System.out.println("Invalid attendance values.");
            return false;
        }

        String sql = "UPDATE enrollments " +
                "SET classes_held = ?, classes_attended = ? " +
                "WHERE roll_no = ? AND course_code = ? AND semester = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, held);
            ps.setInt(2, attended);
            ps.setString(3, rollNo);
            ps.setString(4, courseCode);
            ps.setInt(5, semester);

            if (ps.executeUpdate() > 0) {
                System.out.println("Attendance updated successfully.");
                return true;
            }

            System.out.println("Enrollment record not found.");
            return false;

        } catch (SQLException e) {
            System.out.println("Attendance update failed: "
                    + e.getMessage());
            return false;
        }
    }

    // Display attendance percentage along with enrollment ID
    public void displayAttendance(String rollNo) {

        String sql = "SELECT enrollment_id, course_code, semester, classes_held, " +
                "classes_attended FROM enrollments WHERE roll_no = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, rollNo);

            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;

                while (rs.next()) {
                    found = true;

                    int enrollmentId = rs.getInt("enrollment_id");
                    int held = rs.getInt("classes_held");
                    int attended = rs.getInt("classes_attended");

                    double percentage = held > 0
                            ? attended * 100.0 / held : 0.0;

                    System.out.println(
                            "\nEnrollment ID: " + enrollmentId);
                    System.out.println(
                            "Course: " + rs.getString("course_code"));
                    System.out.println(
                            "Semester: " + rs.getInt("semester"));
                    System.out.printf(
                            "Attendance: %.2f%%%n", percentage);

                    if (held == 0) {
                        System.out.println("No classes recorded.");
                    } else if (percentage >= 75) {
                        System.out.println("Status: Eligible");
                    } else if (percentage >= 65) {
                        System.out.println(
                                "Status: Conditional - medical condonation required");
                    } else {
                        System.out.println(
                                "Status: SA - barred from ESE");
                    }
                }

                if (!found) {
                    System.out.println("No enrollment records found.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Could not retrieve attendance: "
                    + e.getMessage());
        }
    }

    // Display student enrollments with enrollment IDs
    public void displayEnrollments(String rollNo) {
        boolean hasFilter = (rollNo != null && !rollNo.trim().isEmpty());
        String sql = "SELECT e.enrollment_id, e.roll_no, s.student_name, " +
                "e.course_code, c.title, e.semester " +
                "FROM enrollments e " +
                "JOIN students s ON e.roll_no = s.roll_no " +
                "JOIN courses c ON e.course_code = c.course_code " +
                (hasFilter ? "WHERE e.roll_no = ? " : "") +
                "ORDER BY e.semester, e.roll_no, e.course_code";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (hasFilter) {
                ps.setString(1, rollNo.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n========== ENROLLMENT RECORDS ==========");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("Enrollment ID: %-4d | Roll: %-10s | Student: %-18s | Course: %-8s | Sem: %d | Title: %s%n",
                            rs.getInt("enrollment_id"),
                            rs.getString("roll_no"),
                            rs.getString("student_name"),
                            rs.getString("course_code"),
                            rs.getInt("semester"),
                            rs.getString("title"));
                }
                if (!found) {
                    System.out.println("No enrollment records found.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Unable to display enrollments: " + e.getMessage());
        }
    }
    
    // Add a new course
    public boolean addCourse(String courseCode, String title,
                             double credits, String courseType) {

        String sql = "INSERT INTO courses " +
                "(course_code, title, credits, course_type) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, courseCode);
            ps.setString(2, title);
            ps.setDouble(3, credits);
            ps.setString(4, courseType);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Course registration failed: "
                    + e.getMessage());
            return false;
        }
    }

    // Display all courses
    public void displayCourses() {

        String sql = "SELECT course_code, title, credits, course_type " +
                "FROM courses ORDER BY course_code";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n========== COURSE CATALOG ==========");

            while (rs.next()) {
                System.out.println(
                        rs.getString("course_code") + " | " +
                        rs.getString("title") + " | Credits: " +
                        rs.getDouble("credits") + " | Type: " +
                        rs.getString("course_type"));
            }

        } catch (SQLException e) {
            System.out.println("Unable to display courses: "
                    + e.getMessage());
        }
    }

    // Phase 1 (Mid-Semester): Record CIA Test 1 and CIA Test 2
    public boolean enterPhase1CIAMarks(int enrollmentId, int cia1, int cia2) {
        if (cia1 < 0 || cia1 > 100 || cia2 < 0 || cia2 > 100) {
            System.out.println("CIA marks must be between 0 and 100.");
            return false;
        }

        String sql = "UPDATE assessment_marks " +
                "SET cia1 = ?, cia2 = ? " +
                "WHERE enrollment_id = ?";

        try (Connection con = DBConnection.getConnection()) {
            ensureAssessmentRecord(con, enrollmentId);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, cia1);
                ps.setInt(2, cia2);
                ps.setInt(3, enrollmentId);

                if (ps.executeUpdate() > 0) {
                    System.out.println("Phase 1: CIA 1 and CIA 2 marks recorded successfully!");
                    return true;
                }
                System.out.println("Assessment record not found for enrollment ID: " + enrollmentId);
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Phase 1 CIA marks entry failed: " + e.getMessage());
            return false;
        }
    }

    // Phase 2 (End of Semester - CIA): Record Assignments / Quizzes / Lab Evaluations
    public boolean enterPhase2AssignmentMarks(int enrollmentId, int assignment) {
        if (assignment < 0 || assignment > 100) {
            System.out.println("Assignment marks must be between 0 and 100.");
            return false;
        }

        String sql = "UPDATE assessment_marks " +
                "SET assignment_mark = ? " +
                "WHERE enrollment_id = ?";

        try (Connection con = DBConnection.getConnection()) {
            ensureAssessmentRecord(con, enrollmentId);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, assignment);
                ps.setInt(2, enrollmentId);

                if (ps.executeUpdate() > 0) {
                    System.out.println("Phase 2: Assignment marks recorded successfully!");
                    return true;
                }
                System.out.println("Assessment record not found for enrollment ID: " + enrollmentId);
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Phase 2 Assignment entry failed: " + e.getMessage());
            return false;
        }
    }

    // Complete / Combined CIA Marks Entry
    public boolean enterCIAMarks(int enrollmentId, int cia1,
                                 int cia2, int assignment) {

        if (cia1 < 0 || cia1 > 100 ||
            cia2 < 0 || cia2 > 100 ||
            assignment < 0 || assignment > 100) {

            System.out.println("Marks must be between 0 and 100.");
            return false;
        }

        String sql = "UPDATE assessment_marks " +
                "SET cia1 = ?, cia2 = ?, assignment_mark = ? " +
                "WHERE enrollment_id = ?";

        try (Connection con = DBConnection.getConnection()) {
            ensureAssessmentRecord(con, enrollmentId);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, cia1);
                ps.setInt(2, cia2);
                ps.setInt(3, assignment);
                ps.setInt(4, enrollmentId);

                if (ps.executeUpdate() > 0) {
                    System.out.println("CIA marks saved successfully!");
                    return true;
                }

                System.out.println(
                        "Assessment record not found for this enrollment.");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("CIA marks update failed: "
                    + e.getMessage());
            return false;
        }
    }

    // Phase 3 (Post Semester): Record ESE marks or mark absence (AB)
    public boolean enterESEMarks(int enrollmentId, int eseMark,
                                 boolean absent) {

        if (!absent && (eseMark < 0 || eseMark > 100)) {
            System.out.println("ESE marks must be between 0 and 100.");
            return false;
        }

        String sql = "UPDATE assessment_marks " +
                "SET ese_mark = ?, ese_absent = ? " +
                "WHERE enrollment_id = ?";

        try (Connection con = DBConnection.getConnection()) {
            ensureAssessmentRecord(con, enrollmentId);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                if (absent) {
                    ps.setNull(1, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(1, eseMark);
                }

                ps.setBoolean(2, absent);
                ps.setInt(3, enrollmentId);

                if (ps.executeUpdate() > 0) {
                    System.out.println(absent
                            ? "Phase 3: ESE absence (AB) recorded successfully!"
                            : "Phase 3: ESE marks saved successfully!");
                    return true;
                }

                System.out.println("Assessment record not found.");
                return false;
            }
        } catch (SQLException e) {
            System.out.println("ESE entry failed: " + e.getMessage());
            return false;
        }
    }

    // Evaluate a student's marks and determine grade
    public void evaluateStudent(int enrollmentId) {

        String sql = "SELECT s.roll_no, s.student_name, " +
                "c.course_code, c.title, c.credits, c.course_type, " +
                "e.classes_held, e.classes_attended, " +
                "a.cia1, a.cia2, a.assignment_mark, " +
                "a.ese_mark, a.ese_absent " +
                "FROM enrollments e " +
                "JOIN students s ON e.roll_no = s.roll_no " +
                "JOIN courses c ON e.course_code = c.course_code " +
                "JOIN assessment_marks a " +
                "ON e.enrollment_id = a.enrollment_id " +
                "WHERE e.enrollment_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, enrollmentId);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    System.out.println("Enrollment or marks record not found.");
                    return;
                }

                String rollNo = rs.getString("roll_no");
                String name = rs.getString("student_name");
                String course = rs.getString("course_code");
                String type = rs.getString("course_type");

                double credits = rs.getDouble("credits");
                int held = rs.getInt("classes_held");
                int attended = rs.getInt("classes_attended");

                int cia1 = rs.getInt("cia1");
                int cia2 = rs.getInt("cia2");
                int assignment = rs.getInt("assignment_mark");

                boolean absent = rs.getBoolean("ese_absent");
                int ese = rs.getInt("ese_mark");

                double attendance = held > 0
                        ? attended * 100.0 / held : 0.0;

                System.out.println("\n========== EVALUATION ==========");
                System.out.println("Roll No: " + rollNo);
                System.out.println("Name: " + name);
                System.out.println("Course: " + course);
                System.out.printf("Attendance: %.2f%%%n", attendance);

                if (held == 0) {
                    System.out.println("Result: PENDING - attendance not recorded");
                    return;
                }

                if (attendance < 65.0) {
                    System.out.println("Result: SA");
                    System.out.println("Grade Point: 0");
                    return;
                }

                if (attendance < 75.0) {
                    System.out.println(
                            "Result: CONDITIONAL - approved medical condonation must be verified");
                    return;
                }

                if (absent) {
                    System.out.println("ESE: AB");
                    System.out.println("Result: RA");
                    System.out.println("Grade Point: 0");
                    return;
                }

                double cia = (cia1 + cia2 + assignment) / 3.0;
                double ciaWeight;
                double eseWeight;
                double minimumEse;

                switch (type.toUpperCase()) {
                    case "THEORY":
                        ciaWeight = 0.40;
                        eseWeight = 0.60;
                        minimumEse = 45.0;
                        break;

                    case "PRACTICAL":
                        ciaWeight = 0.60;
                        eseWeight = 0.40;
                        minimumEse = 50.0;
                        break;

                    case "EMBEDDED":
                        ciaWeight = 0.50;
                        eseWeight = 0.50;
                        minimumEse = 45.0;
                        break;

                    default:
                        System.out.println("Unknown course type: " + type);
                        return;
                }

                double total = cia * ciaWeight + ese * eseWeight;

                String grade;
                int gradePoint;
                String result;

                if (ese < minimumEse || total < 50.0) {
                    grade = "RA";
                    gradePoint = 0;
                    result = "FAIL";
                } else if (total >= 91) {
                    grade = "O";
                    gradePoint = 10;
                    result = "PASS";
                } else if (total >= 81) {
                    grade = "A+";
                    gradePoint = 9;
                    result = "PASS";
                } else if (total >= 71) {
                    grade = "A";
                    gradePoint = 8;
                    result = "PASS";
                } else if (total >= 61) {
                    grade = "B+";
                    gradePoint = 7;
                    result = "PASS";
                } else if (total >= 50) {
                    grade = "B";
                    gradePoint = 6;
                    result = "PASS";
                } else {
                    grade = "RA";
                    gradePoint = 0;
                    result = "FAIL";
                }

                System.out.printf("CIA Average: %.2f%n", cia);
                System.out.println("ESE Marks: " + ese);
                System.out.printf("Weighted Total: %.2f / 100%n", total);
                System.out.println("Grade: " + grade);
                System.out.println("Grade Point: " + gradePoint);
                System.out.println("Result: " + result);
                System.out.printf("Credits: %.1f%n", credits);
            }

        } catch (SQLException e) {
            System.out.println("Evaluation failed: " + e.getMessage());
        }
    }

    // Calculate SGPA for a student's semester
    public void calculateSGPA(String rollNo, int semester) {

        String sql = "SELECT c.course_code, c.credits, " +
                "c.course_type, e.classes_held, e.classes_attended, " +
                "a.cia1, a.cia2, a.assignment_mark, " +
                "a.ese_mark, a.ese_absent " +
                "FROM enrollments e " +
                "JOIN courses c ON e.course_code = c.course_code " +
                "JOIN assessment_marks a " +
                "ON e.enrollment_id = a.enrollment_id " +
                "WHERE e.roll_no = ? AND e.semester = ?";

        double totalCreditPoints = 0.0;
        double totalCredits = 0.0;
        int evaluatedCourses = 0;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, rollNo);
            ps.setInt(2, semester);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    int held = rs.getInt("classes_held");
                    int attended = rs.getInt("classes_attended");

                    // Skip courses that do not have valid attendance
                    if (held <= 0) {
                        continue;
                    }

                    double attendance = attended * 100.0 / held;

                    // SA: attendance below 65%
                    if (attendance < 65.0) {
                        totalCredits += rs.getDouble("credits");
                        evaluatedCourses++;
                        continue;
                    }

                    // Conditional attendance requires approved condonation.
                    // Do not calculate a final SGPA until it is confirmed.
                    if (attendance < 75.0) {
                        System.out.println(
                                "SGPA pending: conditional attendance in "
                                + rs.getString("course_code"));
                        return;
                    }

                    boolean absent = rs.getBoolean("ese_absent");

                    // AB receives grade point zero
                    if (absent) {
                        totalCredits += rs.getDouble("credits");
                        evaluatedCourses++;
                        continue;
                    }

                    int cia1 = rs.getInt("cia1");
                    int cia2 = rs.getInt("cia2");
                    int assignment = rs.getInt("assignment_mark");
                    int ese = rs.getInt("ese_mark");

                    double cia = (cia1 + cia2 + assignment) / 3.0;

                    String type = rs.getString("course_type");
                    double ciaWeight;
                    double eseWeight;
                    double minimumEse;

                    switch (type.toUpperCase()) {
                        case "THEORY":
                            ciaWeight = 0.40;
                            eseWeight = 0.60;
                            minimumEse = 45.0;
                            break;
                        case "PRACTICAL":
                            ciaWeight = 0.60;
                            eseWeight = 0.40;
                            minimumEse = 50.0;
                            break;
                        case "EMBEDDED":
                            ciaWeight = 0.50;
                            eseWeight = 0.50;
                            minimumEse = 45.0;
                            break;
                        default:
                            System.out.println(
                                    "Unknown course type: " + type);
                            return;
                    }

                    double total = cia * ciaWeight + ese * eseWeight;
                    int gradePoint;

                    if (ese < minimumEse || total < 50.0) {
                        gradePoint = 0;
                    } else if (total >= 91) {
                        gradePoint = 10;
                    } else if (total >= 81) {
                        gradePoint = 9;
                    } else if (total >= 71) {
                        gradePoint = 8;
                    } else if (total >= 61) {
                        gradePoint = 7;
                    } else {
                        gradePoint = 6;
                    }

                    double credits = rs.getDouble("credits");

                    totalCreditPoints += credits * gradePoint;
                    totalCredits += credits;
                    evaluatedCourses++;
                }
            }

            if (evaluatedCourses == 0 || totalCredits == 0) {
                System.out.println(
                        "No eligible course records found for SGPA.");
                return;
            }

            double sgpa = totalCreditPoints / totalCredits;

            System.out.println("\n========== SEMESTER SGPA ==========");
            System.out.println("Roll Number: " + rollNo);
            System.out.println("Semester: " + semester);
            System.out.println("Courses counted: " + evaluatedCourses);
            System.out.printf("Total Credits: %.2f%n", totalCredits);
            System.out.printf("SGPA: %.2f%n", sgpa);
            System.out.printf(
                    "Percentage Equivalent: %.2f%%%n", sgpa * 10);

        } catch (SQLException e) {
            System.out.println("SGPA calculation failed: "
                    + e.getMessage());
        }
    }


public void generateMarksheet(String rollNo, int semester) {
    String sql = """
        SELECT s.student_name, s.roll_no,
               c.course_code, c.title, c.credits, c.course_type,
               e.classes_held, e.classes_attended,
               a.cia1, a.cia2, a.assignment_mark,
               a.ese_mark, a.ese_absent
        FROM enrollments e
        JOIN students s ON e.roll_no = s.roll_no
        JOIN courses c ON e.course_code = c.course_code
        LEFT JOIN assessment_marks a
            ON e.enrollment_id = a.enrollment_id
        WHERE s.roll_no = ? AND e.semester = ?
        ORDER BY c.course_code
        """;

    try (java.sql.Connection con = DBConnection.getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setString(1, rollNo);
        ps.setInt(2, semester);

        try (java.sql.ResultSet rs = ps.executeQuery()) {
            boolean found = false;
            double totalCredits = 0.0;
            double earnedCredits = 0.0;
            double totalCreditPoints = 0.0;
            boolean hasPending = false;
            boolean hasArrear = false;
            int courseCount = 0;

            System.out.println("\n========== SEMESTER MARKSHEET ==========");

            while (rs.next()) {
                found = true;

                int held = rs.getInt("classes_held");
                int attended = rs.getInt("classes_attended");
                double attendance = held > 0
                        ? attended * 100.0 / held : 0;
                double courseCredits = rs.getDouble("credits");
                courseCount++;
                totalCredits += courseCredits;

                System.out.println("\nStudent: "
                        + rs.getString("student_name"));
                System.out.println("Roll Number: "
                        + rs.getString("roll_no"));
                System.out.println("Semester: " + semester);
                System.out.println("Course Code: "
                        + rs.getString("course_code"));
                System.out.println("Course: " + rs.getString("title"));
                System.out.printf("Credits: %.1f%n", courseCredits);
                System.out.printf("Attendance: %.2f%%%n", attendance);

                Object cia1Obj = rs.getObject("cia1");
                Object cia2Obj = rs.getObject("cia2");
                Object assignmentObj = rs.getObject("assignment_mark");
                Object eseObj = rs.getObject("ese_mark");
                boolean absent = rs.getBoolean("ese_absent");

                if (cia1Obj == null || cia2Obj == null
                        || assignmentObj == null) {
                    System.out.println("CIA Marks: Incomplete");
                    System.out.println("Result: Pending");
                    System.out.println("----------------------------------------");
                    hasPending = true;
                    continue;
                }

                double cia1 = rs.getDouble("cia1");
                double cia2 = rs.getDouble("cia2");
                double assignment = rs.getDouble("assignment_mark");
                double cia = (cia1 + cia2 + assignment) / 3.0;

                System.out.printf("CIA 1: %.2f%n", cia1);
                System.out.printf("CIA 2: %.2f%n", cia2);
                System.out.printf("Assignment: %.2f%n", assignment);
                System.out.printf("CIA Average: %.2f / 100%n", cia);

                if (held <= 0 || attendance < 65) {
                    System.out.println("ESE Marks: "
                            + (absent ? "AB" : "Not applicable"));
                    System.out.println("Grade: SA");
                    System.out.println("Grade Point: 0");
                    System.out.println("Result: NOT ELIGIBLE");
                    System.out.println("----------------------------------------");
                    hasArrear = true;
                    continue;
                }

                if (attendance < 75) {
                    System.out.println("Result: Pending medical condonation");
                    System.out.println("----------------------------------------");
                    hasPending = true;
                    continue;
                }

                if (absent) {
                    System.out.println("ESE Marks: AB");
                    System.out.println("Grade: RA");
                    System.out.println("Grade Point: 0");
                    System.out.println("Result: FAIL");
                    System.out.println("----------------------------------------");
                    hasArrear = true;
                    continue;
                }

                if (eseObj == null) {
                    System.out.println("ESE Marks: Not entered");
                    System.out.println("Result: Pending");
                    System.out.println("----------------------------------------");
                    hasPending = true;
                    continue;
                }

                double ese = rs.getDouble("ese_mark");
                String type = rs.getString("course_type");

                double total;
                double minimumEse;

                if ("THEORY".equalsIgnoreCase(type)) {
                    total = cia * 0.40 + ese * 0.60;
                    minimumEse = 45;
                } else if ("PRACTICAL".equalsIgnoreCase(type)) {
                    total = cia * 0.60 + ese * 0.40;
                    minimumEse = 50;
                } else {
                    total = cia * 0.50 + ese * 0.50;
                    minimumEse = 45;
                }

                int gp = 0;
                String grade;

                if (ese < minimumEse || total < 50) {
                    grade = "RA";
                } else if (total >= 91) {
                    grade = "O"; gp = 10;
                } else if (total >= 81) {
                    grade = "A+"; gp = 9;
                } else if (total >= 71) {
                    grade = "A"; gp = 8;
                } else if (total >= 61) {
                    grade = "B+"; gp = 7;
                } else {
                    grade = "B"; gp = 6;
                }

                totalCreditPoints += courseCredits * gp;
                if (gp == 0) {
                    hasArrear = true;
                } else {
                    earnedCredits += courseCredits;
                }

                System.out.println("ESE Marks: " + ese);
                System.out.printf("Weighted Total: %.2f / 100%n", total);
                System.out.println("Grade: " + grade);
                System.out.println("Grade Point: " + gp);
                System.out.println("Result: " + (gp > 0 ? "PASS" : "FAIL"));
                System.out.println("----------------------------------------");
            }

            if (!found) {
                System.out.println(
                        "No enrollments found for this student and semester.");
            } else {
                System.out.println("\n================= SEMESTER SUMMARY =================");
                System.out.printf("Total Courses Enrolled  : %d%n", courseCount);
                System.out.printf("Total Credits Enrolled  : %.1f%n", totalCredits);
                System.out.printf("Total Credits Earned    : %.1f%n", earnedCredits);

                if (hasPending) {
                    System.out.println("Semester Status         : PENDING (Assessments / Condonation Incomplete)");
                    System.out.println("Semester SGPA           : PENDING");
                    System.out.println("Equivalent Percentage   : PENDING");
                } else if (totalCredits > 0) {
                    double sgpa = totalCreditPoints / totalCredits;
                    double percentage = sgpa * 10.0;
                    System.out.printf("Cumulative Grade Points : %.2f%n", totalCreditPoints);
                    System.out.printf("Semester SGPA           : %.2f%n", sgpa);
                    System.out.printf("Equivalent Percentage   : %.2f%%%n", percentage);
                    System.out.println("Final Semester Result   : " + (hasArrear ? "RE-APPEAR (RA)" : "PASS"));
                }
                System.out.println("====================================================");
            }
        }

    } catch (java.sql.SQLException ex) {
        System.out.println("Marksheet error: " + ex.getMessage());
    }
}

public void attendanceDefaulterReport() {
    String sql = """
        SELECT s.roll_no, s.student_name,
               c.course_code, c.title,
               e.classes_held, e.classes_attended
        FROM enrollments e
        JOIN students s ON e.roll_no = s.roll_no
        JOIN courses c ON e.course_code = c.course_code
        WHERE e.classes_held > 0
        ORDER BY s.roll_no, c.course_code
        """;

    try (java.sql.Connection con = DBConnection.getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(sql);
         java.sql.ResultSet rs = ps.executeQuery()) {

        boolean found = false;

        System.out.println("\n===== ATTENDANCE DEFAULTER REPORT (KEC R2024) =====");

        while (rs.next()) {
            int held = rs.getInt("classes_held");
            int attended = rs.getInt("classes_attended");
            double percentage = attended * 100.0 / held;

            if (percentage < 65.0) {
                found = true;
                System.out.printf(
                    "[SA - DEFAULTER] Roll: %-10s | Name: %-18s | Course: %-8s | Attendance: %5.2f%% | Grade: SA (Barred from ESE, 0 GP)%n",
                    rs.getString("roll_no"),
                    rs.getString("student_name"),
                    rs.getString("course_code"),
                    percentage
                );
            } else if (percentage < 75.0) {
                found = true;
                System.out.printf(
                    "[CONDITIONAL]    Roll: %-10s | Name: %-18s | Course: %-8s | Attendance: %5.2f%% | Status: Condonation Required%n",
                    rs.getString("roll_no"),
                    rs.getString("student_name"),
                    rs.getString("course_code"),
                    percentage
                );
            }
        }

        if (!found) {
            System.out.println("No attendance defaulters found (<75%). All students eligible.");
        }

    } catch (java.sql.SQLException ex) {
        System.out.println("Report error: " + ex.getMessage());
    }
}

public void departmentPassFailReport() {
    String sql = """
        SELECT s.department,
               COUNT(*) AS total_students,
               SUM(
                   CASE
                       WHEN a.ese_absent = 1 THEN 0
                       WHEN a.ese_mark IS NULL THEN 0
                       WHEN e.classes_held = 0 THEN 0
                       WHEN e.classes_attended * 100.0
                            / e.classes_held < 75 THEN 0
                       WHEN (
                           CASE
                               WHEN c.course_type = 'THEORY'
                                   THEN (
                                       (COALESCE(a.cia1, 0)
                                       + COALESCE(a.cia2, 0)
                                       + COALESCE(a.assignment_mark, 0))
                                       / 3.0 * 0.40
                                       + a.ese_mark * 0.60
                                   )
                               WHEN c.course_type = 'PRACTICAL'
                                   THEN (
                                       (COALESCE(a.cia1, 0)
                                       + COALESCE(a.cia2, 0)
                                       + COALESCE(a.assignment_mark, 0))
                                       / 3.0 * 0.60
                                       + a.ese_mark * 0.40
                                   )
                               ELSE (
                                       (COALESCE(a.cia1, 0)
                                       + COALESCE(a.cia2, 0)
                                       + COALESCE(a.assignment_mark, 0))
                                       / 3.0 * 0.50
                                       + a.ese_mark * 0.50
                                   )
                           END
                       ) >= 50
                       AND (
                           (c.course_type = 'THEORY'
                               AND a.ese_mark >= 45)
                           OR (c.course_type = 'PRACTICAL'
                               AND a.ese_mark >= 50)
                           OR (c.course_type = 'EMBEDDED'
                               AND a.ese_mark >= 45)
                       )
                       THEN 1
                       ELSE 0
                   END
               ) AS passed
        FROM enrollments e
        JOIN students s ON e.roll_no = s.roll_no
        JOIN courses c ON e.course_code = c.course_code
        LEFT JOIN assessment_marks a
            ON e.enrollment_id = a.enrollment_id
        GROUP BY s.department
        ORDER BY s.department
        """;

    try (java.sql.Connection con = DBConnection.getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(sql);
         java.sql.ResultSet rs = ps.executeQuery()) {

        System.out.println("\n===== DEPARTMENT PASS/FAIL REPORT =====");

        while (rs.next()) {
            int total = rs.getInt("total_students");
            int passed = rs.getInt("passed");
            int failed = total - passed;
            double passPercentage = total > 0
                    ? passed * 100.0 / total : 0;

            System.out.println("Department: "
                    + rs.getString("department"));
            System.out.println("Total Enrollments: " + total);
            System.out.println("Passed: " + passed);
            System.out.println("Failed/Pending: " + failed);
            System.out.printf("Pass Percentage: %.2f%%%n",
                    passPercentage);
            System.out.println("----------------------------------");
        }

    } catch (java.sql.SQLException ex) {
        System.out.println("Report error: " + ex.getMessage());
    }
}


public void classAverageSGPAReport(int semester) {
    String sql = """
        SELECT s.roll_no, s.student_name
        FROM students s
        WHERE EXISTS (
            SELECT 1 FROM enrollments e
            WHERE e.roll_no = s.roll_no
              AND e.semester = ?
        )
        ORDER BY s.roll_no
        """;

    try (java.sql.Connection con = DBConnection.getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, semester);

        double sgpaSum = 0.0;
        int studentsCounted = 0;

        try (java.sql.ResultSet rs = ps.executeQuery()) {
            System.out.println("\n===== CLASS AVERAGE SGPA REPORT =====");
            System.out.println("Semester: " + semester);

            while (rs.next()) {
                String rollNo = rs.getString("roll_no");
                String studentName = rs.getString("student_name");

                double studentSgpa = calculateSGPAValue(con, rollNo, semester);

                if (studentSgpa >= 0) {
                    System.out.printf(
                        "Student: %s (%s) | SGPA: %.2f%n",
                        studentName, rollNo, studentSgpa
                    );

                    sgpaSum += studentSgpa;
                    studentsCounted++;
                } else {
                    System.out.printf(
                        "Student: %s (%s) | SGPA unavailable%n",
                        studentName, rollNo
                    );
                }
            }
        }

        if (studentsCounted > 0) {
            System.out.printf(
                "%nStudents counted: %d%n", studentsCounted
            );
            System.out.printf(
                "Class Average SGPA: %.2f%n",
                sgpaSum / studentsCounted
            );
        } else {
            System.out.println("No students with calculable SGPA.");
        }

    } catch (java.sql.SQLException ex) {
        System.out.println("Report error: " + ex.getMessage());
    }
}

private double calculateSGPAValue(
        java.sql.Connection con, String rollNo, int semester)
        throws java.sql.SQLException {

    String sql = """
        SELECT c.credits, c.course_type,
               e.classes_held, e.classes_attended,
               a.cia1, a.cia2, a.assignment_mark,
               a.ese_mark, a.ese_absent
        FROM enrollments e
        JOIN courses c ON e.course_code = c.course_code
        LEFT JOIN assessment_marks a
            ON e.enrollment_id = a.enrollment_id
        WHERE e.roll_no = ? AND e.semester = ?
        """;

    double weightedPoints = 0.0;
    double totalCredits = 0.0;

    try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
        ps.setString(1, rollNo);
        ps.setInt(2, semester);

        try (java.sql.ResultSet rs = ps.executeQuery()) {
            boolean found = false;

            while (rs.next()) {
                found = true;

                int held = rs.getInt("classes_held");
                int attended = rs.getInt("classes_attended");
                double credits = rs.getDouble("credits");

                if (held <= 0) return -1;

                double attendance = attended * 100.0 / held;

                // Do not include unresolved attendance condonation.
                if (attendance >= 65 && attendance < 75) return -1;

                totalCredits += credits;

                if (attendance < 65 || rs.getBoolean("ese_absent")) {
                    continue;
                }

                Object eseObject = rs.getObject("ese_mark");
                if (eseObject == null) return -1;

                double cia = (
                    rs.getDouble("cia1")
                    + rs.getDouble("cia2")
                    + rs.getDouble("assignment_mark")
                ) / 3.0;

                double ese = rs.getDouble("ese_mark");
                String type = rs.getString("course_type");

                double total;
                double minEse;

                if ("THEORY".equalsIgnoreCase(type)) {
                    total = cia * 0.40 + ese * 0.60;
                    minEse = 45;
                } else if ("PRACTICAL".equalsIgnoreCase(type)) {
                    total = cia * 0.60 + ese * 0.40;
                    minEse = 50;
                } else {
                    total = cia * 0.50 + ese * 0.50;
                    minEse = 45;
                }

                int gp = 0;

                if (ese >= minEse && total >= 50) {
                    if (total >= 91) gp = 10;
                    else if (total >= 81) gp = 9;
                    else if (total >= 71) gp = 8;
                    else if (total >= 61) gp = 7;
                    else gp = 6;
                }

                weightedPoints += credits * gp;
            }

            if (!found || totalCredits == 0) return -1;
        }
    }

    return weightedPoints / totalCredits;
}


}
