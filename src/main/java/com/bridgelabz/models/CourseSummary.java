package com.bridgelabz.models;

public class CourseSummary {

    String courseName;

    String rollNumber;

    String name;

    int submissions;

    double percentage_Of_Submissions;

    public CourseSummary(String courseName, String studentName, String studentRollNumber, int submitted, double percentageOfSubmissions) {
        this.courseName=courseName;
        this.name=studentName;
        this.rollNumber=studentRollNumber;
        this.submissions=submitted;
        this.percentage_Of_Submissions=percentageOfSubmissions;
    }

    public  String getCourseName()
    {
        return courseName;
    }
    public  String getRollNumber()
    {
        return rollNumber;
    }
    public  String getName()
    {
        return name ;
    }
    public int getSubmissions()
    {
        return submissions;
    }
    public  double getPercentageSubmissions()
    {
        return percentage_Of_Submissions;
    }

}
