package xyz.net7.savephotoregion;


import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import xyz.net7.savephotoregion.databinding.ActivityMainBinding;
import xyz.net7.savephotoregion.databinding.FragmentImageBinding;

/*import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;*/

//ImageFragment shows the cropped image
//extend the fragment class
public class ImageFragment extends Fragment {

    private Bitmap bitmap;
    //bitmap = representation of the image

    private FragmentImageBinding binding;//one bind for all id's
    //FragmentImageBinding or ActivityMainBinding?
    /*@BindView(R.id.res_photo)
    ImageView resPhoto;

    @BindView(R.id.res_photo_size)
    TextView resPhotoSize;*/

    public void imageSetupFragment(Bitmap bitmap) {
        if (bitmap != null) {
            this.bitmap = bitmap;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentImageBinding.inflate(inflater, container, false);
        //View view = inflater.inflate(R.layout.fragment_image, container, false);
        //ButterKnife.bind(this, view);
        View view = binding.getRoot();
        //returns LinearLayout root view

        //check if bitmap exist, set to ImageView
        if (bitmap != null) {
            binding.resPhoto.setImageBitmap(bitmap);
            String info = "image with:" + bitmap.getWidth() + "\n" + "image height:" + bitmap.getHeight();
            binding.resPhotoSize.setText(info);//displays size of the photo
        }
        return view;
    }
}
