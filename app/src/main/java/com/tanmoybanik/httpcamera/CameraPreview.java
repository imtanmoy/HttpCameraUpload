package com.tanmoybanik.httpcamera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

/**
 * Created by Tanmoy Banik on 6/21/2015.
 */
public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback {

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    List<Camera.Size> mSupportedPreviewSizes;
    private Camera.Size mPreviewSize;
    Camera.Parameters parameters;
    private static final String TAG=null;
    Context ctx;
    public boolean safeToTakePicture = false;





    public CameraPreview(Context context, Camera camera) {
        super(context);
        ctx=context;
        mCamera= camera;
        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView, 0);
        mHolder=mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }



    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "error setting camera preview: " + e.getMessage());
            stopPreviewAndFreeCamera();
        }

    }


    public void setCamera(Camera camera) {

        if (mCamera == camera)
        {
            return;
        }
        stopPreviewAndFreeCamera();

        mCamera = camera;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPictureSizes();
            Camera.Size mSize=mSupportedPreviewSizes.get(0);
            Camera.Parameters params = mCamera.getParameters();

            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                // set the focus mode
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                parameters.setPreviewSize(mSize.width,mSize.height);
                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
                parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
                parameters.setJpegQuality(100);
                parameters.setJpegThumbnailQuality(100);
                parameters.setExposureCompensation(0);

            }
            mCamera.setParameters(parameters);
            mCamera.setParameters(params);

        }
        requestLayout();
    }



    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        if (mCamera != null) {
            // Call stopPreview() to stop updating the preview surface.
         //   mCamera.stopPreview();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        if (mHolder.getSurface()==null){
            return;
        }
        try {
            mCamera.stopPreview();
        }catch (Exception e){
            Log.e(TAG, "Surface Change " + e.getLocalizedMessage());
        }
        setCamera(mCamera);
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            safeToTakePicture = true;

        } catch (Exception e){
            e.printStackTrace();
            Log.e(TAG, "Surface Change " + e.getLocalizedMessage());
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null)
        {
            mPreviewSize = ImageUtil.getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        if (changed)
        {
            final View cameraView = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null)
            {
                Display display = ((WindowManager)ctx.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

                switch (display.getRotation())
                {
                    case Surface.ROTATION_0:
                        previewWidth = mPreviewSize.height;
                        previewHeight = mPreviewSize.width;
                        mCamera.setDisplayOrientation(90);
                        break;
                    case Surface.ROTATION_90:
                        previewWidth = mPreviewSize.width;
                        previewHeight = mPreviewSize.height;
                        break;
                    case Surface.ROTATION_180:
                        previewWidth = mPreviewSize.height;
                        previewHeight = mPreviewSize.width;
                        break;
                    case Surface.ROTATION_270:
                        previewWidth = mPreviewSize.width;
                        previewHeight = mPreviewSize.height;
                        mCamera.setDisplayOrientation(180);
                        break;
                }
            }

            final int scaledChildHeight = previewHeight * width / previewWidth;

            cameraView.layout(0, height - scaledChildHeight, width, height);

        }

    }
    private void stopPreviewAndFreeCamera() {

        if (mCamera != null) {
            // Call stopPreview() to stop updating the preview surface.
            mCamera.stopPreview();

            // Important: Call release() to release the camera for use by other
            // applications. Applications should release the camera immediately
            // during onPause() and re-open() it during onResume()).
            mCamera.release();

            mCamera = null;
        }
    }
}
