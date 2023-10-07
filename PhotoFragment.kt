package xyz.net7.savephotoregion

import android.content.Context
import xyz.net7.savephotoregion.PhotoFragment.OnFragmentInteractionListener
import android.graphics.Bitmap
import android.os.Bundle
import xyz.net7.savephotoregion.R
import android.hardware.Camera.ShutterCallback
import android.hardware.Camera.PictureCallback
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.os.Environment
import android.widget.Toast
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.OnScanCompletedListener
import android.support.v4.app.Fragment
import android.util.Log
import android.view.*
import xyz.net7.savephotoregion.databinding.FragmentPhotoBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.lang.RuntimeException
import java.text.SimpleDateFormat
import java.util.*

/*import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;*/
//PhotoFragment crops the image
class PhotoFragment : Fragment(), SurfaceHolder.Callback {
    var camera: Camera? = null
    var surfaceView: SurfaceView? = null
    var surfaceHolder: SurfaceHolder? = null
    var previewing = false
    var contexts: Context? = null//changed from context to contexts to avoid jvm override error

    //
    //private ActivityMainBinding binding;
    private var binding: FragmentPhotoBinding? = null

    /* @BindView(R.id.preview_layout)
    LinearLayout previewLayout;

    @BindView(R.id.border_camera)
    View borderCamera;
    @BindView(R.id.res_border_size)
    TextView resBorderSizeTV;*/
    private var mListener: OnFragmentInteractionListener? = null
    var previewSizeOptimal: Camera.Size? = null

