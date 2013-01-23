package org.qii.weiciyuan.othercomponent.unreadnotification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import org.qii.weiciyuan.R;
import org.qii.weiciyuan.bean.AccountBean;
import org.qii.weiciyuan.bean.CommentListBean;
import org.qii.weiciyuan.bean.MessageListBean;
import org.qii.weiciyuan.bean.UnreadBean;
import org.qii.weiciyuan.support.utils.NotificationUtility;
import org.qii.weiciyuan.support.utils.Utility;
import org.qii.weiciyuan.ui.main.MainTimeLineActivity;
import org.qii.weiciyuan.ui.send.WriteCommentActivity;
import org.qii.weiciyuan.ui.send.WriteReplyToCommentActivity;

/**
 * User: qii
 * Date: 12-12-5
 */
public class JBBigTextNotification {
    private Context context;

    private AccountBean accountBean;

    private CommentListBean comment;
    private MessageListBean repost;
    private CommentListBean mentionCommentsResult;

    private UnreadBean unreadBean;

    public JBBigTextNotification(Context context,
                                 AccountBean accountBean,
                                 CommentListBean comment,
                                 MessageListBean repost,
                                 CommentListBean mentionCommentsResult, UnreadBean unreadBean) {
        this.context = context;
        this.accountBean = accountBean;
        this.comment = comment;
        this.repost = repost;
        this.mentionCommentsResult = mentionCommentsResult;
        this.unreadBean = unreadBean;
    }


    private PendingIntent getPendingIntent() {
        Intent i = new Intent(context, MainTimeLineActivity.class);
        i.putExtra("account", accountBean);
        i.putExtra("comment", comment);
        i.putExtra("repost", repost);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, Long.valueOf(accountBean.getUid()).intValue(), i, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    public Notification get() {
        Notification.Builder builder = new Notification.Builder(context)
                .setTicker(NotificationUtility.getTicker(unreadBean))
                .setContentText(accountBean.getUsernick())
                .setSmallIcon(R.drawable.notification)
                .setAutoCancel(true)
                .setContentIntent(getPendingIntent())
                .setOnlyAlertOnce(true);

        builder.setContentTitle(NotificationUtility.getTicker(unreadBean));

        builder.setNumber(1);

        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle(builder);

        if (unreadBean.getMention_status() == 1) {
            bigTextStyle.setBigContentTitle(repost.getItem(0).getUser().getScreen_name() + "：");
            bigTextStyle.bigText(repost.getItem(0).getText());


            Intent intent = new Intent(context, WriteCommentActivity.class);
            intent.putExtra("token", accountBean.getAccess_token());
            intent.putExtra("msg", repost.getItem(0));

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.comment_light, context.getString(R.string.comments), pendingIntent);
        }

        if (unreadBean.getCmt() == 1) {
            bigTextStyle.setBigContentTitle(comment.getItem(0).getUser().getScreen_name() + "：");
            bigTextStyle.bigText(comment.getItem(0).getText());

            Intent intent = new Intent(context, WriteReplyToCommentActivity.class);
            intent.putExtra("token", accountBean.getAccess_token());
            intent.putExtra("msg", comment.getItem(0));
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.reply_to_comment_light, context.getString(R.string.reply_to_comment), pendingIntent);
        }

        if (unreadBean.getMention_cmt() == 1) {
            bigTextStyle.setBigContentTitle(mentionCommentsResult.getItem(0).getUser().getScreen_name() + "：");
            bigTextStyle.bigText(mentionCommentsResult.getItem(0).getText());

            Intent intent = new Intent(context, WriteReplyToCommentActivity.class);
            intent.putExtra("token", accountBean.getAccess_token());
            intent.putExtra("msg", mentionCommentsResult.getItem(0));
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.reply_to_comment_light, context.getString(R.string.reply_to_comment), pendingIntent);
        }


        bigTextStyle.setSummaryText(accountBean.getUsernick());

        builder.setStyle(bigTextStyle);
        Utility.configVibrateLedRingTone(builder);

        return builder.build();
    }

}