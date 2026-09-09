<h1>Google Classroom Grade & Submission Exporter</h1>
<p align="center">
  <strong>Automated Student Engagement & Submission Reporting System</strong>
</p>
<p align="center">
<p>  A Spring Boot application that retrieves course, student, coursework, and submission data from Google Classroom and automatically generates structured reports in Google Sheets and CSV format.</p>
</p>
<p align="center">
<p align="center">
<img src="https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=openjdk" alt="Java">
</p>
<p align="center">
<img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=for-the-badge&logo=springboot" alt="Spring Boot">
</p>
<p align="center">
<img src="https://img.shields.io/badge/Spring%20Security-OAuth2-green?style=for-the-badge&logo=springsecurity" alt="Spring Security">
</p>
<p align="center">
<img src="https://img.shields.io/badge/Google-Classroom-blue?style=for-the-badge&logo=googleclassroom" alt="Google Classroom">
</p>
<p align="center">
<img src="https://img.shields.io/badge/Google-Drive-yellow?style=for-the-badge&logo=googledrive" alt="Google Drive">
</p>
<p align="center">
<img src="https://img.shields.io/badge/Google-Sheets-green?style=for-the-badge&logo=googlesheets" alt="Google Sheets">
</p>
</p>
<hr>
<h2>Overview</h2>
<p>The <strong>Google Classroom Grade & Submission Exporter</strong> automates the process of collecting student engagement and submission information from Google Classroom.</p>
<p>Instead of manually checking multiple courses and assignments, the application:</p>
<pre><code class="language-text">Google Classroom
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
CSV Reports    Google Sheets     Google Drive</code></pre>
<p>The generated reports can be used by educators, administrators, and teams to monitor student activity and submission status.</p>
<hr>
<h1>Key Features</h1>
<table>
<thead><tr><th>Feature</th><th>Description</th></tr></thead>
<tbody>
<tr><td>Google OAuth2</td><td>Secure authentication using Google</td></tr>
<tr><td>Course Management</td><td>Fetch courses from Google Classroom</td></tr>
<tr><td>Student Data</td><td>Retrieve enrolled students</td></tr>
<tr><td>Coursework</td><td>Fetch assignments and coursework</td></tr>
<tr><td>Submission Tracking</td><td>Track student submission status</td></tr>
<tr><td>Batch Organization</td><td>Group courses into B1, B2, B3, B4, etc.</td></tr>
<tr><td>Google Sheets</td><td>Automatically generate structured spreadsheets</td></tr>
<tr><td>Google Drive</td><td>Store generated reports in Drive</td></tr>
<tr><td>CSV Export</td><td>Generate local CSV backup files</td></tr>
<tr><td>Error Handling</td><td>Custom handling for API and application errors</td></tr>
<tr><td>WebClient</td><td>Efficient communication with Google APIs</td></tr>
<tr><td>Modular Architecture</td><td>Separate controller, service, model and utility layers</td></tr>
</tbody></table>
<hr>
<h1>Architecture</h1>
<pre><code class="language-text">                         ┌─────────────────────┐
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
                         └─────────────────────┘</code></pre>
<hr>
<h1>Application Workflow</h1>
<pre><code class="language-text">                    START
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
              Process &amp; Group Data
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
                    END</code></pre>
<hr>
<h1>Batch Organization</h1>
<p>Courses are automatically categorized based on their course names.</p>
<pre><code class="language-text">                    Courses
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
                Other_Batches</code></pre>
