/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */

package motorph.payroll.system;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.Arrays;
import java.util.Comparator;

/*
@author crbnl
*/

    /**
    MotorPH Payroll System
    This program is a basic procedural payroll system that:
    1. Reads employee and attendance data from a CSV file
    2. Authenticates the user using a username and password
    3. Displays different menu options depending on the user role
    4. Computes hours worked based only on the allowed shift period
    5. Computes gross salary, deductions, and net salary
    6. Displays payroll records from June to December
    7. This program uses parallel arrays instead of objects because OOP is not allowed.
    8. Government deductions are computed from combined monthly gross salary.
    9. The first cutoff shows no deductions, while the second cutoff applies all deductions.
    **/

public class MotorPHPayrollSystem {

    // =============
    // SYSTEM LIMITS
    // =============

    /*
    These constants define the maximum number of employees
    and attendance records that the program can store in memory.

    Since this program uses arrays instead of dynamic collections,
    fixed sizes are needed to reserve storage space in advance.
    */
    
    /*
    Explanation:
    Maximum number of employees must be defined because arrays in java needs a fixed size when they are created.
    Since the program uses array instead of dynamic collections, I had to set a limit for how many employee records can be stored
    Why 1000 or 100000? To set the limit, So that the program can be expanded or adjusted if needed
    */
    
    static final int MAX_EMPLOYEES = 1000;
    static final int MAX_ATTENDANCE_RECORDS = 100000;

    // ==================================
    // EMPLOYEE STORAGE (PARALLEL ARRAYS)
    // ==================================

    /*
    employeeCount keeps track of how many unique employees
    have already been loaded from the CSV file.
    */
    static int employeeCount = 0;

    /*
    These arrays store employee master data.
    The same index position across all arrays belongs to one employee.

    Example:
    employeeNumbers[0], employeeNames[0], employeeBirthdays[0], employeeHourlyRates[0]
    all describe the same employee.
    */
    static int[] employeeNumbers = new int[MAX_EMPLOYEES];
    static String[] employeeNames = new String[MAX_EMPLOYEES];
    static String[] employeeBirthdays = new String[MAX_EMPLOYEES];
    static double[] employeeHourlyRates = new double[MAX_EMPLOYEES];
    static double[] employeeBasicSalaries = new double[MAX_EMPLOYEES];

    // ====================================
    // ATTENDANCE STORAGE (PARALLEL ARRAYS)
    // ====================================

    /*
    attendanceCount tracks how many attendance rows
    were successfully loaded from the CSV file.
    */
    static int attendanceCount = 0;

    /*
    These arrays store attendance details.
    Each index position represents one attendance entry.

    Example:
    attendanceEmployeeNumbers[5], attendanceDates[5], attendanceTimeIns[5], attendanceTimeOuts[5]
    all belong to the same attendance record.
    */
    static int[] attendanceEmployeeNumbers = new int[MAX_ATTENDANCE_RECORDS];
    static LocalDate[] attendanceDates = new LocalDate[MAX_ATTENDANCE_RECORDS];
    static LocalTime[] attendanceTimeIns = new LocalTime[MAX_ATTENDANCE_RECORDS];
    static LocalTime[] attendanceTimeOuts = new LocalTime[MAX_ATTENDANCE_RECORDS];
    
    // =================
    // CSV File Location
    // =================
    
    static final String EMPLOYEE_DETAILS_FILE = "resources/MotorPH_Employee_Data_Employee_Details.csv";
    static final String ATTENDANCE_RECORD_FILE = "resources/MotorPH_Employee_Data_Attendance_Record.csv";
    
    
    
    // ==============
    // LOGIN SETTINGS
    // ==============

    /*
    These are the accepted usernames and password
    for accessing the payroll system as per the instructions
    */
    static final String EMPLOYEE_USERNAME = "employee";
    static final String PAYROLL_USERNAME = "payroll_staff";
    static final String SYSTEM_PASSWORD = "12345";

