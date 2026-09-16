package com.example;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DeleteSheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;

import java.io.FileInputStream;
import java.util.Collections;

/**
 * Deletes leftover tabs named with the old
 * BB1/BB2 prefix from both export spreadsheets.
 */
public class DeleteStaleTabs {

    private static final String SPREADSHEET_ID_B1 =
            "1fZsJPXt_6V5e7KnHO0MLE_ihbwrXtI3Ae0Md060bD2Q";

    private static final String SPREADSHEET_ID_B2 =
            "1jgmFkPIFFp71c2EPWMwCXOJ6bG_irJYxFn1rSAoTf_I";

    public static void main(String[] args) throws Exception {
        try (FileInputStream input = new FileInputStream("service_account.json")) {
            GoogleCredential cred = GoogleCredential.fromStream(input)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
            Sheets sheets = new Sheets.Builder(cred.getTransport(), cred.getJsonFactory(), cred)
                    .setApplicationName("stale tab cleanup").build();

            deleteStale(sheets, "B1", SPREADSHEET_ID_B1);
            deleteStale(sheets, "B2", SPREADSHEET_ID_B2);
        }
    }

    private static void deleteStale(Sheets sheets, String label, String spreadsheetId)
            throws Exception {
        System.out.println("\n=== " + label + " ===");
        Spreadsheet spreadsheet = sheets.spreadsheets().get(spreadsheetId).execute();
        for (Sheet sheet : spreadsheet.getSheets()) {
            String title = sheet.getProperties().getTitle();
            String upper = title.toUpperCase();
            if (upper.startsWith("BB1") || upper.startsWith("BB2")) {
                System.out.println("Deleting stale tab: " + title);
                sheets.spreadsheets().batchUpdate(spreadsheetId,
                        new BatchUpdateSpreadsheetRequest().setRequests(
                                Collections.singletonList(new Request().setDeleteSheet(
                                        new DeleteSheetRequest()
                                                .setSheetId(sheet.getProperties().getSheetId())
                                ))
                        )).execute();
            } else {
                System.out.println("Keeping tab: " + title);
            }
        }
    }
}
