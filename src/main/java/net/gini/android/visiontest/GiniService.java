package net.gini.android.visiontest;

import android.content.Context;

import net.gini.android.Gini;
import net.gini.android.SdkBuilder;

// TODO implement real service.
public enum GiniService {
    INSTANCE;

    private Gini giniSDK;

    public Gini startService(final Context context) {
        if (giniSDK == null) {
            giniSDK = new SdkBuilder(
                    context,
                    context.getString(R.string.gini_api_client_id),
                    context.getString(R.string.gini_api_client_secret),
                    "example.com"
            ).build();
        }

        return giniSDK;
    }
}
