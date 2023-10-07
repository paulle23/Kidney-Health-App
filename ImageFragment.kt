package xyz.net7.savephotoregion

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import xyz.net7.savephotoregion.databinding.FragmentImageBinding

/*import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;*/
//ImageFragment shows the cropped image
//extend the fragment class
class ImageFragment : Fragment() {
    private var bitmap: Bitmap? = null

    //bitmap = representation of the image
    private var binding //one bind for all id's
            : FragmentImageBinding? = null

    //FragmentImageBinding or ActivityMainBinding?
    /*@BindView(R.id.res_photo)
    ImageView resPhoto;

    @BindView(R.id.res_photo_size)
    TextView resPhotoSize;*/
    fun imageSetupFragment(bitmap: Bitmap?) {
        if (bitmap != null) {
            this.bitmap = bitmap
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentImageBinding.inflate(inflater, container, false)
        //View view = inflater.inflate(R.layout.fragment_image, container, false);
        //ButterKnife.bind(this, view);
        val view: View = binding!!.root
        //returns LinearLayout root view

        //check if bitmap exist, set to ImageView
        if (bitmap != null) {
            binding!!.resPhoto.setImageBitmap(bitmap)
            val info = """
                image with:${bitmap!!.width}
                image height:${bitmap!!.height}
                """.trimIndent()
            binding!!.resPhotoSize.text = info //displays size of the photo
        }
        return view
    }
}