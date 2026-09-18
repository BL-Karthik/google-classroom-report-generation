package com.bridgelabz.service;

import com.bridgelabz.models.ClassroomCellData;

public class SubmissionService {


    // ============================================================
    // CELL PARSING
    // ============================================================

    public  static String buildCombinedText(
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

    public static void addText(
            StringBuilder builder,
            String text) {

        if (text != null &&
                !text.trim().isEmpty()) {

            builder.append(" ")
                    .append(text.trim());
        }
    }

    public  static String extractGrade(
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

    public  static String extractStatus(
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

}
