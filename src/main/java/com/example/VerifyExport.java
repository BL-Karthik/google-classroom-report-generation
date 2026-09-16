package com.example;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.FileInputStream;
import java.util.*;

/** Reads back the B1 and B2 export spreadsheets. */
public class VerifyExport {

    private static final String SPREADSHEET_ID_B1 =
            "1fZsJPXt_6V5e7KnHO0MLE_ihbwrXtI3Ae0Md060bD2Q";

    private static final String SPREADSHEET_ID_B2 =
            "1jgmFkPIFFp71c2EPWMwCXOJ6bG_irJYxFn1rSAoTf_I";

    public static void main(String[] args) throws Exception {
        try (FileInputStream input = new FileInputStream("service_account.json")) {
            GoogleCredential cred = GoogleCredential.fromStream(input)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
            Sheets sheets = new Sheets.Builder(cred.getTransport(), cred.getJsonFactory(), cred)
                    .setApplicationName("verify").build();

            String[] targets = {"B1: " + SPREADSHEET_ID_B1, "B2: " + SPREADSHEET_ID_B2};

            for (String target : targets) {
                String id = target.substring(target.indexOf(':') + 2);
                System.out.println("\n########## SPREADSHEET " + target + " ##########");
                if (id.contains("HERE")) {
                    System.out.println("(not configured)");
                    continue;
                }
                Spreadsheet spreadsheet =
                        sheets.spreadsheets().get(id).execute();
                System.out.println("TABS: " + spreadsheet.getSheets().size());
                for (Sheet sheet : spreadsheet.getSheets()) {
                    String title = sheet.getProperties().getTitle();
                    ValueRange vr = sheets.spreadsheets().values()
                            .get(id, "'" + title + "'!A1:AZ3").execute();
                    List<List<Object>> rows = vr.getValues();
                    System.out.println("\n=== TAB: " + title + " ===");
                    if (rows == null || rows.isEmpty()) {
                        System.out.println("(empty)");
                        continue;
                    }
                    System.out.println("HEADER COLS: " + rows.get(0).size());
                    System.out.println("HEADER: " + rows.get(0));
                    for (int i = 1; i < Math.min(3, rows.size()); i++) {
                        System.out.println("ROW" + i + ": " + rows.get(i));
                    }
                }
            }
        }
    }
}
