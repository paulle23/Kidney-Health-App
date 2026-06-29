# Kidney-Health-App

:CurrentVersion
📱 Android User Authentication & Camera Gallery App

An Android application that integrates user authentication, session management, custom Camera2 API functionality, and a personalized image gallery system. Users can register, log in, capture images, and view their photos in a structured gallery.

Features
🔐 User Authentication
User registration and login system using SQLite database
Secure credential validation
Error handling for incorrect login attempts
👤 Session Management
Global user session using singleton (UserSession)
Stores logged-in user details across activities
Logout functionality with session clearing
📷 Custom Camera (Camera2 API)
Live camera preview using TextureView
Manual focus control using SeekBar
High-resolution image capture using ImageReader
Saves images directly to device storage
🖼 Image Storage & Gallery
Images saved in MediaStore

User-specific folder structure:

Pictures/MyCameraApp/<username>/
Gallery view using RecyclerView (GridLayoutManager)
Efficient image loading with Glide
🔍 Image Preview
Full-screen image preview using PreviewActivity
Smooth navigation from gallery or camera capture
🧱 Tech Stack
Kotlin
Android SDK
Camera2 API
SQLite Database
RecyclerView
Glide
MediaStore
View Binding
📱 App Flow
App Launch
   ↓
Login / Register Screen
   ↓
Login Success
   ↓
Dashboard (Results)
   ↓
 ├── Camera (Capture Images)
 ├── Gallery (View Images)
 └── Logout
📷 Camera Workflow
Live Preview (TextureView)
        ↓
User adjusts focus (SeekBar)
        ↓
Capture Image
        ↓
ImageReader receives JPEG
        ↓
Save to MediaStore
        ↓
Open Preview Screen
🖼 Gallery Workflow
Query MediaStore
        ↓
Filter images by username folder
        ↓
Display using RecyclerView grid
        ↓
Click image → Open PreviewActivity
🧠 Key Concepts Used
Android Activity lifecycle
Camera2 API implementation
SQLite database operations
Singleton pattern (session management)
RecyclerView Adapter pattern
MediaStore file storage system

OldVersion - The version of the code I submitted for my Design Project. It has the login and register screen, login, results page, and camera view. User login info is kept through intents. 
    old_welcome_window.kt - original code for the camera screen
    welcome_window.kt is the cleaned up version with some things changed up and unnecessary bloat deleted
 
Overall Application starts at the Login and Register Screen
Once logged in, takes you to results.kt
From there you can click on the + on the top left to access the built in camera preview and manipulate the focus to your desire. 
     You can also click on Gallery to see all the pictures associated with an account or logout to log out.
     
Click the photo button, which saves the photo directly into your android phone gallery instead of inside the Android/data/com.example.firedatabase_assis/files folder
