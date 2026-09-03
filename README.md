

#  Google Classroom Grade & Submission Exporter

<p align="center">
  <strong>Automated Student Engagement & Submission Reporting System</strong>
</p>

<p align="center">
  A Spring Boot application that retrieves course, student, coursework, and submission data from Google Classroom and automatically generates structured reports in Google Sheets and CSV format.
</p>

<p align="center">

![Java](https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge\&logo=openjdk)

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=for-the-badge\&logo=springboot)

![Spring Security](https://img.shields.io/badge/Spring%20Security-OAuth2-green?style=for-the-badge\&logo=springsecurity)

![Google Classroom](https://img.shields.io/badge/Google-Classroom-blue?style=for-the-badge\&logo=googleclassroom)

![Google Drive](https://img.shields.io/badge/Google-Drive-yellow?style=for-the-badge\&logo=googledrive)

![Google Sheets](https://img.shields.io/badge/Google-Sheets-green?style=for-the-badge\&logo=googlesheets)

</p>

---

## Overview

The **Google Classroom Grade & Submission Exporter** automates the process of collecting student engagement and submission information from Google Classroom.

Instead of manually checking multiple courses and assignments, the application:

```text
Google Classroom
       ↓
Courses
       ↓
Students
       ↓
Coursework
       ↓
Submissions
       ↓
Data Processing
       ↓
┌──────────────────┬──────────────────┐
│                  │                  │
▼                  ▼                  ▼
CSV Reports    Google Sheets     Google Drive
```

The generated reports can be used by educators, administrators, and teams to monitor student activity and submission status.

---

#  Key Features

| Feature                 | Description                                            |
| ----------------------- | ------------------------------------------------------ |
|  Google OAuth2        | Secure authentication using Google                     |
|  Course Management    | Fetch courses from Google Classroom                    |
|  Student Data      | Retrieve enrolled students                             |
|  Coursework           | Fetch assignments and coursework                       |
|  Submission Tracking  | Track student submission status                        |
|  Batch Organization     | Group courses into B1, B2, B3, B4, etc.                |
|  Google Sheets        | Automatically generate structured spreadsheets         |
|  Google Drive         | Store generated reports in Drive                       |
|  CSV Export           | Generate local CSV backup files                        |
|  Error Handling      | Custom handling for API and application errors         |
|  WebClient             | Efficient communication with Google APIs               |
|  Modular Architecture | Separate controller, service, model and utility layers |

---

#  Architecture

```text
                         ┌─────────────────────┐
                         │       USER          │
                         │     Browser         │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │   Spring Security   │
                         │      OAuth2 Login   │
                         └──────────┬──────────┘
                                    │
                                    ▼
                         ┌─────────────────────┐
                         │     Controller      │
                         │ ClassroomController │
                         └──────────┬──────────┘
                                    │
                                    ▼
                    ┌───────────────────────────────┐
                    │           SERVICES            │
                    │                               │
                    │ GoogleClassroomService       │
                    │ GoogleSheetsDriveService      │
                    │ CourseService                  │
                    │ StudentService                 │
                    │ CourseworkService              │
                    │ SubmissionService              │
                    └───────────────┬───────────────┘
                                    │
                 ┌──────────────────┼──────────────────┐
                 │                  │                  │
                 ▼                  ▼                  ▼
        ┌────────────────┐ ┌────────────────┐ ┌────────────────┐
        │ Google         │ │ Google         │ │ Google         │
        │ Classroom API  │ │ Sheets API     │ │ Drive API      │
        └────────────────┘ └────────────────┘ └────────────────┘
                 │                  │                  │
                 └──────────────────┼──────────────────┘
                                    ▼
                         ┌─────────────────────┐
                         │   Generated Reports │
                         │                     │
                         │  Google Sheets   │
                         │  CSV Files       │
                         └─────────────────────┘
```

---

#  Application Workflow

```text
                    START
                      │
                      ▼
              Open Application
                      │
                      ▼
               Google Login
                      │
                      ▼
              OAuth2 Authorization
                      │
                      ▼
              Access Token Created
                      │
                      ▼
              Fetch All Courses
                      │
                      ▼
              Fetch Students
                      │
                      ▼
             Fetch Coursework
                      │
                      ▼
            Fetch Submissions
                      │
                      ▼
              Process & Group Data
                      │
             ┌────────┴────────┐
             │                 │
             ▼                 ▼
        Generate CSV     Generate Sheets
             │                 │
             │                 ▼
             │          Save to Drive
             │                 │
             └────────┬────────┘
                      ▼
              Export Completed
                      │
                      ▼
                    END
```

---

#  Batch Organization

Courses are automatically categorized based on their course names.

```text
                    Courses
                       │
        ┌──────────────┼──────────────┐
        │              │              │
        ▼              ▼              ▼
       B1             B2             B3
        │              │              │
        ▼              ▼              ▼
     Courses        Courses        Courses
        │              │              │
        └──────────────┼──────────────┘
                       │
                       ▼
                      B4
                       │
                       ▼
                Other_Batches
```

### Supported Batches

* `B1`
* `B2`
* `B3`
* `B4`
* `Other_Batches`

---

#  Report Structure

A generated report contains student information along with coursework submission statuses.

| Student Name | Email                                               | Assignment 1 | Assignment 2 | Assignment 3 |
| ------------ | --------------------------------------------------- | ------------ | ------------ | ------------ |
| Student 1    | [student1@example.com](mailto:student1@example.com) |  Submitted  |  Missing    |  Submitted  |
| Student 2    | [student2@example.com](mailto:student2@example.com) |  Submitted  | Submitted  |  Assigned  |
| Student 3    | [student3@example.com](mailto:student3@example.com) |  Missing    | Submitted  | Submitted  |

### Submission Statuses

| Status      | Meaning                               |
| ----------- | ------------------------------------- |
|  Submitted | Student submitted the coursework      |
|  Assigned | Coursework assigned but not submitted |
|  Missing   | Submission is missing                 |
|  Returned | Submission has been returned          |

---

#  Technology Stack

```text
┌──────────────────────────────────────────┐
│              TECHNOLOGY STACK            │
├──────────────────────────────────────────┤
│                                          │
│   Java 17+                             │
│   Spring Boot                          │
│   Spring Security OAuth2               │
│   Spring WebClient                     │
│   Google Classroom API                 │
│   Google Sheets API                    │
│   Google Drive API                     │
│   OpenCSV                              │
│   Maven                                │
│                                          │
└──────────────────────────────────────────┘
```

---

#  Project Structure

```text
google-classroom-report-generation/
│
├── src/
│   └── main/
│       │
│       ├── java/
│       │   └── com/
│       │       └── bridgelabz/
│       │           │
│       │           ├── Success2Application.java
│       │           │
│       │           ├── controller/
│       │           │   └── ClassroomController.java
│       │           │
│       │           ├── service/
│       │           │   ├── GoogleClassroomService.java
│       │           │   ├── GoogleSheetsDriveService.java
│       │           │   ├── CourseService.java
│       │           │   ├── StudentService.java
│       │           │   ├── CourseworkService.java
│       │           │   └── SubmissionService.java
│       │           │
│       │           ├── model/
│       │           │   └── StudentInfo.java
│       │           │
│       │           ├── csvUtiles/
│       │           │   └── CSVWriterUtil.java
│       │           │
│       │           ├── security/
│       │           │   └── SecurityConfig.java
│       │           │
│       │           ├── error/
│       │           │   └── ErrorController.java
│       │           │
│       │           └── handler/
│       │               └── OAuth2ErrorController.java
│       │
│       └── resources/
│           └── application.properties
│
├── .gitignore
├── pom.xml
└── README.md
```

---

#  Important Components

### `ClassroomController`

Responsible for handling application requests and triggering the Classroom data export process.

### `GoogleClassroomService`

Core service responsible for interacting with Google Classroom.

Responsibilities:

* Fetch courses
* Fetch students
* Fetch coursework
* Fetch submissions
* Organize Classroom data
* Prepare data for export

### `GoogleSheetsDriveService`

Responsible for Google Sheets and Google Drive operations.

Responsibilities:

* Create spreadsheets
* Create worksheets
* Write report data
* Manage Drive folders
* Upload/store generated reports

### `CSVWriterUtil`

Responsible for generating local CSV files.

### `SecurityConfig`

Configures Spring Security and Google OAuth2 authentication.

### `StudentInfo`

Represents student information used during report generation.

---

#  Setup & Installation

## 1️ Prerequisites

Install:

* Java 17 or higher
* Maven
* Git
* Google account
* Google Cloud project

---

#  Google Cloud Configuration

## 2️ Create Google Cloud Project

Go to Google Cloud Console and:

1. Create a new project.
2. Select the project.
3. Navigate to **APIs & Services → Library**.

---

## 3️ Enable Required APIs

Enable the following:

```text
Google Classroom API
Google Sheets API
Google Drive API
```

---

## 4️ Create OAuth2 Credentials

Navigate to:

```text
APIs & Services
        ↓
Credentials
        ↓
Create Credentials
        ↓
OAuth Client ID
```

Select:

```text
Web Application
```

Configure the redirect URI:

```text
http://localhost:8084/login/oauth2/code/google
```

Google will provide:

```text
Client ID
Client Secret
```

---

#  Application Configuration

Open:

```text
src/main/resources/application.properties
```

Configure:

```properties
spring.application.name=Success2

server.port=8084

spring.codec.max-in-memory-size=50MB

spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}

spring.security.oauth2.client.registration.google.redirect-uri=http://localhost:8084/login/oauth2/code/google

spring.security.oauth2.client.registration.google.client-name=Google

spring.security.oauth2.client.registration.google.authorization-grant-type=authorization_code

spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/auth

spring.security.oauth2.client.provider.google.token-uri=https://oauth2.googleapis.com/token

spring.security.oauth2.client.provider.google.user-info-uri=https://www.googleapis.com/oauth2/v3/userinfo
```

---

#  OAuth2 Scopes

The application requires access to Google Classroom, Sheets, and Drive resources.

Example scopes:

```text
openid
profile
email

Google Classroom
├── classroom.courses.readonly
├── classroom.rosters.readonly
├── classroom.coursework.students.readonly
├── classroom.student-submissions.students.readonly
└── classroom.profile.emails

Google Sheets
└── spreadsheets

Google Drive
└── drive.file
```

> **Security:** Never commit the actual Google Client ID or Client Secret to GitHub.

Use environment variables:

```text
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
```

---

#  Running the Application

## 1. Clone the Repository

```bash
git clone <repository-url>
```

```bash
cd google-classroom-report-generation
```

---

## 2. Configure Environment Variables

### Windows CMD

```cmd
set GOOGLE_CLIENT_ID=your-client-id
set GOOGLE_CLIENT_SECRET=your-client-secret
```

### Linux / macOS

```bash
export GOOGLE_CLIENT_ID=your-client-id
export GOOGLE_CLIENT_SECRET=your-client-secret
```

---

## 3. Build the Project

```bash
mvn clean install
```

---

## 4. Start the Application

```bash
mvn spring-boot:run
```

Application URL:

```text
http://localhost:8084
```

---

#  Using the Application

### Step 1 — Open Application

```text
http://localhost:8084
```

### Step 2 — Login

Sign in using a Google account that has access to the required Classroom courses.

### Step 3 — Grant Permissions

Allow the required Google Classroom, Sheets, and Drive permissions.

### Step 4 — Generate Reports

The application retrieves:

```text
Courses
   ↓
Students
   ↓
Coursework
   ↓
Submissions
```

### Step 5 — View Reports

Generated reports are available in:

```text
Google Drive
    └── Generated Google Sheets

 Project Directory
    └── Generated CSV Files
```

---

#  Main API Endpoint

The primary export endpoint is:

```http
GET /classroom/all-student-submissions
```

### Processing

```text
GET /classroom/all-student-submissions
                │
                ▼
        Fetch Classroom Data
                │
                ▼
          Process Data
                │
        ┌───────┴────────┐
        ▼                ▼
      CSV            Google Sheets
                         │
                         ▼
                    Google Drive
```

Successful execution returns:

```text
All Sheets exported successfully!
```

---

#  Error Handling

The application provides custom error handling for common failures.

| HTTP Status | Description                         |
| ----------- | ----------------------------------- |
| `401`       | Unauthorized / Invalid OAuth2 token |
| `404`       | Requested resource not found        |
| `500`       | Internal server / Google API error  |

OAuth2 errors are handled through a dedicated error handler.

---

#  Troubleshooting

<details>
<summary> Invalid OAuth2 Credentials</summary>

Check:

* Client ID
* Client Secret
* Redirect URI
* Google Cloud OAuth configuration

The redirect URI must exactly match:

```text
http://localhost:8084/login/oauth2/code/google
```

</details>

<details>
<summary> 403 Insufficient Permission</summary>

If you receive:

```text
ACCESS_TOKEN_SCOPE_INSUFFICIENT
```

verify that the required Google API scopes are configured.

After changing scopes, log in again and grant the updated permissions.

</details>

<details>
<summary> Google Classroom API Error</summary>

Verify:

* Google Classroom API is enabled.
* The logged-in Google account has access to the required courses.
* OAuth scopes are correctly configured.

</details>

<details>
<summary> Google Sheets / Drive Error</summary>

Verify:

* Google Sheets API is enabled.
* Google Drive API is enabled.
* Required OAuth scopes are present.
* The authenticated user has access to the destination Drive.

</details>

<details>
<summary> Memory Issue</summary>

The application currently uses:

```properties
spring.codec.max-in-memory-size=50MB
```

Increase the value if the application processes very large API responses.

</details>

<details>
<summary> Enable Debug Logs</summary>

Add:

```properties
logging.level.org.springframework.security=DEBUG
```

Then check the Spring Boot console for OAuth2 authentication details.

</details>

---

#  Security

This project follows basic credential-management practices.

###  Never commit

```text
Client Secrets
Access Tokens
Refresh Tokens
Passwords
API Keys
Private Credentials
```

###  Use

```properties
spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}

spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}
```

And add:

```gitignore
target/
*.class
.env
application-local.properties
```

---

#  Future Enhancements

The application can be extended with:

*  Date-based course filtering
*  Submission status filtering
*  Student performance dashboard
*  Email notifications
*  Excel export
*  Scheduled report generation
*  Student engagement analytics
*  Admin dashboard
*  Report download functionality
*  Export history
*  Automated periodic synchronization

---

#  Use Cases

This application can be used by:

###  Educators

Track student assignments and submission status.

###  Training Organizations

Generate batch-wise student engagement reports.

###  Administrators

Monitor multiple Classroom courses from centralized reports.

###  Management

Analyze student participation and coursework completion.

---

#  Project Benefits

```text
              ┌────────────────────────┐
              │   Google Classroom     │
              └────────────┬───────────┘
                           │
                           ▼
                 ┌──────────────────┐
                 │ Automated System │
                 └────────┬─────────┘
                          │
             ┌────────────┼────────────┐
             ▼            ▼            ▼
          Courses      Students    Submissions
             │            │            │
             └────────────┼────────────┘
                          ▼
                    Data Processing
                          │
             ┌────────────┴────────────┐
             ▼                         ▼
       Google Sheets                 CSV
             │                         │
             ▼                         ▼
       Google Drive              Local Backup
```

### Key Advantages

*  Reduces manual reporting effort
*  Produces structured reports
*  Organizes data batch-wise
*  Automatically stores reports in Google Drive
*  Provides CSV backups
*  Uses OAuth2 authentication
*  Modular and maintainable architecture
*  Easy to extend with additional reporting features

---

#  Project Documentation

Detailed project documentation is available in:

```text
google-classroom-documentation.pdf
```

It contains additional information about:

* Google Cloud setup
* OAuth2 configuration
* API integration
* Application workflow
* Report generation
* Troubleshooting
* Future enhancements

---

#  Conclusion

The **Google Classroom Grade & Submission Exporter** automates the collection and reporting of student engagement data from Google Classroom.

By integrating:

```text
Java
   +
Spring Boot
   +
Spring Security OAuth2
   +
Google Classroom API
   +
Google Sheets API
   +
Google Drive API
   +
WebClient
   +
OpenCSV
```

the application provides a centralized solution for generating **batch-wise, course-wise, and student-wise submission reports**.

The modular architecture also makes it easy to extend the system with dashboards, analytics, Excel exports, scheduled reports, and additional Google Workspace integrations.

---

<p align="center">

### Built with Java & Spring Boot

**Google Classroom → Process → Report → Google Drive**

</p>


