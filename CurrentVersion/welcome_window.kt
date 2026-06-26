package com.example.firedatabase_assis

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class welcome_window : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var session: CameraCaptureSession
    private lateinit var previewRequest: CaptureRequest.Builder

    private lateinit var imageReader: ImageReader
    private lateinit var cameraId: String
    private lateinit var previewSize: Size

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var focusDistance = 0f
    private var isCapturing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome_window)

        textureView = findViewById(R.id.texture_view)
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        findViewById<Button>(R.id.take_photo_btn).setOnClickListener {
            capturePhoto()
        }

        findViewById<SeekBar>(R.id.seekBar).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (!::previewRequest.isInitialized) return

                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
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

    // ---------------- CAMERA SETUP ----------------

    private fun openCamera() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
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
        cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
    }

    private fun setupCamera() {
        for (id in cameraManager.cameraIdList) {
            val chars = cameraManager.getCameraCharacteristics(id)

            if (chars.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT
            ) continue

            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue

            cameraId = id
            previewSize = map.getOutputSizes(SurfaceTexture::class.java).maxByOrNull { it.width * it.height }!!

            val jpegSize = map.getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.width * it.height }

            if (jpegSize != null) {
                imageReader = ImageReader.newInstance(
                    jpegSize.width,
                    jpegSize.height,
                    ImageFormat.JPEG,
                    2
                )
            }

            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                image.close()

                saveToGallery(bytes)
                runOnUiThread {
                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                }

                isCapturing = false
            }, backgroundHandler)

            break
        }
    }

    // ---------------- PREVIEW ----------------

    private fun createPreviewSession() {
        val surface = Surface(textureView.surfaceTexture)

        previewRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewRequest.addTarget(surface)

        cameraDevice.createCaptureSession(
            listOf(surface, imageReader.surface),
            object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(s: CameraCaptureSession) {
                    session = s

                    previewRequest.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CameraMetadata.CONTROL_AF_MODE_OFF
                    )

                    session.setRepeatingRequest(previewRequest.build(), null, backgroundHandler)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            },
            backgroundHandler
        )
    }

    private fun updatePreview() {
        session.setRepeatingRequest(previewRequest.build(), null, backgroundHandler)
    }

    // ---------------- CAPTURE ----------------

    private fun capturePhoto() {
        if (isCapturing || !::cameraDevice.isInitialized) return
        isCapturing = true

        val request = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)

        request.addTarget(imageReader.surface)
        request.set(CaptureRequest.LENS_FOCUS_DISTANCE, focusDistance)

        session.capture(request.build(), object : CameraCaptureSession.CaptureCallback() {}, backgroundHandler)
    }

    // ---------------- BACKGROUND THREAD ----------------

    private fun startThread() {
        backgroundThread = HandlerThread("camera").apply { start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopThread() {
        backgroundThread?.quitSafely()
        backgroundThread?.join()
        backgroundThread = null
        backgroundHandler = null
    }

    // ---------------- LIFECYCLE ----------------

    override fun onResume() {
        super.onResume()
        startThread()

        if (textureView.isAvailable) {
            openCamera()
        } else {
            textureView.surfaceTextureListener = surfaceListener
        }
    }

    override fun onPause() {
        super.onPause()

        session.close()
        cameraDevice.close()
        imageReader.close()

        stopThread()
    }

    // ---------------- SURFACE ----------------

    private val surfaceListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
            setupCamera()
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {}
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = true
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    // ---------------- CAMERA CALLBACK ----------------

    private val stateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
        }
    }

    // ---------------- SAVE IMAGE ----------------

    private fun saveToGallery(bytes: ByteArray) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyCameraApp")
        }

        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

        uri?.let {
            contentResolver.openOutputStream(it)?.use { out ->
                out.write(bytes)
            }
        }
    }

    // ---------------- PERMISSION ----------------

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
}