    interface OnFragmentInteractionListener {
        fun onFragmentInteraction(bitmap: Bitmap?)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        /*View view = inflater.inflate(R.layout.fragment_photo, container, false);
        ButterKnife.bind(this, view);*/
        binding = FragmentPhotoBinding.inflate(inflater, container, false)
        val view: View = binding!!.root
        //returns LinearLayout root view
        contexts = getContext()//returns context of current activity
        //
        binding!!.makePhotoButton.setOnClickListener { makePhoto() }
        //
        surfaceView = view.findViewById<View>(R.id.camera_preview_surface) as SurfaceView
        surfaceHolder = surfaceView!!.holder
        surfaceHolder?.addCallback(this)
        surfaceHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException(
                context.toString()
                        + " must implement OnFragmentInteractionListener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        camera = Camera.open()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (previewing) {
            camera!!.stopPreview()
            previewing = false
        }
        if (camera != null) {
            try {
                val parameters = camera!!.parameters
                //get preview sizes
                val previewSizes = parameters.supportedPreviewSizes

                //find optimal - it very important
                previewSizeOptimal = getOptimalPreviewSize(
                    previewSizes, parameters.pictureSize.width,
                    parameters.pictureSize.height
                )

                //set parameters
                if (previewSizeOptimal != null) {
                    parameters.setPreviewSize(
                        previewSizeOptimal!!.width,
                        previewSizeOptimal!!.height
                    )
                }
                if (camera!!.parameters.focusMode.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                }
                if (camera!!.parameters.flashMode.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                    parameters.flashMode = Camera.Parameters.FLASH_MODE_AUTO
                }
                camera!!.parameters = parameters

                //rotate screen, because camera sensor usually in landscape mode
                val display =
                    (context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
                if (display.rotation == Surface.ROTATION_0) {
                    camera!!.setDisplayOrientation(90)
                } else if (display.rotation == Surface.ROTATION_270) {
                    camera!!.setDisplayOrientation(180)
                }

                //write some info
                val x1 = binding!!.previewLayout.width
                val y1 = binding!!.previewLayout.height
                val x2 = binding!!.borderCamera.width
                val y2 = binding!!.borderCamera.height
                val info = """
                    Preview width:$x1
                    Preview height:$y1
                    Border width:$x2
                    Border height:$y2
                    """.trimIndent()
                binding!!.resBorderSize.text = info
                //resBorderSizeTV for some reason before
                camera!!.setPreviewDisplay(surfaceHolder)
                camera!!.startPreview()
                previewing = true
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
    }

    fun getOptimalPreviewSize(sizes: List<Camera.Size>?, w: Int, h: Int): Camera.Size? {
        val ASPECT_TOLERANCE = 0.1
        val targetRatio = w.toDouble() / h
        if (sizes == null) return null
        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE

        // Try to find an size match aspect ratio and size
        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - h).toDouble()
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - h).toDouble()
                }
            }
        }
        return optimalSize
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        camera!!.stopPreview()
        camera!!.release()
        camera = null
        previewing = false
    }

    //@OnClick(R.id.make_photo_button)
    fun makePhoto() {
        if (camera != null) {
            camera!!.takePicture(
                myShutterCallback,
                myPictureCallback_RAW, myPictureCallback_JPG
            )
        }
    }

    var myShutterCallback = ShutterCallback { }
    var myPictureCallback_RAW = PictureCallback { data, camera -> }
    var myPictureCallback_JPG = PictureCallback { data, camera ->
        val bitmapPicture = BitmapFactory.decodeByteArray(data, 0, data.size)
        var croppedBitmap: Bitmap? = null
        val display =
            (context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        if (display.rotation == Surface.ROTATION_0) {

            //rotate bitmap, because camera sensor usually in landscape mode
            val matrix = Matrix()
            matrix.postRotate(90f)
            val rotatedBitmap = Bitmap.createBitmap(
                bitmapPicture,
                0,
                0,
                bitmapPicture.width,
                bitmapPicture.height,
                matrix,
                true
            )
            //save file
            createImageFile(rotatedBitmap)

            //calculate aspect ratio
            val koefX = rotatedBitmap.width.toFloat() / binding!!.previewLayout.width.toFloat()
            val koefY = rotatedBitmap.height.toFloat() / binding!!.previewLayout.height.toFloat()

            //get viewfinder border size and position on the screen
            val x1 = binding!!.borderCamera.left
            val y1 = binding!!.borderCamera.top
            val x2 = binding!!.borderCamera.width
            val y2 = binding!!.borderCamera.height

            //calculate position and size for cropping
            val cropStartX = Math.round(x1 * koefX)
            val cropStartY = Math.round(y1 * koefY)
            val cropWidthX = Math.round(x2 * koefX)
            val cropHeightY = Math.round(y2 * koefY)

            //check limits and make crop
            croppedBitmap =
                if (cropStartX + cropWidthX <= rotatedBitmap.width && cropStartY + cropHeightY <= rotatedBitmap.height) {
                    Bitmap.createBitmap(
                        rotatedBitmap,
                        cropStartX,
                        cropStartY,
                        cropWidthX,
                        cropHeightY
                    )
                } else {
                    null
                }

            //save result
            croppedBitmap?.let { createImageFile(it) }
        } else if (display.rotation == Surface.ROTATION_270) {
            // for Landscape mode
        }

        //pass to another fragment
        if (mListener != null) {
            if (croppedBitmap != null) mListener!!.onFragmentInteraction(croppedBitmap)
        }
        camera?.startPreview()
    }

    fun createImageFile(bitmap: Bitmap) {
        val path = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val timeStamp = SimpleDateFormat("MMdd_HHmmssSSS").format(Date())
        val imageFileName = "region_$timeStamp.jpg"
        val file = File(path, imageFileName)
        try {
            // Make sure the Pictures directory exists.
            if (path.mkdirs()) {
                Toast.makeText(context, "Not exist :" + path.name, Toast.LENGTH_SHORT).show()
            }
            val os: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.flush()
            os.close()
            Log.i("ExternalStorage", "Writed " + path + file.name)
            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(
                context, arrayOf(file.toString()), null
            ) { path, uri ->
                Log.i("ExternalStorage", "Scanned $path:")
                Log.i("ExternalStorage", "-> uri=$uri")
            }
            Toast.makeText(context, file.name, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w("ExternalStorage", "Error writing $file", e)
        }
    }
}