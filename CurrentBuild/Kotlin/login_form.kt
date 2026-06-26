package com.example.firedatabase_assis
import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.database.SQLException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.firedatabase_assis.databinding.ActivityLoginFormBinding


//LOGIN SCREEN
class login_form : AppCompatActivity() {//extends base compatability class of Android
    private lateinit var bind : ActivityLoginFormBinding //binding object to access views from XML without findViewById().
    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind= ActivityLoginFormBinding.inflate(layoutInflater) //Inflates the XML layout
        setContentView(bind.root) //Sets it as the activity UI
        var dbhelp=DB_class(applicationContext) //SQLite helper
        var db=dbhelp.readableDatabase //opens database for reading
        bind.btnlogin.setOnClickListener {//btnlogin = login button when clicked
            var username=bind.logtxt.text.toString();//converts input to username
            var password=bind.ed3.text.toString()//converts input to password
            val query="SELECT * FROM user WHERE username='"+username+"' AND pswd='"+password+"'"//select all data from user where username and password are given
            val rs=db.rawQuery(query,null)
            if(rs.moveToFirst()){//if user exists
                val intent= Intent(this,results::class.java);//move to results activity after login
                val name=rs.getString(rs.getColumnIndex("name"))//get the values of the user from the column
                val pswd=rs.getString(rs.getColumnIndex("pswd"))
                val username=rs.getString(rs.getColumnIndex("username"))
                val demographic=rs.getString(rs.getColumnIndex("demographic"))
                val email=rs.getString(rs.getColumnIndex("email"))

                rs.close() //close cursor after database resources are released
                //startActivity(Intent(this,welcome_window::class.java).putExtra("name",name))
                //startActivity(Intent(this,results::class.java).putExtra("name",name))
                UserSession.name = name
                UserSession.username = username
                UserSession.password = pswd
                UserSession.email = email
                UserSession.demographic = demographic
                startActivity(intent)//start the next activity in results
            }
            else{
                var ad = AlertDialog.Builder(this)
                ad.setTitle("Message")
                ad.setMessage("Username or password is incorrect!")
                ad.setPositiveButton("Ok", null)
                ad.show()
            }
        }
        //registration button that opens MainActivity
        bind.regisLink.setOnClickListener {
            val intent= Intent(this,MainActivity::class.java)
            startActivity(intent)
        }
    }
}
