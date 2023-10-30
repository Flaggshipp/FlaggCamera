package orgg.flaggshipp.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import android.view.ScaleGestureDetector;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private Camera camera;
    private SurfaceView surfaceView;

    private SurfaceView surfaceView1;
    private SurfaceHolder surfaceHolder;
    private boolean isPreviewing = false;
    private boolean useBackCamera = true;

    private ScaleGestureDetector scaleGestureDetector;

    private static final int NONE = 0;

    private static final int ZOOM = 1;
    private float currentZoom = 0;

    private float previousSpan = 0;

    private int mode = NONE;

    private float oldDist = 1f;

    private static final int PERMISSIONS_REQUEST_CODE = 123;

    @SuppressLint("MissingInflatedId")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        surfaceView = (SurfaceView) findViewById(R.id.camera_preview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        autoFocus();

        ImageButton galleryBtn = findViewById(R.id.gallery_button);
        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLastImage();
            }
        });

        ImageButton captureBtn = findViewById(R.id.capture_button);
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

        ImageButton switchCamBtn = findViewById(R.id.switch_cam_button);
        switchCamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        ImageButton flashlightBtn = findViewById(R.id.flashlight);
        flashlightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFlashlight();
            }
        });

        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoFocus();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "Permessions are needed to run the Application!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isFlashlightOn = false;

    private void toggleFlashlight() {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();

            if (isFlashlightOn) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                isFlashlightOn = false;
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                isFlashlightOn = true;
            }

            camera.setParameters(parameters);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera = Camera.open();

            camera.setDisplayOrientation(90);

            camera.setPreviewDisplay(holder);
            if (!isPreviewing) {
                camera.startPreview();
                isPreviewing = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            if (isPreviewing) {
                camera.stopPreview();
                isPreviewing = false;
            }
            camera.release();
            camera = null;
        }
    }

    public void openLastImage() {
        File imageFile = findLatestImageInDirectory("/sdcard/DCIM/FlaggCamera");
        Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(contentUri, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private File findLatestImageInDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();

        if (files != null && files.length > 0) {
            File latestImage = files[0];
            for (File file : files) {
                if (file.lastModified() > latestImage.lastModified()) {
                    latestImage = file;
                }
            }
            return latestImage;
        }

        return null;
    }

    private Camera.Size getBestPreviewSize(Camera.Parameters parameters, double targetRatio) {
        Camera.Size bestSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            double ratio = (double) size.width / size.height;
            double diff = Math.abs(ratio - targetRatio);
            if (diff < minDiff) {
                bestSize = size;
                minDiff = diff;
            }
        }

        return bestSize;
    }

    private void autoFocus() {
        if (camera != null) {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {

                    } else {

                    }
                }
            });
        }
    }

    private void manualFocus(float focusValue) {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            camera.setParameters(parameters);

            int maxFocus = parameters.getMaxNumFocusAreas();
            List<Camera.Area> focusAreas = new ArrayList<>();

            for (int i = 0; i < maxFocus; i++) {
                Camera.Area area = new Camera.Area(new Rect(-100, -100, 100, 100), 100);
                focusAreas.add(area);
            }

            parameters.setFocusAreas(focusAreas);
            camera.setParameters(parameters);

            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {

                    } else {

                    }
                }
            });
        }
    }

    private void initCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }

        if (useBackCamera) {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            camera.setDisplayOrientation(90);
        } else {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            camera.setDisplayOrientation(90);
        }

        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();

            Camera.Size bestSize = getBestPreviewSize(parameters, 16.0 / 9.0);
            if (bestSize != null) {
                parameters.setPreviewSize(bestSize.width, bestSize.height);
            }

            if (useBackCamera) {
                parameters.setRotation(0);
            } else {
                parameters.setRotation(0);
            }

            camera.setParameters(parameters);

            try {
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                isPreviewing = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    private void switchCamera() {
        if (camera != null) {
            releaseCamera();

            int numberOfCameras = Camera.getNumberOfCameras();
            if (numberOfCameras > 1) {
                useBackCamera = !useBackCamera;
                initCamera();
            } else {
                Toast.makeText(this, "Only one camera available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void takePicture() {
        if (camera != null) {
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    savePicture(data);
                    camera.startPreview();
                }
            });
        }
    }

    private void savePicture(byte[] data) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "FlaggCamera");

        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.e("FlaggCamera", "Error when creating the directory");
            return;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timestamp + ".jpg";
        File pictureFile = new File(mediaStorageDir.getPath() + File.separator + fileName);

        try {
            File tempPictureFile = File.createTempFile("temp", ".jpg", getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempPictureFile);
            fos.write(data);
            fos.close();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            Bitmap tempBitmap = BitmapFactory.decodeFile(tempPictureFile.getAbsolutePath(), options);

            Matrix matrix = new Matrix();

            if (useBackCamera) {
                matrix.postRotate(90);
            } else {
                matrix.postRotate(270);
            }

            Bitmap rotatedBitmap = Bitmap.createBitmap(tempBitmap, 0, 0, tempBitmap.getWidth(), tempBitmap.getHeight(), matrix, true);

            FileOutputStream rotatedFos = new FileOutputStream(pictureFile);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, rotatedFos);
            rotatedFos.close();

            tempPictureFile.delete();

            addToGallery(pictureFile);

            Log.d("FlaggCamera", "Image saved: " + pictureFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            Log.e("FlaggCamera", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e("FlaggCamera", "Error accessing the file: " + e.getMessage());
        }
    }

    private void savePictureToGallery(Image image) throws IOException {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        File imageFile = createImageFile();

        try (FileOutputStream output = new FileOutputStream(imageFile)) {
            output.write(bytes);

            addToGallery(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "FlaggCamera");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void addToGallery(File imageFile) {
        MediaScannerConnection.scanFile(
                this,
                new String[]{imageFile.getAbsolutePath()},
                null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {

                    }
                }
        );
    }
}