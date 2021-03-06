package com.tanmoybanik.httpcamera;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "";
    String upLoadServerUri = "http://192.168.0.100/HttpCameraServer/upload_image.php";
    int serverResponseCode = 0;



    // Camera Variables/////////

    private Camera mCamera;
    private CameraPreview mPreview;
    FrameLayout preview;


    private int mInterval = 5000;
    private Handler mHandler;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        mCamera=getCameraInstance();
        mPreview = new CameraPreview(this, mCamera);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        mPreview.setKeepScreenOn(true);


        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }


        if(isNetworkAvailable())
        {
            showAlertDialog("Title","Internet Connected",true);
            mHandler = new Handler();
            startRepeatTask();
        }
        else
        {
            showAlertDialog("error", "Internet is not Connected", false);
        }


       // mHandler.post(startUploadImg);



       /* preview.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCamera.takePicture(null, null, mPicture);
            }
        });*/

    }

    Runnable startUploadImg=new Runnable() {
        @Override
        public void run() {
            mCamera.takePicture(null, null, mPicture);
           // Log.e("Handlers", "Called");
            mHandler.postDelayed(startUploadImg, mInterval);
        }
    };


    void startRepeatTask()
    {
        startUploadImg.run();
    }
    void stopRepeatTask()
    {
        mHandler.removeCallbacks(startUploadImg);
    }


