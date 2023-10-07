package xyz.net7.savephotoregion

import android.Manifest
import android.content.Context
//import xyz.net7.savephotoregion.ImageFragment.imageSetupFragment
import android.support.v7.app.AppCompatActivity
import xyz.net7.savephotoregion.PhotoFragment.OnFragmentInteractionListener
import android.os.Bundle
import xyz.net7.savephotoregion.R
import xyz.net7.savephotoregion.PhotoFragment
import xyz.net7.savephotoregion.MainActivity
import android.graphics.Bitmap
import xyz.net7.savephotoregion.ImageFragment
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.view.View
import xyz.net7.savephotoregion.databinding.ActivityMainBinding

/*import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;*/ /*import butterknife.ButterKnife;
import butterknife.OnClick;*/
//top photo button takes you to camera, bottom photo button takes the picture once you are in camera
class MainActivity : AppCompatActivity(), OnFragmentInteractionListener {
    var PERMISSION_ALL = 1
    var flagPermissions = false
    var PERMISSIONS = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )
    private var binding: ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*setContentView(R.layout.activity_main);
        ButterKnife.bind(this);*/binding = ActivityMainBinding.inflate(
            layoutInflater
        )
        val view: View = binding!!.root
        setContentView(view)
        checkPermissions()
        binding!!.makePhotoButton.setOnClickListener { onClickScanButton() }
    }

    //@OnClick(R.id.make_photo_button)
    fun onClickScanButton() {
        // check permissions
        if (!flagPermissions) {
            checkPermissions()
            return
        }
        //start photo fragment
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.res_photo_layout, PhotoFragment())
            .addToBackStack(null)
            .commit()
    }

    fun checkPermissions() {
        if (!hasPermissions(this, *PERMISSIONS)) {
            requestPermissions(
                PERMISSIONS,
                PERMISSION_ALL
            )
            flagPermissions = false
        }
        flagPermissions = true
    }

    override fun onFragmentInteraction(bitmap: Bitmap?) {
        if (bitmap != null) {
            val imageFragment = ImageFragment()
            imageFragment.imageSetupFragment(bitmap)
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.res_photo_layout, imageFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {
        fun hasPermissions(context: Context?, vararg permissions: String?): Boolean {
            if (context != null && permissions != null) {
                for (permission in permissions) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            permission!!
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return false
                    }
                }
            }
            return true
        }
    }
}