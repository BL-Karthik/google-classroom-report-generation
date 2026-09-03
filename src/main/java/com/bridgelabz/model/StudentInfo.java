package com.bridgelabz.model;


import java.util.HashMap;
import java.util.Map;



import java.util.LinkedHashMap;
import java.util.Map;

public class StudentInfo {
    private String studentName;
    private String studentEmail;
    private Map<String, String> courseStatusMap = new LinkedHashMap<>();

    public StudentInfo(String studentName, String studentEmail) {
        this.studentName=studentName;
        this.studentEmail=studentEmail;
    }

    // Getters and setters
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    public Map<String, String> getCourseStatusMap() { return courseStatusMap; }
    public void setCourseStatusMap(Map<String, String> courseStatusMap) { this.courseStatusMap = courseStatusMap; }
}


