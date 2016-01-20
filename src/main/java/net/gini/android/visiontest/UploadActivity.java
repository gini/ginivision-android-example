package net.gini.android.visiontest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import net.gini.android.DocumentTaskManager;
import net.gini.android.Gini;
import net.gini.android.models.Document;
import net.gini.android.models.SpecificExtraction;
import net.gini.android.vision.DocumentType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import bolts.Continuation;
import bolts.Task;


public class UploadActivity extends net.gini.android.vision.UploadActivity {

    private static final Logger LOG = LoggerFactory.getLogger(UploadActivity.class);
    public static final String EXTRA_EXTRACTIONS = "extractions";
    public static final String EXTRA_ERROR_STRING = "error";
    public static final int RESULT_UPLOAD_ERROR = RESULT_FIRST_USER;

    private DocumentTaskManager documentTaskManager;
    private String documentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Gini gini = GiniService.INSTANCE.startService(this);
        documentTaskManager = gini.getDocumentTaskManager();
    }

    @Override
    public void uploadDocument(Bitmap bitmap) {
        // this is deprecated, the new uploadDocument method returns a jpeg with metadata, too, if requested and available
    }

    public void uploadDocument(Bitmap document, byte[] jpeg) {
        // Get the doctype, if available
        DocumentType documentType = DocumentType.INVOICE;
        final Bundle documentTypeBundle = getIntent().getBundleExtra(UploadActivity.EXTRA_DOCTYPE_BUNDLE);
        if (documentTypeBundle != null) {
            DocumentType docType = documentTypeBundle.getParcelable(UploadActivity.EXTRA_DOCTYPE);
            if (docType != null) documentType = docType;
        }

        DocumentTaskManager.DocumentType docType = null;
        switch (documentType) {
            case INVOICE:
                docType = DocumentTaskManager.DocumentType.INVOICE;
                break;
            case REMITTANCE_SLIP:
                docType = DocumentTaskManager.DocumentType.REMITTANCE_SLIP;
                break;
            case INTEGRATED_REMITTANCE_SLIP:
                docType = DocumentTaskManager.DocumentType.REMITTANCE_SLIP;
                break;
        }

        // Upload the jpeg, if available, otherwise try to upload the Bitmap
        Task<Document> createDocumentTask = null;
        if (jpeg != null) {
            createDocumentTask = documentTaskManager.createDocument(jpeg, null, docType);
        } else if (document != null) {
            createDocumentTask = documentTaskManager.createDocument(document, null, docType);
        }

        if (createDocumentTask == null) {
            final Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_ERROR_STRING, "No bitmap or jpeg to upload.");
            setResult(RESULT_UPLOAD_ERROR, resultIntent);
            finish();
        } else {
            createDocumentTask.onSuccessTask(new Continuation<Document, Task<Document>>() {
                @Override
                public Task<Document> then(Task<Document> task) throws Exception {
                    final Document document = task.getResult();
                    documentId = document.getId();
                    return documentTaskManager.pollDocument(document);
                }
            })
                    .onSuccessTask(new Continuation<Document, Task<Map<String, SpecificExtraction>>>() {
                        @Override
                        public Task<Map<String, SpecificExtraction>> then(Task<Document> task) throws Exception {
                            return documentTaskManager.getExtractions(task.getResult());
                        }
                    })
                    .onSuccess(new Continuation<Map<String, SpecificExtraction>, Object>() {
                        @Override
                        public Object then(Task<Map<String, SpecificExtraction>> task) throws Exception {
                            final Map<String, SpecificExtraction> extractions = task.getResult();
                            final Bundle extractionsBundle = new Bundle();
                            for (Map.Entry<String, SpecificExtraction> entry : extractions.entrySet()) {
                                extractionsBundle.putParcelable(entry.getKey(), entry.getValue());
                            }
                            final Intent result = new Intent();
                            result.putExtra(EXTRA_DOCUMENT, documentId);
                            result.putExtra(EXTRA_EXTRACTIONS, extractionsBundle);
                            setResult(RESULT_OK, result);
                            return null;
                        }
                    })
                    .continueWith(new Continuation<Object, Object>() {
                        @Override
                        public Object then(Task<Object> task) throws Exception {
                            if (task.isFaulted()) {
                                //noinspection ThrowableResultOfMethodCallIgnored
                                final Exception exception = task.getError();
                                // We could inspect the exception here -- should always be a subclass of a
                                // com.android.volley.VolleyError. For example:
                                // * AuthFailureError: Client credentials or access token was wrong
                                // * ConnectError, TimeoutError: can be caused by a flaky connection
                                // * NoConnectionError: (Likely) no Internet connection available
                                LOG.debug("Upload failed:", exception);
                                // Signal to parent activity that the uploading failed
                                final Intent resultIntent = new Intent();
                                resultIntent.putExtra(EXTRA_ERROR_STRING, exception.toString());
                                setResult(RESULT_UPLOAD_ERROR, resultIntent);
                            }
                            LOG.debug("done");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            });
                            return null;
                        }
                    });
        }
    }
}
