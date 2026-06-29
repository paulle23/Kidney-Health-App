package com.example.firedatabase_assis

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.OrientationEventListener

/*Camera Screen with Manual Focus
* Opens the camera
* Shows live preview on screen through TextureView
* Captures image and saves to gallery
* TextureView → SurfaceTexture
        ↓
CameraDevice
        ↓
CaptureSession
        ↓
Preview (repeating request)
        ↓
ImageReader (captures JPEG)
        ↓
MediaStore (saves image)*/
class welcome_window : AppCompatActivity() {
    /*lateinit keeps property from being initialized until later, avoid initializing a property when an object is constructed.
    If property is referenced before being initialized, UninitializedPropertyAccessException.*/
    private lateinit var textureView: TextureView
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice//camera
    private lateinit var session: CameraCaptureSession//preview and capture requests
    private lateinit var previewRequest: CaptureRequest.Builder//settings for camera like focus, exposure
    private lateinit var imageReader: ImageReader//receives captured JPEG images
    private lateinit var cameraId: String
    private lateinit var previewSize: Size
    private var backgroundThread: HandlerThread? = null//camera operations thread
    private var backgroundHandler: Handler? = null//sends tasks to the thread
    private var focusDistance = 0f
    private var isCapturing = false
    private var lastImageUri: Uri? = null//store the last captured URI
    private var username: String? = null
    private var name: String? = null
    private val ORIENTATIONS = SparseIntArray().apply {
        append(Surface.ROTATION_0, 90)
        append(Surface.ROTATION_90, 0)
        append(Surface.ROTATION_180, 270)
        append(Surface.ROTATION_270, 180)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome_window)

        textureView = findViewById(R.id.texture_view)//shows camera preview
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        username = UserSession.username
        name = UserSession.name

        findViewById<Button>(R.id.take_photo_btn).setOnClickListener {//button to take photo
            capturePhoto()
        }

