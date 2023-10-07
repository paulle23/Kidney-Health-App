package xyz.net7.savephotoregion;

import android.content.Context;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

/*import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;*/

import xyz.net7.savephotoregion.databinding.ActivityMainBinding;

/*import butterknife.ButterKnife;
import butterknife.OnClick;*/
//top photo button takes you to camera, bottom photo button takes the picture once you are in camera

public class MainActivity extends AppCompatActivity implements PhotoFragment.OnFragmentInteractionListener {

    int PERMISSION_ALL = 1;
    boolean flagPermissions = false;

    String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
    };

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*setContentView(R.layout.activity_main);
        ButterKnife.bind(this);*/
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        checkPermissions();
        binding.makePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                onClickScanButton();
            }
        });

    }

    //@OnClick(R.id.make_photo_button)
    void onClickScanButton() {
        // check permissions
        if (!flagPermissions) {
            checkPermissions();
            return;
        }
        //start photo fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.res_photo_layout, new PhotoFragment())
                .addToBackStack(null)
                .commit();
    }

    void checkPermissions() {
        if (!hasPermissions(this, PERMISSIONS)) {
            requestPermissions(PERMISSIONS,
                    PERMISSION_ALL);
            flagPermissions = false;
        }
        flagPermissions = true;

    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public void onFragmentInteraction(Bitmap bitmap) {
        if (bitmap != null) {
            ImageFragment imageFragment = new ImageFragment();
            imageFragment.imageSetupFragment(bitmap);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.res_photo_layout, imageFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}
