package net.gini.android.visiontest;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import net.gini.android.vision.BitmapFuture;
import net.gini.android.vision.CaptureActivity;
import net.gini.android.vision.ScannerActivity;
import net.hockeyapp.android.CrashManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static net.gini.android.vision.Helpers.fitsGiniVisionRequirements;


public class StartActivity extends Activity {

    protected static final int IMAGE_REQUEST = 1;
    protected boolean shouldStoreOriginal = false;
    protected boolean shouldStoreRectified = false;
    protected LogbackConfigurator logbackConfigurator = new LogbackConfigurator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_start);

        ((Switch)findViewById(R.id.store_original)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                shouldStoreOriginal = isChecked;
            }
        });
        ((Switch)findViewById(R.id.store_rectified)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                shouldStoreRectified = isChecked;
            }
        });
        ((Switch)findViewById(R.id.enable_logging)).setOnCheckedChangeListener(
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
        ((TextView)findViewById(R.id.version_number)).setText(getVersion());

        if (!fitsGiniVisionRequirements(this)) {
            final Toast toast = Toast.makeText(this, "Device not supported by Gini Vision", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bundle documentBundle;
        BitmapFuture originalFuture = null;
        BitmapFuture rectifiedFuture = null;

        if (requestCode == IMAGE_REQUEST && data != null) {
            documentBundle = data.getBundleExtra(ScannerActivity.EXTRA_DOCUMENT_BUNDLE);
            if (documentBundle != null) {
                originalFuture = documentBundle.getParcelable(CaptureActivity.EXTRA_ORIGINAL);
                rectifiedFuture = documentBundle.getParcelable(CaptureActivity.EXTRA_DOCUMENT);
            }
        }

        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK) {
            final String imageFilename = getImageFilename();

            if (shouldStoreOriginal && originalFuture != null) {
                storeImage(originalFuture.get(), imageFilename + "_original.jpg");
            }
            if (shouldStoreRectified && rectifiedFuture != null) {
                storeImage(rectifiedFuture.get(), imageFilename + "_rectified.jpg");
            }

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
        Intent scanIntent = new Intent(this, CaptureActivity.class);
        scanIntent.putExtra(CaptureActivity.EXTRA_STORE_ORIGINAL, shouldStoreOriginal);
        scanIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        CaptureActivity.setUploadActivityExtra(scanIntent, this, UploadActivity.class);
        startActivityForResult(scanIntent, IMAGE_REQUEST);
    }
}