<h3>Supported Batches</h3>
<ul>
<li>`B1`</li>
<li>`B2`</li>
<li>`B3`</li>
<li>`B4`</li>
<li>`Other_Batches`</li>
</ul>
<hr>
<h1>Report Structure</h1>
<p>A generated report contains student information along with coursework submission statuses.</p>
<table>
<thead><tr><th>Student Name</th><th>Email</th><th>Assignment 1</th><th>Assignment 2</th><th>Assignment 3</th></tr></thead>
<tbody>
<tr><td>Student 1</td><td>[student1@example.com](mailto:student1@example.com)</td><td>Submitted</td><td>Missing</td><td>Submitted</td></tr>
<tr><td>Student 2</td><td>[student2@example.com](mailto:student2@example.com)</td><td>Submitted</td><td>Submitted</td><td>Assigned</td></tr>
<tr><td>Student 3</td><td>[student3@example.com](mailto:student3@example.com)</td><td>Missing</td><td>Submitted</td><td>Submitted</td></tr>
</tbody></table>
<h3>Submission Statuses</h3>
<table>
<thead><tr><th>Status</th><th>Meaning</th></tr></thead>
<tbody>
<tr><td>Submitted</td><td>Student submitted the coursework</td></tr>
<tr><td>Assigned</td><td>Coursework assigned but not submitted</td></tr>
<tr><td>Missing</td><td>Submission is missing</td></tr>
<tr><td>Returned</td><td>Submission has been returned</td></tr>
</tbody></table>
<hr>
<h1>Technology Stack</h1>
<pre><code class="language-text">┌──────────────────────────────────────────┐
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
└──────────────────────────────────────────┘</code></pre>
<hr>
<h1>Project Structure</h1>
<pre><code class="language-text">google-classroom-report-generation/
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
└── README.md</code></pre>
<hr>
<h1>Important Components</h1>
<h3>ClassroomController</h3>
<p>Responsible for handling application requests and triggering the Classroom data export process.</p>
<h3>GoogleClassroomService</h3>
<p>Core service responsible for interacting with Google Classroom.</p>
<p>Responsibilities:</p>
<ul>
<li>Fetch courses</li>
<li>Fetch students</li>
<li>Fetch coursework</li>
<li>Fetch submissions</li>
<li>Organize Classroom data</li>
<li>Prepare data for export</li>
</ul>
<h3>GoogleSheetsDriveService</h3>
<p>Responsible for Google Sheets and Google Drive operations.</p>
<p>Responsibilities:</p>
<ul>
<li>Create spreadsheets</li>
<li>Create worksheets</li>
<li>Write report data</li>
<li>Manage Drive folders</li>
<li>Upload/store generated reports</li>
</ul>
<h3>CSVWriterUtil</h3>
<p>Responsible for generating local CSV files.</p>
<h3>SecurityConfig</h3>
<p>Configures Spring Security and Google OAuth2 authentication.</p>
<h3>StudentInfo</h3>
<p>Represents student information used during report generation.</p>
<hr>
<h1>Setup & Installation</h1>
<h2>1️ Prerequisites</h2>
<p>Install:</p>
<ul>
<li>Java 17 or higher</li>
<li>Maven</li>
<li>Git</li>
<li>Google account</li>
<li>Google Cloud project</li>
</ul>
<hr>
<h1>Google Cloud Configuration</h1>
<h2>2️ Create Google Cloud Project</h2>
<p>Go to Google Cloud Console and:</p>
<ol>
<li>Create a new project.</li>
<li>Select the project.</li>
<li>Navigate to **APIs & Services → Library**.</li>
</ol>
<hr>
<h2>3️ Enable Required APIs</h2>
<p>Enable the following:</p>
<pre><code class="language-text">Google Classroom API
Google Sheets API
Google Drive API</code></pre>
<hr>
<h2>4️ Create OAuth2 Credentials</h2>
<p>Navigate to:</p>
<pre><code class="language-text">APIs &amp; Services
        ↓
Credentials
        ↓
Create Credentials
        ↓
OAuth Client ID</code></pre>
<p>Select:</p>
<pre><code class="language-text">Web Application</code></pre>
<p>Configure the redirect URI:</p>
<pre><code class="language-text">http://localhost:8084/login/oauth2/code/google</code></pre>
<p>Google will provide:</p>
<pre><code class="language-text">Client ID
Client Secret</code></pre>
<hr>
<h1>Application Configuration</h1>
<p>Open:</p>
<pre><code class="language-text">src/main/resources/application.properties</code></pre>
<p>Configure:</p>
<pre><code class="language-properties">spring.application.name=Success2

server.port=8084

spring.codec.max-in-memory-size=50MB

spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}
spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}

spring.security.oauth2.client.registration.google.redirect-uri=http://localhost:8084/login/oauth2/code/google

spring.security.oauth2.client.registration.google.client-name=Google

