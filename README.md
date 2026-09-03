Google Classroom API – Grade & Submission Data Exporter

A Spring Boot application that fetches student, coursework, and submission data from Google Classroom and exports the data into Google Sheets and CSV files.

The application uses Google OAuth2 for authentication and integrates with the Google Classroom, Google Sheets, and Google Drive APIs. Course data is organized based on batches such as B1, B2, B3, B4, and Other_Batches.

1. Project Overview
Purpose

The Google Classroom Submission Exporter automates the process of collecting student submission information from Google Classroom and exporting it into organized and readable formats.

Main Outputs
Google Sheets
CSV files
Batch-wise course reports
Course-wise submission reports
Key Features
Google OAuth2 authentication
Fetch Google Classroom courses
Fetch students enrolled in courses
Fetch coursework information
Fetch student submission statuses
Organize courses based on batches
Generate Google Sheets reports
Store generated Google Sheets in Google Drive
Generate local CSV backup files
Custom error handling
OAuth2 error handling
Support for large API responses
2. Technologies Used
Technology	Purpose
Java 17+	Programming Language
Spring Boot	Backend Framework
Spring Security	OAuth2 Authentication
Google Classroom API	Fetch course and submission data
Google Sheets API	Create and update spreadsheets
Google Drive API	Store generated spreadsheets
WebClient	Calling Google APIs
OpenCSV	Generate CSV files
Maven	Dependency Management
3. Project Architecture

The application follows a layered architecture.

                    ┌───────────────────────┐
                    │       Browser         │
                    └───────────┬───────────┘
                                │
                                ▼
                    ┌───────────────────────┐
                    │   Spring Security     │
                    │      OAuth2 Login     │
                    └───────────┬───────────┘
                                │
                                ▼
                    ┌───────────────────────┐
                    │      Controller       │
                    │ ClassroomController   │
                    └───────────┬───────────┘
                                │
                                ▼
                    ┌───────────────────────┐
                    │       Services        │
                    │                       │
                    │ GoogleClassroomService│
                    │ GoogleSheetsDriveSvc  │
                    │ StudentService        │
                    │ CourseService          │
                    │ CourseworkService      │
                    │ SubmissionService      │
                    └───────────┬───────────┘
                                │
                ┌───────────────┼────────────────┐
                ▼               ▼                ▼
       Google Classroom   Google Sheets    Google Drive
             API               API               API
                │               │                │
                └───────────────┼────────────────┘
                                ▼
                    ┌───────────────────────┐
                    │      CSV Reports      │
                    │   Google Sheet Files  │
                    └───────────────────────┘
4. Project Structure
src
└── main
    ├── java
    │   └── com.bridgelabz
    │       ├── Success2Application.java
    │       │
    │       ├── controller
    │       │   └── ClassroomController.java
    │       │
    │       ├── service
    │       │   ├── GoogleClassroomService.java
    │       │   ├── GoogleSheetsDriveService.java
    │       │   ├── CourseService.java
    │       │   ├── StudentService.java
    │       │   ├── CourseworkService.java
    │       │   └── SubmissionService.java
    │       │
    │       ├── model
    │       │   └── StudentInfo.java
    │       │
    │       ├── csvUtiles
    │       │   └── CSVWriterUtil.java
    │       │
    │       ├── security
    │       │   └── SecurityConfig.java
    │       │
    │       ├── error
    │       │   └── ErrorController.java
    │       │
    │       └── handler
    │           └── OAuth2ErrorController.java
    │
    └── resources
        └── application.properties
Important Classes
ClassroomController

Handles application endpoints and initiates the Classroom data export process.

GoogleClassroomService

Responsible for:

Fetching courses
Fetching students
Fetching coursework
Fetching submissions
Organizing data batch-wise
GoogleSheetsDriveService

Responsible for:

Creating Google Sheets
Creating worksheets/tabs
Writing data into spreadsheets
Creating or using Google Drive folders
Saving generated spreadsheets in Google Drive
CSVWriterUtil

Generates CSV files as local backups.

SecurityConfig

Configures Spring Security and Google OAuth2 authentication.

5. Prerequisites

Before running the application, install the following:

Java 17 or higher
Maven
Google account
Google Cloud Project
Google Classroom access

The following Google APIs must be enabled:

Google Classroom API
Google Sheets API
Google Drive API
6. Google Cloud Configuration
Step 1: Create a Google Cloud Project
Open Google Cloud Console.
Create a new project or select an existing project.
Select the project.
Step 2: Enable Google APIs

Navigate to:

Google Cloud Console
        ↓
