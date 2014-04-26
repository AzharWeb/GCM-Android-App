package com.pixelart.gcm;
import android.app.IntentService;
import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.os.SystemClock;
import android.content.Context;
import android.app.PendingIntent;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import android.media.RingtoneManager;
import android.net.Uri;

public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
	String TAG = "Px GCM IntentService";
	String newms;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " + extras.toString());
				// If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // Post notification of received message.
				newms = extras.getString("message").toString();
                sendNotification("Received: " + newms);;
            }
        }
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
			this.getSystemService(Context.NOTIFICATION_SERVICE);
			
        Intent intent = new Intent(this, MainActivity.class);
		intent.putExtra("alert", "1");
		intent.putExtra("msg",newms);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,intent, 0);

		Uri uri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		
        NotificationCompat.Builder mBuilder =
			new NotificationCompat.Builder(this)
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle("GCM Notification")
			.setSound(uri)
			.setStyle(new NotificationCompat.BigTextStyle()
					  .bigText(msg))
			.setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