/////// Creating a camera instance which will open the camera//////

    /////// Creating a camera instance which will open the camera//////

    public Camera getCameraInstance(){

        Camera c = null;
        if (!hasCamera(getApplicationContext()))
        {
            Toast.makeText(getApplicationContext(), "Your Mobile doesn't have camera!!!", Toast.LENGTH_LONG).show();
        }else
        {
            try{
                c=openBackFacingCamera();
            }
            catch (Exception e){
                Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
            }
        }

        return c;
    }


    /////////////////////front facing camera open//////////////

    private static Camera openBackFacingCamera()
    {

        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for ( int camIdx = 0; camIdx < cameraCount; camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo );
            if ( cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK  ) {
                try {
                    cam = Camera.open( camIdx );
                } catch (RuntimeException e) {
                    Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }

        return cam;
    }


    /////Checking if the device has camera////////////////

    public boolean hasCamera(Context context){
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        }else {
            return false;
        }
    }

    private void releaseCamera(){
        if (mCamera!=null){
            mCamera.release();
            mCamera=null;
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        mCamera.stopPreview();
        stopRepeatTask();
        releaseCamera();
//        mHandler.removeCallbacks(startUploadImg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRepeatTask();
       // mHandler.removeCallbacks(startUploadImg);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mCamera == null) {
            mCamera = getCameraInstance();
            mPreview = new CameraPreview(this, mCamera);
            mPreview.setCamera(mCamera);
            preview.addView(mPreview);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera != null) {
            if (Build.VERSION.SDK_INT >= 14)
                setCameraDisplayOrientation(this,
                        Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
            mPreview.setCamera(mCamera);
        }
    }

    ///Refreshing the gallery too show the image///////////////s

 /*   private void refreshFallery(File file){

        Intent mediaScanintent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanintent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanintent);
    }*/
 public static void setCameraDisplayOrientation(Activity activity,
                                                int cameraId, android.hardware.Camera camera) {
     android.hardware.Camera.CameraInfo info =
             new android.hardware.Camera.CameraInfo();
     android.hardware.Camera.getCameraInfo(cameraId, info);
     int rotation = activity.getWindowManager().getDefaultDisplay()
             .getRotation();
     int degrees = 0;
     switch (rotation) {
         case Surface.ROTATION_0: degrees = 0; break;
         case Surface.ROTATION_90: degrees = 90; break;
         case Surface.ROTATION_180: degrees = 180; break;
         case Surface.ROTATION_270: degrees = 270; break;
     }

     int result;
     if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
         result = (info.orientation + degrees) % 360;
         result = (360 - result) % 360;  // compensate the mirror
     } else {  // back-facing
         result = (info.orientation - degrees + 360) % 360;
     }
     camera.setDisplayOrientation(result);
 }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void showAlertDialog(String title, String message, boolean status) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();

        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setCancelable(false);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        if (status)
        {
            alertDialog.setIcon(R.drawable.ok);
        }
        else
        {
            alertDialog.setIcon(R.drawable.error);
        }

        alertDialog.show();
    }


    private static File getoutputMediaFile() {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCameraApp");
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Log.d("MycameraApp", "Failed to create directory");
                return null;
            }
        }

        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile= new File(mediaStorageDir.getPath()+File.separator+"newImage.jpg");
        return mediaFile;
    }


    File pictureFile = null;

    boolean status;
    private Camera.PictureCallback mPicture= new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            pictureFile = getoutputMediaFile();


            if (pictureFile==null)
            {
                return;
            }
            try{
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.flush();
                fos.close();
              //  refreshFallery(pictureFile);
                status=uploadImage(pictureFile);


            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e(TAG, "Camera failed to take picture: " + e.getLocalizedMessage());
            }catch (IOException e){
                e.printStackTrace();
                Log.e(TAG, "Camera failed to take picture: " + e.getLocalizedMessage());
            }

            if (status)
            {
                pictureFile.delete();
            }

        }
    };












    boolean uploadImage(File sourceFile){
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        String fileName = "IMG_"+timeStamp+".jpg";




        HttpURLConnection conn = null;
        DataOutputStream outputStream = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        //File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {

         /*   dialog.dismiss();

            Log.e("uploadFile", "Source File not exist :"
                    + uploadFilePath + "" + uploadFileName);

            runOnUiThread(new Runnable() {
                public void run() {
                    messageText.setText("Source File not exist :"
                            +uploadFilePath + "" + uploadFileName);
                }
            });*/
           // Log.e("uploadFile", "Source File not exist :" + uploadFilePath + "" + uploadFileName);


        } else {
          //  Log.e("uploadFile", "Source File  exist :"+ uploadFilePath + "" + uploadFileName);
            try{
                FileInputStream fileInputStream = new FileInputStream(sourceFile);

                URL url = new URL(upLoadServerUri);
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);


                outputStream = new DataOutputStream(conn.getOutputStream());
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);


                outputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + fileName + "\"" + lineEnd);
                outputStream.writeBytes("Content-Type: image/jpeg" + lineEnd);
                outputStream.writeBytes("Content-Length: " + sourceFile.length() + lineEnd);
                outputStream.writeBytes(lineEnd);


                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                while (bytesRead > 0) {

                    outputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                /*int bufferLength = 1024;
                for (int i = 0; i < bytes.length; i += bufferLength) {
                    // publishing the progress....
                    Bundle resultData = new Bundle();
                    resultData.putInt("progress" ,(int)((i / (float) bytes.length) * 100));
                   // receiver.send(UPDATE_PROGRESS, resultData);

                    if (bytes.length - i >= bufferLength) {
                        outputStream.write(bytes, i, bufferLength);
                    } else {
                        outputStream.write(bytes, i, bytes.length - i);
                    }
                }*/





                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();
                Log.i("uploadFile", "HTTP Response is : " + serverResponseMessage + ": " + serverResponseCode);


                if(serverResponseCode == 200){

                   /* runOnUiThread(new Runnable() {
                        public void run() {

                            String msg = "File Upload Completed.\n\n See uploaded file here : \n\n"
                                    +" http://www.androidexample.com/media/uploads/"
                                    +uploadFileName;

                            messageText.setText(msg);
                            Toast.makeText(MainActivity.this, "File Upload Complete.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });*/

                    Toast.makeText(MainActivity.this, "File Upload Complete.",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                fileInputStream.close();
                outputStream.flush();
                outputStream.close();


            } catch (FileNotFoundException e) {

            } catch (MalformedURLException ex) {
                /*dialog.dismiss();
                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(MainActivity.this, "MalformedURLException",
                                Toast.LENGTH_SHORT).show();
                    }
                });*/

                ex.printStackTrace();
                Toast.makeText(MainActivity.this, "MalformedURLException",
                        Toast.LENGTH_SHORT).show();
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        return false;

    }

}
