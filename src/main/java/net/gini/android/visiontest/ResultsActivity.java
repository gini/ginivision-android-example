package net.gini.android.visiontest;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import net.gini.android.DocumentTaskManager;
import net.gini.android.Gini;
import net.gini.android.models.Document;
import net.gini.android.models.SpecificExtraction;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bolts.Continuation;
import bolts.Task;


public class ResultsActivity extends ListActivity {

    private static final Logger LOG = LoggerFactory.getLogger(ResultsActivity.class);
    public static final String EXTRA_DOCUMENT = "document";
    public static final String EXTRA_EXTRACTIONS = "extractions";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        final Map<String, SpecificExtraction> extractionMap = unpackExtractions();
        setListAdapter(ResultsListAdapter.fromExtractionMap(extractionMap, this));
    }

    @NotNull
    private Map<String, SpecificExtraction> unpackExtractions() {
        Intent intent = getIntent();
        final Bundle extractionBundle = intent.getBundleExtra(EXTRA_EXTRACTIONS);
        final Map<String, SpecificExtraction> extractionMap = new HashMap<>();
        for (String key: extractionBundle.keySet()){
                SpecificExtraction extraction = extractionBundle.getParcelable(key);
                extractionMap.put(key, extraction);
        }
        return extractionMap;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_report_document) {
            reportBug();
        }

        return super.onOptionsItemSelected(item);
    }

    private void displayBuginatorToast() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ResultsActivity.this, "Thanks for letting us know.",
                               Toast.LENGTH_LONG).show();
            }
        });
    }

    public void reportBug() {
        final String documentId = getIntent().getStringExtra(EXTRA_DOCUMENT);
        if (documentId != null) {
            final Gini gini = GiniService.INSTANCE.startService(this);
            final DocumentTaskManager documentTaskManager = gini.getDocumentTaskManager();
            documentTaskManager.getDocument(documentId)
                    .onSuccessTask(new Continuation<Document, Task<String>>() {
                        @Override
                        public Task<String> then(Task<Document> task) throws Exception {
                            final Document document = task.getResult();
                            return documentTaskManager
                                    .reportDocument(document, "Android Testing Session", null);
                        }
                    })
                    .onSuccess(new Continuation<String, Object>() {
                        @Override
                        public Object then(Task<String> task) throws Exception {
                            LOG.debug(task.getResult());
                            displayBuginatorToast();
                            return null;
                        }
                    });
        }
    }

    private static class ResultsListAdapter extends ArrayAdapter<SpecificExtraction> {

        private static final int layoutResource = R.layout.results_list_item;

        public ResultsListAdapter(Activity context, final List<SpecificExtraction> extractions) {
            super(context, layoutResource, extractions);
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            if (convertView == null) {
                // Get a new instance of the row layout view
                LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
                convertView = inflater.inflate(layoutResource, null);
            }

            final SpecificExtraction extraction = getItem(position);
            ((TextView) convertView.findViewById(R.id.resultTitle)).setText(extraction.getName());
            ((TextView) convertView.findViewById(R.id.resultValue)).setText(extraction.getValue());
            return convertView;
        }

        private static ResultsListAdapter fromExtractionMap(
                final Map<String, SpecificExtraction> extractionMap, final Activity context) {
            final ArrayList<SpecificExtraction> extractions = new ArrayList<>();
            for (final Map.Entry<String, SpecificExtraction> entry : extractionMap.entrySet()) {
                extractions.add(entry.getValue());
            }
            return new ResultsListAdapter(context, extractions);
        }
    }
}
