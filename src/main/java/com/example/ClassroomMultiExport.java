package com.example;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.WheelInput;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileInputStream;
import java.time.Duration;
import java.util.*;

public class ClassroomMultiExport {

    /*
     * B1 courses (name starts with B1/BB1)
     * export to the B1 spreadsheet; B2
     * courses to the B2 spreadsheet.
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

    // Signed in as karthik.reddy@bridgelabz.com
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
            if (!testGoogleSheetsAccess()) {
                System.out.println("\nGoogle Sheets access failed.");
                System.out.println("Fix Google Sheets permission first.");
                return;
            }

            System.out.println("\nGoogle Sheets access confirmed.");

            // STEP 2 - Start Selenium
            WebDriver driver = createDriver();

            try {

                // STEP 3 - Discover all classrooms
                openClassroomHome(driver);

                List<CourseInfo> courses =
                        getCourses(driver);

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

                for (int i = 0;
                     i < courses.size();
                     i++) {

                    CourseInfo course =
                            courses.get(i);

                    /*
                     * Optional filter: pass a course
                     * name fragment as the first CLI
                     * argument to re-export only that
                     * course, e.g.
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

                        String tabName =
                                uniqueTabName(
                                        sanitizeTabName(
                                                normalizeTabPrefix(
                                                        course.name)),
                                        usedTabNames
                                );

                        if (exportToGoogleSheets(
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

                System.out.println("\n======================================");
                System.out.println("EXPORT COMPLETED FOR "
                        + exported + "/" + courses.size()
                        + " CLASSROOMS");
                System.out.println("======================================");

            } finally {

                if (isBrowserAlive(driver)) {
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

    /*
     * Routes a course to its target spreadsheet
     * by name prefix: B1/BB1 -> B1 file,
     * B2/BB2 -> B2 file, anything else -> null
     * (course is skipped).
     */
    private static String routeSpreadsheetId(
            String courseName) {

        String name =
                courseName == null
                        ? ""
                        : courseName.trim()
                                .toUpperCase();

        if (name.startsWith("BB1")
                || name.startsWith("B1")) {
            return SPREADSHEET_ID_B1;
        }

        if (name.startsWith("BB2")
                || name.startsWith("B2")) {
            return SPREADSHEET_ID_B2;
        }

        return null;
    }

    // ============================================================
    // GOOGLE SHEETS
    // ============================================================

    private static Sheets createSheetsService() throws Exception {

        try (FileInputStream input =
                new FileInputStream(SERVICE_ACCOUNT_FILE)) {

            GoogleCredential credential =
                    GoogleCredential
                            .fromStream(input)
                            .createScoped(
                                    Collections.singleton(
                                            SheetsScopes.SPREADSHEETS
                                    )
                            );

            System.out.println(
                    "Authenticated Service Account: "
                            + credential.getServiceAccountId()
            );

            return new Sheets.Builder(
                    credential.getTransport(),
                    credential.getJsonFactory(),
                    credential
            )
                    .setApplicationName("Classroom Grade Exporter")
                    .build();
        }
    }