spring.security.oauth2.client.registration.google.authorization-grant-type=authorization_code

spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/auth

spring.security.oauth2.client.provider.google.token-uri=https://oauth2.googleapis.com/token

spring.security.oauth2.client.provider.google.user-info-uri=https://www.googleapis.com/oauth2/v3/userinfo</code></pre>

<hr>
<h1>OAuth2 Scopes</h1>
<p>The application requires access to Google Classroom, Sheets, and Drive resources.</p>
<p>Example scopes:</p>
<pre><code class="language-text">openid
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
└── drive.file</code></pre>

<blockquote>**Security:** Never commit the actual Google Client ID or Client Secret to GitHub.</blockquote>
<p>Use environment variables:</p>
<pre><code class="language-text">GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET</code></pre>
<hr>
<h1>Running the Application</h1>
<h2>1. Clone the Repository</h2>
<pre><code class="language-bash">git clone &lt;repository-url&gt;</code></pre>
<pre><code class="language-bash">cd google-classroom-report-generation</code></pre>
<hr>
<h2>2. Configure Environment Variables</h2>
<h3>Windows CMD</h3>
<pre><code class="language-cmd">set GOOGLE_CLIENT_ID=your-client-id
set GOOGLE_CLIENT_SECRET=your-client-secret</code></pre>
<h3>Linux / macOS</h3>
<pre><code class="language-bash">export GOOGLE_CLIENT_ID=your-client-id
export GOOGLE_CLIENT_SECRET=your-client-secret</code></pre>
<hr>
<h2>3. Build the Project</h2>
<pre><code class="language-bash">mvn clean install</code></pre>
<hr>
<h2>4. Start the Application</h2>
<pre><code class="language-bash">mvn spring-boot:run</code></pre>
<p>Application URL:</p>
<pre><code class="language-text">http://localhost:8084</code></pre>
<hr>
<h1>Using the Application</h1>
<h3>Step 1 — Open Application</h3>
<pre><code class="language-text">http://localhost:8084</code></pre>
<h3>Step 2 — Login</h3>
<p>Sign in using a Google account that has access to the required Classroom courses.</p>
<h3>Step 3 — Grant Permissions</h3>
<p>Allow the required Google Classroom, Sheets, and Drive permissions.</p>
<h3>Step 4 — Generate Reports</h3>
<p>The application retrieves:</p>
<pre><code class="language-text">Courses
   ↓
Students
   ↓
Coursework
   ↓
Submissions</code></pre>
<h3>Step 5 — View Reports</h3>
<p>Generated reports are available in:</p>
<pre><code class="language-text">Google Drive
    └── Generated Google Sheets

Project Directory
└── Generated CSV Files</code></pre>

<hr>
<h1>Main API Endpoint</h1>
<p>The primary export endpoint is:</p>
<pre><code class="language-http">GET /classroom/all-student-submissions</code></pre>
<h3>Processing</h3>
<pre><code class="language-text">GET /classroom/all-student-submissions
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
                    Google Drive</code></pre>
<p>Successful execution returns:</p>
<pre><code class="language-text">All Sheets exported successfully!</code></pre>
<hr>
<h1>Error Handling</h1>
<p>The application provides custom error handling for common failures.</p>
<table>
<thead><tr><th>HTTP Status</th><th>Description</th></tr></thead>
<tbody>
<tr><td>`401`</td><td>Unauthorized / Invalid OAuth2 token</td></tr>
<tr><td>`404`</td><td>Requested resource not found</td></tr>
<tr><td>`500`</td><td>Internal server / Google API error</td></tr>
</tbody></table>
<p>OAuth2 errors are handled through a dedicated error handler.</p>
<hr>
<h1>Troubleshooting</h1>
<details>
<summary> Invalid OAuth2 Credentials</summary>

Check:

Client ID

Client Secret

Redirect URI

Google Cloud OAuth configuration

The redirect URI must exactly match:

http://localhost:8084/login/oauth2/code/google

</details>
<details>
<summary> 403 Insufficient Permission</summary>

If you receive:

ACCESS_TOKEN_SCOPE_INSUFFICIENT

