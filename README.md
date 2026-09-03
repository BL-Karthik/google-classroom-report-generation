

[google-classroom-documentation (2).pdf](https://github.com/user-attachments/files/22420534/google-classroom-documentation.2.pdf)
        Google Classroom API – Export Grades Data in Tabular Format 
 
1. Introduction  
  The Google Classroom Submission Exporter is a Spring Boot application designed to fetch student submission data from Google Classroom and export it to Google Sheets and local CSV files. It uses OAuth2 for secure authentication and interacts with Google Classroom, 
Google Sheets, and Google Drive APIs. The application organizes data by course batches (B1, B2, B3, B4, or Other_Batches) and generates detailed reports for educators or administrators.  
This documentation provides a clear, step-by-step explanation of the project, including its purpose, setup, functionality, and usage, with screenshots for clarity.  
  
2. Project Overview 
2.1 Purpose  
The application automates the process of collecting student submission data from Google Classroom and exporting it into organized formats:  
Google Sheets: One sheet per batch, with tabs for each course.  
CSV Files: Local backups for each course.  
2.2 Key Features  
Authenticate users via Google OAuth2.  
Fetch courses, students, coursework, and submission statuses from Google Classroom.  
Organize data by batch (e.g., B1, B2, etc.).  
Export data to Google Sheets in a structured format.  
Save google sheet files in drive.  
Handle errors gracefully with custom error pages.  
2.3 Technologies Used  
Spring Boot: Backend framework.  
Google APIs: Classroom, Sheets, and Drive APIs.  
OAuth2: For secure authentication.  
OpenCSV: For generating CSV files.  
WebClient: For making API calls.  
Java: Programming language.  
3.Project Structure  
  The project is organized into packages for modularity:  
com.bridgelabz: Main application class (Success2Application). com.bridgelabz.controller: REST controllers for handling API requests. com.bridgelabz.service: Services for interacting with Google APIs and processing data. com.bridgelabz.csvUtiles: Utility for generating CSV files.  com.bridgelabz.model: Data models (e.g., StudentInfo). com.bridgelabz.security: Security configuration for OAuth2.  com.bridgelabz.error: Custom error handling. com.bridgelabz.handler: OAuth2 error handling.  
Key Files application.properties: Configuration for server port, OAuth2, and Google API settings.  
ClassroomController.java: Handles the main endpoint to fetch and export data.  
GoogleClassroomService.java: Core logic for fetching and organizing data.  
GoogleSheetsDriveService.java: Exports data to Google Sheets and manages Drive folders.  
4. Setup and Installation  
4.1 Prerequisites  
Java 17 or higher.  
Maven: For dependency management.  
Google Cloud Project: With Classroom, Sheets, and Drive APIs enabled.  
OAuth2 Credentials: Client ID and Client Secret from Google Cloud Console.  
4.2 Configuration  Create a new project.  
Enable the following APIs:  
Google Classroom API 
Google Sheets API  
Google Drive API  
 
Step 1: Enable Google Classroom API 
1.	Go to the Google Cloud Console. 
2.	Create a new project or select an existing one.  
   
3.	Navigate to APIs & Services → Library.  
4.	Search for Google Classroom API and enable it. 
  
As well as do same for following API’S 
Google Sheets API  
  
Google Drive API  
  
5.	Go to APIs & Services → Credentials. 
6.	Click on + Create Credentials → OAuth 2.0 Client IDs. 
  
 
7.	Choose Desktop App or Web Application (for testing purposes). 
   
Set the redirect URI to http://localhost:8084/login/oauth2/code/google.  
  
 
   
Download the credentials (Client ID and Client Secret).  
  
Update application.properties: Open src/main/resources/application.properties and add your credentials in properties file.  
server.port=8084  
spring.security.oauth2.client.registration.google.client-id=<your-client-id> 
spring.security.oauth2.client.registration.google.client-secret=<your-client-secret> spring.security.oauth2.client.registration.google.redirecturi=http://localhost:8084/login/oau th2/code/google google.drive.folder-id=<your-drive-folder-id>  
5. How It Works  
 5.1 Authentication: 
   The application uses OAuth2 to authenticate users with their Google accounts. Upon accessing the application, users are redirected to Google’s login page. 
   
   
After successful login, users are redirected to the /classroom/all-student-submissions endpoint.  
5.2 Fetching Data  
The GoogleClassroomService fetches data in the following steps:  
Courses: Retrieves all courses using the Classroom API.  
Students: Fetches student details (name, email) for each course.  
Coursework: Collects coursework titles for each course.  
Submissions: Retrieves submission statuses (e.g., Submitted, Assigned, Missing) for each student and coursework.  
5.3 Organizing Data  
Courses are grouped by batch (B1, B2, B3, B4, or Other_Batches) based on their names.  
Student data is stored in a map: batch -> course -> student -> (email, submission statuses).  
5.4 Exporting Data Google Sheets:  
One spreadsheet is created per batch.  
Each course in the batch gets its own tab.  
Columns include student name, email, and submission statuses for each coursework.  
The spreadsheet is saved in a specified Google Drive folder.  
Google sheet Files:  
A google sheet  file is generated for each course, saved in drive.  
Columns match the Google Sheets format. 	  
   
 
 
  
5.6 Error Handling  
If the OAuth2 token is invalid, a 401 error is returned.  
API errors are caught and logged, with a 500 error response.  
Custom error pages are displayed for 404 (Not Found) and 500 (Server Error) statuses. 
    
6. Usage  
Access the Application: Open a browser and go to http://localhost:8084.  
Log In: Sign in with a Google account that has access to the Classroom courses.  
Fetch and Export Data: After login, the application automatically calls the /classroom/allstudent-submissions endpoint, which Fetches all course data.  
Generates Google Sheets and CSV files.  
Returns a success message: “All Sheets exported successfully!” View 
Outputs:  
Check the Google Drive folder for the generated spreadsheets.  
Look in the project’s root directory for CSV files.  
   
  
  
7. Troubleshooting  
 7.1 Common Issues  
Invalid OAuth2 Credentials:  
Ensure the Client ID and Client Secret are correct in application.properties.  
Verify the redirect URI matches the one in Google Cloud Console.  
API Errors:  
Check if the required APIs are enabled in Google Cloud Console.  
Ensure the Google account has access to the Classroom courses.  
Memory Issues:  
The application is configured to handle large responses (spring.codec.max-inmemorysize=50MB). Increase this value if needed.  
7.2 Logs  
Enable debug logging in application.properties to troubleshoot:  logging.level.org.springframework.security=DEBUG Check the console for detailed error messages.  
8. Future Improvements  
Add support for filtering courses by date or status.  
Implement a user interface for easier interaction.  
Allow users to customize the export format (e.g., include additional columns).  
Add support for exporting to other formats (e.g., Excel).  
9. Conclusion  
The Google Classroom Submission Exporter simplifies the process of collecting and organizing student submission data. By integrating with Google APIs, it provides a scalable solution for educators to track student progress across multiple courses. The application is secure, modular, and easy to extend for additional features.  
  

