package com.neshijain.videomerger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;


import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements AsyncTaskCompleteListener {

    String localVideoPath;
    String downloadedVideoPath;
    String OutputPAth;
    private final int GALLERY = 3, CAMERA = 2, UPLOAD =4;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    String downloadUrl = "http://d37krj2vgms093.cloudfront.net/f07e4b96-f0ba-4f4b-86fe-8d9a6619c92c-trimmedVideo_1603209348078.mp4";
    String uploadUrl = "http://13.235.86.46:5288/upload?mediacontent";
    String OutputFolder = "";
    private long downloadID;

    TextView text ;
    FrameLayout frameLayout;
    com.google.android.material.button.MaterialButton ButtonVideoTwo, ButtonVideoOne, MergeButton;
    FloatingActionButton fab,info;
   // using broadcast method
    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadID == id) {
                downloadID = -1;

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerReceiver(onDownloadComplete,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        CheckStorageDirectories();
        initButtons();
        verifyStoragePermissions(this);
        requestMultiplePermissions();
        localVideoPath = "";
        downloadedVideoPath = "";
        downloadID = -1;
        frameLayout.setVisibility(View.GONE);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        // using broadcast method
        unregisterReceiver(onDownloadComplete);
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }


    public void initButtons()
    {

        frameLayout = (FrameLayout)findViewById(R.id.progress_view);
        ButtonVideoOne = (MaterialButton) findViewById(R.id.selectvideo);
        ButtonVideoTwo = (MaterialButton) findViewById(R.id.downloadvideo);
        MergeButton = (MaterialButton) findViewById(R.id.merge);
        info = findViewById(R.id.info);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showinfo(null);
            }
        });


    }
    public  boolean CheckIfVideosAvailable()
    {
        if(localVideoPath.equals(""))
        {
            Toast toast = Toast.makeText(getApplicationContext(), "Video 1 is not available. Please upload", Toast.LENGTH_LONG);
            toast.show();
            return false;
        }
        if(downloadedVideoPath.equals(""))
        {
            Toast toast = Toast.makeText(getApplicationContext(), "Video 2 is not available. Please download", Toast.LENGTH_LONG);
            toast.show();
            return false;
        }
        return true;
    }

    public  void mergeVideos( View v)
    {
        if(!CheckIfVideosAvailable())
            return;
        onMergeStart();

        try {
         int code1 = FFmpeg.execute(new String[]{"-noautorotate","-i", localVideoPath,"-y","-c","copy","-bsf:v", "h264_mp4toannexb", "-f", "mpegts", OutputFolder+"/intermediate1.ts"});
         int code2 = FFmpeg.execute(new String[]{"-i", downloadedVideoPath,"-y","-c","copy","-bsf:v", "h264_mp4toannexb", "-f", "mpegts", OutputFolder+"/intermediate2.ts"});
         int code3 = FFmpeg.execute(new String[]{"-i","concat:"+OutputFolder+"/intermediate1.ts|"+OutputFolder+"/intermediate2.ts","-y","-c","copy","-bsf:v", "h264_mp4toannexb", OutputPAth});
          if(code1 !=0) {
                Toast toast = Toast.makeText(getApplicationContext(), "Error Decoding video 1", Toast.LENGTH_LONG);
                toast.show();
                onMergeError();
            }
            else
            {}
            if(code2 !=0) {
                Toast toast = Toast.makeText(getApplicationContext(), "Error Decoding video 2", Toast.LENGTH_LONG);
                toast.show();
                onMergeError();
            }
            else
            {}
            if(code3 !=0) {
                onMergeError();
            }
            else
            {
                onMergeDone();
            }

        } catch (Exception e) {
        }
    }

    public void onMergeStart()
    {
        frameLayout.setVisibility(View.VISIBLE);
    }
    public void onMergeDone()
    {
      //  Toast toast = Toast.makeText(getApplicationContext(), "Video saved at "+OutputPAth, Toast.LENGTH_LONG);
       // toast.show();
        uploadVideofile(OutputPAth);
    }
    public void onMergeError()
    {
        Toast toast = Toast.makeText(getApplicationContext(), "Error Merging Videos", Toast.LENGTH_LONG);
        frameLayout.setVisibility(View.INVISIBLE);
        resetvideomergeicon();
        toast.show();
    }
    public void onUploadDone(String toastmessage)
    {
        Toast toast = Toast.makeText(getApplicationContext(), toastmessage, Toast.LENGTH_LONG);
        toast.show();
        frameLayout.setVisibility(View.INVISIBLE);
        setvideomergeicon();
    //    text.setText("File uploaded at ");

    }

    public void onUploadStart()
    {
        frameLayout.setVisibility(View.VISIBLE);
    }

    public void onVideoSelected()
    {
        setvideoselecticon();
    }

    public void onVideoDownloadStart()
    {
        frameLayout.setVisibility(View.VISIBLE);
    }

    public void onVideoDownloaded(int status)
    {
        if(status == 0)
            setvideodownloadicon();
        else
            resetvideodownloadicon();
        frameLayout.setVisibility(View.INVISIBLE);
    }
    public void setvideoselecticon()
    {
        Drawable icon = ResourcesCompat.getDrawable(getResources(),android.R.drawable.ic_menu_camera,null);
        icon.setTint(2);
        icon.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.green),
                PorterDuff.Mode.MULTIPLY);
     //   ButtonVideoOne.setIcon(icon);
        ButtonVideoOne.setText("Video Selected");
    }

    public void resetvideoselecticon()
    {
        Drawable icon = ResourcesCompat.getDrawable(getResources(),android.R.drawable.ic_menu_camera,null);
    //    ButtonVideoOne.setIcon(icon);
        ButtonVideoOne.setText("Select Video");
    }
    public void setvideodownloadicon()
    {
        Drawable icon = ResourcesCompat.getDrawable(getResources(),android.R.drawable.ic_menu_save,null);
        icon.setTint(2);
        icon.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.green),
                PorterDuff.Mode.MULTIPLY);
    //    ButtonVideoTwo.setIcon(icon);
        ButtonVideoTwo.setText("Video Downloaded");
    }

    public void resetvideodownloadicon()
    {
        Drawable icon = ResourcesCompat.getDrawable(getResources(),android.R.drawable.ic_menu_save,null);
     //   ButtonVideoTwo.setIcon(icon);
        ButtonVideoTwo.setText("Download Video");
    }


    public void setvideomergeicon()
    {
        Drawable icon = ResourcesCompat.getDrawable(getResources(),android.R.drawable.ic_menu_add,null);
        icon.setTint(2);
        icon.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.green),
                PorterDuff.Mode.MULTIPLY);
      //  MergeButton.setIcon(icon);
        MergeButton.setText("Video Uploaded");
    }

    public void resetvideomergeicon()
    {
        Drawable icon = ResourcesCompat.getDrawable(getResources(),android.R.drawable.ic_menu_add,null);
      //  MergeButton.setIcon(icon);
        MergeButton.setText("Merge and Upload");
    }

    public void refresh(View v)
    {

        resetvideodownloadicon();
        resetvideomergeicon();
        resetvideoselecticon();
        localVideoPath = "";
        downloadedVideoPath="";
        frameLayout.setVisibility(View.INVISIBLE);
        text.setText("");


    }

    public void showinfo(View v)
    {

        Toast toast = Toast.makeText(getApplicationContext(), "OutPut folder is "+OutputFolder, Toast.LENGTH_LONG);
        toast.show();

    }



    private void uploadVideofile(String path) {

        onUploadStart();
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("url", uploadUrl);
        map.put("mediacontent", path);
        new MultiPartRequester(this, map, UPLOAD, this);
    }


   public void CheckStorageDirectories()
    {

        OutputFolder = Environment.getExternalStorageDirectory().getAbsolutePath()+"/videomerger";
        OutputPAth = OutputFolder+"/Output.mp4";
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode,resultCode,intent);
        if(resultCode != RESULT_OK)
        {
            Toast toast = Toast.makeText(getApplicationContext(), "Error Recording/Fetching video.", Toast.LENGTH_LONG);
            toast.show();
            return;
        }
        if (requestCode == GALLERY) {
            if (intent != null) {
                Uri contentURI = intent.getData();

                String selectedVideoPath = getPath(contentURI);
                saveVideoToInternalStorage(selectedVideoPath);
            }

        }
        else if (requestCode == CAMERA) {
            Uri contentURI = intent.getData();
            String recordedVideoPath = getPath(contentURI);
            saveVideoToInternalStorage(recordedVideoPath);
        }

        onVideoSelected();

    }
    public void showPictureDialog(View v){
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this);
        pictureDialog.setTitle("Select Action");
        String[] pictureDialogItems = {
                "Select video from gallery",
                "Record video from camera" };
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                chooseVideoFromGallary();
                                break;
                            case 1:
                                takeVideoFromCamera();
                                break;
                        }
                    }
                });
        pictureDialog.show();
    }

    public void chooseVideoFromGallary() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, GALLERY);
    }

    private void takeVideoFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(intent, CAMERA);
    }


    @Override
    public void onTaskCompleted(String response, int serviceCode) {
        Log.d("res", response);
        String toastmessage = "";

        switch (serviceCode) {

            case UPLOAD:
                try {

                    JSONObject jsonObject = new JSONObject(response);
                    jsonObject.toString().replace("\\\\","");
                    if ( jsonObject.has("video_url")) {
                        toastmessage = "Upload Completed. ";
                    }
                    else
                    {
                        toastmessage = "Upload Error !!";
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    toastmessage = "Upload Error !!";
                }

                onUploadDone(toastmessage);
        }

    }



    private void  requestMultiplePermissions(){
        Dexter.withActivity(this)
                .withPermissions(

                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {

                        if (report.areAllPermissionsGranted()) {
                        }


                        if (report.isAnyPermissionPermanentlyDenied()) {


                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(getApplicationContext(), "Some Error! ", Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();
    }


    private void saveVideoToInternalStorage (String filePath) {

        File newfile;

        try {

            File currentFile = new File(filePath);
            File OutputDir = new File(OutputFolder);
            newfile = new File(OutputDir, Calendar.getInstance().getTimeInMillis() + ".mp4");

            if (!OutputDir.exists()) {
                OutputDir.mkdirs();
            }

            if(currentFile.exists()){

                InputStream in = new FileInputStream(currentFile);
                OutputStream out = new FileOutputStream(newfile);

                byte[] buf = new byte[1024];
                int len;

                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                Log.v(Config.TAG, "Video file saved successfully.");
                localVideoPath = newfile.getAbsolutePath();
            }else{
                Log.v(Config.TAG, "Video saving failed. Source file missing.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Video.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
    }

    public void beginDownload(View v){

        onVideoDownloadStart();
        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
        fileName = fileName.substring(0,1).toUpperCase() + fileName.substring(1);
        File file = new File(OutputFolder+"/"+fileName);
        downloadedVideoPath = file.getAbsolutePath();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationUri(Uri.fromFile(file))
                .setTitle(fileName)
                .setDescription("Downloading")
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true);
        DownloadManager downloadManager= (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadID = downloadManager.enqueue(request);
        Toast.makeText(MainActivity.this, "Download Started", Toast.LENGTH_SHORT).show();
        boolean finishDownload = false;
        int progress;
        while (!finishDownload) {
            Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadID));
            Log.d(Config.TAG,"not finished");
            if (cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                switch (status) {
                    case DownloadManager.STATUS_FAILED: {

                        downloadID = -1;
                        finishDownload = true;
                        onVideoDownloaded(-1);
                        break;
                    }
                    case DownloadManager.STATUS_PAUSED:
                        break;
                    case DownloadManager.STATUS_PENDING:
                        break;
                    case DownloadManager.STATUS_RUNNING: {
                        final long total = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        if (total >= 0) {
                            final long downloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            progress = (int) ((downloaded * 100L) / total);
                        }
                        break;
                    }
                    case DownloadManager.STATUS_SUCCESSFUL: {
                        progress = 100;
                        downloadID = -1;
                        finishDownload = true;
                        onVideoDownloaded(0);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (frameLayout.getVisibility() == View.VISIBLE) {
            frameLayout.setVisibility(View.GONE);
            refresh(null);
        } else {
            super.onBackPressed();
        }

    }

}