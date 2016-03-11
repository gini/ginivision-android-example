package net.gini.android.visiontest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import net.gini.android.vision.BitmapFuture;
import net.gini.android.vision.CaptureActivity;
import net.gini.android.vision.JpegFuture;
import net.gini.android.vision.ScannerActivity;
import net.hockeyapp.android.CrashManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static net.gini.android.vision.Helpers.fitsGiniVisionRequirements;

public class StartActivity extends Activity {

    private static final int PERMISSION_REQUEST_CAMERA = 1;

    protected static final int IMAGE_REQUEST = 1;
    protected boolean shouldStoreOriginal = false;
    protected boolean shouldStoreRectified = false;
    protected boolean shouldStoreJpeg = false;
    protected LogbackConfigurator logbackConfigurator = new LogbackConfigurator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_start);

        ((Switch) findViewById(R.id.store_original)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                shouldStoreOriginal = isChecked;
            }
        });
        ((Switch) findViewById(R.id.store_rectified)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                shouldStoreRectified = isChecked;
            }
        });
        ((Switch) findViewById(R.id.store_jpeg)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                shouldStoreJpeg = isChecked;
            }
        });
        ((Switch) findViewById(R.id.enable_logging)).setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            logbackConfigurator.enableFileLogging(StartActivity.this);
                        } else {
                            logbackConfigurator.disableFileLogging();
                        }
                    }
                });

        logbackConfigurator.configureBasicLogging();
        ((TextView) findViewById(R.id.version_number)).setText(getVersion());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkForCrashes();
    }

    private void checkForCrashes() {
        CrashManager.register(this, "3689cb24647eca8143c6b6a3a7f6a0da");
    }

    private String getVersion() {
        String version = "Gini Scan ";
        try {
            version += getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return version;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_gini_settings) {
            Intent giniSettingsIntent = new Intent(this, GiniSettingsActivity.class);
            startActivity(giniSettingsIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    private String getImageFilename() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:s", Locale.GERMANY).format(new Date());
    }

    protected String storeImage(final Bitmap image, final String filename) {
        final File storageDirectory = getExternalFilesDir(null);
        final File imageFile = new File(storageDirectory, filename);
        try {
            final FileOutputStream outputStream = new FileOutputStream(imageFile);
            image.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast toast = Toast.makeText(this, "Could not save image", Toast.LENGTH_LONG);
            toast.show();
        }
        return imageFile.toString();
    }

    protected String storeImage(final byte[] image, final String filename) {
        final File storageDirectory = getExternalFilesDir(null);
        final File imageFile = new File(storageDirectory, filename);
        try {
            final FileOutputStream outputStream = new FileOutputStream(imageFile);
            outputStream.write(image);
        } catch (IOException e) {
            e.printStackTrace();
            Toast toast = Toast.makeText(this, "Could not save image", Toast.LENGTH_LONG);
            toast.show();
        }
        return imageFile.toString();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bundle documentBundle;
        BitmapFuture originalFuture = null;
        BitmapFuture rectifiedFuture = null;
        JpegFuture jpegFuture = null;

        if (requestCode == IMAGE_REQUEST && data != null) {
            documentBundle = data.getBundleExtra(ScannerActivity.EXTRA_DOCUMENT_BUNDLE);
            if (documentBundle != null) {
                originalFuture = documentBundle.getParcelable(CaptureActivity.EXTRA_ORIGINAL);
                rectifiedFuture = documentBundle.getParcelable(CaptureActivity.EXTRA_DOCUMENT);
                jpegFuture = documentBundle.getParcelable(CaptureActivity.EXTRA_DOCUMENT_JPEG);

                final String imageFilename = getImageFilename();
                if (shouldStoreOriginal && originalFuture != null) {
                    storeImage(originalFuture.get(), imageFilename + "_original.jpg");
                }
                if (shouldStoreRectified && rectifiedFuture != null) {
                    storeImage(rectifiedFuture.get(), imageFilename + "_rectified.jpg");
                }
                if (shouldStoreJpeg && jpegFuture != null) {
                    storeImage(jpegFuture.get(), imageFilename + "_rectified_exif.jpg");
                }
            }
        }

        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK) {
            final Intent resultIntent = new Intent(this, ResultsActivity.class);
            resultIntent.putExtra(ResultsActivity.EXTRA_DOCUMENT,
                    data.getStringExtra(UploadActivity.EXTRA_DOCUMENT));

            final Bundle extractionsBundle = data.getBundleExtra(ResultsActivity.EXTRA_EXTRACTIONS);
            resultIntent.putExtra(ResultsActivity.EXTRA_EXTRACTIONS,
                    extractionsBundle);
            startActivity(resultIntent);
        } else if (requestCode == IMAGE_REQUEST && resultCode == ScannerActivity.RESULT_ERROR) {
            final ScannerActivity.Error error = data.getParcelableExtra(ScannerActivity.EXTRA_ERROR);
            final Toast toast = Toast.makeText(this, "Error! " + error.toString(), Toast.LENGTH_LONG);
            toast.show();
        } else if (requestCode == IMAGE_REQUEST && resultCode == UploadActivity.RESULT_UPLOAD_ERROR) {
            final String error = data.getStringExtra(UploadActivity.EXTRA_ERROR_STRING);
            final Toast toast = Toast.makeText(this, "Getting the extractions failed! " + error, Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void scanDocument(View view) {
        requestCameraPermission();
    }

    public void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this)
                        .setTitle("Camera Permission");

                final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                        .size() > 0) {
                    dialogBuilder.setMessage("The app needs the camera, otherwise there is no Vision in GiniVision. Please allow" +
                            " camera access in the app info screen under Permissions.")
                            .setPositiveButton("Open app info screen", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    startActivity(intent);
                                }
                            });
                } else {
                    dialogBuilder.setMessage("The app needs the camera, otherwise there is no Vision in GiniVision.");
                    dialogBuilder.setPositiveButton("OK", null);
                }

                dialogBuilder.create().show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_CAMERA);

                // PERMISSION_REQUEST_CAMERA is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            startGiniVision();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startGiniVision();
                } else {
                    Toast.makeText(this, "The app needs the camera, otherwise there is no Vision in GiniVision.",
                            Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void startGiniVision() {
        // Requirements checked here, to allow requesting the camera permission first
        if (!fitsGiniVisionRequirements(this)) {
            final Toast toast = Toast.makeText(this, "Device not supported by Gini Vision", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }

        Intent captureActivity = new Intent(this, CaptureActivity.class);
        captureActivity.putExtra(CaptureActivity.EXTRA_STORE_ORIGINAL, shouldStoreOriginal);
        captureActivity.putExtra(CaptureActivity.EXTRA_SET_WINDOW_FLAG_SECURE, false);
        captureActivity.putExtra(CaptureActivity.EXTRA_CREATE_JPEG_DOCUMENT_WITH_METADATA, true);
        captureActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        ScannerActivity.setUploadActivityExtra(captureActivity, this, UploadActivity.class);
        startActivityForResult(captureActivity, IMAGE_REQUEST);

        // Comment the code above and uncomment the following to start the scanner directly.
//        Intent scanIntent = new Intent(this, ScannerActivity.class);
//        scanIntent.putExtra(ScannerActivity.EXTRA_STORE_ORIGINAL, shouldStoreOriginal);
//        scanIntent.putExtra(ScannerActivity.EXTRA_SET_WINDOW_FLAG_SECURE, false);
//        scanIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
//        final Bundle docTypeBundle = new Bundle();
//        // Change the DocumenType as required
//        docTypeBundle.putParcelable(ScannerActivity.EXTRA_DOCTYPE, DocumentType.INVOICE);
//        scanIntent.putExtra(ScannerActivity.EXTRA_DOCTYPE_BUNDLE, docTypeBundle);
//        ScannerActivity.setUploadActivityExtra(scanIntent, this, UploadActivity.class);
//        startActivityForResult(scanIntent, IMAGE_REQUEST);
    }
}
