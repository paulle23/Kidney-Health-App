package com.example.firedatabase_assis

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.firedatabase_assis.databinding.ActivityResultsBinding

/*results page where user has their information
displayed and can choose to go to camera, gallery, or logout*/
class results : AppCompatActivity() {
    private lateinit var bind: ActivityResultsBinding//binding object to access ui elements without findViewbyID
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind= ActivityResultsBinding.inflate(layoutInflater)//inflates layout 
        setContentView(bind.root)//shows layout on screen
        //Get all the user information
        bind.uname.text = UserSession.username
        bind.password.text = UserSession.password//value1 (optional: avoid storing passwords globally)
        bind.name.text = UserSession.name
        bind.demographic.text = UserSession.demographic
        bind.email.text = UserSession.email
        bind.logout.setOnClickListener {//logout button
            UserSession.clear()//clear the session 
            val intent = Intent(this, login_form::class.java)
            //remove all previous activities from memory and prevents user from pressing back to return after logout
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        bind.txt2.setOnClickListener {//send the information over to the camera view as well
            val intent = Intent(this, welcome_window::class.java) /*.apply {
                putExtra("name", value)
                putExtra("username", value2)
                putExtra("email", value4)
            }*/
            startActivity(intent)
        }
        bind.Gallery.setOnClickListener {//go to the gallery of each user
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }
    }
}
