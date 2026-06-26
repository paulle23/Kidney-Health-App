package com.example.firedatabase_assis

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
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
import java.io.FileOutputStream
import java.nio.file.Files.createFile
import java.text.SimpleDateFormat
import kotlin.math.abs
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
    private var currentFocusDistance = 0f
    private var isCapturing = false
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private var orientations : SparseIntArray = SparseIntArray(4).apply {
        append(Surface.ROTATION_0, 0)
        append(Surface.ROTATION_90, 90)
        append(Surface.ROTATION_180, 180)
        append(Surface.ROTATION_270, 270)
    }
    //added august
    private lateinit var dbSq: DB_class
    //private lateinit var mCameraCharacteristics: CameraCharacteristics//added
    //private val Seekbar
    // focusSeekbar;
    //used to start an activity
    //checking for focus, turn on autofocus, af trigger,
    //states to transition - inactive -> active scan -> focused locked
    private fun updatePreview() {
        if (!::cameraCaptureSession.isInitialized) return
        try {
            cameraCaptureSession.setRepeatingRequest(
                previewRequestBuilder.build(),
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
        dbSq = DB_class(this)
        textureView = findViewById(R.id.texture_view)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        //possibly here, need first id  CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
        findViewById<Button>(R.id.take_photo_btn).apply {
            setOnClickListener {
                takePhoto()
            }
        }
        val seek = findViewById<SeekBar>(R.id.seekBar)//R.id.id from the activity_main layout
        seek?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seek: SeekBar) {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
            }
            override fun onProgressChanged(seek: SeekBar,
                                           progress: Int, fromUser: Boolean) {
                val manager = getSystemService(CAMERA_SERVICE) as CameraManager
                if (!::cameraDevice.isInitialized) return
                val characteristics = manager.getCameraCharacteristics(cameraDevice.id)
                val minimumLens =
                    characteristics.get(
                        CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
                    ) ?: 0f
                if (minimumLens == 0f) {
                    // Fixed-focus camera
                }
                if (minimumLens > 0f) {
                    val focus = minimumLens * (1f - seek.progress / 100f)
                    previewRequestBuilder.set(
                        CaptureRequest.LENS_FOCUS_DISTANCE,
                        focus
                    )
                }
                /*val num = progress.toFloat() * minimumLens / 100
                captureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, num)*/
                //num changes with progress bar and minimumLens is 10.0
                updatePreview()
            }
            override fun onStopTrackingTouch(seek: SeekBar) {
                val manager = getSystemService(CAMERA_SERVICE) as CameraManager
                if (!::cameraDevice.isInitialized) return
                val characteristics = manager.getCameraCharacteristics(cameraDevice.id)
                val minimumLens =
                    characteristics.get(
                        CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE
                    ) ?: 0f
                if (minimumLens == 0f) {
                    // Fixed-focus camera
                }
                if (minimumLens > 0f) {
                    val focus = minimumLens * (1f - seek.progress / 100f)
                    currentFocusDistance = focus
                    previewRequestBuilder.set(
                        CaptureRequest.LENS_FOCUS_DISTANCE,
                        focus
                    )
                }
                val num = seek.progress.toFloat() * minimumLens / 100
                Toast.makeText(this@welcome_window,
                    "Focus is: " + num,
                    Toast.LENGTH_SHORT).show()
            }
        })
        if (!wasCameraPermissionWasGiven()) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_RESULT)
        }
    }
    //when activity is about to be moved to foreground
    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (textureView.isAvailable) {
            surfaceTextureListener.onSurfaceTextureAvailable(
                textureView.surfaceTexture!!,
                textureView.width,
                textureView.height
            )
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }
    override fun onPause() {
        super.onPause()
        stopBackgroundThread()
        if (::cameraCaptureSession.isInitialized) {
            cameraCaptureSession.close()
        }
        if (::cameraDevice.isInitialized) {
            cameraDevice.close()
        }
        if (::imageReader.isInitialized) {
            imageReader.close()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
    }
    private fun setupCamera(viewWidth: Int, viewHeight: Int) {
        val cameraIds = cameraManager.cameraIdList
        for (id in cameraIds) {
            val chars = cameraManager.getCameraCharacteristics(id)
            if (chars.get(CameraCharacteristics.LENS_FACING)
                == CameraCharacteristics.LENS_FACING_FRONT
            ) continue
            val map = chars.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
            ) ?: continue
            cameraId = id
            /*previewSize = chooseOptimalSize(
                map.getOutputSizes(SurfaceTexture::class.java),
                viewWidth,
                viewHeight
            )*/
            val textureRatio = viewWidth.toFloat() / viewHeight
            previewSize = map.getOutputSizes(SurfaceTexture::class.java)
                .filter {
                    abs((it.width.toFloat() / it.height) - textureRatio) < 0.1
                }
                .minByOrNull {
                    abs(it.width * it.height - viewWidth * viewHeight)
                }
                ?: map.getOutputSizes(SurfaceTexture::class.java)[0]
            val largest = map.getOutputSizes(ImageFormat.JPEG)
                .maxByOrNull { it.width * it.height }!!
            imageReader = ImageReader.newInstance(
                largest.width,
                largest.height,
                ImageFormat.JPEG,
                3
            )
            imageReader.setOnImageAvailableListener(
                onImageAvailableListener,
                backgroundHandler
            )
            require(::cameraId.isInitialized) { "cameraId not set" }
            require(::previewSize.isInitialized) { "previewSize not set" }
            break
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
            //surfaceTextureListener.onSurfaceTextureAvailable(textureView.surfaceTexture!!, textureView.width, textureView.height)
            textureView.surfaceTexture?.let {
                surfaceTextureListener.onSurfaceTextureAvailable(it, textureView.width, textureView.height)
            }
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
    private fun chooseOptimalSize(
        sizes: Array<Size>,
        viewWidth: Int,
        viewHeight: Int
    ): Size {
        val targetRatio = viewWidth.toFloat() / viewHeight
        return sizes
            .filter {
                val ratio = it.width.toFloat() / it.height.toFloat()
                kotlin.math.abs(ratio - targetRatio) < 0.2
            }
            .minByOrNull {
                abs(it.width * it.height - viewWidth * viewHeight)
            }
            ?: sizes.maxByOrNull { it.width * it.height }!!
    }
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val rotation = windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(
            0f, 0f,
            previewSize.width.toFloat(),
            previewSize.height.toFloat()
        )
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        val swap = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
        val rotatedBuffer = if (swap) {
            RectF(0f, 0f, bufferRect.height(), bufferRect.width())
        } else {
            bufferRect
        }
        matrix.setRectToRect(viewRect, rotatedBuffer, Matrix.ScaleToFit.FILL)
        matrix.postScale(1f, 1f, centerX, centerY)
        matrix.postRotate((90 * rotation).toFloat(), centerX, centerY)
        textureView.setTransform(matrix)
    }
    private fun getJpegOrientation(//rotation of preview
        rotation: Int,
        sensorOrientation: Int
    ): Int {
        val deviceRotation = when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        return (sensorOrientation + deviceRotation) % 360
    }
    //take the photo, goes to onCaptureCompleted after
    private fun takePhoto() {
        if (isCapturing) return
        isCapturing = true
        Log.d("Camera", "takePhoto()")
        if (!::cameraDevice.isInitialized) {
            isCapturing = false
            return
        }//first check if the camera is ready
        //cameraCaptureSession.stopRepeating()
        val stillBuilder =
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        stillBuilder.set(
            CaptureRequest.CONTROL_AF_MODE,
            CameraMetadata.CONTROL_AF_MODE_OFF
        )
        stillBuilder.set(
            CaptureRequest.LENS_FOCUS_DISTANCE,
            currentFocusDistance
        )
        stillBuilder.addTarget(imageReader.surface)
        //display?.rotation ?: Surface.ROTATION_0 instead of the next line since it is deprecated?
        val characteristics = cameraManager.getCameraCharacteristics(cameraDevice.id)
        val sensorOrientation =
            characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        //captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientations.get(rotation))
        val rotation = windowManager.defaultDisplay.rotation
        val jpegOrientation = getJpegOrientation(rotation, sensorOrientation)
        stillBuilder.set(
            CaptureRequest.JPEG_ORIENTATION,
            jpegOrientation
        )
        val rect = Rect(1000, 1100, 2772, 2472)
        val activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        //stillBuilder.set(CaptureRequest.SCALER_CROP_REGION, activeArraySize);
        cameraCaptureSession.capture(stillBuilder.build(), captureCallback, backgroundHandler)
        /*try {
            cameraCaptureSession.stopRepeating()
        } catch (e: Exception) {
            Log.e("Camera", "stopRepeating failed", e)
        }*/
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
            if (!wasCameraPermissionWasGiven()) return
            setupCamera(width, height)
            //configureTransform(width, height)
            connectCamera()
        }
        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
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

            val surfaceTexture = textureView.surfaceTexture ?: return

            val rotation = windowManager.defaultDisplay.rotation
            val swapped = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270

            val width = if (swapped) previewSize.height else previewSize.width
            val height = if (swapped) previewSize.width else previewSize.height

            surfaceTexture.setDefaultBufferSize(width, height)

            val previewSurface = Surface(surfaceTexture)

            previewRequestBuilder =
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            previewRequestBuilder.addTarget(previewSurface)

            cameraDevice.createCaptureSession(
                listOf(previewSurface, imageReader.surface),
                captureStateCallback,
                backgroundHandler
            )
        }
        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
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
        if (::backgroundHandlerThread.isInitialized) return
        backgroundHandlerThread = HandlerThread("CameraThread")
        backgroundHandlerThread.start()
        backgroundHandler = Handler(backgroundHandlerThread.looper)
    }
    private fun stopBackgroundThread() {
        if (::backgroundHandlerThread.isInitialized) {
            backgroundHandlerThread.quitSafely()
            backgroundHandlerThread.join()
        }
    }
    /**
     * Capture State Callback
     * A callback object for receiving updates about the state of a camera capture session.
     */
    private val captureStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            Log.d("CAMERA", "SESSION CONFIGURED")

            cameraCaptureSession = session

            previewRequestBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CameraMetadata.CONTROL_AF_MODE_OFF
            )

            cameraCaptureSession.setRepeatingRequest(
                previewRequestBuilder.build(),
                null,
                backgroundHandler
            )
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.e("CAMERA", "SESSION FAILED ❌")
        }
    }
    /**Capture Callback*/
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
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
            // restart preview AFTER short delay OR safely
            if (::cameraCaptureSession.isInitialized) {
                cameraCaptureSession.setRepeatingRequest(
                    previewRequestBuilder.build(),
                    null,
                    backgroundHandler
                )
            }
        }
        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            Log.e("Camera", "Capture failed: ${failure.reason}")
        }
    }
    /**
     * ImageAvailable Listener
     */
    val onImageAvailableListener = object: ImageReader.OnImageAvailableListener{
        override fun onImageAvailable(reader: ImageReader) {
            val image = reader.acquireLatestImage() ?: return
            Log.d("Camera", "onImageAvailable()")
            //to save the image and prevent it from leaking
            val bytes = try {
                val buffer = image.planes[0].buffer
                ByteArray(buffer.remaining()).also {
                    buffer.get(it)
                }
            } finally {
                image.close() // MUST be immediate
            }
            saveImageToMediaStore(bytes)
            runOnUiThread {
                Toast.makeText(this@welcome_window, "Photo Saved!", Toast.LENGTH_SHORT).show()
            }
            isCapturing = false
        }
    }
    /**
     * File Creation
     */
    /*private fun createFile(): File {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
        return File(filesDir, "IMG_${sdf.format(Date())}.jpg")
    }*/
    private fun saveImageToMediaStore(bytes: ByteArray) {//uses mediastore to save in galleryinstead of private storage
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/MyCameraApp"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = applicationContext.contentResolver
        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )
        uri?.let {
            resolver.openOutputStream(it)?.use { output ->
                output.write(bytes)
                output.flush()
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(it, values, null, null)
            }
        }
    }
}
