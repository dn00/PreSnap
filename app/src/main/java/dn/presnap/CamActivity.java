package dn.presnap;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import android.media.MediaRecorder;
import android.media.CamcorderProfile;
import android.widget.Button;
import android.view.View;
import java.io.IOException;
import java.lang.reflect.Method;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CamActivity extends Activity  {
    private SurfaceView preview=null;
    private SurfaceHolder previewHolder=null;
    private Camera camera=null;
    private boolean inPreview=false;
    private boolean cameraConfigured=false;
    private MediaRecorder mMediaRecorder;
    private boolean isRecording = false;
    //private String lastVid;
    private static String currentVideo;
    private Button captureButton;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cam);

        preview=(SurfaceView)findViewById(R.id.cpPreview);
        previewHolder=preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        ((AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_SYSTEM, true);

        captureButton = (Button) findViewById(R.id.startstop);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isRecording) {
                            stopRecord();
                        } else {
                            startRecord();
                        }
                    }

                }
        );
    }
    private void stopRecord()
    {
        // stop recording and release camera
        mMediaRecorder.stop();  // stop the recording
        releaseMediaRecorder(); // release the MediaRecorder object
        camera.lock();         // take camera access back from MediaRecorder

        // inform the user that recording has stopped
        //setCaptureButtonText("Capture");
        isRecording = false;
        splitIt();
    }
    private void startRecord()
    {
        if ( prepareVideoRecorder()) {
            // Camera is available and unlocked, MediaRecorder is prepared,
            // now you can start recording
            mMediaRecorder.start();

            // inform the user that recording has started
            // setCaptureButtonText("Stop");
            isRecording = true;
        } else {
            // prepare didn't work, release the camera
            releaseMediaRecorder();
            // inform user
        }
    }
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
    private boolean prepareVideoRecorder(){
       // camera = Camera.open();
        mMediaRecorder = new MediaRecorder();
        // Step 1: Unlock and set camera to MediaRecorder
        camera.unlock();
        mMediaRecorder.setCamera(camera);
        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        // Step 4: Set output file
          mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
       // mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
       // mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        //mMediaRecorder.setOutputFile("/sdcard/video.mp4");
        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(previewHolder.getSurface());
        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d("PreSnap", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d("PreSnap", "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            camera.lock();           // lock camera for later use
        }
    }
    private void releaseCamera() {
        if (camera != null) {
            camera.release();        // release the camera for other applications
            camera = null;
        }
    }
    @Override
    public void onResume() {
        super.onResume();

        camera=getCameraInstance();
        //captureButton.performClick();
        camera.startPreview();

       // startPreview();
       // inPreview = true;
        //startRecord();
        //captureButton.performClick();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isRecording)
        {
            stopRecord();
        }
        if (inPreview) {
            camera.stopPreview();
        }

        inPreview=false;
        //releaseMediaRecorder();
        releaseCamera();
      //  captureButton.performClick();

    }
    private void splitIt()
    {
        try {
            SplitterShortener.split(currentVideo);
        }  catch(IOException ie) {
            ie.printStackTrace();
         }
    }
    private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result=null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width<=width && size.height<=height) {
                if (result==null) {
                    result=size;
                }
                else {
                    int resultArea=result.width*result.height;
                    int newArea=size.width*size.height;

                    if (newArea>resultArea) {
                        result=size;
                    }
                }
            }
        }
        return(result);
    }

    private void initPreview(int width, int height) {
        if (camera!=null && previewHolder.getSurface()!=null) {
            try {
                camera.setPreviewDisplay(previewHolder);
            }
            catch (Throwable t) {
                Log.e("surfaceCallback",
                        "Exception in setPreviewDisplay()", t);
                Toast
                        .makeText(CamActivity.this, t.getMessage(), Toast.LENGTH_LONG)
                        .show();
            }

            if (!cameraConfigured) {
                Camera.Parameters parameters=camera.getParameters();
                Camera.Size size=getBestPreviewSize(width, height,
                        parameters);

                if (size!=null) {
                    parameters.setPreviewSize(size.width, size.height);
                    camera.setParameters(parameters);
                    cameraConfigured=true;
                }
            }
        }
    }

    protected void setDisplayOrientation(Camera camera, int angle){
        Method downPolymorphic;
        try
        {
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[] { int.class });
            if (downPolymorphic != null)
                downPolymorphic.invoke(camera, new Object[] { angle });
        }
        catch (Exception e1)
        {
        }
    }

    private void startPreview() {
        if (cameraConfigured && camera!=null) {
            Camera.Parameters p = camera.getParameters();
            if (Integer.parseInt(Build.VERSION.SDK) >= 8)
                setDisplayOrientation(camera, 90);
            else
            {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                {
                    p.set("orientation", "portrait");
                    p.set("rotation", 90);
                }
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                {
                    p.set("orientation", "landscape");
                    p.set("rotation", 90);
                }
            }
            inPreview = true;
        }

    }

    SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            // no-op -- wait until surfaceChanged()
        }
        public void surfaceChanged(SurfaceHolder holder,
                                   int format, int width,
                                   int height) {
            initPreview(width, height);
            startPreview();
            captureButton.performClick();
        }
        public void surfaceDestroyed(SurfaceHolder holder) {
            // no-op
        }
    };

    /* Media Saving

     */
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /** Create a file Uri for saving an image or video */
   // private static Uri getOutputMediaFileUri(int type){
    //    return Uri.fromFile(getOutputMediaFile(type));
    //}

    /** Create a File for saving an image or video */
    public static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "PreSnap/temp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("PreSnap", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }
        currentVideo = mediaFile.toString();
        return mediaFile;
    }
}

