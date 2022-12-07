package me.trive.trivemessage;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.CarExtender;
import android.support.v4.app.NotificationCompat.CarExtender.UnreadConversation;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.app.RemoteInput.Builder;

public class AndroidAutoService {

    // Initialization and attributes
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_VOICE_REPLY = "extra_voice_reply";
    public static final String READ_ACTION = "me.trive.verkehrsjodelprototyp.ACTION_MESSAGE_READ";
    public static final String REPLY_ACTION = "me.trive.verkehrsjodelprototyp.ACTION_MESSAGE_REPLY";
    private int MyconverisonID;
    private Context ctx;
    private NotificationManagerCompat mNotificationManager;

    // get the current context
    public AndroidAutoService(Context ctx) {
        this.ctx = ctx;
        this.MyconverisonID = 1;
    }

    private Intent getMessageReadIntent(int id) {
        return new Intent().addFlags(32).setAction(READ_ACTION).putExtra(CONVERSATION_ID, id);
    }

    private Intent getMessageReplyIntent(int conversationId) {
        return new Intent().addFlags(32).setAction(REPLY_ACTION).putExtra(CONVERSATION_ID, conversationId);
    }

    // create the Android Auto notification
    public void sendNotification(String Heading, String Message, int SmallIconRessource, int LargeIconRessource, int ConversionID) {
        this.mNotificationManager = NotificationManagerCompat.from(this.ctx);
        MyconverisonID = ConversionID;
        long Timestamp = System.currentTimeMillis();
        PendingIntent readPendingIntent = PendingIntent.getBroadcast(this.ctx, this.MyconverisonID, getMessageReadIntent(this.MyconverisonID), 0);
        RemoteInput remoteInput = new Builder(EXTRA_VOICE_REPLY).setLabel("3").build();
        UnreadConversation.Builder unreadConvBuilder = new UnreadConversation.Builder(Heading).setLatestTimestamp(Timestamp).setReadPendingIntent(readPendingIntent).setReplyAction(PendingIntent.getBroadcast(this.ctx, this.MyconverisonID, getMessageReplyIntent(this.MyconverisonID), 0), remoteInput);
        unreadConvBuilder.addMessage(Message);
        this.mNotificationManager.notify(this.MyconverisonID, new NotificationCompat.Builder(this.ctx).setSmallIcon(SmallIconRessource).setLargeIcon(BitmapFactory.decodeResource(this.ctx.getResources(), LargeIconRessource)).setContentText(Message).setWhen(Timestamp).setContentTitle(Heading).setContentIntent(readPendingIntent).extend(new CarExtender().setUnreadConversation(unreadConvBuilder.build()).setColor(R.color.ColorPrimary)).build());
    }
}