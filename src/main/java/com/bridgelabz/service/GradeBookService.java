package com.bridgelabz.service;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.WheelInput;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class GradeBookService {


    public  static boolean clickGradesTab(
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

    public  static void waitForGradebook(
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

//        scrollGradebook(driver);
//        // Wait for any assignments triggered by
//        // scrolling to finish loading.
//        waitUntilLoaded(driver, 60);

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
    public  static void waitUntilLoaded(
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

    public  static int countLoadingCells(
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

    public  static void scrollGradebook(
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
}
