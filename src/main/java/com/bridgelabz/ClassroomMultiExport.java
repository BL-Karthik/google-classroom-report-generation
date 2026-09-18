package com.bridgelabz;
import com.bridgelabz.models.*;
import com.bridgelabz.service.BrowserService;
import com.bridgelabz.service.CourseService;
import com.bridgelabz.service.GoogleSheetService;
import com.bridgelabz.service.SummaryService;
import org.openqa.selenium.*;
import java.util.*;
import static com.bridgelabz.service.AssignmentService.getAssignments;
import static com.bridgelabz.service.CourseService.*;
import static com.bridgelabz.service.GradeBookService.clickGradesTab;
import static com.bridgelabz.service.GradeBookService.waitForGradebook;
import static com.bridgelabz.service.StudentService.extractStudents;
import static com.bridgelabz.service.StudentService.processStudents;

public class ClassroomMultiExport {

    /*
     * B1 courses (name starts with B1/BB1)
     * export to the B1 spreadsheet;
     * B2 courses to the B2 spreadsheet.
     */
    private static final String SPREADSHEET_ID_B1 =
            "1fZsJPXt_6V5e7KnHO0MLE_ihbwrXtI3Ae0Md060bD2Q";

    private static final String SPREADSHEET_ID_B2 =
            "1jgmFkPIFFp71c2EPWMwCXOJ6bG_irJYxFn1rSAoTf_I";

    private static final String SERVICE_ACCOUNT_FILE =
            "service_account.json";

    // Copy of the main Chrome profile (Profile 55).
    // Chrome 136+ blocks Selenium debugging on the
    // real default "User Data" directory, so a
    // dedicated copy is required. Refresh the copy
    // if the saved login expires.
    private static final String PROFILE =
            "C:\\Users\\karth\\SeleniumClassroomMain";

    private static final String CHROME_PROFILE_DIR =
            "Profile 55";

    private static final String HOME_URL =
            "https://classroom.google.com/u/0/";

    // Google sign-in page that returns straight
    // to Classroom after the user signs in.
    private static final String SIGNIN_URL =
            "https://accounts.google.com/ServiceLogin"
                    + "?continue=https%3A%2F%2Fclassroom.google.com%2Fu%2F0%2Fh";

