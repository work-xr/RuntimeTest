package com.sprd.runtime.camera;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
//import android.os.EnvironmentEx;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.Toast;

import com.sprd.runtime.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.UUID;

import static android.os.Environment.DIRECTORY_DCIM;
import static com.sprd.runtime.Const.AFTER_SLEEP_AUTO_CAPTURE_DURATION;
import static com.sprd.runtime.Const.EXTRA_PHOTO_FILENAME;

/**
 * Created by hefeng on 18-5-25.
 */

public class RuntimeFrontCameraTestActivity extends Activity implements TextureView.SurfaceTextureListener{

    private static final String TAG = "RuntimeFrontCameraTestActivity";

    private static String CAPTURE_IMAGE_DEFAULT_PATH = "/storage/emulated/0/DCIM/Camera";  //              /sdcard/DCIM/Camera
    private static final int MSG_CAPTURE = 0x0000;
    private static final int MSG_SHOW_ERROR_TIP = 0x0001;
    //private SurfaceHolder holder = null;
    //private static boolean isSurfaceCreated = false;
    private Handler uiHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            //super.handleMessage(msg);
            switch (msg.what)
            {
                case MSG_CAPTURE:
                    mCamera.takePicture(mShutterCallback, null, mJpegCallback);
                    break;
                case MSG_SHOW_ERROR_TIP:
                    Log.d(TAG, "mCamera is null.");
                    Toast.makeText(getApplicationContext(),
                            RuntimeFrontCameraTestActivity.this.getString(R.string.front_camera_capture_failed), Toast.LENGTH_SHORT)
                            .show();
                    finish();
                    break;
                default:
                    break;
            }
        }
    };

    private Camera.ShutterCallback mShutterCallback = new Camera.ShutterCallback()
    {
        @Override
        public void onShutter() {
            //mProgress.setVisibility(View.VISIBLE);
        }
    };

    private Camera.PictureCallback mJpegCallback = new Camera.PictureCallback()
    {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            String fileName = UUID.randomUUID().toString() + ".jpg";
            CAPTURE_IMAGE_DEFAULT_PATH = Environment.getExternalStoragePublicDirectory(DIRECTORY_DCIM).getAbsolutePath();
            File imgFile = new File(CAPTURE_IMAGE_DEFAULT_PATH + File.separator + fileName);
            FileOutputStream os = null;
            boolean success = true;
            Log.i(TAG, "onPictureTaken: begin save   " + fileName);
            try
            {
                //os = openFileOutput(fileName, Context.MODE_PRIVATE);
                os = new FileOutputStream(imgFile);
                os.write(data);
                //Log.i(TAG, "onPictureTaken: 1");
            }
            catch (Exception e)
            {
                e.printStackTrace();
                success = false;
            }
            finally {
                try
                {
                    //Log.i(TAG, "onPictureTaken: 2");
                    if (os != null)
                    {
                        os.close();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    success = false;
                }
            }
            //Log.i(TAG, "onPictureTaken: 3");
            if (success)
            {
                Log.i(TAG, "onPictureTaken: jpeg saved at: " + fileName + ", fileAbsolutePath: " + imgFile.getAbsolutePath());
                Intent i = new Intent();
                i.putExtra(EXTRA_PHOTO_FILENAME, imgFile.getAbsolutePath());
                setResult(Activity.RESULT_OK, i);
            }
            else
            {
                setResult(Activity.RESULT_CANCELED);
            }
            finish();
        }
    };

    private RuntimeFrontCameraTestActivity.CameraScreenNailProxy mCameraScreenNailProxy;
    private Camera mCamera = null;
    private int mCameraId = 0;
    private TextureView mTextureView = null;
    private SurfaceTexture mSurfaceTexture = null;
    private PreviewFrameLayout mPreviewFrameLayout;
    private boolean mFlag = false;
    private ComboPreferences mPreferences;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;

    protected class CameraScreenNailProxy{
        private static final String TAG = "CameraScreenNailProxy";

        public static final int KEY_SIZE_PICTURE = 0;
        public static final int KEY_SIZE_PREVIEW = 1;

        private Tuple<Integer, Integer> mScreenSize;
        protected CameraScreenNailProxy(){
            initializeScreenSize();
        }

        private void initializeScreenSize(){
            Display display = getWindowManager().getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            mScreenSize = new Tuple<Integer, Integer>(
                    metrics.widthPixels, metrics.heightPixels);
            Log.d(TAG,
                    String.format("screen size = { %dx%d }",
                            new Object[] { mScreenSize.first, mScreenSize.second }));
        }
        protected Tuple<Integer, Integer>
        getOptimalSize(int key, ComboPreferences pref){

            Tuple<Integer, Integer> result;
            Size size;
            boolean b_full_screen = getScreenState(pref);
            int orientation = getOrientation();
            int width = mScreenSize.first,  height = mScreenSize.second;
            Camera.Parameters mParameters;
            mParameters = mCamera.getParameters();

            if (KEY_SIZE_PICTURE == key) {
                size = mParameters.getPictureSize();
                width = size.width;
                height = size.height;
                result = Util.getOptimalSize(
                        mScreenSize.first, mScreenSize.second, width, height, b_full_screen);
                width = result.first;
                height = result.second;
                if (orientation % 180 == 0) {
                    int tmp = width;
                    width = height;
                    height = tmp;
                }
            }

            if (KEY_SIZE_PREVIEW == key) {
                size = mParameters.getPreviewSize();
                width = size.width;
                height = size.height;
                result = Util.getOptimalSize(
                        mScreenSize.first, mScreenSize.second, width, height, b_full_screen);
                width = result.first;
                height = result.second;
                if (orientation % 180 == 0){
                    int tmp = width;
                    width = height;
                    height = tmp;
                }
            }

            result = new Tuple<Integer, Integer>(width, height);
            Log.d(TAG,
                    String.format("get optimal size: key = %d, is_full_screen = %b, size = { %dx%d }",
                            new Object[] { key, b_full_screen, result.first, result.second }));
            return result;
        }

        private int getOrientation() {
            return getCameraDisplayOrientation(mCameraId, mCamera);
        }
    }
    protected boolean getScreenState(ComboPreferences pref) {
        boolean result = false;
        if (pref != null) {
            String str_on = getString(R.string.pref_entry_value_on);
            String str_val = pref.getString("pref_camera_video_full_screen_key", null);
            result = (str_val != null && str_val.equals(str_on));
        }
        return result;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        setContentView(R.layout.camera_new);
        //setTitle(R.string.name_front_camera_tesing);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mCameraScreenNailProxy = new RuntimeFrontCameraTestActivity.CameraScreenNailProxy();
        mPreviewFrameLayout = (PreviewFrameLayout) findViewById(R.id.frame);
        mTextureView = (TextureView) findViewById(R.id.surfaceView);
        mTextureView.setSurfaceTextureListener(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(AFTER_SLEEP_AUTO_CAPTURE_DURATION);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    if (mCamera != null)
                    {
                        uiHandler.sendEmptyMessage(MSG_CAPTURE);
                    }
                    else
                    {
                        uiHandler.sendEmptyMessage(MSG_SHOW_ERROR_TIP);
                    }
                }
            }
        }).start();
    }

    private void startCamera() {
        if (mFlag) {
            Log.e(TAG, "stop & close");
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mFlag = false;
            }
        }
        try {
            Log.e(TAG, "open");
            mCamera = Camera.open(mCameraId);
        } catch (RuntimeException e) {
            Log.e(TAG, "fail to open camera");
            e.printStackTrace();
            mCamera = null;
        }
        if (mCamera != null) {
            setCameraDisplayOrientation(mCameraId, mCamera);
            Camera.Parameters parameters = null;
            parameters = mCamera.getParameters();
            Size size = parameters.getPictureSize();
            List<Size> sizes = parameters.getSupportedPreviewSizes();
            Size optimalSize = getOptimalPreviewSize(this, sizes, (double) size.width / size.height);
            Size original = parameters.getPreviewSize();
            if (!original.equals(optimalSize)) {
                parameters.setPreviewSize(optimalSize.width, optimalSize.height);
                parameters = mCamera.getParameters();
            }
            Log.v(TAG, "Preview size is " + optimalSize.width + "x" + optimalSize.height);
            mPreviewWidth = optimalSize.width;
            mPreviewHeight = optimalSize.height;
            parameters.setPictureFormat(PixelFormat.JPEG);
            parameters.set("orientation", "portrait");
      //      parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(parameters);
            try {
                /*SPRD: fix bug349132 change the SurfaceView to TextureView @{*/
//              mCamera.setPreviewDisplay(holder);
                mCamera.setPreviewTexture(mSurfaceTexture);
                /* @}*/
                Log.e(TAG, "start preview");
                mCamera.startPreview();
                mFlag = true;
                initializeCameraOpenAfter();
            } catch (Exception e) {
                mCamera.release();
            }
        }
    }
    public static Size getOptimalPreviewSize(Activity currentActivity,
            List<Size> sizes, double targetRatio) {
        final double ASPECT_TOLERANCE = 0.001;
        if (sizes == null) return null;
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        Display display = currentActivity.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int targetHeight = Math.min(point.x, point.y);
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
                break;
            }
        }
        if (optimalSize == null) {
            Log.w(TAG, "No preview size match the aspect ratio");
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                                          int height) {
        Log.d(TAG, "onSurfaceTextureAvailable");
        mSurfaceTexture = surface;
        startCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                                            int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }


    @Override
    protected void onResume() {
        super.onResume();
        /*SPRD: fix bug349132 change the SurfaceView to TextureView @{*/
        /*holder = mSurfaceView.getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);*/
        /* @}*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (mCamera != null) {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getDisplayRotation() {
        return 0;
    }
    public static void setCameraDisplayOrientation(
            int cameraId, Camera camera) {
        int result = getCameraDisplayOrientation(cameraId,camera);
        camera.setDisplayOrientation(result);
    }
    public static int getCameraDisplayOrientation(
            int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int degrees = getDisplayRotation();
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private void initializeCameraOpenAfter() {
        // SPRD:Fixbug454827,The preview picture of take photo has some defective.
        Tuple<Integer, Integer> size =
                mCameraScreenNailProxy.getOptimalSize(
                        RuntimeFrontCameraTestActivity.CameraScreenNailProxy.KEY_SIZE_PICTURE, mPreferences);
        if (mPreviewFrameLayout != null) {
            mPreviewFrameLayout.setAspectRatio((double) size.first / (double) size.second, true);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    /*
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private View mProgress;

    private SurfaceHolder.Callback holderCallback = new SurfaceHolder.Callback()
    {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated: before mCamera = " + mCamera);
            openFrontCamera();
            Log.d(TAG, "surfaceCreated: after mCamera = " + mCamera);
            try
            {
                if (mCamera != null)
                {
                    mCamera.setPreviewDisplay(holder);
                }
                isSurfaceCreated = true;
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged:  mCamera = " + mCamera);
            if (mCamera == null)
                return;

            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size s = getBestSupportedSize(parameters.getSupportedPreviewSizes(), width, height);
            parameters.setPictureSize(s.width, s.height);
            mCamera.setParameters(parameters);
            try
            {
                mCamera.startPreview();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                mCamera.release();
                mCamera = null;
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mCamera != null)
            {
                mCamera.stopPreview();
            }
            isSurfaceCreated = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.camera);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        setTitle(R.string.name_front_camera_tesing);

        Log.d(TAG, "onCreate: begin ");
        mSurfaceView = (SurfaceView) findViewById(R.id.crime_camera_surface);

        holder = mSurfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(holderCallback);

        mProgress = findViewById(R.id.crime_camera_progressContainer);
        mProgress.setVisibility(View.INVISIBLE);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(AFTER_SLEEP_AUTO_CAPTURE_DURATION);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    if (mCamera != null)
                    {
                        uiHandler.sendEmptyMessage(MSG_CAPTURE);
                    }
                    else
                    {
                        uiHandler.sendEmptyMessage(MSG_SHOW_ERROR_TIP);
                    }
                }
            }
        }).start();
    }

    private void openFrontCamera()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            //mCamera = Camera.open(1);
            for (int i = 0; i < Camera.getNumberOfCameras(); i++)
            {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    mCamera = Camera.open(i);
                    Log.d(TAG, "onResume: front camera open success.");
                    break;
                }
            }

            if (mCamera == null) {
                mCamera = Camera.open(1);
                Log.d(TAG, "onResume: front camera open failed.");
                //Process.killProcess(Process.myPid());
            }
        }
        //else
        {
            //mCamera = Camera.open();
            // not support
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        openFrontCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCamera != null)
        {
            mCamera.release();
            mCamera = null;
        }

        if (!isSurfaceCreated)
        {
            holder.removeCallback(holderCallback);
        }
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    private Camera.Size getBestSupportedSize(List<Camera.Size> sizes, int w, int h)
    {
        Camera.Size bestS = sizes.get(0);
        int largest = bestS.width * bestS.height;

        for (Camera.Size s:sizes)
        {
            int area = s.width * s.height;

            if (area > largest)
            {
                bestS = s;
                largest = area;
            }
        }

        return  bestS;
    }*/
}
