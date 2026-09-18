<h1>Google Classroom Gradebook Automation</h1>

<h2>Project Overview</h2>
<p>
This project automates the extraction of student gradebook data from
Google Classroom using Selenium UI Automation and exports the data
to Google Sheets.
</p>

<h2>Technologies Used</h2>
<ul>
    <li>Java</li>
    <li>Selenium WebDriver</li>
    <li>Google Sheets API</li>
    <li>Maven</li>
    <li>Chrome Browser</li>
    <li>Git</li>
</ul>

<h2>Project Structure</h2>

<pre>
src/main/java/com/bridgelabz/

├── ClassroomMultiExport.java
│
├── models/
│   ├── AssignmentColumn.java
│   ├── AssignmentResult.java
│   ├── ClassroomCellData.java
│   ├── CourseInfo.java
│   ├── CourseSummary.java
│   ├── StudentData.java
│   └── StudentRawData.java
│
└── service/
    ├── BrowserService.java
    ├── CourseService.java
    ├── GradeBookService.java
    ├── AssignmentService.java
    ├── StudentService.java
    ├── SubmissionService.java
    ├── GoogleSheetService.java
    └── SummaryService.java
</pre>

<h2>Services</h2>

<h3>BrowserService</h3>
<ul>
    <li>Creates Selenium WebDriver</li>
    <li>Loads Chrome profile</li>
    <li>Manages browser configuration</li>
    <li>Closes browser</li>
</ul>

<h3>CourseService</h3>
<ul>
    <li>Opens Google Classroom</li>
    <li>Fetches courses</li>
    <li>Processes course information</li>
    <li>Routes courses to B1 or B2 spreadsheets</li>
</ul>

<h3>GradeBookService</h3>
<ul>
    <li>Opens the Grades section</li>
    <li>Waits for gradebook loading</li>
    <li>Handles gradebook scrolling</li>
</ul>

<h3>AssignmentService</h3>
<ul>
    <li>Extracts assignments</li>
    <li>Creates assignment information</li>
</ul>

<h3>StudentService</h3>
<ul>
    <li>Extracts student information</li>
    <li>Processes student data</li>
    <li>Creates StudentData objects</li>
</ul>

<h3>SubmissionService</h3>
<ul>
    <li>Extracts submission information</li>
    <li>Identifies submission status</li>
    <li>Maps submissions to assignments</li>
</ul>

<h3>GoogleSheetService</h3>
<ul>
    <li>Creates Google Sheets service</li>
    <li>Creates and clears sheets</li>
    <li>Exports course data</li>
    <li>Formats Google Sheets</li>
</ul>

<h3>SummaryService</h3>
<ul>
    <li>Creates course/student summaries</li>
    <li>Calculates submission counts</li>
    <li>Calculates submission percentages</li>
    <li>Exports summary data</li>
</ul>

<h2>Data Flow</h2>

<pre>
Google Classroom
       ↓
BrowserService
       ↓
CourseService
       ↓
GradeBookService
       ↓
AssignmentService
       ↓
StudentService
       ↓
SubmissionService
       ↓
SummaryService
       ↓
GoogleSheetService
       ↓
Google Sheets
</pre>

<h2>Course Routing</h2>

<ul>
    <li>B1 / BB1 → B1 Spreadsheet</li>
    <li>B2 / BB2 → B2 Spreadsheet</li>
    <li>Unsupported courses → Skipped</li>
</ul>

<h2>Summary Sheet</h2>

<table>
    <tr>
        <th>Course Name</th>
        <th>Roll Number</th>
        <th>Student Name</th>
        <th>Submissions</th>
        <th>Percentage</th>
    </tr>
    <tr>
        <td>B1 Java</td>
        <td>101</td>
        <td>Student 1</td>
        <td>8</td>
        <td>80%</td>
    </tr>
</table>

<h2>Configuration</h2>

<ul>
    <li>Google Service Account</li>
    <li>Google Spreadsheet IDs</li>
    <li>Chrome Profile Path</li>
    <li>Chrome Profile Directory</li>
</ul>

<h2>Service Account</h2>

<p>
The <b>service_account.json</b> file is required for Google Sheets access.
</p>

<p>
<strong>Important:</strong> Do not commit the service account JSON file
to Git because it contains credentials.
</p>

<h2>Running the Application</h2>

<ol>
    <li>Clone the repository</li>
    <li>Configure the Google service account</li>
    <li>Configure the Chrome profile</li>
    <li>Configure spreadsheet IDs</li>
    <li>Run <code>mvn clean install</code></li>
    <li>Run <code>ClassroomMultiExport.java</code></li>
</ol>

<h2>Course Filtering</h2>

<p>
The application supports optional course-name filtering.
</p>

<pre>
java ClassroomMultiExport "Course Name"
</pre>

<h2>Error Handling</h2>

<ul>
    <li>Google Sheets API errors</li>
    <li>Missing spreadsheet access</li>
    <li>Empty courses</li>
    <li>Courses without assignments</li>
    <li>Courses without students</li>
    <li>Selenium loading issues</li>
    <li>Individual course processing failures</li>
</ul>

<h2>Output</h2>

<p>
The application exports course-wise student submission data and
summary information to Google Sheets.
</p>

<h2>Future Improvements</h2>

<ul>
    <li>Improve Selenium wait handling</li>
    <li>Add retry mechanism</li>
    <li>Move configuration to properties</li>
    <li>Improve logging</li>
    <li>Add automated tests</li>
</ul>