    private static boolean testGoogleSheetsAccess() {

        System.out.println("\n======================================");
        System.out.println("GOOGLE SHEETS ACCESS TEST");
        System.out.println("======================================");

        try {

            Sheets sheets = createSheetsService();

            String[] targets = {
                    "B1: " + SPREADSHEET_ID_B1,
                    "B2: " + SPREADSHEET_ID_B2
            };

            boolean allOk = true;

            for (String target : targets) {

                String id = target.substring(
                        target.indexOf(':') + 2);

                System.out.println(
                        "\nChecking spreadsheet: "
                                + target
                );

                if (id.contains("HERE")) {

                    System.out.println(
                            "NOT CONFIGURED - paste the "
                                    + "spreadsheet ID into "
                                    + "ClassroomMultiExport.java"
                    );

                    allOk = false;

                    continue;
                }

                Spreadsheet spreadsheet =
                        sheets.spreadsheets()
                                .get(id)
                                .execute();

                System.out.println(
                        "OK - Title: "
                                + spreadsheet
                                .getProperties()
                                .getTitle()
                );
            }

            return allOk;

        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {

            System.out.println(
                    "\nGOOGLE SHEETS ERROR"
            );

            System.out.println(
                    "HTTP Status: "
                            + e.getStatusCode()
            );

            if (e.getStatusCode() == 404) {

                System.out.println(
                        "\n404 - Spreadsheet not found."
                );

                System.out.println(
                        "\nCHECK THESE:"
                );

                System.out.println(
                        "1. Spreadsheet ID is correct."
                );

                System.out.println(
                        "2. Spreadsheet is shared with the service account."
                );

                System.out.println(
                        "3. Service account has Editor permission."
                );

                System.out.println(
                        "4. service_account.json is correct."
                );

                System.out.println(
                        "5. Google Sheets API is enabled."
                );
            }

            if (e.getStatusCode() == 403) {

                System.out.println(
                        "\n403 - Permission denied."
                );

                System.out.println(
                        "Give the service account Editor access."
                );
            }

            return false;

        } catch (Exception e) {

            System.out.println(
                    "\nUnable to connect to Google Sheets."
            );

            e.printStackTrace();

            return false;
        }
    }

    private static boolean exportToGoogleSheets(
            String spreadsheetId,
            String tabName,
            List<AssignmentColumn> assignments,
            List<StudentData> students) {

        try {

            Sheets sheets = createSheetsService();

            String sheetName = tabName;

            int sheetId =
                    getOrCreateSheet(
                            sheets,
                            spreadsheetId,
                            sheetName
                    );

            clearSheet(
                    sheets,
                    spreadsheetId,
                    sheetName
            );

            List<List<Object>> values =
                    new ArrayList<>();

            // Every coursework exports as a single
            // Status column (grades are tracked in
            // Classroom itself), followed by two
            // summary columns.
            int statusCount =
                    assignments.size();

            List<Object> header =
                    new ArrayList<>();
            header.add("# Roll Number ");
            header.add("Student Name");
            header.add("Submissions");
            header.add("Percentage");


            for (int i = assignments.size()-1;
                 i >=0;
                 i--) {

                header.add(
                        assignments.get(i).title
                                + " - Status"
                );
            }



            values.add(header);

            // 0-based indexes of the first status
            // column (B), the Submissions column and
            // the Percentage column in each row.
            String firstStatusCol =
                    columnLetter(4);

            String lastStatusCol =
                    columnLetter(4+statusCount-1);

            String submissionsCol =
                    columnLetter(2);

            // STUDENTS
            for (StudentData student : students) {

                List<Object> row =
                        new ArrayList<>();
                row.add(student.rollNumber);
                row.add(student.name);
                int rowNum =
                        values.size() + 1;

                if (statusCount > 0) {

                    row.add(
                            "=COUNTIF("
                                    + firstStatusCol
                                    + rowNum + ":"
                                    + lastStatusCol
                                    + rowNum
                                    + ",\"Submitted\")"
                    );

                    row.add(
                            "=" + submissionsCol
                                    + rowNum
                                    + "/" + statusCount
                    );

                } else {

                    row.add(0);

                    row.add("");
                }

                for (int i = student.assignments.size()-1;
                     i >=0;
                     i--) {

                    row.add(
                            student.assignments
                                    .get(i)
                                    .status
                    );
                }

                values.add(row);
            }


            ValueRange body =
                    new ValueRange()
                            .setValues(values);

            sheets.spreadsheets()
                    .values()
                    .update(
                            spreadsheetId,
                            sheetName + "!A1",
                            body
                    )
                    .setValueInputOption(
                            "USER_ENTERED"
                    )
                    .execute();

            formatSheet(
                    sheets,
                    spreadsheetId,
                    sheetId,
                    values.size(),
                    header.size()
            );

            System.out.println(
                    "\nData exported to Google Sheets."
            );

            return true;

        } catch (
                com.google.api.client.googleapis.json.GoogleJsonResponseException e) {

            System.out.println(
                    "\nGoogle Sheets export failed."
            );

            System.out.println(
                    "HTTP Status: "
                            + e.getStatusCode()
            );

            if (e.getStatusCode() == 404) {

                System.out.println(
                        "Spreadsheet not found or service account "
                                + "does not have access."
                );
            }

            return false;

        } catch (Exception e) {

            System.out.println(
                    "\nGoogle Sheets export failed."
            );

            e.printStackTrace();

            return false;
        }
    }

    /*
     * Converts a 0-based column index into its
     * spreadsheet letter, e.g. 0 -> "A",
     * 27 -> "AB" (used to build the summary
     * column formulas).
     */
    private static String columnLetter(
            int zeroBasedIndex) {

        StringBuilder letter =
                new StringBuilder();

        int index = zeroBasedIndex;

        while (index >= 0) {

            letter.insert(
                    0,
                    (char) ('A' + index % 26)
            );

            index = index / 26 - 1;
        }

        return letter.toString();
    }

    private static int getOrCreateSheet(
            Sheets sheets,
            String spreadsheetId,
            String sheetName) throws Exception {

        Spreadsheet spreadsheet =
                sheets.spreadsheets()
                        .get(spreadsheetId)
                        .execute();

        if (spreadsheet.getSheets() != null) {

            for (Sheet sheet :
                    spreadsheet.getSheets()) {

                if (sheet.getProperties()
                        .getTitle()
                        .equals(sheetName)) {

                    return sheet.getProperties()
                            .getSheetId();
                }
            }
        }

        AddSheetRequest addSheetRequest =
                new AddSheetRequest()
                        .setProperties(
                                new SheetProperties()
                                        .setTitle(sheetName)
                        );

        BatchUpdateSpreadsheetRequest request =
                new BatchUpdateSpreadsheetRequest()
                        .setRequests(
                                Collections.singletonList(
                                        new Request()
                                                .setAddSheet(
                                                        addSheetRequest
                                                )
                                )
                        );

        BatchUpdateSpreadsheetResponse response =
                sheets.spreadsheets()
                        .batchUpdate(
                                spreadsheetId,
                                request
                        )
                        .execute();

        return response
                .getReplies()
                .get(0)
                .getAddSheet()
                .getProperties()
                .getSheetId();
    }

    private static void clearSheet(
            Sheets sheets,
            String spreadsheetId,
            String sheetName) throws Exception {

        sheets.spreadsheets()
                .values()
                .clear(
                        spreadsheetId,
                        sheetName + "!A:ZZ",
                        new ClearValuesRequest()
                )
                .execute();
    }

    private static void formatSheet(
            Sheets sheets,
            String spreadsheetId,
            int sheetId,
            int rows,
            int columns) throws Exception {

        List<Request> requests =
                new ArrayList<>();

        // Bold header
        requests.add(
                new Request()
                        .setRepeatCell(
                                new RepeatCellRequest()
                                        .setRange(
                                                new GridRange()
                                                        .setSheetId(sheetId)
                                                        .setStartRowIndex(0)
                                                        .setEndRowIndex(1)
                                                        .setStartColumnIndex(0)
                                                        .setEndColumnIndex(columns)
                                        )
                                        .setCell(
                                                new com.google.api.services.sheets.v4.model.CellData()
                                                        .setUserEnteredFormat(
                                                                new CellFormat()
                                                                        .setTextFormat(
                                                                                new TextFormat()
                                                                                        .setBold(true)
                                                                        )
                                                        )
                                        )
                                        .setFields(
                                                "userEnteredFormat.textFormat.bold"
                                        )
                        )
        );

        // Freeze first row
        requests.add(
                new Request()
                        .setUpdateSheetProperties(
                                new UpdateSheetPropertiesRequest()
                                        .setProperties(
                                                new SheetProperties()
                                                        .setSheetId(sheetId)
                                                        .setGridProperties(
                                                                new GridProperties()
                                                                        .setFrozenRowCount(1)
                                                        )
                                        )
                                        .setFields(
                                                "gridProperties.frozenRowCount"
                                        )
                        )
        );

        // Auto resize
        requests.add(
                new Request()
                        .setAutoResizeDimensions(
                                new AutoResizeDimensionsRequest()
                                        .setDimensions(
                                                new DimensionRange()
                                                        .setSheetId(sheetId)
                                                        .setDimension("COLUMNS")
                                                        .setStartIndex(0)
                                                        .setEndIndex(columns)
                                        )
                        )
        );

        // Percent format on the Percentage
        // column (last header column) so the
        // formula shows e.g. "70%" not "0.7".
        requests.add(
                new Request()
                        .setRepeatCell(
                                new RepeatCellRequest()
                                        .setRange(
                                                new GridRange()
                                                        .setSheetId(sheetId)
                                                        .setStartRowIndex(1)
                                                        .setEndRowIndex(rows)
                                                        .setStartColumnIndex(3)
                                                        .setEndColumnIndex(4)
                                        )
                                        .setCell(
                                                new com.google.api.services.sheets.v4.model.CellData()
                                                        .setUserEnteredFormat(
                                                                new CellFormat()
                                                                        .setNumberFormat(
                                                                                new NumberFormat()
                                                                                        .setType("PERCENT")
                                                                                        .setPattern("0%")
                                                                        )
                                                        )
                                        )
                                        .setFields(
                                                "userEnteredFormat.numberFormat"
                                        )
                        )
        );

        sheets.spreadsheets()
                .batchUpdate(
                        spreadsheetId,
                        new BatchUpdateSpreadsheetRequest()
                                .setRequests(requests)
                )
                .execute();
    }

    // ============================================================
    // SELENIUM
    // ============================================================

    private static WebDriver createDriver() {

        ChromeOptions options =
                new ChromeOptions();

        options.addArguments(
                "--user-data-dir=" + PROFILE
        );

        options.addArguments(
                "--profile-directory=" + CHROME_PROFILE_DIR
        );

        options.addArguments(
                "--start-maximized"
        );

        options.addArguments(
                "--disable-extensions"
        );

        options.addArguments(
                "--remote-allow-origins=*"
        );

        // Selenium Manager automatically
        // finds the correct ChromeDriver.
        WebDriver driver =
                new ChromeDriver(options);

        return driver;
    }

    private static void openClassroomHome(
            WebDriver driver)
            throws InterruptedException {

        System.out.println(
                "\nOpening Google Classroom home..."
        );

        driver.get(HOME_URL);

        /*
         * If the profile is not signed in, Google
         * bounces to accounts.google.com. Give the
         * user time to sign in manually in the
         * Selenium window instead of failing.
         */
        if (isSignInPage(driver)) {
            waitForManualSignIn(driver);

            driver.get(HOME_URL);
        }

        WebDriverWait wait =
                new WebDriverWait(
                        driver,
                        Duration.ofSeconds(30)
                );

        wait.until(
                ExpectedConditions
                        .presenceOfElementLocated(
                                By.tagName("body")
                        )
        );

        System.out.println(
                "Classroom home loaded."
        );
    }

    private static boolean isSignInPage(
            WebDriver driver) {

        String url = driver.getCurrentUrl();

        return url != null
                && (url.contains("accounts.google.com")
                    || url.contains("ServiceLogin"));
    }

    private static void waitForManualSignIn(
            WebDriver driver)
            throws InterruptedException {

        System.out.println("\n======================================");
        System.out.println(" SIGN-IN REQUIRED");
        System.out.println(" Sign in with your email and password");
        System.out.println(" in the Selenium Chrome window.");
        System.out.println(" Waiting up to 5 minutes...");
        System.out.println("======================================\n");

        long deadline =
                System.currentTimeMillis()
                        + Duration.ofMinutes(5).toMillis();

        while (System.currentTimeMillis() < deadline) {

            if (!isSignInPage(driver)) {
                System.out.println("Signed in. Continuing...");
                return;
            }

            Thread.sleep(3000);
        }

        throw new IllegalStateException(
                "Sign-in was not completed within 5 minutes."
        );
    }

    private static List<CourseInfo> getCourses(
            WebDriver driver) {

        List<CourseInfo> courses =
                new ArrayList<>();

        try {

            // Wait for class cards to render.
            WebDriverWait wait =
                    new WebDriverWait(
                            driver,
                            Duration.ofSeconds(20)
                    );

            wait.until(
                    ExpectedConditions
                            .presenceOfElementLocated(
                                    By.cssSelector(
                                            "a[href*='/c/']"
                                    )
                            )
            );

            Thread.sleep(2000);

            /*
             * Large accounts lazy-load class cards
             * as the page scrolls; scroll until the
             * card count stops growing so ALL
             * courses (80+) are captured, not just
             * the first screenful.
             */
            scrollCourseList(driver);

            String script =
                    """
                    const anchors = Array.from(
                        document.querySelectorAll(
                            "a[href*='/c/']"
                        )
                    );

                    const seen = new Set();
                    const out = [];

                    for (const a of anchors) {

                        const match =
                            a.href.match(/\\/c\\/([A-Za-z0-9_-]+)/);

                        if (!match || seen.has(match[1])) {
                            continue;
                        }

                        const name =
                            (a.innerText || '')
                                .split('\\n')[0]
                                .trim();

                        if (!name || name.length < 2) {
                            continue;
                        }

                        seen.add(match[1]);
                        out.push({
                            id: match[1],
                            name: name
                        });
                    }

                    return out;
                    """;

            Object result =
                    ((JavascriptExecutor) driver)
                            .executeScript(script);

            if (!(result instanceof List<?>)) {
                return courses;
            }

            for (Object item :
                    (List<?>) result) {

                if (!(item instanceof Map)) {
                    continue;
                }

                Map<?, ?> map =
                        (Map<?, ?>) item;

                CourseInfo course =
                        new CourseInfo();

                course.id =
                        safeString(map.get("id"));

                course.name =
                        safeString(map.get("name"));

                if (!course.id.isEmpty()) {
                    courses.add(course);
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Error reading classroom list."
            );

            e.printStackTrace();
        }

        return courses;
    }

    /*
     * Scrolls the Classroom home page repeatedly
     * until no new class cards appear, so every
     * course on a large account is loaded into
     * the DOM before the list is read.
     */
    private static void scrollCourseList(
            WebDriver driver)
            throws InterruptedException {

        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        int previousCount = -1;

        int stalls = 0;

        for (int i = 0; i < 200; i++) {

            Object count = js.executeScript(
                    "return document.querySelectorAll(\"a[href*='/c/']\").length;"
            );

            int current =
                    count instanceof Number
                            ? ((Number) count).intValue()
                            : 0;

            if (current == previousCount) {

                stalls++;

                if (stalls >= 3) {
                    break;
                }

            } else {

                stalls = 0;
                previousCount = current;
            }

            js.executeScript(
                    """
                    const elements =
                        Array.from(document.querySelectorAll('*'));

                    let target = null;
                    let maxHeight = 0;

                    for (const el of elements) {

                        if (
                            el.scrollHeight >
                            el.clientHeight &&
                            el.scrollHeight > maxHeight
                        ) {
                            target = el;
                            maxHeight = el.scrollHeight;
                        }
                    }

                    if (target) {
                        target.scrollTop +=
                            target.clientHeight * 0.8;
                    } else {
                        window.scrollBy(0, 800);
                    }
                    """
            );

            Thread.sleep(600);
        }
    }

    /*
     * Course names use the BB1/BB2 prefix but the
     * spreadsheets are organised as B1/B2, so the
     * tab name drops the leading extra "B":
     * "BB1P3-..." -> "B1P3-...".
     */
    private static String normalizeTabPrefix(
            String name) {

        if (name == null) {
            return "";
        }

        String upper =
                name.trim().toUpperCase();

        if (upper.startsWith("BB1")) {
            return "B1" + name.substring(3);
        }

        if (upper.startsWith("BB2")) {
            return "B2" + name.substring(3);
        }

        return name;
    }

    /*
     * Google Sheets tab rules: no [ ] * / ? \ :,
     * max 100 chars, no leading/trailing
     * apostrophes.
     */
    private static String sanitizeTabName(
            String name) {

        String cleaned = name == null
                ? ""
                : name.replaceAll(
                        "[\\[\\]*/?:\\\\]",
                        "-").trim();

        cleaned = cleaned.replaceAll("^'+|'+$",
                "");

        if (cleaned.length() > 100) {
            cleaned =
                    cleaned.substring(0, 100);
        }

        if (cleaned.isEmpty()) {
            cleaned = "Course";
        }

        return cleaned;
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

    private static boolean clickGradesTab(
            WebDriver driver) {

        try {

            WebDriverWait wait =
                    new WebDriverWait(
                            driver,
                            Duration.ofSeconds(30)
                    );

            WebElement grades =
                    wait.until(
                            ExpectedConditions
                                    .elementToBeClickable(
                                            By.xpath(
                                                    "//a[contains(@href,'/gb')]"
                                            )
                                    )
                    );

            ((JavascriptExecutor) driver)
                    .executeScript(
                            "arguments[0].click();",
                            grades
                    );

            System.out.println(
                    "Grades tab clicked."
            );

            return true;

        } catch (Exception e) {

            System.out.println(
                    "Grades tab not found."
            );

            return false;
        }
    }

    private static void waitForGradebook(
            WebDriver driver) throws InterruptedException {

        WebDriverWait wait =
                new WebDriverWait(
                        driver,
                        Duration.ofSeconds(30)
                );

        wait.until(
                ExpectedConditions
                        .presenceOfElementLocated(
                                By.cssSelector(
                                        "tbody tr, [role='row']"
                                )
                        )
        );

        // Wait for column headers to finish
        // loading ("Loading more assignments"
        // disappears when the last column is in).
        waitUntilLoaded(driver, 60);

        System.out.println(
                "Gradebook loaded."
        );
    }

    /*
     * Polls until Classroom's lazy loading is
     * finished. Uses textContent on grid cells
     * because innerText EXCLUDES off-screen
     * content, so placeholders hidden beyond the
     * right edge would otherwise go unnoticed.
     * Hidden (display:none) cells are skipped:
     * the grid keeps a permanent invisible
     * sentinel column that never loads.
     */
    private static void waitUntilLoaded(
            WebDriver driver,
            int timeoutSeconds)
            throws InterruptedException {

        long deadline =
                System.currentTimeMillis()
                        + timeoutSeconds * 1000L;

        while (System.currentTimeMillis()
                < deadline) {

            int loadingCells =
                    countLoadingCells(driver);

            if (loadingCells == 0) {
                return;
            }

            Thread.sleep(1000);
        }

        System.out.println(
                "Warning: page still loading after "
                        + timeoutSeconds + "s ("
                        + countLoadingCells(driver)
                        + " cells pending)."
        );
    }

    private static int countLoadingCells(
            WebDriver driver) {

        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        Object count =
                js.executeScript(
                        """
                        const cells = document.querySelectorAll(
                            'td, th, [role="gridcell"], [role="cell"], [role="columnheader"]'
                        );

                        let pending = 0;

                        for (const c of cells) {

                            if (c.getClientRects().length === 0) {
                                continue;
                            }

                            const t =
                                c.textContent || '';

                            if (t.includes(
                                    'Loading submission details')
                                    || t.includes(
                                    'Loading more assignments')) {
                                pending++;
                            }
                        }

                        return pending;
                        """
                );

        return count instanceof Number
                ? ((Number) count).intValue()
                : 0;
    }

    // ============================================================
    // ASSIGNMENTS
    // ============================================================

    private static List<AssignmentColumn> getAssignments(
            WebDriver driver) {

        List<AssignmentColumn> assignments =
                new ArrayList<>();

        try {

            // Return every header cell with its raw
            // position so data cells can be matched
            // by index, not by guesswork. Hidden
            // cells (the grid's permanent sentinel
            // column) are excluded so header and
            // data indexes stay aligned.
            String script =
                    """
                    return Array.from(
                        document.querySelectorAll(
                            'thead th, [role="columnheader"]'
                        )
                    ).filter(e =>
                        e.getClientRects().length > 0
                    ).map((e, index) => ({
                        text: e.innerText.trim(),
                        index: index
                    }));
                    """;

            Object result =
                    ((JavascriptExecutor) driver)
                            .executeScript(script);

            if (result instanceof List<?>) {

                for (Object item :
                        (List<?>) result) {

                    if (!(item instanceof Map)) {
                        continue;
                    }

                    Map<?, ?> map =
                            (Map<?, ?>) item;

                    String raw =
                            safeString(map.get("text"));

                    // Header cells contain DOM noise
                    // like "No due date" and
                    // "more_vert" on separate lines;
                    // keep the meaningful lines only.
                    StringBuilder cleaned =
                            new StringBuilder();

                    for (String line :
                            raw.split("\n")) {

                        String part =
                                line.trim();

                        if (part.isEmpty()
                                || part.equalsIgnoreCase("No due date")
                                || part.equalsIgnoreCase("more_vert")
                                || part.matches("out of \\d+")) {
                            continue;
                        }

                        if (cleaned.length() > 0) {
                            cleaned.append(" ");
                        }

                        cleaned.append(part);
                    }

                    String text =
                            cleaned.toString().trim();

                    if (text.isEmpty()) {
                        continue;
                    }

                    // Sort dropdown captured as a
                    // header cell; not an assignment.
                    if (text.contains("Sort by")) {
                        continue;
                    }

                    // Loading spinner placeholder
                    // captured as a phantom column.
                    if (text.contains(
                            "Loading more assignments")) {
                        continue;
                    }

                    if (text.equalsIgnoreCase(
                            "Sort by first name")) {
                        continue;
                    }

                    if (text.equalsIgnoreCase(
                            "Students")) {
                        continue;
                    }

                    boolean duplicate = false;

                    for (AssignmentColumn existing :
                            assignments) {

                        if (existing.title.equals(
                                text)) {
                            duplicate = true;
                            break;
                        }
                    }

                    if (duplicate) {
                        continue;
                    }

                    AssignmentColumn column =
                            new AssignmentColumn();

                    column.title = text;
                    column.cellIndex =
                            ((Number) map.get("index"))
                                    .intValue();

                    assignments.add(column);
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Error reading assignments."
            );

            e.printStackTrace();
        }

        return assignments;
    }

    // ============================================================
    // STUDENT EXTRACTION
    // ============================================================

    private static List<StudentRawData> extractStudents(
            WebDriver driver) {

        List<StudentRawData> students =
                new ArrayList<>();

        try {

            scrollGradebook(driver);

            // Cells load lazily as rows scroll
            // into view; make sure none still
            // show "Loading submission details".
            waitUntilLoaded(driver, 60);

            String script =
                    """
                    const rows = Array.from(
                        document.querySelectorAll(
                            'tbody tr, [role="row"]'
                        )
                    ).filter(row =>
                        row.getClientRects().length > 0
                    );

                    return rows.map(row => {

                        const cells =
                            Array.from(
                                row.querySelectorAll(
                                    'th, td, [role="rowheader"], [role="gridcell"], [role="cell"]'
                                )
                            ).filter(cell =>
                                cell.getClientRects().length > 0
                            );

                        const links =
                            Array.from(
                                row.querySelectorAll('a')
                            );

                        let name = '';

                        for (const a of links) {

                            const text =
                                (a.innerText || '')
                                .trim();

                            if (
                                text &&
                                !text.includes('@') &&
                                text.length > 2
                            ) {
                                name = text;
                                break;
                            }
                        }

                        if (!name) {

                            const firstCell =
                                cells.length
                                    ? cells[0]
                                    : null;

                            if (firstCell) {
                                name =
                                    firstCell.innerText
                                        .trim();
                            }
                        }

                        const emailElement =
                            row.querySelector(
                                '[data-email], [aria-label*="@"]'
                            );

                        let email =
                            emailElement
                                ? (
                                    emailElement
                                        .getAttribute(
                                            'data-email'
                                        ) ||
                                    emailElement
                                        .getAttribute(
                                            'aria-label'
                                        ) ||
                                    emailElement.innerText ||
                                    ''
                                ).trim()
                                : '';

                        // Fallback: scan every
                        // attribute in the row for
                        // an email-looking value.
                        if (!email) {

                            const emailPattern =
                                /[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}/;

                            for (const el of Array.from(row.querySelectorAll('*'))) {

                                for (const attr of Array.from(el.attributes)) {

                                    const match =
                                        attr.value.match(emailPattern);

                                    if (match) {
                                        email = match[0];
                                        break;
                                    }
                                }

                                if (email) break;
                            }
                        }

                        return {
                            name: name,
                            email: email,
                            cells: cells.map(cell => {

                                const attributes = [];

                                for (
                                    const attr
                                    of Array.from(cell.attributes)
                                ) {
                                    attributes.push(
                                        attr.name +
                                        '=' +
                                        attr.value
                                    );
                                }

                                return {
                                    text:
                                        cell.innerText || '',

                                    ariaLabel:
                                        cell.getAttribute(
                                            'aria-label'
                                        ) || '',

                                    title:
                                        cell.getAttribute(
                                            'title'
                                        ) || '',

                                    tooltip:
                                        cell.getAttribute(
                                            'data-tooltip'
                                        ) || '',

                                    childAttributes:
                                        Array.from(
                                            cell.querySelectorAll('*')
                                        ).flatMap(el =>
                                            Array.from(
                                                el.attributes
                                            ).map(
                                                attr =>
                                                    attr.name +
                                                    '=' +
                                                    attr.value
                                            )
                                        )
                                };
                            })
                        };
                    });
                    """;

            Object result =
                    ((JavascriptExecutor) driver)
                            .executeScript(script);

            if (!(result instanceof List<?>)) {
                return students;
            }

            for (Object rowObject :
                    (List<?>) result) {

                if (!(rowObject instanceof Map)) {
                    continue;
                }

                Map<?, ?> row =
                        (Map<?, ?>) rowObject;

                String name =
                        safeString(row.get("name"));

                if (name.isEmpty()) {
                    continue;
                }

                if (name.equalsIgnoreCase(
                        "Sort by first name")) {
                    continue;
                }

                // Gradebook placeholder/summary
                // row, not a real student.
                if (name.equalsIgnoreCase(
                        "N/A")
                        || name.equalsIgnoreCase(
                        "Class average")) {
                    continue;
                }

                // Unloaded placeholder row
                // ("...Loading submission details").
                if (name.toLowerCase()
                        .contains("loading")) {
                    continue;
                }

                StudentRawData student =
                        new StudentRawData();

                student.name = name;
                student.email =
                        safeString(row.get("email"));

                Object cellsObject =
                        row.get("cells");

                if (cellsObject instanceof List<?>) {

                    for (Object cellObject :
                            (List<?>) cellsObject) {

                        if (!(cellObject instanceof Map)) {
                            continue;
                        }

                        Map<?, ?> cell =
                                (Map<?, ?>) cellObject;

                        ClassroomCellData cellData =
                                new ClassroomCellData();

                        cellData.text =
                                safeString(
                                        cell.get("text")
                                );

                        cellData.ariaLabel =
                                safeString(
                                        cell.get("ariaLabel")
                                );

                        cellData.title =
                                safeString(
                                        cell.get("title")
                                );

                        cellData.tooltip =
                                safeString(
                                        cell.get("tooltip")
                                );

                        Object attrs =
                                cell.get(
                                        "childAttributes"
                                );

                        if (attrs instanceof List<?>) {

                            for (Object attr :
                                    (List<?>) attrs) {

                                cellData.childAttributes
                                        .add(
                                                safeString(attr)
                                        );
                            }
                        }

                        student.cells.add(cellData);
                    }
                }

                students.add(student);
            }

        } catch (Exception e) {

            System.out.println(
                    "Student extraction error."
            );

            e.printStackTrace();
        }

        return students;
    }

    /*
     * Scrolls the gradebook fully so Classroom's
     * lazy loading completes: first vertically
     * (rows load on downward scroll), then
     * horizontally (right-most columns load only
     * when scrolled into view), repeating until
     * no grid cell is left in a loading state.
     */
    private static void scrollGradebook(
            WebDriver driver)
            throws InterruptedException {

        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        String lastHeight = "";

        // Vertical pass
        for (int i = 0; i < 20; i++) {

            String height =
                    (String) js.executeScript(
                            """
                            const elements =
                                Array.from(document.querySelectorAll('*'));

                            let target = null;
                            let maxHeight = 0;

                            for (const el of elements) {

                                if (
                                    el.scrollHeight >
                                    el.clientHeight &&
                                    el.scrollHeight > maxHeight
                                ) {
                                    target = el;
                                    maxHeight = el.scrollHeight;
                                }
                            }

                            if (target) {
                                target.scrollTop =
                                    target.scrollHeight;
                                return String(maxHeight);
                            }

                            return '0';
                            """
                    );

            Thread.sleep(700);

            if (height.equals(lastHeight)
                    && !"0".equals(height)) {
                break;
            }

            lastHeight = height;
        }

        // Horizontal pass - Classroom now ignores
        // synthetic scrolls (scrollLeft and even
        // scrollIntoView); only REAL wheel events
        // trigger the lazy column loading.
        try {

            WebElement scroller = (WebElement)
                    js.executeScript(
                            """
                            let target = null;
                            let maxOverflow = 0;

                            for (const el of
                                document.querySelectorAll('*')) {

                                const overflow =
                                    el.scrollWidth
                                    - el.clientWidth;

                                if (overflow > 4
                                        && overflow > maxOverflow) {
                                    target = el;
                                    maxOverflow = overflow;
                                }
                            }

                            return target;
                            """
                    );

            if (scroller != null) {

                Actions actions =
                        new Actions(driver);

                actions.moveToElement(
                        scroller
                ).perform();

                long previousLeft = -1;

                int stalls = 0;

                for (int i = 0; i < 60; i++) {

                    if (countLoadingCells(driver) == 0) {
                        break;
                    }

                    actions.scrollFromOrigin(
                            WheelInput.ScrollOrigin
                                    .fromElement(scroller),
                            800,
                            0
                    ).perform();

                    Thread.sleep(700);

                    Object left =
                            js.executeScript(
                                    "return arguments[0].scrollLeft;",
                                    scroller
                            );

                    long current =
                            left instanceof Number
                                    ? ((Number) left)
                                            .longValue()
                                    : -1;

                    // Stop when the container cannot
                    // scroll any further.
                    if (current == previousLeft) {

                        stalls++;

                        if (stalls >= 4) {
                            break;
                        }

                    } else {
                        stalls = 0;
                    }

                    previousLeft = current;
                }
            }

        } catch (Exception e) {

            System.out.println(
                    "Wheel scrolling failed: " + e
            );
        }

        // Fallback - legacy scrollLeft nudge in
        // case the wheel events did not reach the
        // grid container.
        for (int i = 0; i < 10; i++) {

            if (countLoadingCells(driver) == 0) {
                break;
            }

            js.executeScript(
                    """
                    const wide = Array.from(
                        document.querySelectorAll('*')
                    ).filter(el =>
                        el.scrollWidth > el.clientWidth + 4
                    );

                    for (const el of wide) {
                        el.scrollLeft = el.scrollWidth;
                    }
                    """
            );

            Thread.sleep(800);
        }

        // Final safety wait for stragglers.
        waitUntilLoaded(driver, 60);
    }

    // ============================================================
    // STUDENT PROCESSING
    // ============================================================

    private static List<StudentData>
    processStudents(
            List<StudentRawData> rawStudents,
            List<AssignmentColumn> assignmentColumns) {

        // Pure in-memory string parsing; a plain
        // loop is simpler and just as fast.
        List<StudentData> students =
                new ArrayList<>();

        for (StudentRawData raw :
                rawStudents) {

            try {

                students.add(
                        processStudent(
                                raw,
                                assignmentColumns
                        )
                );

            } catch (Exception e) {

                System.out.println(
                        "Student processing failed for "
                                + raw.name + ": "
                                + e
                );
            }
        }

        return students;
    }

    private static StudentData processStudent(
            StudentRawData raw,
            List<AssignmentColumn> assignmentColumns) {

        StudentData student =
                new StudentData();

        student.name = raw.name;
        student.email = raw.email;
       String [] seperation =student.name.split(" \\(");
       student.name=seperation[0].trim();
       student.rollNumber=seperation[1].replace(")"," ").trim();

        for (AssignmentColumn column :
                assignmentColumns) {

            AssignmentResult result =
                    new AssignmentResult();

            ClassroomCellData cell =
                    column.cellIndex < raw.cells.size()
                            ? raw.cells.get(column.cellIndex)
                            : null;

            if (cell != null) {

                String combined =
                        buildCombinedText(cell);

                // A cell still showing its loading
                // placeholder must NOT be exported
                // as a real "Assigned" status - mark
                // it so bad data is visible.
                String lower =
                        combined.toLowerCase();

                if (lower.contains(
                        "loading submission")
                        || lower.contains(
                        "loading more")) {

                    result.grade = "";

                    result.status = "NotLoaded";

                } else {

                    result.grade =
                            extractGrade(
                                    combined,
                                    cell.text
                            );

                    result.status =
                            extractStatus(combined);
                }

            } else {

                // Blank gradebook cell = work
                // assigned, nothing submitted.
                result.status =
                        "Assigned";
            }

            student.assignments.add(result);
        }

        return student;
    }

    // ============================================================
    // CELL PARSING
    // ============================================================

    private static String buildCombinedText(
            ClassroomCellData cell) {

        StringBuilder builder =
                new StringBuilder();

        addText(builder, cell.text);
        addText(builder, cell.ariaLabel);
        addText(builder, cell.title);
        addText(builder, cell.tooltip);

        /*
         * Child-element attributes are deliberately
         * NOT included: every gradebook cell embeds
         * hidden controller markup whose attributes
         * contain status words ("Turned in" etc.)
         * regardless of the real state, which made
         * every student export as "Submitted".
         */

        return builder.toString()
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();
    }

    private static void addText(
            StringBuilder builder,
            String text) {

        if (text != null &&
                !text.trim().isEmpty()) {

            builder.append(" ")
                    .append(text.trim());
        }
    }

    private static String extractGrade(
            String text,
            String visibleText) {

        if (text == null) {
            return "";
        }

        String normalized =
                text.replace(",", " ")
                        .trim();

        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile(
                        "(?<!\\d)(\\d+(?:\\.\\d+)?)\\s*(?:/|out\\s+of)\\s*\\d+(?!\\d)",
                        java.util.regex.Pattern.CASE_INSENSITIVE
                );

        java.util.regex.Matcher matcher =
                pattern.matcher(normalized);

        if (matcher.find()) {

            return matcher.group(1);
        }

        /*
         * Bare-number fallback: only trust a plain
         * number when the visible cell text itself
         * is just that number. Scanning the combined
         * text would match dates, tabindex=0, ids,
         * etc. and export fake grades.
         */
        String visible =
                visibleText == null
                        ? ""
                        : visibleText.trim();

        if (visible.matches("\\d+(\\.\\d+)?")) {
            return visible;
        }

        return "";
    }

    private static String extractStatus(
            String text) {

        if (text == null ||
                text.trim().isEmpty()) {

            // Blank cell = assigned only.
            return "Assigned";
        }

        String value =
                text.toLowerCase();

        if (value.contains("turned in")) {
            return "Submitted";
        }

        if (value.contains("missing")) {
            return "Assigned";
        }

        // Late means the work WAS turned in,
        // just after the due date.
        if (value.contains("late")) {
            return "Submitted";
        }

        if (value.contains("returned")) {
            return "Returned";
        }

        if (value.contains("assigned")) {
            return "Assigned";
        }

        if (value.contains("excused")) {
            return "Excused";
        }

        if (value.contains("draft")) {
            return "Draft";
        }

        // Classroom shows "No grade" when work was
        // turned in but not graded yet; export it
        // as Submitted.
        if (value.contains("no grade")) {
            return "Submitted";
        }

        if (value.contains("graded")) {
            return "Graded";
        }

        // Classroom renders un-submitted cells
        // as blank or "Not submitted"; both
        // mean the work was only assigned.
        if (value.contains("not submitted")) {
            return "Assigned";
        }

        return "Assigned";
    }

    // ============================================================
    // UTILITIES
    // ============================================================

    private static String safeString(
            Object object) {

        return object == null
                ? ""
                : String.valueOf(object).trim();
    }

    private static boolean isBrowserAlive(
            WebDriver driver) {

        try {

            driver.getWindowHandle();

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // ============================================================
    // DATA CLASSES
    // ============================================================

    private static class CourseInfo {

        String name;
        String id;
    }

    private static class StudentRawData {

        String name;
        String email;

        List<ClassroomCellData> cells =
                new ArrayList<>();
    }

    private static class AssignmentColumn {

        String title;
        int cellIndex;
    }

    private static class ClassroomCellData {

        String text = "";
        String ariaLabel = "";
        String title = "";
        String tooltip = "";

        List<String> childAttributes =
                new ArrayList<>();
    }

    private static class StudentData {

        String name;
        String email;
        String rollNumber;
        List<AssignmentResult> assignments =
                new ArrayList<>();
    }

    private static class AssignmentResult {

        String grade = "";

        String status =
                "Not submitted";
    }





}