    public static void main(String[] args) {

        System.out.println("\n======================================");
        System.out.println(" GOOGLE CLASSROOM GRADE EXPORTER");
        System.out.println("======================================");


        try {

            // STEP 1 - Test Google Sheets access
            if (!GoogleSheetService.testGoogleSheetsAccess()) {
                System.out.println("\nGoogle Sheets access failed.");
                System.out.println("Fix Google Sheets permission first.");
                return;
            }

            System.out.println("\nGoogle Sheets access confirmed.");

            // STEP 2 - Start Selenium
            WebDriver driver = BrowserService.createDriver();

            try {

                // STEP 3 - Discover all classrooms
                openClassroomHome(driver);

                List<CourseInfo> courses =
                        CourseService.getCourses(driver);

                /*
                 * No classes usually means the profile
                 * copy is not signed in. Offer manual
                 * sign-in with email and password, then
                 * try the home page again.
                 */
                if (courses.isEmpty()) {

                    System.out.println(
                            "\nNo classrooms found. "
                                    + "You are probably not signed in."
                    );

                    System.out.println(
                            "Opening Google sign-in page..."
                    );

                    driver.get(SIGNIN_URL);

                    waitForManualSignIn(driver);

                    openClassroomHome(driver);

                    courses = getCourses(driver);

                    if (courses.isEmpty()) {

                        System.out.println(
                                "Still no classrooms found on the home page."
                        );

                        return;
                    }
                }

                System.out.println("\n======================================");
                System.out.println("CLASSROOMS FOUND: " + courses.size());
                System.out.println("======================================");

                for (int i = 0; i < courses.size(); i++) {
                    System.out.println(
                            (i + 1) + ". "
                                    + courses.get(i).name
                    );
                }

                // STEP 4 - Export each classroom to
                // its own tab named after the class
                Set<String> usedTabNames =
                        new HashSet<>();

                int exported = 0;
                List<CourseSummary>b1summaries=new ArrayList<>();
                List<CourseSummary>b2summaries=new ArrayList<>();
                for (int i = 0;
                     i < courses.size();
                     i++)
                {

                    CourseInfo course =
                            courses.get(i);

                    /*
                     * Optional filter: pass a course
                     * name fragment as the first CLI
                     * argument to re-export only that
                     * course,
                     *   mvn exec:java -Dexec.args="BB1P7-702C2"
                     */
                    if (args.length > 0
                            && !course.name.replace(" ", "")
                                    .contains(
                                            args[0].replace(" ", ""))
                    ) {
                        continue;
                    }

                    System.out.println("\n--------------------------------------");
                    System.out.println("COURSE "
                            + (i + 1) + "/"
                            + courses.size() + ": "
                            + course.name);
                    System.out.println("--------------------------------------");

                    /*
                     * One broken course (timeout, UI
                     * change, network hiccup) must not
                     * abort the remaining ones.
                     */
                    try {

                        String spreadsheetId =
                                routeSpreadsheetId(
                                        course.name);

                        if (spreadsheetId == null) {

                            System.out.println(
                                    "Skipping - course name does "
                                            + "not start with B1 or B2."
                            );

                            continue;
                        }

                        driver.get(
                                HOME_URL + "c/"
                                        + course.id
                        );

                        if (!clickGradesTab(driver)) {
                            System.out.println(
                                    "Skipping - no Grades tab."
                            );
                            continue;
                        }

                        waitForGradebook(driver);

                        List<AssignmentColumn> assignments =
                                getAssignments(driver);

                        System.out.println(
                                "Assignments found: "
                                        + assignments.size()
                        );

                        List<StudentRawData> rawStudents =
                                extractStudents(driver);

                        System.out.println(
                                "Students found: "
                                        + rawStudents.size()
                        );

                        List<StudentData> students =
                                processStudents(
                                        rawStudents,
                                        assignments
                                );

                        // Empty classroom (or scrape got
                        // nothing) - do not write a
                        // blank tab into the sheet.
                        if (assignments.isEmpty()
                                && students.isEmpty()) {

                            System.out.println(
                                    "No assignments or students "
                                            + "found - export skipped."
                            );

                            continue;
                        }

                        String spread_Sheet_Id= CourseService.routeSpreadsheetId(course.name);

                        for(StudentData student:students) {
                            CourseSummary summary = SummaryService.createSummary(course, student);


                            if (spread_Sheet_Id.equals(SPREADSHEET_ID_B1)) {
                                b1summaries.add(summary);
                            } else if (spread_Sheet_Id.equals(SPREADSHEET_ID_B2)) {
                                b2summaries.add(summary);
                            }
                        }


                        String tabName =
                                uniqueTabName(
                                        sanitizeTabName(
                                                normalizeTabPrefix(
                                                        course.name)),
                                        usedTabNames
                                );

                        if (GoogleSheetService.exportToGoogleSheets(
                                spreadsheetId,
                                tabName,
                                assignments,
                                students)) {

                            exported++;
                        }

                    } catch (Exception e) {

                        System.out.println(
                                "COURSE FAILED - moving to "
                                        + "the next one: " + e
                        );

                        e.printStackTrace();
                    }
                }
                SummaryService.exportToSummarySheet(SPREADSHEET_ID_B1,"Summary",b1summaries);
                SummaryService.exportToSummarySheet(SPREADSHEET_ID_B2,"summary",b2summaries);

                System.out.println("\n======================================");
                System.out.println("EXPORT COMPLETED FOR "
                        + exported + "/" + courses.size()
                        + " CLASSROOMS");
                System.out.println("======================================");

            } finally {

                if (BrowserService.isBrowserAlive(driver)) {
                    driver.quit();
                }

                System.out.println("\nChrome closed.");
            }

        } catch (Exception e) {

            System.out.println("\n======================================");
            System.out.println("APPLICATION ERROR");
            System.out.println("======================================");

            e.printStackTrace();
        }
    }



    private static String uniqueTabName(
            String baseName,
            Set<String> used) {

        String candidate = baseName;

        int suffix = 2;

        while (used.contains(candidate)) {
            String tail = " (" + suffix++ + ")";

            int keep =
                    Math.min(
                            baseName.length(),
                            100 - tail.length()
                    );

            candidate =
                    baseName.substring(0, keep)
                            + tail;
        }

        used.add(candidate);

        return candidate;
    }



    public  static String safeString(
            Object object) {

        return object == null
                ? ""
                : String.valueOf(object).trim();
    }

}