    /*
    currentLoggedInRole stores the username used during login.
    It helps the program decide which menu to display next.
    */
    static String currentLoggedInRole = "";

    // ===================
    // WORK SCHEDULE RULES
    // ===================

    /*
    The payroll system only counts time worked from 8:00 AM to 5:00 PM.
    Anything before 8:00 AM or after 5:00 PM is not counted.
    
    GRACE_LIMIT means employees who log in at 8:05 AM or earlier
    are still treated as if they started exactly at 8:00 AM.
    */
    static final LocalTime WORKDAY_START = LocalTime.of(8, 0);
    static final LocalTime WORKDAY_END = LocalTime.of(17, 0);
    static final LocalTime GRACE_LIMIT = LocalTime.of(8, 5);

    public static void main(String[] args) {
        Scanner inputScanner = new Scanner(System.in);
        
        System.out.println("=== MotorPH Basic Payroll System ===");
        if (!loadEmployeeDetailsCsv() || !loadAttendanceRecordsCsv()) {
            System.out.println("Program terminated due to CSV read error.");
            return;
        }

        /*
        Step 1:
        Ask the user for login credentials.
        If the username or password is incorrect, the program stops.
        */
        
        if (!authenticateUser(inputScanner)) {
            System.out.println("Incorrect username and/or password.");
            System.out.println("Terminating program.");
            return;
        }

        /*
        Step 2:
        Display the correct menu based on the user's role.
        */
        if (currentLoggedInRole.equals(EMPLOYEE_USERNAME)) {
            showEmployeeMenu(inputScanner);
        } else {
            showPayrollStaffMenu(inputScanner);
        }
    }

    // =============
    // LOGIN METHODS
    // =============

    /*
    This method checks whether the entered username and password
    match the system's valid credentials.
    
    If successful, it stores the username in currentLoggedInRole.
    */
    static boolean authenticateUser(Scanner inputScanner) {   
        // Ask for the username
        System.out.print("Username: ");
        String enteredUsername = inputScanner.nextLine().trim();
        // Ask for the Password
        System.out.print("Password: ");
        String enteredPassword = inputScanner.nextLine().trim();
        // Check if the Username placed is valid to EMPLOYEE or PAYROLL_STAFF
        boolean validUsername = enteredUsername.equals(EMPLOYEE_USERNAME)
                || enteredUsername.equals(PAYROLL_USERNAME);
        // Check if the Password placed is valid to 12345
        boolean validPassword = enteredPassword.equals(SYSTEM_PASSWORD);

        if (validUsername && validPassword) {
            currentLoggedInRole = enteredUsername;
            return true;
        }
        return false;
    }

    // ============
    // MENU METHODS
    // ============
    
    // =============
    // EMPLOYEE MENU
    // =============
    
    /*
    This menu is shown to users who logged in as "employee".
    They are only allowed to:
    1. Enter their employee number and view payroll details
    2. Exit the program
    */

