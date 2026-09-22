package com.bridgelabz.service;

import com.bridgelabz.models.CourseInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bridgelabz.ClassroomMultiExport.safeString;

public class CourseService {

    private static final String SPREADSHEET_ID_B1 =
            "1_ptnLvsphtf2WAER_oOBwCMcunKmvbguer0u_GUk_F8";
    private static final String SPREADSHEET_ID_B2 =
            "1waRl-38hE8NEROWbnTcJItDs8DuKnl6n-t107MWWa5w";


    private static final String HOME_URL =
            "https://classroom.google.com/u/0/";

    public  static void openClassroomHome(
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

    public  static boolean isSignInPage(
            WebDriver driver) {

        String url = driver.getCurrentUrl();

        return url != null
                && (url.contains("accounts.google.com")
                || url.contains("ServiceLogin"));
    }

    public  static void waitForManualSignIn(
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

    public  static List<CourseInfo> getCourses(
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
    public  static void scrollCourseList(
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
    public  static String normalizeTabPrefix(
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
    public  static String sanitizeTabName(
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

    /*
     * Routes a course to its target spreadsheet
     * by name prefix: B1/BB1 -> B1 file,
     * B2/BB2 -> B2 file, anything else -> null
     * (course is skipped).
     */
    public static String routeSpreadsheetId(
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


}
