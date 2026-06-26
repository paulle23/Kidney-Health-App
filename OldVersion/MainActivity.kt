package com.example.firedatabase_assis

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.firedatabase_assis.databinding.ActivityMainBinding


//REGISTER SCREEN
class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)//binding gives direct access to views
        setContentView(binding.root)//same as setContentView(R.layout.activity_main) but better
        var dbhelp=DB_class(applicationContext)//creates database helper
        var db=dbhelp.writableDatabase//opens up database for writing
        binding.btnrgs.setOnClickListener {//btnrgs = register button
            var name=binding.ed1.text.toString()//full name
            var username=binding.ed2.text.toString()//user name
            var password=binding.ed3.text.toString() // password
            var dem=binding.ed4.text.toString()//demographic
            var email=binding.ed5.text.toString()//email
            //var age=binding.ed3.text.toString() // password
            if(name.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty()) {
                var data = ContentValues() //stores column value pairs for database insertion
                data.put("name", binding.ed1.text.toString())
                data.put("username", binding.ed2.text.toString())
                data.put("pswd", binding.ed3.text.toString())
                data.put("demographic", binding.ed4.text.toString())
                data.put("email", binding.ed5.text.toString())
                var rs:Long = db.insert("user", null, data)//insert name, username, and password to a user in database
                if(!rs.equals(-1)) {//if it does not fail to insert
                    var ad = AlertDialog.Builder(this)
                    ad.setTitle("Message")
                    ad.setMessage("Account registered successfully")
                    ad.setPositiveButton("Ok", null)
                    ad.show()
                    binding.ed1.text.clear()//clear ed1 after
                    binding.ed2.text.clear()
                    binding.ed3.text.clear()
                    binding.ed4.text.clear()
                    binding.ed5.text.clear()
                }else{//it fails to insert data
                    var ad = AlertDialog.Builder(this)
                    ad.setTitle("Message")
                    ad.setMessage("Record not added")
                    ad.setPositiveButton("Ok", null)
                    ad.show()
                    binding.ed1.text.clear()
                    binding.ed2.text.clear()
                    binding.ed3.text.clear()
                    binding.ed4.text.clear()
                    binding.ed5.text.clear()
                }
            }else{//if empty, toast to tell user to fill fields to register
                Toast.makeText(this,"All fields required",Toast.LENGTH_SHORT).show()
            }
        }
        binding.loginLink.setOnClickListener {//open login screen after
            val intent=Intent(this,login_form::class.java)
            startActivity(intent)
        }
    }
}
