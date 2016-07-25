package com.altplus.democustomcamera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ImageSurfaceView mImageSurfaceView;
    public Camera camera = null;
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_WRITE_PERMISSION = 2;
    private FrameLayout cameraPreviewLayout;
    private ImageView capturedImageHolder;
    private float mDist;
    private Bitmap bm;
    private float ratio = 9f / 16f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        cameraPreviewLayout = (FrameLayout) findViewById(R.id.camera_preview);
        capturedImageHolder = (ImageView) findViewById(R.id.captured_image);
        Button captureButton = (Button) findViewById(R.id.button);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.takePicture(null, null, pictureCallback);
            }
        });
        cameraPreviewLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (cameraPreviewLayout.getLayoutParams().height == (int) (cameraPreviewLayout.getLayoutParams().width * ratio)) {
                    return;
                }
                cameraPreviewLayout.getLayoutParams().width = cameraPreviewLayout.getWidth();
                cameraPreviewLayout.getLayoutParams().height = (int) (cameraPreviewLayout.getLayoutParams().width * ratio);
                cameraPreviewLayout.requestLayout();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestCameraPermission();
    }

    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            bm = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                // Setting post rotate to 90
                Matrix mtx = new Matrix();
                mtx.postRotate(90);
                bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), mtx, true);
                bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), (int) (bm.getWidth() * ratio));
            } else {// LANDSCAPE MODE
                //No need to reverse width and height
                Bitmap scaled = Bitmap.createScaledBitmap(bm, bm.getWidth(), bm.getHeight(), true);
                bm = scaled;
            }
            if (bm == null) {
                Toast.makeText(MainActivity.this, "khoong duoc roi", Toast.LENGTH_LONG).show();
                return;
            }
//            capturedImageHolder.setImageBitmap(bm);

            Intent intent = new Intent(MainActivity.this, CaptureResultActivity.class);
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, bs);
            intent.putExtra("bitmap", bs.toByteArray());
            startActivity(intent);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestWritePermission();
            } else {
                asyncSave();
            }
//            camera.startPreview();
        }
    };

    public void asyncSave() {
        new AsyncTask<Bitmap, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Bitmap... bitmaps) {
                return saveBitmapImage(bitmaps[0]);
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                if (aBoolean) {
                    Toast.makeText(MainActivity.this, "Save Image Success", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to save image...", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute(bm);
    }

    private boolean saveBitmapImage(Bitmap bitmap) {
        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (pictureFile == null) {
            Log.d("BBB", "Error creating media file, check storage permissions: ");
            return false;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
//                fos.write(data);
            fos.close();
            galleryAddPic(pictureFile);
            return true;
        } catch (FileNotFoundException e) {
            Log.d("BBB", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("BBB", "Error accessing file: " + e.getMessage());
        }
        return false;
    }

    private static File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_LINH" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    // zoom
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Get the pointer ID
        Camera.Parameters params = camera.getParameters();
        int action = event.getAction();


        if (event.getPointerCount() > 1) {
            // handle multi-touch events
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                mDist = getFingerSpacing(event);
            } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {
                camera.cancelAutoFocus();
                handleZoom(event, params);
            }
        } else {
            // handle single touch events
            if (action == MotionEvent.ACTION_UP) {
                handleFocus(event, params);
            }
        }
        return true;
    }

    private void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist > mDist) {
            //zoom in
            if (zoom < maxZoom)
                zoom++;
        } else if (newDist < mDist) {
            //zoom out
            if (zoom > 0)
                zoom--;
        }
        mDist = newDist;
        params.setZoom(zoom);
        camera.setParameters(params);
    }

    public void handleFocus(MotionEvent event, Camera.Parameters params) {
        int pointerId = event.getPointerId(0);
        int pointerIndex = event.findPointerIndex(pointerId);
        // Get the pointer's current position
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    // currently set to auto-focus on single touch
                }
            });
        }
    }

    /**
     * Determine the space between the first two fingers
     */
    private float getFingerSpacing(MotionEvent event) {
        // ...
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    //Request Permission

    private void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA},
                            REQUEST_CAMERA_PERMISSION);
                }
            } else {
                camera = Camera.open();
                mImageSurfaceView = new ImageSurfaceView(this, camera);
                cameraPreviewLayout.addView(mImageSurfaceView);
            }
        } else {
            camera = Camera.open();
            mImageSurfaceView = new ImageSurfaceView(this, camera);
            cameraPreviewLayout.addView(mImageSurfaceView);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    camera = Camera.open(0);
                    mImageSurfaceView = new ImageSurfaceView(this, camera);
                    cameraPreviewLayout.addView(mImageSurfaceView);

                } else {
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            case REQUEST_WRITE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    asyncSave();
                } else {
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    //Request Write Permission

    private void requestWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_PERMISSION);
//                }
            } else {
                asyncSave();
            }
        } else {
            //api < 21
        }
    }

    //Add photo to the gallery
    private void galleryAddPic(File f) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
}

