package com.bridgelabz;

import com.bridgelabz.config.ReportConfig;
import com.bridgelabz.models.*;
import com.bridgelabz.service.BrowserService;
import com.bridgelabz.service.CourseService;
import com.bridgelabz.service.GoogleSheetService;
import com.bridgelabz.service.SummaryService;
import org.openqa.selenium.WebDriver;

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
    private static final String PROFILE =
            "C:\\Users\\karth\\SeleniumClassroomMain";

    private static final String CHROME_PROFILE_DIR =
            "Profile 55";

    private static final String HOME_URL =
            "https://classroom.google.com/u/0/";

    private static final String SIGNIN_URL =
            "https://accounts.google.com/ServiceLogin"
                    + "?continue=https%3A%2F%2Fclassroom.google.com%2Fu%2F0%2Fh";


    public static void main(String[] args) {

        /*
         * =====================================================
         * COMMAND LINE OPTIONS
         * =====================================================
         *
         * Week 6:
         * mvn exec:java -Dexec.args="6"
         *
         * Week 7:
         * mvn exec:java -Dexec.args="7"
         *
         * Week 7 + specific course:
         * mvn exec:java -Dexec.args="7 BB1P7-702C2"
         *
         * Old command:
         * mvn exec:java -Dexec.args="BB1P7-702C2"
         *
         * Old command will default to Week 6.
         */

        int weekNumber = 6;

        String courseFilter = "";

        /*
         * Read command-line arguments.
         */
        if (args.length > 0) {

            try {

                /*
                 * First argument is the week number.
                 */
                weekNumber =
                        Integer.parseInt(args[0]);

            } catch (NumberFormatException e) {

                /*
                 * If first argument is not a number,
                 * treat it as the course filter.
                 *
                 * Example:
                 *
                 * -Dexec.args="BB1P7-702C2"
                 */

                courseFilter = args[0];
            }
        }

        /*
         * Second argument is always treated
         * as the course filter.
         *
         * Example:
         *
         * -Dexec.args="7 BB1P7-702C2"
         */
        if (args.length > 1) {

            courseFilter = args[1];
        }

        /*
         * Set selected week globally.
         */
        ReportConfig.setMaxWeek(weekNumber);

        System.out.println(
                "\n======================================"
        );

        System.out.println(
                " GOOGLE CLASSROOM GRADE EXPORTER"
        );

        System.out.println(
                "======================================"
        );

        System.out.println(
                "Report generated up to Week "
                        + ReportConfig.getMaxWeek()
        );

        if (!courseFilter.isEmpty()) {

            System.out.println(
                    "Course filter: "
                            + courseFilter
            );
        } else {

            System.out.println(
                    "Course filter: ALL COURSES"
            );
        }

        System.out.println(
                "======================================"
        );


        try {

            // =====================================================
            // STEP 1 - Test Google Sheets access
            // =====================================================

            if (!GoogleSheetService.testGoogleSheetsAccess()) {

                System.out.println(
                        "\nGoogle Sheets access failed."
                );

                System.out.println(
                        "Fix Google Sheets permission first."
                );

                return;
            }

            System.out.println(
                    "\nGoogle Sheets access confirmed."
            );


            // =====================================================
            // STEP 2 - Start Selenium
            // =====================================================

            WebDriver driver =
                    BrowserService.createDriver();

            try {

                // =====================================================
                // STEP 3 - Discover all classrooms
                // =====================================================

                openClassroomHome(driver);

                List<CourseInfo> courses =
                        CourseService.getCourses(driver);

                /*
                 * No classes usually means the profile
                 * copy is not signed in.
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

                    courses =
                            getCourses(driver);

                    if (courses.isEmpty()) {

                        System.out.println(
                                "Still no classrooms found on "
                                        + "the home page."
                        );

                        return;
                    }
                }


                System.out.println(
                        "\n======================================"
                );

                System.out.println(
                        "CLASSROOMS FOUND: "
                                + courses.size()
                );

                System.out.println(
                        "======================================"
                );

                for (int i = 0;
                     i < courses.size();
                     i++) {

                    System.out.println(
                            (i + 1)
                                    + ". "
                                    + courses.get(i).name
                    );
                }


                // =====================================================
                // STEP 4 - Export each classroom
                // =====================================================

                Set<String> usedTabNames =
                        new HashSet<>();

                int exported = 0;

                /*
                 * These lists collect summary data
                 * from ALL B1 and B2 course sheets.
                 */
                List<CourseSummary> b1summaries =
                        new ArrayList<>();

                List<CourseSummary> b2summaries =
                        new ArrayList<>();


                for (int i = 0;
                     i < courses.size();
                     i++) {

                    CourseInfo course =
                            courses.get(i);


                    /*
                     * Optional course filter.
                     *
                     * Example:
                     *
                     * 7 BB1P7-702C2
                     */
                    if (!courseFilter.isEmpty()
                            && !course.name
                            .replace(" ", "")
                            .contains(
                                    courseFilter
                                            .replace(" ", "")
                            )) {

                        continue;
                    }


                    System.out.println(
                            "\n--------------------------------------"
                    );

                    System.out.println(
                            "COURSE "
                                    + (i + 1)
                                    + "/"
                                    + courses.size()
                                    + ": "
                                    + course.name
                    );

                    System.out.println(
                            "--------------------------------------"
                    );


                    /*
                     * One broken course must not abort
                     * the remaining courses.
                     */
                    try {

                        String spread_sheet_Id =
                                routeSpreadsheetId(
                                        course.name
                                );


                        if (spread_sheet_Id == null) {

                            System.out.println(
                                    "Skipping - course name does "
                                            + "not start with B1 or B2."
                            );

                            continue;
                        }


                        driver.get(
                                HOME_URL
                                        + "c/"
                                        + course.id
                        );


                        if (!clickGradesTab(driver)) {

                            System.out.println(
                                    "Skipping - no Grades tab."
                            );

                            continue;
                        }


                        waitForGradebook(driver);


                        // =====================================================
                        // Get assignments
                        // =====================================================

                        List<AssignmentColumn> assignments =
                                getAssignments(driver);

                        System.out.println(
                                "Assignments found: "
                                        + assignments.size()
                        );


                        // =====================================================
                        // Get students
                        // =====================================================

                        List<StudentRawData> rawStudents =
                                extractStudents(driver);

                        System.out.println(
                                "Students found: "
                                        + rawStudents.size()
                        );


                        // =====================================================
                        // Process students
                        // =====================================================

                        List<StudentData> students =
                                processStudents(
                                        rawStudents,
                                        assignments
                                );


                        /*
                         * Empty classroom
                         * Do not create blank sheet.
                         */
                        if (assignments.isEmpty()
                                && students.isEmpty()) {

                            System.out.println(
                                    "No assignments or students "
                                            + "found - export skipped."
                            );

                            continue;
                        }


                        // =====================================================
                        // Create unique tab name
                        // =====================================================

                        String tabName =
                                uniqueTabName(
                                        sanitizeTabName(
                                                normalizeTabPrefix(
                                                        course.name
                                                )
                                        ),
                                        usedTabNames
                                );


                        // =====================================================
                        // STEP 5 - Export course sheet
                        // =====================================================

                        if (GoogleSheetService
                                .exportToGoogleSheets(
                                        spread_sheet_Id,
                                        tabName,
                                        assignments,
                                        students
                                )) {

                            exported++;


                            // =====================================================
                            // STEP 6 - Read summary from course sheet
                            // =====================================================

                            List<CourseSummary> summaries =
                                    GoogleSheetService
                                            .getSummariesFromSheet(
                                                    spread_sheet_Id,
                                                    tabName,
                                                    course
                                            );


                            // =====================================================
                            // STEP 7 - Add to B1/B2 summary
                            // =====================================================

                            if (spread_sheet_Id.equals(
                                    SPREADSHEET_ID_B1)) {

                                b1summaries.addAll(
                                        summaries
                                );

                                System.out.println(
                                        "Added "
                                                + summaries.size()
                                                + " students to B1 summary."
                                );


                            } else if (spread_sheet_Id.equals(
                                    SPREADSHEET_ID_B2)) {

                                b2summaries.addAll(
                                        summaries
                                );

                                System.out.println(
                                        "Added "
                                                + summaries.size()
                                                + " students to B2 summary."
                                );
                            }
                        }


                    } catch (Exception e) {

                        System.out.println(
                                "COURSE FAILED - moving to "
                                        + "the next one: "
                                        + e
                        );

                        e.printStackTrace();
                    }
                }


                // =====================================================
                // STEP 8 - Create B1 Summary
                // =====================================================

                System.out.println(
                        "\n======================================"
                );

                System.out.println(
                        "CREATING B1 SUMMARY"
                );

                System.out.println(
                        "B1 students collected: "
                                + b1summaries.size()
                );

                System.out.println(
                        "======================================"
                );


                SummaryService.exportToSummarySheet(
                        SPREADSHEET_ID_B1,
                        "Summary",
                        b1summaries
                );


                // =====================================================
                // STEP 9 - Create B2 Summary
                // =====================================================

                System.out.println(
                        "\n======================================"
                );

                System.out.println(
                        "CREATING B2 SUMMARY"
                );

                System.out.println(
                        "B2 students collected: "
                                + b2summaries.size()
                );

                System.out.println(
                        "======================================"
                );


                SummaryService.exportToSummarySheet(
                        SPREADSHEET_ID_B2,
                        "Summary",
                        b2summaries
                );


                // =====================================================
                // FINAL RESULT
                // =====================================================

                System.out.println(
                        "\n======================================"
                );

                System.out.println(
                        "EXPORT COMPLETED FOR "
                                + exported
                                + "/"
                                + courses.size()
                                + " CLASSROOMS"
                );

                System.out.println(
                        "REPORT GENERATED UP TO WEEK "
                                + ReportConfig.getMaxWeek()
                );

                System.out.println(
                        "======================================"
                );


            } finally {

                if (BrowserService.isBrowserAlive(
                        driver)) {

                    driver.quit();
                }

                System.out.println(
                        "\nChrome closed."
                );
            }


        } catch (Exception e) {

            System.out.println(
                    "\n======================================"
            );

            System.out.println(
                    "APPLICATION ERROR"
            );

            System.out.println(
                    "======================================"
            );

            e.printStackTrace();
        }
    }


    // =====================================================
    // Generate unique Google Sheet tab name
    // =====================================================

    private static String uniqueTabName(
            String baseName,
            Set<String> used) {

        String candidate = baseName;

        int suffix = 2;

        while (used.contains(candidate)) {

            String tail =
                    " (" + suffix++ + ")";

            int keep =
                    Math.min(
                            baseName.length(),
                            100 - tail.length()
                    );

            candidate =
                    baseName.substring(
                            0,
                            keep
                    ) + tail;
        }

        used.add(candidate);

        return candidate;
    }


    // =====================================================
    // Safe String
    // =====================================================

    public static String safeString(
            Object object) {

        return object == null
                ? ""
                : String.valueOf(object).trim();
    }
}