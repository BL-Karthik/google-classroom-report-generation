package com.bridgelabz.service;

import com.bridgelabz.models.*;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bridgelabz.ClassroomMultiExport.safeString;
import static com.bridgelabz.service.GradeBookService.waitUntilLoaded;

public class StudentService {
    public  static List<StudentRawData> extractStudents(
            WebDriver driver) {

        List<StudentRawData> students =
                new ArrayList<>();

        try {

            GradeBookService.scrollGradebook(driver);

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


    // ============================================================
    // STUDENT PROCESSING
    // ============================================================

    public static List<StudentData>
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

    public static StudentData processStudent(
            StudentRawData raw,
            List<AssignmentColumn> assignmentColumns) {

        StudentData student =
                new StudentData();

        student.name = raw.name;
        student.email = raw.email;

        String[] separation =
                student.name.split(" \\(");

        student.name =
                separation[0].trim();

        if (separation.length > 1) {
            student.rollNumber =
                    separation[1]
                            .replace(")", "")
                            .trim();
        }

        for (AssignmentColumn column :
                assignmentColumns) {

            AssignmentResult result =
                    new AssignmentResult();

            // IMPORTANT:
            // Store the actual Classroom assignment title.
            result.title = column.title;

            ClassroomCellData cell =
                    column.cellIndex < raw.cells.size()
                            ? raw.cells.get(column.cellIndex)
                            : null;

            if (cell != null) {

                String combined =
                        SubmissionService.buildCombinedText(cell);

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
                            SubmissionService.extractGrade(
                                    combined,
                                    cell.text
                            );

                    result.status =
                            SubmissionService.extractStatus(
                                    combined
                            );
                }

            } else {

                result.status = "Assigned";
            }

            student.assignments.add(result);
        }

        return student;
    }
}
