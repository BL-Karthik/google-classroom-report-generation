package com.example;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;

import java.io.FileInputStream;
import java.util.Collections;

/** Throwaway: print titles/tabs of the two candidate spreadsheets. */
public class ProbeTitles {
    public static void main(String[] args) throws Exception {
        try (FileInputStream input = new FileInputStream("service_account.json")) {
            GoogleCredential cred = GoogleCredential.fromStream(input)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
            Sheets sheets = new Sheets.Builder(cred.getTransport(), cred.getJsonFactory(), cred)
                    .setApplicationName("probe").build();
            for (String id : new String[]{
                    "1fZsJPXt_6V5e7KnHO0MLE_ihbwrXtI3Ae0Md060bD2Q",
                    "1jgmFkPIFFp71c2EPWMwCXOJ6bG_irJYxFn1rSAoTf_I"}) {
                Spreadsheet sp = sheets.spreadsheets().get(id).execute();
                System.out.println(id + " -> \"" + sp.getProperties().getTitle()
                        + "\" tabs=" + sp.getSheets().size());
                sp.getSheets().forEach(s ->
                        System.out.println("   tab: " + s.getProperties().getTitle()));
            }
        }
    }
}
