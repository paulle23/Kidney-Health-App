package com.example.firedatabase_assis

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.firedatabase_assis.databinding.ActivityLoginRegisterBinding

//first page with login and register
class login_register : AppCompatActivity() {
    private lateinit var bind: ActivityLoginRegisterBinding /*bind gives direct access to UI elements in the XML layout.
    lateinit means: Variable will be initialized later Avoids null values*/
    //@SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) //Calls the parent activity setup
        bind = ActivityLoginRegisterBinding.inflate(layoutInflater) //creates binding object and connects code to XML layout
        setContentView(bind.root) //display activity layout
        bind.btnlogin.setOnClickListener {//if you click login, takes you to login_form screen
            val intent = Intent(this, login_form::class.java)
            startActivity(intent)
        }
        bind.regisLink.setOnClickListener {//if register is clicked, takes you to MainActivity, which is the register screen
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
