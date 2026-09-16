package com.example;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.api.services.sheets.v4.model.CellData;

import java.io.FileInputStream;
import java.util.*;

/**
 * Temporary verification : writes known values to a
 * scratch sheet, reads them back, prints MATCH/MISMATCH,
 * then deletes the scratch sheet.
 */
public class TestSheetsRoundTrip {

    private static final String SPREADSHEET_ID =
            "1jgmFkPIFFp71c2EPWMwCXOJ6bG_irJYxFn1rSAoTf_I";

    private static final String SERVICE_ACCOUNT_FILE =
            "service_account.json";

    public static void main(String[] args) throws Exception {

        Sheets sheets = createSheetsService();

        String sheetName = "TEST_" + System.currentTimeMillis();

        // 0. Create the scratch sheet (values.update does
        //    not create tabs on its own)
        AddSheetRequest addSheet = new AddSheetRequest()
                .setProperties(new SheetProperties().setTitle(sheetName));
        sheets.spreadsheets().batchUpdate(SPREADSHEET_ID,
                new BatchUpdateSpreadsheetRequest().setRequests(
                        Collections.singletonList(new Request().setAddSheet(addSheet))))
                .execute();
        System.out.println("CREATED: " + sheetName);

        // 1. Write known values
        List<List<Object>> values = new ArrayList<>();
        values.add(Arrays.asList("Student Name", "Email", "Test1 - Grade", "Test1 - Status"));
        values.add(Arrays.asList("TEST_STUDENT_A", "a@test.com", "8", "Turned in"));
        values.add(Arrays.asList("TEST_STUDENT_B", "b@test.com", "", "Missing"));

        sheets.spreadsheets().values()
                .update(SPREADSHEET_ID, sheetName + "!A1",
                        new ValueRange().setValues(values))
                .setValueInputOption("USER_ENTERED")
                .execute();
        System.out.println("WROTE: 3 rows to " + sheetName);

        // 2. Read back
        ValueRange readBack = sheets.spreadsheets().values()
                .get(SPREADSHEET_ID, sheetName + "!A1:D3")
                .execute();

        List<List<Object>> got = readBack.getValues();
        boolean ok = got != null
                && got.size() == 3
                && "TEST_STUDENT_A".equals(got.get(1).get(0))
                && "8".equals(got.get(1).get(2))
                && "Turned in".equals(got.get(1).get(3))
                && "TEST_STUDENT_B".equals(got.get(2).get(0))
                && "Missing".equals(got.get(2).get(3));

        System.out.println("READBACK: " + got);

        // 3. Cleanup scratch sheet
        Spreadsheet spreadsheet =
                sheets.spreadsheets().get(SPREADSHEET_ID).execute();
        Integer testSheetId = null;
        for (Sheet s : spreadsheet.getSheets()) {
            if (s.getProperties().getTitle().equals(sheetName)) {
                testSheetId = s.getProperties().getSheetId();
            }
        }
        if (testSheetId != null) {
            sheets.spreadsheets().batchUpdate(SPREADSHEET_ID,
                    new BatchUpdateSpreadsheetRequest().setRequests(
                            Collections.singletonList(new Request().setDeleteSheet(
                                    new DeleteSheetRequest().setSheetId(testSheetId)))))
                    .execute();
            System.out.println("CLEANUP: deleted " + sheetName);
        }

        System.out.println(ok
                ? "RESULT: PASS - data round-tripped through the spreadsheet"
                : "RESULT: FAIL - readback did not match");
        if (!ok) {
            System.exit(1);
        }
    }

    private static Sheets createSheetsService() throws Exception {
        try (FileInputStream input =
                     new FileInputStream(SERVICE_ACCOUNT_FILE)) {
            GoogleCredential credential =
                    GoogleCredential.fromStream(input)
                            .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
            return new Sheets.Builder(
                    credential.getTransport(),
                    credential.getJsonFactory(),
                    credential)
                    .setApplicationName("Classroom Grade Exporter")
                    .build();
        }
    }
}
