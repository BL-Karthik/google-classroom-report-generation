package com.bridgelabz.csvUtiles;

import com.opencsv.CSVWriter;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class CSVWriterUtil {

    public static String writeCSV(String courseName, Map<String, Map<String, String>> students, List<String> titles) throws Exception {
        String fileName = courseName + "_Submissions.csv";
        System.out.println("CSV File Saved  : " + fileName);

        List<String[]> csv = new ArrayList<>();
        List<String> header = new ArrayList<>(List.of("Student Name", "Email"));
        header.addAll(titles);  // Preserve coursework titles in original order
        csv.add(header.toArray(new String[0]));

        for (Map.Entry<String, Map<String, String>> entry : students.entrySet()) {
            String email = entry.getKey();
            Map<String, String> student = entry.getValue();

            List<String> row = new ArrayList<>();
            row.add(student.getOrDefault("name", "Unknown"));
            row.add(email); // Use key as email

            for (String title : titles) {
                row.add(student.getOrDefault(title, "Missing"));
            }

            csv.add(row.toArray(new String[0]));
        }

        try (Writer writer = Files.newBufferedWriter(Paths.get(fileName), StandardCharsets.UTF_8)) {
            CSVWriter csvWriter = new CSVWriter(writer);
            csvWriter.writeAll(csv);
        }

        return fileName;
    }
}
