package ep.radu.don.photofakereading;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.DomainCombiner;
import java.util.Iterator;
import java.util.List;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.OnClick;
import util.IabHelper;
import util.IabResult;
import util.Inventory;
import util.Purchase;

import static android.content.ContentValues.TAG;


public class MainActivity extends Activity implements SurfaceHolder.Callback {
    static final String ITEM_SKU = "android.test.purchased";
    private Button buyButton;
    Camera camera;
    private static final String TAG = "ep.radu.don";
    IabHelper mHelper;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    public boolean WRITE_PERMISSION;
    public static final Integer REQUEST_CODE_WRITE_SDCARD = 5;
    private static final String FOLDER_OUTPUT_NAME = "Fake_reading_photos";
    PictureCallback rawCallback;
    ShutterCallback shutterCallback;
    PictureCallback jpegCallback;
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener
            = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result,
                                          Purchase purchase)
        {
            if (result.isFailure()) {
                // Handle error
                return;
            }
            else if (purchase.getSku().equals(ITEM_SKU)) {
                consumeItem();
                buyButton.setEnabled(false);
            }

        }
    };
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener =
            new IabHelper.OnConsumeFinishedListener() {
                public void onConsumeFinished(Purchase purchase,
                                              IabResult result) {

                    if (result.isSuccess()) {
                        //ToDo here on purchase
                    } else {
                        // handle error
                    }
                }
            };
    IabHelper.QueryInventoryFinishedListener mReceivedInventoryListener
            = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result,
                                             Inventory inventory) {


            if (result.isFailure()) {
                // Handle failure
            } else {
                try {
                    mHelper.consumeAsync(inventory.getPurchase(ITEM_SKU),
                            mConsumeFinishedListener);
                } catch (Exception e) {
                    Log.e("Error", "cant start async process consume: " + e);
                }
            }
        }
    };


    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.e("Permission: ", "ITS  NOT OK");
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        } else {
            Log.e("Permission: ", "ITS OK");
        }
    }
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        WRITE_PERMISSION = CheckStoragePermission();
        verifyStoragePermissions(MainActivity.this);


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
            File fileOut =  getOutputFile();
            if (fileOut != null) {
                Log.e("FILE:", fileOut.getAbsolutePath());
                fileOut.createNewFile();
                String fileName = fileOut.getAbsolutePath();
                outStream = new FileOutputStream(fileOut);
                outStream.write(data);
                outStream.close();
                boolean resultWorldReadable = fileOut.setReadable(true, false);
                Toast.makeText(getApplicationContext(), fileName, Toast.LENGTH_SHORT).show();
                Log.e("Log", "onPictureTaken: " + fileName + ", wrote bytes: " + data.length);
            } else {
                Toast.makeText(getApplicationContext(), "Something went wrong! Sorry", Toast.LENGTH_SHORT).show();
            }
        }
        catch(Exception se){
            Log.e("Error", se.toString());
        }
        refreshCamera();
            }
        };
    }
    public void consumeItem() {
        try {
            mHelper.queryInventoryAsync(mReceivedInventoryListener);
        } catch (Exception e){
            Log.e("ERROR", "can't consume item: " + e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        buyButton = findViewById(R.id.btn_prev);

        String base64EncodedPublicKey =
                "<your license key here>";

        mHelper = new IabHelper(this, base64EncodedPublicKey);

        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
               public void onIabSetupFinished(IabResult result)
               {
                   if (!result.isSuccess()) {
                       Log.d(TAG, "In-app Billing failed:" + result);
                   } else {
                       Log.d(TAG, "In-app Billing is set up OK");
                   }
               }
       });
    }

    public void buyClick(View view) {
        try {
            mHelper.launchPurchaseFlow(this, ITEM_SKU, 10001,
                    mPurchaseFinishedListener, "mypurchasetoken");
        } catch (Exception e){
            Log.e("ERROR", "buy click not working: " + e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data)
    {
        if (!mHelper.handleActivityResult(requestCode,
                resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
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
    @TargetApi(Build.VERSION_CODES.M)
    public boolean CheckStoragePermission() {
        int permissionCheckRead = ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheckRead != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_WRITE_SDCARD);
            } else {
                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_WRITE_SDCARD);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
            return false;
        } else
            return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        // BEGIN_INCLUDE(onRequestPermissionsResult)
        if (requestCode == REQUEST_CODE_WRITE_SDCARD) {
            // Request for camera permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Start camera preview Activity.
                Log.e("PERM; ", "yup");
            } else {
                Log.e("PERM; ", "meh");
                // Permission request was denied.
            }
        }
        // END_INCLUDE(onRequestPermissionsResult)
    }
    // get paths to writing partitions on phone
    public  File getOutputFile() {
        //Map<String, String> storageLocations = new HashMap<>(10);
        //File sdCard = new File(Environment.getExternalStorageDirectory(), "DCIM");
        //storageLocations.put(SD_CARD, sdCard.getAbsolutePath());
        final String internalPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
        String externalPath = "";
        if (!TextUtils.isEmpty(internalPath)) {
            String[] externalCards = internalPath.split(":");
            for (int i = 0; i < externalCards.length; i++) {
                externalPath =  externalCards[i];
                //storageLocations.put(EXTERNAL_SD_CARD + String.format(i == 0 ? "" : "_%d", i), externalPath);
            }
        }
        Log.e("p",internalPath);
        Log.e("p",externalPath);
        File fileReturn;
        File outDir;
        if (isExternalStorageAvailable() && !isExternalStorageReadOnly() && WRITE_PERMISSION) { //return external memory
            outDir = new File(externalPath + File.separator + FOLDER_OUTPUT_NAME);
        } else { //return internal memory
            outDir = new File(internalPath + File.separator + FOLDER_OUTPUT_NAME);
        }
        if (!outDir.exists()){
            try {
                outDir.mkdir();
                boolean resWorldReadable = outDir.setReadable(true, false);
                return (new File(outDir.getAbsoluteFile() +  File.separator +
                        System.currentTimeMillis() + ".png"));
            } catch (Exception e) {
                Log.e("ERROR: ", "Couldn't create outDir: " + e);
                return (null);
            }
        }
        return (new File(outDir.getAbsoluteFile() + File.separator +
                System.currentTimeMillis() + ".png"));
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
    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
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