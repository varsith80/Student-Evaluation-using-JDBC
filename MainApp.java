
import java.util.Scanner;

public class MainApp {

    private static final Scanner sc = new Scanner(System.in);
    private static final StudentDAO studentDAO = new StudentDAO();
    private static final AcademicDAO academicDAO = new AcademicDAO();

    public static void main(String[] args) {

        System.out.println("======================================");
        System.out.println(" KEC ACADEMIC MANAGEMENT SYSTEM");
        System.out.println("======================================");

        while (true) {
            System.out.println("\n========== MAIN MENU ==========");
            System.out.println("1. Register Student");
            System.out.println("2. Display Students");
            System.out.println("3. Enroll Student in Course");
            System.out.println("4. Update Attendance");
            System.out.println("5. Display Student Attendance");
            System.out.println("6. Add Course");
            System.out.println("7. Display Courses");
            System.out.println("8. Enter CIA Marks (Phase 1 / Phase 2)");
            System.out.println("9. Enter ESE Marks / Record AB (Phase 3)");
            System.out.println("10. Evaluate Student Result");
            System.out.println("11. Calculate Semester SGPA");
            System.out.println("12. Generate Student Semester Marksheet");
            System.out.println("13. Attendance Defaulter Report");
            System.out.println("14. Department Pass/Fail Report");
            System.out.println("15. Class Average SGPA Report");
            System.out.println("16. Display Enrollments (View Enrollment IDs)");
            System.out.println("0. Exit");
            System.out.print("Enter your choice: ");

            int choice = readInt();

            switch (choice) {
                case 1:
                    registerStudent();
                    break;

                case 2:
                    studentDAO.displayStudents();
                    break;

                case 3:
                    enrollStudent();
                    break;

                case 4:
                    updateAttendance();
                    break;

                case 5:
                    System.out.print("Enter student roll number: ");
                    String rollNo = sc.nextLine().trim();
                    academicDAO.displayAttendance(rollNo);
                    break;
                    case 6:
    addCourse();
    break;

case 7:
    academicDAO.displayCourses();
    break;
    case 8:
    enterCIAMarks();
    break;
    case 9:
    enterESEMarks();
    break;
    case 10:
    System.out.print("Enter enrollment ID: ");
    int id = readInt();
    academicDAO.evaluateStudent(id);
    break;
case 11:
    System.out.print("Enter student roll number: ");
    String sgpaRollNo = sc.nextLine().trim();

    System.out.print("Enter semester: ");
    int sgpaSemester = readInt();

    academicDAO.calculateSGPA(sgpaRollNo, sgpaSemester);
    break;
    case 12:
    System.out.print("Enter Roll Number: ");
    String marksheetRollNo = sc.nextLine();

    System.out.print("Enter Semester: ");
    int marksheetSemester = readInt();

    academicDAO.generateMarksheet(
        marksheetRollNo, marksheetSemester
    );
    break;
    case 13:
    academicDAO.attendanceDefaulterReport();
    break;
    case 14:
    academicDAO.departmentPassFailReport();
    break;
    case 15:
    System.out.print("Enter Semester: ");
    int reportSemester = readInt();

    academicDAO.classAverageSGPAReport(reportSemester);
    break;
                case 16:
                    System.out.print("Enter student roll number (leave blank for all): ");
                    String enrollRoll = sc.nextLine().trim();
                    academicDAO.displayEnrollments(enrollRoll);
                    break;

                case 0:
                    System.out.println("Thank you for using KEC AMS!");
                    sc.close();
                    return;

                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private static void registerStudent() {
        System.out.println("\n--- Student Registration ---");

        System.out.print("Enter roll number: ");
        String rollNo = sc.nextLine().trim();

        System.out.print("Enter student name: ");
        String name = sc.nextLine().trim();

        System.out.print("Enter department (IT, CSE, etc.): ");
        String department = sc.nextLine().trim();

        System.out.print("Enter current semester: ");
        int semester = readInt();

        if (rollNo.isEmpty() || name.isEmpty()
                || department.isEmpty()
                || semester < 1 || semester > 8) {
            System.out.println("Invalid student details.");
            return;
        }

        boolean success = studentDAO.addStudent(
                rollNo, name, department, semester);

        if (success) {
            System.out.println("Student registered successfully!");
        } else {
            System.out.println("Student registration unsuccessful.");
        }
    }

    private static void enrollStudent() {
        System.out.println("\n--- Course Enrollment ---");

        System.out.print("Enter student roll number: ");
        String rollNo = sc.nextLine().trim();

        System.out.print("Enter course code: ");
        String courseCode = sc.nextLine().trim();

        System.out.print("Enter semester: ");
        int semester = readInt();

        if (rollNo.isEmpty() || courseCode.isEmpty()
                || semester < 1 || semester > 8) {
            System.out.println("Invalid enrollment details.");
            return;
        }

        boolean success = academicDAO.enrollStudent(
                rollNo, courseCode, semester);

        if (success) {
            System.out.println("Student enrolled successfully!");
        } else {
            System.out.println("Enrollment unsuccessful.");
        }
    }

    private static void updateAttendance() {
        System.out.println("\n--- Update Attendance ---");

        System.out.print("Enter student roll number: ");
        String rollNo = sc.nextLine().trim();

        System.out.print("Enter course code: ");
        String courseCode = sc.nextLine().trim();

        System.out.print("Enter semester: ");
        int semester = readInt();

        System.out.print("Enter total classes held: ");
        int held = readInt();

        System.out.print("Enter classes attended: ");
        int attended = readInt();

        if (rollNo.isEmpty() || courseCode.isEmpty()
                || semester < 1 || semester > 8) {
            System.out.println("Invalid details.");
            return;
        }

        academicDAO.updateAttendance(
                rollNo, courseCode, semester, held, attended);
    }

    private static int readInt() {
        while (true) {
            String value = sc.nextLine().trim();

            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.print("Enter a valid whole number: ");
            }
        }
    }
    
    private static void addCourse() {
        System.out.println("\n--- Add Course ---");

        System.out.print("Enter course code: ");
        String code = sc.nextLine().trim();

        System.out.print("Enter course title: ");
        String title = sc.nextLine().trim();

        System.out.print("Enter credits: ");
        double credits;

        try {
            credits = Double.parseDouble(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid credits.");
            return;
        }

        System.out.print("Enter course type (THEORY, PRACTICAL, EMBEDDED): ");
        String type = sc.nextLine().trim().toUpperCase();

        if (code.isEmpty() || title.isEmpty() || credits <= 0
                || !(type.equals("THEORY")
                || type.equals("PRACTICAL")
                || type.equals("EMBEDDED"))) {
            System.out.println("Invalid course details.");
            return;
        }

        if (academicDAO.addCourse(code, title, credits, type)) {
            System.out.println("Course added successfully!");
        } else {
            System.out.println("Could not add course.");
        }
    }

    private static void enterCIAMarks() {
        System.out.println("\n--- Incremental CIA Marks Entry (KEC R2024) ---");
        System.out.println("1. Phase 1 (Mid-Semester): Record CIA Test 1 & CIA Test 2");
        System.out.println("2. Phase 2 (End of Semester): Record Assignment / Quizzes / Lab Evaluations");
        System.out.println("3. Complete CIA Entry: Record CIA 1, CIA 2 & Assignment Marks");
        System.out.print("Select Phase (1-3): ");
        int mode = readInt();

        System.out.print("Enter enrollment ID: ");
        int enrollmentId = readInt();

        if (mode == 1) {
            System.out.print("Enter CIA 1 marks (0-100): ");
            int cia1 = readInt();

            System.out.print("Enter CIA 2 marks (0-100): ");
            int cia2 = readInt();

            academicDAO.enterPhase1CIAMarks(enrollmentId, cia1, cia2);
        } else if (mode == 2) {
            System.out.print("Enter assignment / quiz / lab evaluation marks (0-100): ");
            int assignment = readInt();

            academicDAO.enterPhase2AssignmentMarks(enrollmentId, assignment);
        } else {
            System.out.print("Enter CIA 1 marks (0-100): ");
            int cia1 = readInt();

            System.out.print("Enter CIA 2 marks (0-100): ");
            int cia2 = readInt();

            System.out.print("Enter assignment marks (0-100): ");
            int assignment = readInt();

            academicDAO.enterCIAMarks(
                    enrollmentId, cia1, cia2, assignment);
        }
    }

    private static void enterESEMarks() {
        System.out.println("\n--- ESE Marks Entry ---");

        System.out.print("Enter enrollment ID: ");
        int enrollmentId = readInt();

        System.out.print("Was the student absent? (yes/no): ");
        String answer = sc.nextLine().trim();

        boolean absent;

        if (answer.equalsIgnoreCase("yes")) {
            absent = true;
        } else if (answer.equalsIgnoreCase("no")) {
            absent = false;
        } else {
            System.out.println("Please enter yes or no.");
            return;
        }

        int eseMark = 0;

        if (!absent) {
            System.out.print("Enter ESE marks (0-100): ");
            eseMark = readInt();
        }

        academicDAO.enterESEMarks(enrollmentId, eseMark, absent);
    }

}