APIs & Services
        ↓
Library

Enable:

Google Classroom API
Google Sheets API
Google Drive API
Step 3: Create OAuth2 Credentials

Navigate to:

APIs & Services
        ↓
Credentials
        ↓
Create Credentials
        ↓
OAuth Client ID

Select:

Application Type: Web Application

Configure the redirect URI:

http://localhost:8084/login/oauth2/code/google

After creating the credentials, Google provides:

Client ID
Client Secret
7. Application Configuration

Create or update:

src/main/resources/application.properties

Use environment variables for OAuth credentials instead of hardcoding secrets.

spring.application.name=Success2

server.port=8084

spring.codec.max-in-memory-size=50MB

# Google OAuth2
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}

spring.security.oauth2.client.registration.google.redirect-uri=http://localhost:8084/login/oauth2/code/google

spring.security.oauth2.client.registration.google.client-name=Google
spring.security.oauth2.client.registration.google.authorization-grant-type=authorization_code

# Google OAuth2 Scopes
spring.security.oauth2.client.registration.google.scope=openid,profile,email,https://www.googleapis.com/auth/classroom.coursework.me,https://www.googleapis.com/auth/classroom.courses.readonly,https://www.googleapis.com/auth/classroom.coursework.me.readonly,https://www.googleapis.com/auth/classroom.coursework.students.readonly,https://www.googleapis.com/auth/classroom.rosters.readonly,https://www.googleapis.com/auth/classroom.student-submissions.students.readonly,https://www.googleapis.com/auth/classroom.profile.emails,https://www.googleapis.com/auth/classroom.coursework.students,https://www.googleapis.com/auth/spreadsheets,https://www.googleapis.com/auth/drive.file

# Google OAuth2 Provider
spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/auth
spring.security.oauth2.client.provider.google.token-uri=https://oauth2.googleapis.com/token
spring.security.oauth2.client.provider.google.user-info-uri=https://www.googleapis.com/oauth2/v3/userinfo

# Logging
logging.level.org.springframework.security=DEBUG
Important

Do not commit actual Google OAuth credentials to GitHub.

Set them as environment variables:

GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET

For example, on Windows CMD:

set GOOGLE_CLIENT_ID=your-client-id
set GOOGLE_CLIENT_SECRET=your-client-secret
8. OAuth2 Authentication Flow

The application uses Spring Security OAuth2 to authenticate the user.

User
 │
 ▼
Open Application
 │
 ▼
Spring Security
 │
 ▼
Google Login Page
 │
 ▼
User Grants Permission
 │
 ▼
Google Authorization Code
 │
 ▼
Spring Security
 │
 ▼
Access Token
 │
 ▼
Google Classroom / Sheets / Drive APIs

After successful authentication, the user is redirected to the application.

9. Application Workflow

The complete application workflow is:

Start Application
       │
       ▼
User Opens Application
       │
       ▼
Google OAuth2 Login
       │
       ▼
Authentication Successful
       │
       ▼
Fetch Courses
       │
       ▼
Fetch Students
       │
       ▼
Fetch Coursework
       │
       ▼
Fetch Student Submissions
       │
       ▼
Organize Data by Batch
       │
       ├───────────────┐
       ▼               ▼
Generate CSV      Generate Google Sheet
       │               │
       │               ▼
       │         Save to Google Drive
       │
       ▼
Export Completed
10. Fetching Google Classroom Data

The application retrieves data in multiple stages.

10.1 Fetch Courses

The application calls the Google Classroom API to retrieve available courses.

Example:

B1 - Java Full Stack
B2 - Spring Boot
B3 - React
B4 - Advanced Java
10.2 Fetch Students

For each course, the application retrieves enrolled students.

The student information may include:

Student Name
Email
Student ID
10.3 Fetch Coursework

For each course, the application retrieves coursework such as:

Java Assignment
Spring Boot Assignment
REST API Assignment
Project Assignment
10.4 Fetch Submissions

The application retrieves submission information for each student and coursework.

Possible statuses include:

Submitted
Assigned
Missing
Returned
11. Batch-wise Data Organization

Courses are grouped according to their batch names.

Supported batches:

B1
B2
B3
B4
Other_Batches

The application internally organizes the information approximately as:

Batch
  │
  ├── Course
  │     │
  │     ├── Student
  │     │      ├── Email
  │     │      ├── Coursework 1
  │     │      ├── Coursework 2
  │     │      └── Coursework 3
  │     │
  │     └── Student
  │
  └── Course

This makes it easier to generate batch-wise reports.

12. Google Sheets Export