verify that the required Google API scopes are configured.

After changing scopes, log in again and grant the updated permissions.

</details>
<details>
<summary> Google Classroom API Error</summary>

Verify:

Google Classroom API is enabled.

The logged-in Google account has access to the required courses.

OAuth scopes are correctly configured.

</details>
<details>
<summary> Google Sheets / Drive Error</summary>

Verify:

Google Sheets API is enabled.

Google Drive API is enabled.

Required OAuth scopes are present.

The authenticated user has access to the destination Drive.

</details>
<details>
<summary> Memory Issue</summary>

The application currently uses:

spring.codec.max-in-memory-size=50MB

Increase the value if the application processes very large API responses.

</details>
<details>
<summary> Enable Debug Logs</summary>

Add:

logging.level.org.springframework.security=DEBUG

Then check the Spring Boot console for OAuth2 authentication details.

</details>
<hr>
<h1>Security</h1>
<p>This project follows basic credential-management practices.</p>
<h3>Never commit</h3>
<pre><code class="language-text">Client Secrets
Access Tokens
Refresh Tokens
Passwords
API Keys
Private Credentials</code></pre>
<h3>Use</h3>
<pre><code class="language-properties">spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}

spring.security.oauth2.client.registration.google.client-secret=${GOOGLE_CLIENT_SECRET}</code></pre>

<p>And add:</p>
<pre><code class="language-gitignore">target/
*.class
.env
application-local.properties</code></pre>
<hr>
<h1>Future Enhancements</h1>
<p>The application can be extended with:</p>
<ul>
<li>Date-based course filtering</li>
<li>Submission status filtering</li>
<li>Student performance dashboard</li>
<li>Email notifications</li>
<li>Excel export</li>
<li>Scheduled report generation</li>
<li>Student engagement analytics</li>
<li>Admin dashboard</li>
<li>Report download functionality</li>
<li>Export history</li>
<li>Automated periodic synchronization</li>
</ul>
<hr>
<h1>Use Cases</h1>
<p>This application can be used by:</p>
<h3>Educators</h3>
<p>Track student assignments and submission status.</p>
<h3>Training Organizations</h3>
<p>Generate batch-wise student engagement reports.</p>
<h3>Administrators</h3>
<p>Monitor multiple Classroom courses from centralized reports.</p>
<h3>Management</h3>
<p>Analyze student participation and coursework completion.</p>
<hr>
<h1>Project Benefits</h1>
<pre><code class="language-text">              ┌────────────────────────┐
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
       Google Drive              Local Backup</code></pre>
<h3>Key Advantages</h3>
<ul>
<li>Reduces manual reporting effort</li>
<li>Produces structured reports</li>
<li>Organizes data batch-wise</li>
<li>Automatically stores reports in Google Drive</li>
<li>Provides CSV backups</li>
<li>Uses OAuth2 authentication</li>
<li>Modular and maintainable architecture</li>
<li>Easy to extend with additional reporting features</li>
</ul>
<hr>
<h1>Project Documentation</h1>
<p>Detailed project documentation is available in:</p>
<pre><code class="language-text">google-classroom-documentation.pdf</code></pre>
<p>It contains additional information about:</p>
<ul>
<li>Google Cloud setup</li>
<li>OAuth2 configuration</li>
<li>API integration</li>
<li>Application workflow</li>
<li>Report generation</li>
<li>Troubleshooting</li>
<li>Future enhancements</li>
</ul>
<hr>
<h1>Conclusion</h1>
<p>The <strong>Google Classroom Grade & Submission Exporter</strong> automates the collection and reporting of student engagement data from Google Classroom.</p>
<p>By integrating:</p>
<pre><code class="language-text">Java
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
OpenCSV</code></pre>
<p>the application provides a centralized solution for generating <strong>batch-wise, course-wise, and student-wise submission reports</strong>.</p>
<p>The modular architecture also makes it easy to extend the system with dashboards, analytics, Excel exports, scheduled reports, and additional Google Workspace integrations.</p>
<hr>
<p align="center">
<h3>Built with Java & Spring Boot</h3>
<p><strong>Google Classroom → Process → Report → Google Drive</strong></p>
</p>