    static void showEmployeeMenu(Scanner inputScanner) {
        while (true) {
            System.out.println(" ");
            System.out.println("================");
            System.out.println("=== EMPLOYEE ===");
            System.out.println("Display options: ");
            System.out.println("1. Enter your employee number ");
            System.out.println("2. Exit the program ");
            System.out.print("Choose: ");

            String selectedOption = inputScanner.nextLine().trim();

            switch (selectedOption) {
                case "1" -> {
                    System.out.print("Enter employee number: ");
                    String employeeNumberInput = inputScanner.nextLine().trim();
                    int employeeNumber;
                    try {
                        employeeNumber = Integer.parseInt(employeeNumberInput);
                    } catch (NumberFormatException e) {
                        System.out.println("Employee number does not exist.");
                        continue;
                    }   int employeeIndex = findEmployeeIndex(employeeNumber);
                    if (employeeIndex == -1) {
                        System.out.println("Employee number does not exist.");
                    } else {
                        printEmployeeHeader(employeeIndex);
                        printPayrollRecordsForEmployee(employeeNumber);
                    }
                }
                case "2" -> {
                    System.out.println("Terminate the program.");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }
    
    // ==================
    // PAYROLL STAFF MENU
    // ==================
    /*
    This menu is shown to users who logged in as "payroll_staff".
    Payroll staff can open the payroll processing menu or exit.
    */
    static void showPayrollStaffMenu(Scanner inputScanner) {
        while (true) {
            System.out.println(" ");
            System.out.println("=====================");
            System.out.println("=== PAYROLL STAFF ===");
            System.out.println("Display options:");
            System.out.println("1. Process Payroll");
            System.out.println("2. Exit the program");
            System.out.print("Choose: ");

            String selectedOption = inputScanner.nextLine().trim();

            switch (selectedOption) {
                case "1" -> showPayrollProcessingMenu(inputScanner);
                case "2" -> {
                    System.out.println("Terminate the program.");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    /*
    This menu is only for payroll staff.
    It allows processing payroll for one employee,
    all employees, or exiting back out of the menu.
    */
    static void showPayrollProcessingMenu(Scanner inputScanner) {
        while (true) {
            System.out.println("===============");
            System.out.println("Process Payroll");
            System.out.println("Display Options: ");
            System.out.println("1. One employee");
            System.out.println("2. All employees");
            System.out.println("3. Exit the program");
            System.out.print("Choose: ");

            String selectedOption = inputScanner.nextLine().trim();

            switch (selectedOption) {
                case "1" -> {
                    System.out.print("Enter employee number: ");
                    var employeeNumberInput = inputScanner.nextLine().trim();
                    int employeeNumber;
                    try {
                        employeeNumber = Integer.parseInt(employeeNumberInput);
                    } catch (NumberFormatException e) {
                        System.out.println("Employee number does not exist.");
                        continue;
                    }   int employeeIndex = findEmployeeIndex(employeeNumber);
                    if (employeeIndex == -1) {
                        System.out.println("Employee number does not exist.");
                    } else {
                        printEmployeeHeader(employeeIndex);
                        printPayrollRecordsForEmployee(employeeNumber);
                    }
                }
                case "2" -> {
                    for (int i = 0; i < employeeCount; i++) {
                        printEmployeeHeader(i);
                        printPayrollRecordsForEmployee(employeeNumbers[i]);
                        System.out.println("==================================================");
                    }
                }
                case "3" -> {
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ===============================
    // DISPLAY EMPLOYEE IDENTIFICATION
    // ===============================

    /*
    Prints the basic identifying information of one employee.
    This is used before showing detailed payroll records.git 
    */
    static void printEmployeeHeader(int employeeIndex) {
        System.out.println("\nEmployee #: " + employeeNumbers[employeeIndex]);
        System.out.println("Employee Name: " + employeeNames[employeeIndex]);
        System.out.println("Birthday: " + employeeBirthdays[employeeIndex]);
        System.out.println("Hourly Rate: " + employeeHourlyRates[employeeIndex]);
    }
    
    // =========================
    // PAYROLL PROCESSING METHOD
    // =========================
    
    /*
    Payroll records are displayed from June to December.

    Cutoff rules:
    - First cutoff (1st–15th): gross salary and net salary with no deductions.
    - Second cutoff (16th–end): gross salary minus all monthly deductions.

    Deductions are based on the employee’s basic monthly salary,
    not on actual gross earned from hours worked. This matches statutory rules
    (SSS, PhilHealth, Pag‑IBIG, and TRAIN Law tax are bracket‑based).

    This ensures compliance: deductions remain consistent even if attendance varies.
    */
    static void printPayrollRecordsForEmployee(int employeeNumber) {
        
        // Find the employee number is not invalid number
        
        int employeeIndex = findEmployeeIndex(employeeNumber);

        if (employeeIndex == -1) {
            return;
        }
        // Find the hourly rate of the employee if existing employee number
        
        double hourlyRate = employeeHourlyRates[employeeIndex];
        
        double basicSalary  = employeeBasicSalaries[employeeIndex];

        // Loop method to find the data and process it for a specific month till the end of the year or the month of december
        for (int monthNumber = 6; monthNumber <= 12; monthNumber++) {
            Integer detectedYear = inferYearForEmployeeMonth(employeeNumber, monthNumber);
            // Skipping the month if no data is existing
            if (detectedYear == null) {
                continue;
            }
            // Create a value for the month like June 2025 july 2025 etc... since some months has different numbers of days
            YearMonth currentYearMonth = YearMonth.of(detectedYear, monthNumber);
            
            // Defining the cutoff dates
            // Defining the first Cutoff
            LocalDate firstCutoffStart = LocalDate.of(detectedYear, monthNumber, 1);
            LocalDate firstCutoffEnd = LocalDate.of(detectedYear, monthNumber, 15);
            
            // Defining the second Cutoff
            LocalDate secondCutoffStart = LocalDate.of(detectedYear, monthNumber, 16);
            LocalDate secondCutoffEnd = LocalDate.of(detectedYear, monthNumber, currentYearMonth.lengthOfMonth());
            
            // Compute for the first Cutoff
            double firstCutoffHours = computeTotalHoursForPeriod(employeeNumber, firstCutoffStart, firstCutoffEnd);
            // Compute for the second Cutoff
            double secondCutoffHours = computeTotalHoursForPeriod(employeeNumber, secondCutoffStart, secondCutoffEnd);
            // Total of the first Cutoff
            double firstCutoffGross = firstCutoffHours * hourlyRate;
            // Total of the Second Cutoff
            double secondCutoffGross = secondCutoffHours * hourlyRate;
            // Define the deduction on the computation
            double sssDeduction = computeSSS(basicSalary);
            double philHealthDeduction = computePhilHealth(basicSalary);
            double pagIbigDeduction = computePagIbig(basicSalary);
            double incomeTaxDeduction = computeIncomeTax(basicSalary * 12);
            double monthlyTaxDeduction = incomeTaxDeduction / 12;
            // Add all deductions
            double totalMonthlyDeductions = sssDeduction + philHealthDeduction + pagIbigDeduction + monthlyTaxDeduction;
            // First Cutoff no deductions
            double firstCutoffNet = firstCutoffGross;
            // Second Cutoff with deductions
            double secondCutoffNet = secondCutoffGross - totalMonthlyDeductions;
            //syntax if secondcutoffnet is 0
            // Total Net Salary of the Month
            double totalMonthlyNetSalary = firstCutoffNet + secondCutoffNet;
            
            
            // First Cut Off Display
            System.out.println("======================================");
            System.out.println("Cutoff Date: " + getMonthName(monthNumber) + " 1 to " + getMonthName(monthNumber) + " 15");
            System.out.println("======================================");
            System.out.println("Total Hours Worked: " + firstCutoffHours);
            System.out.println("Gross Salary: " + firstCutoffGross);
            System.out.println("Net Salary: " + firstCutoffNet);
            System.out.println("======================================");
            // Second Cutoff Period + Deductions Display
            System.out.println("Cutoff Date: " + getMonthName(monthNumber) + " 16 to " + getMonthName(monthNumber) + " " + currentYearMonth.lengthOfMonth());
            System.out.println("======================================");
            System.out.println("Total Hours Worked: " + secondCutoffHours);
            System.out.println("Gross Salary: " + secondCutoffGross);
            System.out.println("Net Salary: " + secondCutoffNet);
            System.out.println("======================================");
            System.out.println("Tax Breakdown: ");
            System.out.println("======================================");
            System.out.println("SSS: " + sssDeduction);
            System.out.println("PhilHealth: " + philHealthDeduction);
            System.out.println("Pag-IBIG: " + pagIbigDeduction);
            System.out.println("IncomeTax: " + monthlyTaxDeduction);
            System.out.println("======================================");
            // Total Deductions and Netsalary for the month
            System.out.println("Total Deductions: " + totalMonthlyDeductions);
            System.out.println("Net Salary: " + secondCutoffNet);
            System.out.println("Total Net Salary of the Month: " +totalMonthlyNetSalary);
            System.out.println("======================================");
            System.out.println(" ");
            System.out.println(" ");
        }
    }

    // ====================
    // HOURS WORKED METHODS
    // ====================

    /*
    computeTotalHoursForPeriod:
    Computes total hours worked by one employee between two dates.
    Uses binary search to find the first relevant record, then scans forward
    until the end date. This is faster than scanning all records (O(N log N) vs O(N)).

    computeDailyWorkedHours:
    Adjusts time in/out to company rules:
    - Start time = 8:00 AM (or grace if ≤ 8:05).
    - End time capped at 5:00 PM.
    - If time out < time in, record counts as 0.
    - Duration converted to hours.
    This enforces MotorPH’s strict attendance policy.
    */
    static double computeTotalHoursForPeriod(int employeeNumber, LocalDate startDate, LocalDate endDate) {
    double accumulatedHours = 0.0;

    // Binary search for the first record of this employee
    int low = 0, high = attendanceCount - 1;
    int startIndex = -1;

    while (low <= high) {
        int mid = (low + high) / 2;
        if (attendanceEmployeeNumbers[mid] == employeeNumber &&
            !attendanceDates[mid].isBefore(startDate)) {
            startIndex = mid;
            high = mid - 1; // keep searching left
        } else if (attendanceEmployeeNumbers[mid] < employeeNumber ||
                  (attendanceEmployeeNumbers[mid] == employeeNumber &&
                   attendanceDates[mid].isBefore(startDate))) {
            low = mid + 1;
        } else {
            high = mid - 1;
        }
    }

    // If no records found, return 0
    if (startIndex == -1) return 0.0;

    // Traverse forward until dates exceed endDate or employee changes
    for (int i = startIndex; i < attendanceCount; i++) {
        if (attendanceEmployeeNumbers[i] != employeeNumber) break;
        if (attendanceDates[i].isAfter(endDate)) break;

        accumulatedHours += computeDailyWorkedHours(attendanceTimeIns[i], attendanceTimeOuts[i]);
    }

    return accumulatedHours;
}

    /*
    Computes the number of valid worked hours for one attendance row.
    
    Rules applied:
    * Work only counts from 8:00 AM to 5:00 PM
    * Logging in before 8:00 AM does not add extra hours
    * Logging out after 5:00 PM does not add extra hours
    * Logging in at 8:05 AM or earlier is treated as exactly 8:00 AM
    * If time out is earlier than adjusted time in, the record counts as 0
    */
    static double computeDailyWorkedHours(LocalTime timeIn, LocalTime timeOut) {
        if (timeIn == null || timeOut == null) {
            return 0.0;
        }

        LocalTime adjustedStartTime;

        if (!timeIn.isAfter(GRACE_LIMIT)) {
            adjustedStartTime = WORKDAY_START;
        } else if (timeIn.isBefore(WORKDAY_START)) {
            adjustedStartTime = WORKDAY_START;
        } else {
            adjustedStartTime = timeIn;
        }

        LocalTime adjustedEndTime = timeOut.isAfter(WORKDAY_END) ? WORKDAY_END : timeOut;

        if (adjustedEndTime.isBefore(WORKDAY_START)) {
            return 0.0;
        }

        if (adjustedEndTime.isBefore(adjustedStartTime)) {
            return 0.0;
        }

        long workedMinutes = Duration.between(adjustedStartTime, adjustedEndTime).toMinutes();
        return workedMinutes / 60.0;
    }

    // =================
    // DEDUCTION METHODS
    // =================

    /*
    CComputes SSS deduction based on the employee's basic monthly salary.
    Salary is clamped to the MSC (Monthly Salary Credit) range ₱5,000–₱35,000.
    Employee share is 5% of MSC
    */
    static double computeSSS(double monthlySalary) {
    if (monthlySalary <= 0) return 0.0;
    // Clamp salary to MSC range (₱5,000–₱35,000)
    double msc = Math.min(Math.max(monthlySalary, 5000), 35000);
    return msc * 0.05; // employee share
}

    /*
    CComputes PhilHealth deduction based on the employee's basic monthly salary.
    Premium is 5% of salary, capped between ₱500 and ₱5,000.
    Employee pays half of the premium (50%)
    Returns 0 if the salary is not positive.
    */
    static double computePhilHealth(double monthlySalary) {
    if (monthlySalary <= 0) return 0.0;
    double premium = monthlySalary * 0.05;
    premium = Math.max(500, Math.min(premium, 5000));
    return premium / 2; // employee share
}

    /*
    Computes Pag-IBIG deduction based on the employee's basic monthly salary.
    Contribution is 2% of salary if above ₱1,500, otherwise 1%.
    Capped at ₱100 maximum contribution.
    */
    static double computePagIbig(double monthlySalary) {
    if (monthlySalary <= 0) return 0.0;
    return Math.min(monthlySalary * 0.02, 100.0);
}

    /*
    Computes income tax deduction based on annual taxable income (basic salary × 12).
    Uses TRAIN Law tax brackets:
    - ≤ ₱250,000: 0%
    - ₱250,001–₱400,000: 20% of excess over ₱250,000
    - ₱400,001–₱800,000: ₱30,000 + 25% of excess over ₱400,000
    - ₱800,001–₱2,000,000: ₱130,000 + 30% of excess over ₱800,000
    - ₱2,000,001–above: ₱490,000 + 35% of excess over ₱2,000,000
    */
    static double computeIncomeTax(double annualIncome) {
        if (annualIncome <= 250000) return 0.0;
        else if (annualIncome <= 400000) return (annualIncome - 250000) * 0.20;
        else if (annualIncome <= 800000) return 30000 + (annualIncome - 400000) * 0.25;
        else if (annualIncome <= 2000000) return 130000 + (annualIncome - 800000) * 0.30;
        else return 490000 + (annualIncome - 2000000) * 0.35;
}

    // ===================
    // CSV LOADING METHODS
    // ===================

  
    static boolean loadEmployeeDetailsCsv() {
        try (BufferedReader csvReader = new BufferedReader(new FileReader(EMPLOYEE_DETAILS_FILE))) {
            String currentLine = csvReader.readLine(); // skip header
            if (currentLine == null) {
                System.out.println("Employee details CSV is empty.");
                return false;
            }

            while ((currentLine = csvReader.readLine()) != null) {
                if (currentLine.trim().isEmpty()) {
                    continue;
                }
                processEmployeeDetailsRow(currentLine);
            }

            // Sort employees after loading
            sortEmployees();

            System.out.println("Loaded employees: " + employeeCount);
            return true;

        } catch (IOException e) {
            System.out.println("Error reading employee details CSV: " + e.getMessage());
            return false;
        }
    }
    static boolean loadAttendanceRecordsCsv() {
    try (BufferedReader csvReader = new BufferedReader(new FileReader(ATTENDANCE_RECORD_FILE))) {

        String currentLine = csvReader.readLine(); // skip header

        if (currentLine == null) {
            System.out.println("Attendance record CSV is empty.");
            return false;
        }

        while ((currentLine = csvReader.readLine()) != null) {
            if (currentLine.trim().isEmpty()) {
                continue;
            }
            processAttendanceRow(currentLine);
        }

        // ✅ Sort after loading so binary search works
        sortAttendanceRecords();

        System.out.println("Loaded attendance records: " + attendanceCount);
        return true;

    } catch (IOException e) {
        System.out.println("Error reading attendance record CSV: " + e.getMessage());
        return false;
    }
}
    /*
    sortEmployees:
    Sorts employees by employee number in ascending order.
    Rebuilds parallel arrays to keep data aligned.
    Ensures binary search can find employees correctly.

    sortAttendanceRecords:
    Sorts attendance records by employee number, then by date.
    Rebuilds parallel arrays to keep data aligned.
    Ensures binary search can find attendance records correctly.
    */
    static void sortAttendanceRecords() {
        // Create index array
        Integer[] indices = new Integer[attendanceCount];
        for (int i = 0; i < attendanceCount; i++) {
            indices[i] = i;
        }

        // Sort indices by employee number, then by date
        Arrays.sort(indices, Comparator
                .comparingInt((Integer i) -> attendanceEmployeeNumbers[i])
                .thenComparing(i -> attendanceDates[i]));

        // Rebuild arrays in sorted order
        int[] newEmployeeNumbers = new int[attendanceCount];
        LocalDate[] newDates = new LocalDate[attendanceCount];
        LocalTime[] newTimeIns = new LocalTime[attendanceCount];
        LocalTime[] newTimeOuts = new LocalTime[attendanceCount];

        for (int i = 0; i < attendanceCount; i++) {
            int idx = indices[i];
            newEmployeeNumbers[i] = attendanceEmployeeNumbers[idx];
            newDates[i] = attendanceDates[idx];
            newTimeIns[i] = attendanceTimeIns[idx];
            newTimeOuts[i] = attendanceTimeOuts[idx];
        }

        attendanceEmployeeNumbers = newEmployeeNumbers;
        attendanceDates = newDates;
        attendanceTimeIns = newTimeIns;
        attendanceTimeOuts = newTimeOuts;
    }
    
    static void sortEmployees() {
    Integer[] indices = new Integer[employeeCount];
    for (int i = 0; i < employeeCount; i++) {
        indices[i] = i;
    }

    Arrays.sort(indices, Comparator.comparingInt(i -> employeeNumbers[i]));

    int[] newNumbers = new int[employeeCount];
    String[] newNames = new String[employeeCount];
    String[] newBirthdays = new String[employeeCount];
    double[] newBasicSalaries = new double[employeeCount];
    double[] newHourlyRates = new double[employeeCount];

    for (int i = 0; i < employeeCount; i++) {
        int idx = indices[i];
        newNumbers[i] = employeeNumbers[idx];
        newNames[i] = employeeNames[idx];
        newBirthdays[i] = employeeBirthdays[idx];
        newBasicSalaries[i] = employeeBasicSalaries[idx];
        newHourlyRates[i] = employeeHourlyRates[idx];
    }

    employeeNumbers = newNumbers;
    employeeNames = newNames;
    employeeBirthdays = newBirthdays;
    employeeBasicSalaries = newBasicSalaries;
    employeeHourlyRates = newHourlyRates;
}
    
static void processEmployeeDetailsRow(String csvLine) {
    String[] columnValues = csvLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

    if (columnValues.length < 19) {
        return;
    }

    int employeeNumber = parseIntegerSafely(columnValues[0]);
    String lastName = columnValues[1].trim();
    String firstName = columnValues[2].trim();
    String completeName = firstName + " " + lastName;
    String birthday = columnValues[3].trim();

    // Hourly Rate and basic salary
    double basicSalary = parseDoubleSafely(columnValues[13].trim());
    double hourlyRate = parseDoubleSafely(columnValues[18].trim());
    
    int existingEmployeeIndex = findEmployeeIndexLinear(employeeNumber);
    if (existingEmployeeIndex == -1 && employeeCount < MAX_EMPLOYEES) {
        employeeNumbers[employeeCount] = employeeNumber;
        employeeNames[employeeCount] = completeName;
        employeeBirthdays[employeeCount] = birthday;
        employeeBasicSalaries[employeeCount] = basicSalary;
        employeeHourlyRates[employeeCount] = hourlyRate;
        employeeCount++;
    }
}
    /*
    Reads one line from the CSV and splits it into fields.
    
    Expected column order:
    0 = Employee Number
    1 = Last Name
    2 = First Name
    3 = Birthday
    4 = Date
    5 = Time In
    6 = Time Out
    
    
    This method stores:
    employee master data if the employee is not yet in the employee arrays
    attendance data for every valid row
    */
    static void processAttendanceRow(String csvLine) {
    String[] columnValues = csvLine.split(",", -1);

    if (columnValues.length < 6) {
        return;
    }

    int employeeNumber = parseIntegerSafely(columnValues[0]);
    LocalDate attendanceDate = parseDateValue(columnValues[3].trim());
    LocalTime timeIn = parseTimeValue(columnValues[4].trim());
    LocalTime timeOut = parseTimeValue(columnValues[5].trim());

    if (attendanceCount < MAX_ATTENDANCE_RECORDS && attendanceDate != null) {
        attendanceEmployeeNumbers[attendanceCount] = employeeNumber;
        attendanceDates[attendanceCount] = attendanceDate;
        attendanceTimeIns[attendanceCount] = timeIn;
        attendanceTimeOuts[attendanceCount] = timeOut;
        attendanceCount++;
    }
}

    // ==========================
    // SEARCH AND PARSING METHODS
    // ==========================

    /*
    Searches for an employee number in the employee array.
    If found, returns the array index.
    If not found, returns -1.
    */
    static int findEmployeeIndexLinear(int employeeNumber) {
    for (int i = 0; i < employeeCount; i++) {
        if (employeeNumbers[i] == employeeNumber) {
            return i;
        }
    }
    return -1;
}
    static int findEmployeeIndex(int employeeNumber) {
        int low = 0;
        int high = employeeCount - 1;

        while (low <= high) {
            int mid = (low + high) / 2;
            if (employeeNumbers[mid] == employeeNumber) {
                return mid;
            } else if (employeeNumbers[mid] < employeeNumber) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1; // not found
    }
    /*
    Safely converts a String into an int.
    If conversion fails, it returns Integer.MIN_VALUE
    as a signal that parsing was unsuccessful.
    */
    static int parseIntegerSafely(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    /*
    Safely converts a String into a double.
    If conversion fails, it returns 0.0.
    */
    static double parseDoubleSafely(String value) {
    try {
        value = value.trim().replace("\"", "").replace(",", "");
        return Double.parseDouble(value);
    } catch (NumberFormatException e) {
        return 0.0;
    }
}

    /*
    Tries to convert a text date into a LocalDate value.
    
    Accepted formats:
    yyyy-MM-dd
    M/d/yyyy
    
    If parsing fails, it returns null.
    */
    static LocalDate parseDateValue(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    /*
    Converts a time string such as 8:05 or 17:30
    into a LocalTime value.
    
    Accepted format:
    H:mm
    
    If parsing fails, it returns null.
    */
    static LocalTime parseTimeValue(String value) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
            return LocalTime.parse(value, formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /*
    Finds the year that should be used for a specific employee and month.
    It checks the employee's attendance records and returns the first matching year.
    
    This helps the program build valid cutoff date ranges.
    */
    static Integer inferYearForEmployeeMonth(int employeeNumber, int monthNumber) {
        for (int i = 0; i < attendanceCount; i++) {
            if (attendanceEmployeeNumbers[i] != employeeNumber) {
                continue;
            }

            if (attendanceDates[i] == null) {
                continue;
            }

            if (attendanceDates[i].getMonthValue() == monthNumber) {
                return attendanceDates[i].getYear();
            }
        }
        return null;
    }

    /*
    Converts a month number into its English month name.
    This is only for readable payroll display output.
    */
    static String getMonthName(int monthNumber) {
        return switch (monthNumber) {
            case 6 -> "June";
            case 7 -> "July";
            case 8 -> "August";
            case 9 -> "September";
            case 10 -> "October";
            case 11 -> "November";
            case 12 -> "December";
            default -> "Month" + monthNumber;
        };
    }
}