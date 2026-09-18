package com.bridgelabz.service;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class BrowserService {

    // ============================================================
    // SELENIUM
    // ============================================================
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
    public static WebDriver createDriver() {

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

    public  static boolean isBrowserAlive(
            WebDriver driver) {

        try {

            driver.getWindowHandle();

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}
