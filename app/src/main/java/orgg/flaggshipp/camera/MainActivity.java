package orgg.flaggshipp.camera;

import static android.app.usage.UsageEvents.Event.NONE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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

    @SuppressLint("MissingInflatedId")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adjustSurfaceViewSize();

        surfaceView = (SurfaceView) findViewById(R.id.camera_preview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        ImageView captureBtn = findViewById(R.id.capture_button);
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

        ImageView switchCamBtn = findViewById(R.id.switch_cam_button);
        switchCamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoFocus();
            }
        });

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera = Camera.open();

            Camera.Parameters parameters = camera.getParameters();
            Camera.Size bestSize = getBestPreviewSize(parameters, 16.0 / 9.0);
            if (bestSize != null) {
                parameters.setPreviewSize(bestSize.width, bestSize.height);
                camera.setParameters(parameters);
            }

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
        } else {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }

        if (camera != null) {
            camera.setDisplayOrientation(90);
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size bestSize = getBestPreviewSize(parameters, 16.0 / 9.0);
            if (bestSize != null) {
                parameters.setPreviewSize(bestSize.width, bestSize.height);
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

    private void zoomCamera(int zoomValue) {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();

            if (parameters.isZoomSupported()) {
                int maxZoom = parameters.getMaxZoom();
                if (zoomValue >= 0 && zoomValue <= maxZoom) {
                    parameters.setZoom(zoomValue);
                    camera.setParameters(parameters);
                } else {
                    if (zoomValue < 0) {
                        parameters.setZoom(0);
                        camera.setParameters(parameters);
                    } else if (zoomValue > maxZoom) {
                        parameters.setZoom(maxZoom);
                        camera.setParameters(parameters);
                    }
                }
            } else {
                Toast.makeText(this, "Zoom wird nicht unterstützt", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void switchCamera() {
        if (camera != null) {
            releaseCamera(); // Freigeben der aktuellen Kamera

            // Überprüfen, ob es eine andere Kamera gibt, die wir verwenden können
            int numberOfCameras = Camera.getNumberOfCameras();
            if (numberOfCameras > 1) {
                useBackCamera = !useBackCamera; // Umschalten zwischen Vorder- und Hinterkamera
                initCamera(); // Initialisieren Sie die ausgewählte Kamera
            } else {
                Toast.makeText(this, "Nur eine Kamera verfügbar", Toast.LENGTH_SHORT).show();
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
            Log.e("FlaggCamera", "Fehler beim Erstellen des Verzeichnisses");
            return;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timestamp + ".jpg";
        File pictureFile = new File(mediaStorageDir.getPath() + File.separator + fileName);

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            Log.d("FlaggCamera", "Bild gespeichert: " + pictureFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            Log.e("FlaggCamera", "Datei nicht gefunden: " + e.getMessage());
        } catch (IOException e) {
            Log.e("FlaggCamera", "Fehler beim Zugriff auf die Datei: " + e.getMessage());
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

    private void adjustSurfaceViewSize() {

        surfaceView1 = (SurfaceView) findViewById(R.id.camera_preview);
        surfaceHolder = surfaceView1.getHolder();
        surfaceHolder.addCallback(this);

        int availableWidthInDp = calculateAvailableWidthInDp();

        int desiredHeightInDp = (int) ((4.0 / 3.0) * availableWidthInDp);

        int desiredHeightInPixels = dpToPx(desiredHeightInDp);

        surfaceView1.getLayoutParams().height = desiredHeightInPixels;
        surfaceView1.requestLayout();
    }

    private int calculateAvailableWidthInDp() {
        surfaceView1 = (SurfaceView) findViewById(R.id.camera_preview);
        surfaceHolder = surfaceView1.getHolder();
        surfaceHolder.addCallback(this);
        ViewParent parent = surfaceView1.getParent();

        if (parent instanceof View) {
            View parentView = (View) parent;

            int parentWidthInPixels = parentView.getWidth();

            int parentWidthInDp = pxToDp(parentWidthInPixels);

            return parentWidthInDp;
        } else {
            return 0;
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private int pxToDp(int px) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(px / density);
    }
}
