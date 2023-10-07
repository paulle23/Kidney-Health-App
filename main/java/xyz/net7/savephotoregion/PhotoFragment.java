package xyz.net7.savephotoregion;


import android.content.Context;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;

import android.os.Bundle;
import android.os.Environment;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import xyz.net7.savephotoregion.databinding.ActivityMainBinding;
import xyz.net7.savephotoregion.databinding.FragmentImageBinding;
import xyz.net7.savephotoregion.databinding.FragmentPhotoBinding;


/*import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;*/

//PhotoFragment crops the image
public class PhotoFragment extends Fragment implements SurfaceHolder.Callback {

    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    boolean previewing = false;
    Context context;
 //
    //private ActivityMainBinding binding;
    private FragmentPhotoBinding binding;
   /* @BindView(R.id.preview_layout)
    LinearLayout previewLayout;

    @BindView(R.id.border_camera)
    View borderCamera;
    @BindView(R.id.res_border_size)
    TextView resBorderSizeTV;*/

    private OnFragmentInteractionListener mListener;

    Camera.Size previewSizeOptimal;

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Bitmap bitmap);
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        /*View view = inflater.inflate(R.layout.fragment_photo, container, false);
        ButterKnife.bind(this, view);*/
        binding = FragmentPhotoBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        //returns LinearLayout root view
        context = getContext();
        //
        binding.makePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                makePhoto();
            }
        });
        //

        surfaceView = (SurfaceView) view.findViewById(R.id.camera_preview_surface);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();

    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (previewing) {
            camera.stopPreview();
            previewing = false;
        }

        if (camera != null) {
            try {
                Camera.Parameters parameters = camera.getParameters();
                //get preview sizes
                List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();

                //find optimal - it very important
                previewSizeOptimal = getOptimalPreviewSize(previewSizes, parameters.getPictureSize().width,
                        parameters.getPictureSize().height);

                //set parameters
                if (previewSizeOptimal != null) {
                    parameters.setPreviewSize(previewSizeOptimal.width, previewSizeOptimal.height);
                }

                if (camera.getParameters().getFocusMode().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                }
                if (camera.getParameters().getFlashMode().contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                }

                camera.setParameters(parameters);

                //rotate screen, because camera sensor usually in landscape mode
                Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                if (display.getRotation() == Surface.ROTATION_0) {
                    camera.setDisplayOrientation(90);
                } else if (display.getRotation() == Surface.ROTATION_270) {
                    camera.setDisplayOrientation(180);
                }

                //write some info
                int x1 = binding.previewLayout.getWidth();
                int y1 = binding.previewLayout.getHeight();

                int x2 = binding.borderCamera.getWidth();
                int y2 = binding.borderCamera.getHeight();

                String info = "Preview width:" + String.valueOf(x1) + "\n" + "Preview height:" + String.valueOf(y1) + "\n" +
                        "Border width:" + String.valueOf(x2) + "\n" + "Border height:" + String.valueOf(y2);
                binding.resBorderSize.setText(info);
                //resBorderSizeTV for some reason before

                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                previewing = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


    public Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
        camera = null;
        previewing = false;
    }


    //@OnClick(R.id.make_photo_button)
    void makePhoto() {
        if (camera != null) {
            camera.takePicture(myShutterCallback,
                    myPictureCallback_RAW, myPictureCallback_JPG);

        }
    }

    Camera.ShutterCallback myShutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {

        }
    };
    Camera.PictureCallback myPictureCallback_RAW = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

        }
    };


    Camera.PictureCallback myPictureCallback_JPG = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bitmapPicture
                    = BitmapFactory.decodeByteArray(data, 0, data.length);


            Bitmap croppedBitmap = null;

            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            if (display.getRotation() == Surface.ROTATION_0) {

                //rotate bitmap, because camera sensor usually in landscape mode
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmapPicture, 0, 0, bitmapPicture.getWidth(), bitmapPicture.getHeight(), matrix, true);
                //save file
                createImageFile(rotatedBitmap);

                //calculate aspect ratio
                float koefX = (float) rotatedBitmap.getWidth() / (float) binding.previewLayout.getWidth();
                float koefY = (float) rotatedBitmap.getHeight() / (float) binding.previewLayout.getHeight();

                //get viewfinder border size and position on the screen
                int x1 = binding.borderCamera.getLeft();
                int y1 = binding.borderCamera.getTop();

                int x2 = binding.borderCamera.getWidth();
                int y2 = binding.borderCamera.getHeight();

                //calculate position and size for cropping
                int cropStartX = Math.round(x1 * koefX);
                int cropStartY = Math.round(y1 * koefY);

                int cropWidthX = Math.round(x2 * koefX);
                int cropHeightY = Math.round(y2 * koefY);

                //check limits and make crop
                if (cropStartX + cropWidthX <= rotatedBitmap.getWidth() && cropStartY + cropHeightY <= rotatedBitmap.getHeight()) {
                    croppedBitmap = Bitmap.createBitmap(rotatedBitmap, cropStartX, cropStartY, cropWidthX, cropHeightY);
                } else {
                    croppedBitmap = null;
                }

                //save result
                if (croppedBitmap != null) {
                    createImageFile(croppedBitmap);
                }

            } else if (display.getRotation() == Surface.ROTATION_270) {
                // for Landscape mode
            }

            //pass to another fragment
            if (mListener != null) {
                if (croppedBitmap != null)
                    mListener.onFragmentInteraction(croppedBitmap);
            }

            if (camera != null) {
                camera.startPreview();
            }
        }
    };


    public void createImageFile(final Bitmap bitmap) {

        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);

        String timeStamp = new SimpleDateFormat("MMdd_HHmmssSSS").format(new Date());
        String imageFileName = "region_" + timeStamp + ".jpg";
        final File file = new File(path, imageFileName);

        try {
            // Make sure the Pictures directory exists.
            if (path.mkdirs()) {
                Toast.makeText(context, "Not exist :" + path.getName(), Toast.LENGTH_SHORT).show();
            }

            OutputStream os = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);

            os.flush();
            os.close();
            Log.i("ExternalStorage", "Writed " + path + file.getName());
            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(context,
                    new String[]{file.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });
            Toast.makeText(context, file.getName(), Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            // Unable to create file, likely because external storage is
            // not currently mounted.
            Log.w("ExternalStorage", "Error writing " + file, e);
        }
    }
}






