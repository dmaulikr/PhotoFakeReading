package ep.radu.don.photofakereading;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    @BindView(R.id.btn_next) Button btn;
    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    public static final String SD_CARD = "sdCard";
    public static final String EXTERNAL_SD_CARD = "externalSdCard";
    private static final String ENV_SECONDARY_STORAGE = "SECONDARY_STORAGE";
    private static final String FOLDER_OUTPUT_NAME = "Fake_reading_photos";
    PictureCallback rawCallback;
    ShutterCallback shutterCallback;
    PictureCallback jpegCallback;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


        surfaceView =  findViewById(R.id.surface_view);
        surfaceHolder = surfaceView.getHolder();
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        surfaceHolder.addCallback(this);


        // deprecated setting, but required on Android versions prior to 3.0
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        jpegCallback = new PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                FileOutputStream outStream;
                try {
                    Boolean ok = false;
                    String mPath = getAllStorageLocations().get(EXTERNAL_SD_CARD);
                    File dir = new File(mPath);
                    if(dir.exists() && dir.isDirectory()) {
                        ok = true;
                    } else {
                        mPath = getAllStorageLocations().get(SD_CARD);
                        dir = new File(mPath);
                        if(dir.exists() && dir.isDirectory()) {
                            ok = true;
                        } else {
                            mPath = getAllStorageLocations().get(ENV_SECONDARY_STORAGE);
                            dir = new File(mPath);
                            if(dir.exists() && dir.isDirectory()) {
                                ok = true;
                            }
                        }
                    }
                    if (ok == true) {
                        File theDir = new File(mPath + File.separator  + FOLDER_OUTPUT_NAME);

                        // if the directory does not exist, create it
                        if (!theDir.exists()) {
                            System.out.println("creating directory: " + theDir.getName());
                        }
                        try{
                            theDir.mkdir();
                            String fPath =  mPath + File.separator  +
                                    FOLDER_OUTPUT_NAME + File.separator +
                                    System.currentTimeMillis() + ".png";
                            File fLocation = new File(fPath);
                            Log.e("_______", mPath);
                            outStream = new FileOutputStream(fLocation);
                            outStream.write(data);
                            outStream.close();
                            Log.e("Log", "onPictureTaken: " + fPath + ", wrote bytes: " + data.length);
                        }
                        catch(Exception se){
                            Log.e("Error", "CREATING OUTPUT DIRECTORY: " + se);
                        }
                    } else {
                        Log.e("Error", "STORAGE FKED");
                    }
                } catch (Exception e) {
                    Log.e("Error", "Getting sdcard path -> mPath uhh: " + e);
                } finally {
                }
                Toast.makeText(getApplicationContext(), "Picture Saved", Toast.LENGTH_SHORT).show();
                refreshCamera();
            }
        };
    }
    @OnClick(R.id.btn_next)
    public void onClick(View view) {
        try {
            System.out.println("DOING STUFF");
            captureImage(view);
        } catch (Exception e){
            System.out.println("Exception: " + e);

        }
    }

    public void captureImage(View v) throws IOException {
        //take the picture
        //camera.takePicture(null, null, jpegCallback);
        camera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    // get paths to writing partitions on phone
    public static Map<String, String> getAllStorageLocations() {
        Map<String, String> storageLocations = new HashMap<>(10);
        File sdCard = new File(Environment.getExternalStorageDirectory(), "DCIM");
        storageLocations.put(SD_CARD, sdCard.getAbsolutePath());
        final String rawSecondaryStorage = System.getenv(ENV_SECONDARY_STORAGE)  +  Environment.DIRECTORY_DCIM ;
        if (!TextUtils.isEmpty(rawSecondaryStorage)) {
            String[] externalCards = rawSecondaryStorage.split(":");
            for (int i = 0; i < externalCards.length; i++) {
                String path = externalCards[i];
                storageLocations.put(EXTERNAL_SD_CARD + String.format(i == 0 ? "" : "_%d", i), path);
            }
        }
        return storageLocations;
    }
    public void refreshCamera() {
        if (surfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            camera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {

        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        refreshCamera();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // open the camera
            camera = Camera.open();
            camera.setDisplayOrientation(90);
        } catch (RuntimeException e) {
            // check for exceptions
            System.err.println(e);
            return;
        }
        Camera.Parameters param;
        param = camera.getParameters();
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break; //Natural orientation
            case Surface.ROTATION_90: degrees = 90; break; //Landscape left
            case Surface.ROTATION_180: degrees = 180; break;//Upside down
            case Surface.ROTATION_270: degrees = 270; break;//Landscape right
        }
        int rotate = (info.orientation - degrees + 360) % 360;

//STEP #2: Set the 'rotation' parameter
        Camera.Size bestSize = null;
        List<Camera.Size> sizeList = camera.getParameters().getSupportedPreviewSizes();
        bestSize = sizeList.get(0);
        for(int i = 1; i < sizeList.size(); i++){
            if((sizeList.get(i).width * sizeList.get(i).height) > (bestSize.width * bestSize.height)){
                bestSize = sizeList.get(i);
            }
        }

        List<Integer> supportedPreviewFormats = param.getSupportedPreviewFormats();
        Iterator<Integer> supportedPreviewFormatsIterator = supportedPreviewFormats.iterator();
        while(supportedPreviewFormatsIterator.hasNext()){
            Integer previewFormat =supportedPreviewFormatsIterator.next();
            if (previewFormat == ImageFormat.YV12) {
                param.setPreviewFormat(previewFormat);
            }
        }
        param.setRotation(rotate);

        param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        param.setPreviewSize(bestSize.width, bestSize.height);

        param.setPictureSize(bestSize.width, bestSize.height);

        camera.setParameters(param);
        try {
            // The Surface has been created, now tell the camera where to draw
            // the preview.
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
            // check for exceptions
            System.err.println(e);
            return;
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // stop preview and release camera
        camera.stopPreview();
        camera.release();
        camera = null;
    }

}