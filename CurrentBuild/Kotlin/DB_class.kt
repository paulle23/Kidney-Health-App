package com.example.firedatabase_assis
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
//SqLiteDataBase that contains all of the user login information
//context gives the class access to the application
class DB_class(context: Context): SQLiteOpenHelper(context,DATABASE_NAME,null,DATABASE_VERSION) {//extends SQLiteOpenHelper
    companion object {//column names, contains variables that belong to the class instead of individual object
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "Database"
        private val TABLE_CONTACTS = "user"//also table_name
        private val KEY_NAME = "name"
        private val KEY_UNAME = "username"
        private val KEY_PSWD = "pswd"
        private val KEY_DEM = "demographic"
        private val KEY_EMAIL = "email"
        //private val KEY_AGE = "age"
        //added
        private val KEY_IMAGE = "Category_img"

    }
    //table has name, username, password, demographic, and email
    override fun onCreate(db: SQLiteDatabase?) {
        val newtb = ("CREATE TABLE " + TABLE_CONTACTS + "("
                + KEY_NAME + " TEXT," + KEY_UNAME + " TEXT,"
                + KEY_PSWD + " TEXT," + KEY_DEM + " TEXT," + KEY_EMAIL + " TEXT," + KEY_IMAGE + " BLOB)"
                )
        /*KEY_DEM + " TEXT," + KEY_EMAIL + " TEXT," + KEY_AGE + " TEXT" + ")"*/
        db?.execSQL(newtb)
    }
    //when database version changes, delete old table and create new
    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS)
        onCreate(db)
    }

    //added, inserts image data into database
    fun insertData(Category_img: ByteArray): Boolean {
        val db = writableDatabase //opens database for writing
        val cv = ContentValues() //key value container for insert operations
        cv.put(KEY_IMAGE, Category_img) //stores image bytes under column "Category_img

        val result = db.insert(TABLE_CONTACTS, null, cv) //inserts into table

        return if (result .equals( -1))
            false
        else
            true
    }
    fun getdata(): ByteArray {//gets image from database
        val db = writableDatabase
        val res = db.rawQuery("select * from " + TABLE_CONTACTS, null)//returns cursor

        if (res.moveToFirst()) {//checks if table returns data
            do {
                return res.getBlob(0)
            } while (res.moveToNext())
        }
        return byteArrayOf()
    }

}