        findViewById<SeekBar>(R.id.seekBar).setOnSeekBarChangeListener(//R.id.id from the activity_main layout
            object : SeekBar.OnSeekBarChangeListener {
            /*reads cameras minimum focus distance, converts slider to focus distance, and updates preview focus in real time
            * Higher = closer, lower = farther*/
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (!::previewRequest.isInitialized) return

                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    //minimum focus distance
                    val minFocus = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f

                    if (minFocus > 0f) {
                        focusDistance = minFocus * (1f - progress / 100f)
                        previewRequest.set(CaptureRequest.LENS_FOCUS_DISTANCE, focusDistance)
                        updatePreview()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            }
        )

        if (hasPermission()) {
            textureView.surfaceTextureListener = surfaceListener
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                1
            )
        }
    }

    //CAMERA SETUP
    private fun openCamera() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA//if not granted, then request permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        //Turns the camera on
        cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
    }

    private fun setupCamera() {
        for (id in cameraManager.cameraIdList) {//loops through all the cameras
            val chars = cameraManager.getCameraCharacteristics(id)

            if (chars.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT
            ) continue//skips front facing, only selects rear facing

            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue

            cameraId = id
            //picks largest resolution available
            previewSize = map.getOutputSizes(SurfaceTexture::class.java).maxByOrNull { it.width * it.height }!!

            val jpegSizes = map.getOutputSizes(ImageFormat.JPEG)
            val jpegSize = jpegSizes?.maxByOrNull { it.width * it.height }//highest quality
                ?: previewSize
            
            if (jpegSize != null) {
                imageReader = ImageReader.newInstance(
                    jpegSize.width,
                    jpegSize.height,
                    ImageFormat.JPEG,
                    2
                )//this is where captured images go
            }
            //ImageReader → JPEG bytes → saveToGallery()
            /*Get image buffer
            * convert to byte array
            * save to gallery
            * show Toast that says "saved"*/
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val buffer = image.planes[0].buffer//read image
                    val bytes = ByteArray(buffer.remaining())//convert to byte array
                    buffer.get(bytes)

                    lastImageUri = saveToGallery(bytes)//saves image
                    Log.d("CAMERA", "Saved URI: $lastImageUri")

                } finally {
                    image.close()
                }

                runOnUiThread {//ui update
                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()

                    // open next page aka PreviewActivity
                    lastImageUri?.let {
                        Log.d("CAMERA", "Sending URI to PreviewActivity: $lastImageUri")
                        val intent = Intent(this, PreviewActivity::class.java)
                        intent.putExtra("image_uri", lastImageUri.toString())//sends image to preview screen
                        startActivity(intent)
                    }
                }

                isCapturing = false
            }, backgroundHandler)

            break
        }
    }

    //PREVIEW
    private fun createPreviewSession() {//live camera feed
        val texture = textureView.surfaceTexture!!
        texture.setDefaultBufferSize(previewSize.width, previewSize.height)
        val surface = Surface(texture)//create surface from TextureView

        //create preview request
        previewRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewRequest.addTarget(surface)//attach surface

        cameraDevice.createCaptureSession(
            listOf(surface, imageReader.surface),
            object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(s: CameraCaptureSession) {
                    session = s

                    previewRequest.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CameraMetadata.CONTROL_AF_MODE_OFF
                    )//disable auto-focus
                    //start live preview that continuously streams camera frames
                    session.setRepeatingRequest(previewRequest.build(), null, backgroundHandler)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            },
            backgroundHandler
        )
    }

    private fun updatePreview() {
        if (!::session.isInitialized || !::previewRequest.isInitialized) return
        session.setRepeatingRequest(previewRequest.build(), null, backgroundHandler)
    }

    //CAPTURE
    private fun capturePhoto() {
        if (!::session.isInitialized) return
        if (isCapturing || !::cameraDevice.isInitialized) return
        isCapturing = true

        val rotation = windowManager.defaultDisplay.rotation
        //build still capture request
        val request = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        //set output target
        request.addTarget(imageReader.surface)
        //set focus
        request.set(CaptureRequest.LENS_FOCUS_DISTANCE, focusDistance)
        //fix image rotation
        request.set(
            CaptureRequest.JPEG_ORIENTATION,
            getJpegOrientation()
        )
        //capture the image once
        session.capture(
            request.build(),
            object : CameraCaptureSession.CaptureCallback() {},
            backgroundHandler
        )
    }
    private fun getJpegOrientation(): Int {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)

        val sensorOrientation = characteristics.get(
            CameraCharacteristics.SENSOR_ORIENTATION
        ) ?: 0

        val deviceRotation = windowManager.defaultDisplay.rotation

        val surfaceRotationDegrees = when (deviceRotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        val result = (sensorOrientation + surfaceRotationDegrees + 360) % 360
        return result
    }

    //BACKGROUND THREAD for Camera2
    private fun startThread() {
        backgroundThread = HandlerThread("camera").apply { start() }//Camera2 does not run on main ui thread
        backgroundHandler = Handler(backgroundThread!!.looper)
    }
    private fun stopThread() {
        backgroundThread?.quitSafely()
        backgroundThread?.join()
        backgroundThread = null
        backgroundHandler = null
    }

    //LIFECYCLE
    override fun onResume() {//start camera thread and attach texture listener
        super.onResume()
        startThread()
        textureView.surfaceTextureListener = surfaceListener
    }
    override fun onRequestPermissionsResult(//callback for result on requesting permissions
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            textureView.surfaceTextureListener = surfaceListener
        }
    }
    override fun onPause() {//close camera and stop background thread
        super.onPause()
        if (::session.isInitialized) session.close()
        if (::cameraDevice.isInitialized) cameraDevice.close()
        if (::imageReader.isInitialized) imageReader.close()
        stopThread()
    }

    //SURFACE
    private val surfaceListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
            setupCamera()
            openCamera()
        }/*when preview surface/ui is ready, setup and open the camera*/

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {}
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = true
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    //CAMERA CALLBACK
    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {//when camera opens
            cameraDevice = camera
            createPreviewSession()//start the preview
        }
        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
        }
        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
        }
    }

    //SAVE IMAGE
    private fun saveToGallery(bytes: ByteArray): Uri? {
        val username = UserSession.username ?: "unknown"//save per user

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}_${username}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            //user-specific folder
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/MyCameraApp/$username"
            )        }
        /*create metadata (name, type, folder)
        * insert into MediaStore
        * write JPEG bytes into output stream
        * appears in Pictures/MyCameraApp*/
        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )
        uri?.let {
            contentResolver.openOutputStream(it)?.use { out ->
                out.write(bytes)
            }
        }
        return uri//returns the uri to show on another activity
    }

    //PERMISSION
    private fun hasPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
}
