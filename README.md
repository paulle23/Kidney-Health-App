# Kidney-Health-App

CurrentVersion is the current form of the application with the files that I have written personally for the implementation. I wrote the database, login and register screens, the camera view and functionality with the manual focus/low-level camera control, and the welcome screen. Both the XML layout files and the Kotlin backend files are written.
Features
    - Login and Register Page
    - Login Page - uses UserSession.kt to store user information throughout the different activities instead of having to use intent
         - once you log in you are taken to the results page
    - Results page - you can click on the + to open the camera view and take a picture, Gallery button to see all of the photos for each user, and a logout button that clears the userSession and logs out the user
    - Welcome_window page - this is the camera view, it receives the userSession username to store the picture in a subfolder for each user, and there is manual focus. ONce you take a picture, it automatically saves the picture and takes you to PreviewActivity, which shows the most recent picture
    - Gallery Page - stores all of the pictures for each user. You can click on each picture, which takes you to the PreviewActivity with whatever picture you chose. 

OldVersion - The version of the code I submitted for my Design Project. It has the login and register screen, login, results page, and camera view. User login info is kept through intents. 
    old_welcome_window.kt - original code for the camera screen
    welcome_window.kt is the cleaned up version with some things changed up and unnecessary bloat deleted
 
Overall Application starts at the Login and Register Screen
Once logged in, takes you to results.kt
From there you can click on the + on the top left to access the built in camera preview and manipulate the focus to your desire. 
     You can also click on Gallery to see all the pictures associated with an account or logout to log out.
     
Click the photo button, which saves the photo directly into your android phone gallery instead of inside the Android/data/com.example.firedatabase_assis/files folder