The application generates Google Sheets containing student submission information.

A typical report contains:

Student Name	Email	Assignment 1	Assignment 2	Assignment 3
Student 1	student1@example.com	Submitted	Missing	Submitted
Student 2	student2@example.com	Submitted	Submitted	Assigned

Depending on the implementation, spreadsheets are organized by batch and/or course.

The generated Google Sheet files are stored in the configured Google Drive location.

13. CSV Export

The application also generates local CSV files.

Example:

B1_Java_FullStack.csv
B2_Spring_Boot.csv
B3_React.csv
B4_Advanced_Java.csv

CSV files act as local backup/export files.

14. Main Endpoint

The main export operation is exposed through:

/classroom/all-student-submissions

The endpoint triggers the process of:

Fetch Courses
      ↓
Fetch Students
      ↓
Fetch Coursework
      ↓
Fetch Submissions
      ↓
Process Data
      ↓
Generate CSV
      ↓
Generate Google Sheets
      ↓
Save Sheets to Google Drive

On successful completion, the application returns:

All Sheets exported successfully!
15. Running the Application
Step 1: Clone the Repository
git clone <repository-url>

Navigate to the project:

cd google-classroom-report-generation
Step 2: Configure Google Credentials

Set:

GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET

as environment variables.

Step 3: Build the Project

Using Maven:

mvn clean install
Step 4: Start the Application
mvn spring-boot:run

The application starts on:

http://localhost:8084
16. Using the Application
Step 1

Open:

http://localhost:8084
Step 2

Sign in using a Google account.

Step 3

Grant the required permissions.

Step 4

The application authenticates the user and starts the Classroom export process.

Step 5

Check the generated files:

Google Drive
    └── Generated Google Sheets

Project Directory
    └── Generated CSV Files
17. Error Handling

The application contains custom error handling for common failures.

401 – Unauthorized

Occurs when the OAuth2 authentication/token is invalid or unavailable.

404 – Not Found

Displayed when a requested resource or endpoint does not exist.

500 – Internal Server Error

Used for unexpected application or Google API failures.

Google API errors are logged for troubleshooting.

18. Troubleshooting
Invalid OAuth2 Credentials

Check:

Client ID
Client Secret
Redirect URI

The redirect URI configured in Google Cloud must exactly match:

http://localhost:8084/login/oauth2/code/google
Google API Errors

Verify that the following APIs are enabled:

Google Classroom API
Google Sheets API
Google Drive API

Also verify that the authenticated Google account has access to the required Classroom courses.

Insufficient Permission / 403 Error

If Google returns an error such as:

ACCESS_TOKEN_SCOPE_INSUFFICIENT

verify that the required OAuth scopes are configured.

After changing OAuth scopes, log in again and reauthorize the application so that a new access token is issued with the updated permissions.

Memory Issues

The application is configured with:

spring.codec.max-in-memory-size=50MB

If very large API responses are received, this value may need to be increased.

Enable Security Debug Logging

For OAuth2 troubleshooting:

logging.level.org.springframework.security=DEBUG

Check the application console for authentication and authorization details.

19. Security Best Practices

Never commit the following to GitHub:

Google Client Secret
Google Client ID
Access Tokens
Refresh Tokens
Passwords
API Keys

Use environment variables:

spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}

Add the following to .gitignore:

target/
*.class
.env
application-local.properties
20. Future Enhancements

The application can be extended with:

Course filtering by date
Filtering by submission status
Web-based dashboard
Custom report formats
Excel export
Scheduled automatic reports
Email notifications
Batch-wise analytics
Student performance statistics
Pagination for large Classroom datasets
Admin dashboard
Export history
Report download functionality
21. Benefits

The application provides an automated way to:

Reduce manual Classroom data collection
Track student submissions
Generate batch-wise reports
Maintain CSV backups
Generate Google Sheets automatically
Store reports in Google Drive
Centralize student engagement information
22. Conclusion

The Google Classroom Grade & Submission Data Exporter provides an automated solution for collecting student and coursework information from Google Classroom and converting it into structured reports.

By integrating Spring Boot, Spring Security OAuth2, Google Classroom API, Google Sheets API, Google Drive API, WebClient, and OpenCSV, the application provides a modular and extensible backend for generating student engagement and submission reports.

The architecture can be further extended to support dashboards, analytics, scheduled reports, Excel exports, and additional Google Workspace integrations.

Author

Google Classroom Report Generation Project

Built using:

Java
Spring Boot
Spring Security OAuth2
Google Classroom API
Google Sheets API
Google Drive API
WebClient
OpenCSV
Maven
