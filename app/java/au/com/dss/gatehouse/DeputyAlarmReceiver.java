package au.com.dss.gatehouse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DeputyAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && "au.com.dss.gatehouse.SATELLITE_PASS_ALERT".equals(intent.getAction())) {
            SatelliteTrackerManager.VisualPass vp = new SatelliteTrackerManager.VisualPass();
            vp.passId = intent.getStringExtra("pass_id");
            vp.satName = intent.getStringExtra("sat_name");
            vp.visualMag = intent.getDoubleExtra("sat_mag", -2.5);
            vp.startAzCompass = intent.getStringExtra("start_az");
            vp.maxEl = intent.getDoubleExtra("max_el", 45.0);
            vp.maxAzCompass = intent.getStringExtra("max_az");
            vp.isStarlinkTrain = intent.getBooleanExtra("is_starlink", false);
            vp.trainSatCount = intent.getIntExtra("train_count", 1);
            vp.durationSec = 360;

            SatelliteTrackerManager.dispatchPassAlert(context, vp, false);
            return;
        }

        DeputyApi api = new DeputyApi(context);
        api.syncRoster(new DeputyApi.ApiCallback<DeputyApi.DeputyRosterResult>() {
            @Override
            public void onSuccess(DeputyApi.DeputyRosterResult result) {
                DeputyNotifier.processSyncResult(context, result);
            }

            @Override
            public void onError(String errorMessage) {}
        });
    }
}