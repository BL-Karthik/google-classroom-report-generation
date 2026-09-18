package com.bridgelabz.service;

import com.bridgelabz.models.AssignmentColumn;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bridgelabz.ClassroomMultiExport.safeString;

public class AssignmentService {

    // ============================================================
    // ASSIGNMENTS
    // ============================================================

    public static List<AssignmentColumn> getAssignments(
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
}
