package com.example;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Diagnostic: opens Classroom with the copied profile,
 * dumps URL, title, links and a screenshot so we can
 * see why course cards are not found.
 */
public class DiagClassroom {

    private static final String PROFILE =
            "C:\\Users\\karth\\SeleniumClassroomMain";

    private static final String CHROME_PROFILE_DIR =
            "Profile 55";

    private static final String HOME_URL =
            "https://classroom.google.com/u/0/";

    public static void main(String[] args)
            throws Exception {

        ChromeOptions options = new ChromeOptions();

        options.addArguments(
                "--user-data-dir=" + PROFILE
        );

        options.addArguments(
                "--profile-directory=" + CHROME_PROFILE_DIR
        );

        options.addArguments("--start-maximized");

        options.addArguments("--disable-extensions");

        options.addArguments("--remote-allow-origins=*");

        WebDriver driver = new ChromeDriver(options);

        try {

            driver.get(HOME_URL);

            Thread.sleep(15000);

            System.out.println("URL:   " + driver.getCurrentUrl());
            System.out.println("TITLE: " + driver.getTitle());

            List<WebElement> links = driver.findElements(
                    By.tagName("a")
            );

            System.out.println("TOTAL LINKS: " + links.size());

            int shown = 0;

            for (WebElement a : links) {

                String href = a.getAttribute("href");

                if (href == null) {
                    continue;
                }

                String text = a.getText().trim();

                if (text.isEmpty()) {
                    text = a.getAttribute("aria-label");
                }

                System.out.println("LINK: " + href
                        + "  |  " + text);

                if (++shown >= 60) {
                    break;
                }
            }

            String body = driver.findElement(
                    By.tagName("body")
            ).getText();

            System.out.println("--- BODY TEXT (first 1500 chars) ---");
            System.out.println(body.substring(
                    0, Math.min(1500, body.length())
            ));

            Path shot = Paths.get("classroom_diag.png");

            Files.write(shot, ((TakesScreenshot) driver)
                    .getScreenshotAs(OutputType.BYTES));

            System.out.println("SCREENSHOT: "
                    + shot.toAbsolutePath());

            Thread.sleep(5000);

        } finally {

            driver.quit();
        }
    }
}
