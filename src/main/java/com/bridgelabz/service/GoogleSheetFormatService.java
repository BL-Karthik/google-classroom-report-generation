package com.bridgelabz.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GoogleSheetFormatService {

    // ============================================================
    // ENSURE SHEET HAS ENOUGH ROWS AND COLUMNS
    // ============================================================

    public static void ensureSheetSize(
            Sheets sheets,
            String spreadsheetId,
            int sheetId,
            int requiredRows,
            int requiredColumns) throws Exception {

        Spreadsheet spreadsheet =
                sheets.spreadsheets()
                        .get(spreadsheetId)
                        .execute();

        if (spreadsheet.getSheets() == null) {
            return;
        }

        for (Sheet sheet : spreadsheet.getSheets()) {

            SheetProperties properties =
                    sheet.getProperties();

            if (properties == null ||
                    properties.getSheetId() != sheetId) {
                continue;
            }

            GridProperties gridProperties =
                    properties.getGridProperties();

            if (gridProperties == null) {
                gridProperties =
                        new GridProperties()
                                .setRowCount(1000)
                                .setColumnCount(26);
            }

            int currentRows =
                    gridProperties.getRowCount();

            int currentColumns =
                    gridProperties.getColumnCount();

            boolean needsUpdate =
                    currentRows < requiredRows
                            || currentColumns < requiredColumns;

            if (!needsUpdate) {
                return;
            }

            GridProperties newGridProperties =
                    new GridProperties()
                            .setRowCount(
                                    Math.max(
                                            currentRows,
                                            requiredRows
                                    )
                            )
                            .setColumnCount(
                                    Math.max(
                                            currentColumns,
                                            requiredColumns
                                    )
                            );

            SheetProperties newProperties =
                    new SheetProperties()
                            .setSheetId(sheetId)
                            .setGridProperties(
                                    newGridProperties
                            );

            UpdateSheetPropertiesRequest updateRequest =
                    new UpdateSheetPropertiesRequest()
                            .setProperties(newProperties)
                            .setFields(
                                    "gridProperties.rowCount,"
                                            + "gridProperties.columnCount"
                            );

            Request request =
                    new Request()
                            .setUpdateSheetProperties(
                                    updateRequest
                            );

            sheets.spreadsheets()
                    .batchUpdate(
                            spreadsheetId,
                            new BatchUpdateSpreadsheetRequest()
                                    .setRequests(
                                            Collections.singletonList(
                                                    request
                                            )
                                    )
                    )
                    .execute();

            System.out.println(
                    "Sheet resized: "
                            + Math.max(currentRows, requiredRows)
                            + " rows x "
                            + Math.max(
                            currentColumns,
                            requiredColumns
                    )
                            + " columns"
            );

            return;
        }

        throw new IllegalStateException(
                "Sheet ID not found: " + sheetId
        );
    }


    // ============================================================
    // FORMAT INDIVIDUAL COURSE SHEET
    // ============================================================

    public static void formatSheet(
            Sheets sheets,
            String spreadsheetId,
            int sheetId,
            int rows,
            int columns) throws Exception {

        List<Request> requests =
                new ArrayList<>();

        // --------------------------------------------------------
        // BOLD HEADER
        // --------------------------------------------------------

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
                                                new CellData()
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

        // --------------------------------------------------------
        // FREEZE FIRST ROW
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // AUTO RESIZE COLUMNS
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // PERCENTAGE FORMAT
        // Column D
        // --------------------------------------------------------

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
                                                new CellData()
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
    // SUMMARY SHEET FORMAT
    // ============================================================

    public static void summaryFormatSheet(
            Sheets sheets,
            String spreadsheetId,
            int sheetId,
            int rows,
            int columns) throws Exception {

        List<Request> requests =
                new ArrayList<>();

        // --------------------------------------------------------
        // BOLD HEADER
        // --------------------------------------------------------

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
                                                new CellData()
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

        // --------------------------------------------------------
        // FREEZE FIRST ROW
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // AUTO RESIZE
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // PERCENTAGE
        // Column E
        // --------------------------------------------------------

        requests.add(
                new Request()
                        .setRepeatCell(
                                new RepeatCellRequest()
                                        .setRange(
                                                new GridRange()
                                                        .setSheetId(sheetId)
                                                        .setStartRowIndex(1)
                                                        .setEndRowIndex(rows)
                                                        .setStartColumnIndex(4)
                                                        .setEndColumnIndex(5)
                                        )
                                        .setCell(
                                                new CellData()
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

        // --------------------------------------------------------
        // SUBMISSIONS NUMBER FORMAT
        // Column D
        // --------------------------------------------------------

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
                                                new CellData()
                                                        .setUserEnteredFormat(
                                                                new CellFormat()
                                                                        .setNumberFormat(
                                                                                new NumberFormat()
                                                                                        .setType("NUMBER")
                                                                                        .setPattern("0")
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
}
