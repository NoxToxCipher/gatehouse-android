package au.com.dss.gatehouse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DeputyAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
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