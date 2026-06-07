package com.example.firedatabase_assis

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.provider.Settings
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
val TAG = welcome_window::class.simpleName
const val CAMERA_REQUEST_RESULT = 1
//camera view
class welcome_window : AppCompatActivity() {
    /*lateinit keeps property from being initialized until later, avoid initializing a property when an object is constructed.
    If property is referenced before being initialized, UninitializedPropertyAccessException.*/
    private lateinit var textureView: TextureView
    private lateinit var cameraId: String
    private lateinit var backgroundHandlerThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSession: CameraCaptureSession// check this
    private lateinit var imageReader: ImageReader
    private lateinit var previewSize: Size
    private var shouldProceedWithOnResume: Boolean = true
    private var orientations : SparseIntArray = SparseIntArray(4).apply {
        append(Surface.ROTATION_0, 0)
        append(Surface.ROTATION_90, 90)
        append(Surface.ROTATION_180, 180)
        append(Surface.ROTATION_270, 270)
    }
    //added august
    var dbSq: DB_class= DB_class(this);
    //
    //private lateinit var mCameraCharacteristics: CameraCharacteristics//added
    //private val Seekbar
    // focusSeekbar;
    //used to start an activity
    //checking for focus, turn on autofocus, af trigger,
    //states to transition - inactive -> active scan -> focused locked
    private fun updatePreview() {
        try {
            cameraCaptureSession.setRepeatingRequest(
                captureRequestBuilder.build(),
                null,
                backgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.i("updatePreview", "ExceptionExceptionException")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome_window)
        textureView = findViewById(R.id.texture_view)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        //possibly here, need first id  CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
        findViewById<Button>(R.id.take_photo_btn).apply {
            setOnClickListener {
                takePhoto()
            }
        }
        /*findViewById<Button>(R.id.seekBar).apply {
            setOnClickListener {
                changeFocus()
            }
        }*/
        val seek = findViewById<SeekBar>(R.id.seekBar)//R.id.id from the activity_main layout
        seek?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seek: SeekBar) {
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
            }
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
                val manager = getSystemService(CAMERA_SERVICE) as CameraManager
                val characteristics = manager.getCameraCharacteristics(cameraDevice.id)
                val minimumLens =
                    characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)!!
                val num = progress.toFloat() * minimumLens / 100
                captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, num)
                //num changes with progress bar and minimumLens is 10.0
                updatePreview()
            }
            override fun onStopTrackingTouch(seek: SeekBar) {
                val manager = getSystemService(CAMERA_SERVICE) as CameraManager
                val characteristics = manager.getCameraCharacteristics(cameraDevice.id)
                val minimumLens =
                    characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)!!
                val num = seek.progress.toFloat() * minimumLens / 100
                Toast.makeText(this@welcome_window,
                    "Focus is: " + num,
                    Toast.LENGTH_SHORT).show()
            }
        })
        if (!wasCameraPermissionWasGiven()) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_RESULT)
        }
        startBackgroundThread()
    }
    /*fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        progressChangedValue = progress
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        val characteristics = manager.getCameraCharacteristics(cameraDevice.id)
        val minimumLens =
            characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)!!
        val num = progress.toFloat() * minimumLens / 100
        previewBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, num)
    }*/
    //when activity is about to be moved to foreground
    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (textureView.isAvailable && shouldProceedWithOnResume) {
            setupCamera()
        } else if (!textureView.isAvailable){
            textureView.surfaceTextureListener = surfaceTextureListener
        }
        shouldProceedWithOnResume = !shouldProceedWithOnResume
    }
    private fun setupCamera() {
        val cameraIds: Array<String> = cameraManager.cameraIdList
        for (id in cameraIds) {
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(id)
            //If we want to choose the rear facing camera instead of the front facing one
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                continue
            }
            val streamConfigurationMap : StreamConfigurationMap? = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            if (streamConfigurationMap != null) {
                previewSize = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.height * it.width }!!
                //videoSize = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(MediaRecorder::class.java).maxByOrNull { it.height * it.width }!!
                imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 1)
                imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
            }
            cameraId = id
        }
    }

    private fun wasCameraPermissionWasGiven() : Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        {
            return true
        }
        return false
    }

    //callback for result on requesting permissions
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            surfaceTextureListener.onSurfaceTextureAvailable(textureView.surfaceTexture!!, textureView.width, textureView.height)
        } else {
            Toast.makeText(
                this,
                "Camera permission is needed to run this application",
                Toast.LENGTH_LONG
            )
                .show()
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )) {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.fromParts("package", this.packageName, null)
                startActivity(intent)
            }
        }
    }
    /*private class MySeekBarListener : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seek: SeekBar,
                                       progress: Int, fromUser: Boolean) {
            val manager = ContextCompat.getSystemService(CAMERA_SERVICE) as CameraManager
            val characteristics = manager.getCameraCharacteristics(cameraDevice.id)
            val minimumLens =
                characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)!!
            val num = progress.toFloat() * minimumLens / 100
            captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, num)
        }
        //updatePreview()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
    }
}
    private fun changeFocus() {
        val seek = findViewById<SeekBar>(R.id.seekBar)//R.id.id from the activity_main layout
        seek?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seek: SeekBar) {
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
            }
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
                /*val cameraIds: Array<String> = cameraManager.cameraIdList

                /*for (id in cameraIds) {
                    val cameraCharacteristics = cameraManager.getCameraCharacteristics(id)*/
                val characteristics: CameraCharacteristics =
                    cameraManager.getCameraCharacteristics(cameraId)
                    val minimumLens: Float =
                        mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)//likely 0
                    val num = Int as Float * minimumLens / 100
                    val num = progress.toFloat() * minimumLens / 100
                    captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, num)*/
                //}
                val manager = getSystemService(CAMERA_SERVICE) as CameraManager
                val characteristics = manager.getCameraCharacteristics(cameraDevice.id)
                val minimumLens =
                    characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)!!
                val num = progress.toFloat() * minimumLens / 100
                captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, num)
                /*Toast.makeText(this@MainActivity,
                    "Focus is: " + num + " and minimum lens is " + minimumLens,
                    Toast.LENGTH_SHORT).show()*/
                //num changes with progress bar and minimumLens is 10.0
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                // what happens when progress is stopped
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
                Toast.makeText(this@MainActivity,
                    "Progress is: " + seek.progress + "%",
                    Toast.LENGTH_SHORT).show()
            }
        })

    }*/
    //take the photo, goes to onCaptureCompleted after
    private fun takePhoto() {
        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder.addTarget(imageReader.surface)
        val rotation = windowManager.defaultDisplay.rotation
        captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientations.get(rotation))
        val rect = Rect(1000, 1100, 2772, 2472)
        captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, rect);
        cameraCaptureSession.capture(captureRequestBuilder.build(), captureCallback, null)

    }

    //connect camera
    @SuppressLint("MissingPermission")
    private fun connectCamera() {
        cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
    }

    /**
     * Surface Texture Listener
     */

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        @SuppressLint("MissingPermission")
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            if (wasCameraPermissionWasGiven()) {
                setupCamera()
                connectCamera()
            }
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {

        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {

        }
    }

    /**
     * Camera State Callbacks
     */

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            val surfaceTexture : SurfaceTexture? = textureView.surfaceTexture
            surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
            val previewSurface: Surface = Surface(surfaceTexture)

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(previewSurface)

            cameraDevice.createCaptureSession(listOf(previewSurface, imageReader.surface), captureStateCallback, null)
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {

        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            val errorMsg = when(error) {
                ERROR_CAMERA_DEVICE -> "Fatal (device)"
                ERROR_CAMERA_DISABLED -> "Device policy"
                ERROR_CAMERA_IN_USE -> "Camera in use"
                ERROR_CAMERA_SERVICE -> "Fatal (service)"
                ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                else -> "Unknown"
            }
            Log.e(TAG, "Error when trying to connect camera $errorMsg")
        }
    }

    /**
     * Background Thread
     */
    private fun startBackgroundThread() {
        backgroundHandlerThread = HandlerThread("CameraVideoThread")
        backgroundHandlerThread.start()
        backgroundHandler = Handler(backgroundHandlerThread.looper)
    }

    private fun stopBackgroundThread() {
        backgroundHandlerThread.quitSafely()
        backgroundHandlerThread.join()
    }

    /**
     * Capture State Callback
     * A callback object for receiving updates about the state of a camera capture session.
     */

    private val captureStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {

        }
        override fun onConfigured(session: CameraCaptureSession) {
            cameraCaptureSession = session
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)//good
            /*launch{
                val result = loops()
                onresult(result)
           }*/
            //captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0f);//this sets the different focus levels, 0 is for far, 10 is for close
            //figure out how to change it while the app is running
            cameraCaptureSession.setRepeatingRequest(//use new bundle after changes to update captureRequestBuilder
                captureRequestBuilder.build(),
                null,
                backgroundHandler
            )
        }
    }/*
suspend fun loops(): Float{
    for (i in 0..10) {
        val num = i.toFloat()
        captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, num)
        delay(2000)
    }
}
    fun onresult(num: Float){

    }*/
    /*private val captureStateVideoCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.e(TAG, "Configuration failed")
        }
        override fun onConfigured(session: CameraCaptureSession) {
            cameraCaptureSession = session
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
            try {
                cameraCaptureSession.setRepeatingRequest(
                    captureRequestBuilder.build(), null,
                    backgroundHandler
                )
                mediaRecorder.start()
            } catch (e: CameraAccessException) {
                e.printStackTrace()
                Log.e(TAG, "Failed to start camera preview because it couldn't access the camera")
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }

        }
    }*/
    /*private CameraCaptureSession.CaptureCallback mCaptureCallback
    = new CameraCaptureSession.CaptureCallback() {}*/
    /*private fun process(result: CaptureResult) {
        when (mState) {
            STATE_PREVIEW -> {
                val afState = result.get(CaptureResult.CONTROL_AF_STATE)!!
                if (CaptureResult.CONTROL_AF_TRIGGER_START == afState) {
                    if (areWeFocused) {
                        //Run specific task here
                    }
                }
                if (CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState) {
                    areWeFocused = true
                } else {
                    areWeFocused = false
                }
            }
        }
    }*/
    /**Capture Callback*/
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        /*private fun process(result: CaptureResult) {
            when (mState) {
                STATE_PREVIEW -> {
                    val afState = result.get(CaptureResult.CONTROL_AF_STATE)!!
                    if (CaptureResult.CONTROL_AF_TRIGGER_START == afState) {
                        if (areWeFocused) {
                            //Run specific task here
                        }
                    }
                    if (CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED == afState) {
                        areWeFocused = true
                    } else {
                        areWeFocused = false
                    }
                }
            }
        }*/
        override fun onCaptureStarted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            timestamp: Long,
            frameNumber: Long
        ) {}

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            //process(partialResult)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            //process(result) process result function
        }
    }

    /**
     * ImageAvailable Listener
     */
    val onImageAvailableListener = object: ImageReader.OnImageAvailableListener{
        override fun onImageAvailable(reader: ImageReader) {
            Toast.makeText(this@welcome_window, "Photo Taken!", Toast.LENGTH_SHORT).show()
            val image: Image = reader.acquireLatestImage()
            image.close()
        }
    }

    /**
     * File Creation
     */

    private fun createFile(): File {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
        return File(filesDir, "VID_${sdf.format(Date())}.mp4")
    }
}