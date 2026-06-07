package com.example.firedatabase_assis

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.firedatabase_assis.databinding.ActivityResultsBinding


//readings page
class results : AppCompatActivity() {
    private lateinit var bind: ActivityResultsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind= ActivityResultsBinding.inflate(layoutInflater)
        setContentView(bind.root)
        var value=intent.getStringExtra("name")//receive from previous activity through intent
        var value1=intent.getStringExtra("pswd")
        var value2=intent.getStringExtra("username")
        var value3=intent.getStringExtra("demographic")
        var value4=intent.getStringExtra("email")
        bind.uname.text=value2//display data on screen
        bind.password.text=value1
        bind.name.text=value
        bind.demographic.text=value3
        bind.email.text=value4
        bind.logout.setOnClickListener {
            startActivity(Intent(this,login_form::class.java))
        }
        bind.txt2.setOnClickListener {
            startActivity(Intent(this,welcome_window::class.java))
        }
    }
}