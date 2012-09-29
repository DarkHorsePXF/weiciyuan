package org.qii.weiciyuan.ui.browser;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StrikethroughSpan;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import org.qii.weiciyuan.R;
import org.qii.weiciyuan.bean.GeoBean;
import org.qii.weiciyuan.bean.MessageBean;
import org.qii.weiciyuan.dao.show.ShowStatusDao;
import org.qii.weiciyuan.support.error.ErrorCode;
import org.qii.weiciyuan.support.error.WeiboException;
import org.qii.weiciyuan.support.file.FileLocationMethod;
import org.qii.weiciyuan.support.lib.MyAsyncTask;
import org.qii.weiciyuan.support.utils.GlobalContext;
import org.qii.weiciyuan.support.utils.ListViewTool;
import org.qii.weiciyuan.ui.Abstract.IToken;
import org.qii.weiciyuan.ui.Abstract.IWeiboMsgInfo;
import org.qii.weiciyuan.ui.send.CommentNewActivity;
import org.qii.weiciyuan.ui.send.RepostNewActivity;
import org.qii.weiciyuan.ui.task.FavAsyncTask;
import org.qii.weiciyuan.ui.userinfo.UserInfoActivity;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * User: qii
 * Date: 12-9-1
 */
public class BrowserWeiboMsgFragment extends Fragment {

    private MessageBean msg;

    private TextView username;
    private TextView content;
    private TextView recontent;
    private TextView time;
    private TextView location;
    private TextView source;

    private ImageView avatar;
    private ImageView content_pic;
    private ImageView repost_pic;


    private ProgressBar content_pic_pb;
    private ProgressBar repost_pic_pb;

    private UpdateMsgTask task = null;
    private GetGoogleLocationInfo geoTask = null;
    private FavAsyncTask favTask = null;

    private ShareActionProvider mShareActionProvider;

    private MenuItem refreshItem;

    public BrowserWeiboMsgFragment() {
    }


    public BrowserWeiboMsgFragment(MessageBean msg) {
        this.msg = msg;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("msg", msg);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        if (savedInstanceState != null) {
            msg = (MessageBean) savedInstanceState.getSerializable("msg");
        } else {
            task = new UpdateMsgTask();
            task.execute();
        }
        buildViewData();


    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (task != null) {
            task.cancel(true);
        }
        if (geoTask != null) {
            geoTask.cancel(true);
        }
        avatar.setImageDrawable(null);
        content_pic.setImageDrawable(null);
        repost_pic.setImageDrawable(null);
    }

    class UpdateMsgTask extends MyAsyncTask<Void, Void, MessageBean> {
        WeiboException e;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_action_view, null);

            Animation rotation = AnimationUtils.loadAnimation(getActivity(), R.anim.refresh);
            iv.startAnimation(rotation);

            refreshItem.setActionView(iv);
        }

        @Override
        protected MessageBean doInBackground(Void... params) {
            try {
                return new ShowStatusDao(((IToken) getActivity()).getToken(), msg.getId()).getMsg();
            } catch (WeiboException e) {
                this.e = e;
                cancel(true);
            }
            return null;
        }

        @Override
        protected void onCancelled(MessageBean weiboMsgBean) {
            super.onCancelled(weiboMsgBean);
            if (getActivity() != null) {
                if (this.e != null) {
                    Toast.makeText(getActivity(), e.getError(), Toast.LENGTH_SHORT).show();
                    if (e.getError_code() == ErrorCode.DELETED) {
                        setTextViewDeleted();
                    }
                }
                completeRefresh();
            }
        }

        @Override
        protected void onPostExecute(MessageBean newValue) {
            if (newValue != null && e == null) {
                msg = newValue;
                buildViewData();

            }
            completeRefresh();
            super.onPostExecute(newValue);
        }
    }

    private void completeRefresh() {
        if (refreshItem.getActionView() != null) {
            refreshItem.getActionView().clearAnimation();
            refreshItem.setActionView(null);
        }
    }

    private void setTextViewDeleted() {
        SpannableString ss = new SpannableString(content.getText().toString());
        ss.setSpan(new StrikethroughSpan(), 0, ss.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        content.setText(ss);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.browserweibomsgactivity_layout, container, false);

        username = (TextView) view.findViewById(R.id.username);
        content = (TextView) view.findViewById(R.id.content);
        recontent = (TextView) view.findViewById(R.id.repost_content);
        time = (TextView) view.findViewById(R.id.time);
        location = (TextView) view.findViewById(R.id.location);
        source = (TextView) view.findViewById(R.id.source);

        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GeoBean bean = msg.getGeo();
                String geoUriString = "geo:" + bean.getLat() + "," + bean.getLon() + "?q=" + location.getText();
                Uri geoUri = Uri.parse(geoUriString);
                Intent mapCall = new Intent(Intent.ACTION_VIEW, geoUri);
                PackageManager packageManager = getActivity().getPackageManager();
                List<ResolveInfo> activities = packageManager.queryIntentActivities(mapCall, 0);
                boolean isIntentSafe = activities.size() > 0;
                if (isIntentSafe) {
                    startActivity(mapCall);
                }


            }
        });

        avatar = (ImageView) view.findViewById(R.id.avatar);
        content_pic = (ImageView) view.findViewById(R.id.content_pic);
        repost_pic = (ImageView) view.findViewById(R.id.repost_content_pic);

        content_pic_pb=(ProgressBar)view.findViewById(R.id.content_pic_pb);
        repost_pic_pb=(ProgressBar)view.findViewById(R.id.repost_content_pic_pb);

        view.findViewById(R.id.first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), UserInfoActivity.class);
                intent.putExtra("token", ((IToken) getActivity()).getToken());
                intent.putExtra("user", msg.getUser());
                startActivity(intent);
            }
        });

        content_pic.setOnClickListener(picOnClickListener);
        repost_pic.setOnClickListener(picOnClickListener);

        //        LinearLayout repost_layout = (LinearLayout) findViewById(R.id.repost_layout);
        recontent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (recontent.getSelectionStart() == -1 && recontent.getSelectionEnd() == -1) {
                    //This condition will satisfy only when it is not an autolinked text
                    //onClick action

                    Intent intent = new Intent(getActivity(), BrowserWeiboMsgActivity.class);
                    intent.putExtra("token", ((IToken) getActivity()).getToken());
                    intent.putExtra("msg", msg.getRetweeted_status());
                    startActivity(intent);
                }

            }
        });
        return view;
    }

    private void buildViewData() {
        if (msg.getUser() != null) {
            username.setText(msg.getUser().getScreen_name());
            //50px avatar or 180px avatar
            String url;
            FileLocationMethod method;
            if (GlobalContext.getInstance().getEnableBigAvatar()) {
                url = msg.getUser().getAvatar_large();
                method = FileLocationMethod.avatar_large;
            } else {
                url = msg.getUser().getProfile_image_url();
                method = FileLocationMethod.avatar_small;
            }
            Bitmap bitmap = GlobalContext.getInstance().getAvatarCache().get(url);
            if (bitmap != null) {
                avatar.setImageBitmap(bitmap);
            } else {

                SimpleBitmapWorkerTask avatarTask = new SimpleBitmapWorkerTask(avatar, method);
                avatarTask.execute(url);
            }
        }
        content.setText(msg.getText());
        ListViewTool.addLinks(content);

        time.setText(msg.getTimeInFormat());

        if (msg.getGeo() != null) {
            if (geoTask == null || geoTask.getStatus() == MyAsyncTask.Status.FINISHED) {
                geoTask = new GetGoogleLocationInfo(msg.getGeo());
                geoTask.execute();
            }
        }
        if (!TextUtils.isEmpty(msg.getSource())) {
            source.setText(Html.fromHtml(msg.getSource()).toString());
        }
        if (msg.getRetweeted_status() != null) {
            recontent.setVisibility(View.VISIBLE);
            if (msg.getRetweeted_status().getUser() != null) {
                recontent.setText("@" + msg.getRetweeted_status().getUser().getScreen_name() + "：" + msg.getRetweeted_status().getText());
                ListViewTool.addLinks(recontent);

            } else {
                recontent.setText(msg.getRetweeted_status().getText());

            }
            if (!TextUtils.isEmpty(msg.getRetweeted_status().getBmiddle_pic())) {
//                repost_pic.setVisibility(View.VISIBLE);
                SimpleBitmapWorkerTask task = new SimpleBitmapWorkerTask(repost_pic, FileLocationMethod.picture_bmiddle,repost_pic_pb);
                task.execute(msg.getRetweeted_status().getBmiddle_pic());
            } else if (!TextUtils.isEmpty(msg.getRetweeted_status().getThumbnail_pic())) {
                repost_pic.setVisibility(View.VISIBLE);
                SimpleBitmapWorkerTask task = new SimpleBitmapWorkerTask(repost_pic, FileLocationMethod.picture_thumbnail);
                task.execute(msg.getRetweeted_status().getThumbnail_pic());

            }
        }


        if (!TextUtils.isEmpty(msg.getBmiddle_pic())) {
//            content_pic.setVisibility(View.VISIBLE);

            SimpleBitmapWorkerTask task = new SimpleBitmapWorkerTask(content_pic, FileLocationMethod.picture_bmiddle,content_pic_pb);
            task.execute(msg.getBmiddle_pic());
        } else if (!TextUtils.isEmpty(msg.getThumbnail_pic())) {
//            content_pic.setVisibility(View.VISIBLE);
            SimpleBitmapWorkerTask task = new SimpleBitmapWorkerTask(content_pic, FileLocationMethod.picture_thumbnail);
            task.execute(msg.getThumbnail_pic());

        }

        getActivity().getActionBar().getTabAt(1).setText(getString(R.string.comments) + "(" + msg.getComments_count() + ")");
        getActivity().getActionBar().getTabAt(2).setText(getString(R.string.repost) + "(" + msg.getReposts_count() + ")");

        buildShareActionMenu();
    }

    private class GetGoogleLocationInfo extends MyAsyncTask<Void, String, String> {

        GeoBean geoBean;

        public GetGoogleLocationInfo(GeoBean geoBean) {
            this.geoBean = geoBean;
        }

        @Override
        protected String doInBackground(Void... params) {

            Geocoder geocoder = new Geocoder(getActivity(), Locale.getDefault());

            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(geoBean.getLat(), geoBean.getLon(), 1);
            } catch (IOException e) {
                cancel(true);
            }
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);

                StringBuilder builder = new StringBuilder();
                int size = address.getMaxAddressLineIndex();
                for (int i = 0; i < size; i++) {
                    builder.append(address.getAddressLine(i));
                }
                return builder.toString();
            }

            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            location.setVisibility(View.VISIBLE);
            location.setText(s);
            super.onPostExecute(s);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.browserweibomsgactivity_menu, menu);

        MenuItem item = menu.findItem(R.id.menu_share);
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();
        buildShareActionMenu();
        refreshItem = menu.findItem(R.id.menu_refresh);

        super.onCreateOptionsMenu(menu, inflater);
    }

    private void buildShareActionMenu() {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        if (msg != null) {
            sharingIntent.putExtra(Intent.EXTRA_TEXT, msg.getText());
            PackageManager packageManager = getActivity().getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(sharingIntent, 0);
            boolean isIntentSafe = activities.size() > 0;
            if (isIntentSafe && mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(sharingIntent);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.repostsbyidtimelinefragment_repost:
                intent = new Intent(getActivity(), RepostNewActivity.class);
                intent.putExtra("token", ((IToken) getActivity()).getToken());
                intent.putExtra("id", ((IWeiboMsgInfo) getActivity()).getMsg().getId());
                intent.putExtra("msg", ((IWeiboMsgInfo) getActivity()).getMsg());
                startActivity(intent);
                break;
            case R.id.commentsbyidtimelinefragment_comment:

                intent = new Intent(getActivity(), CommentNewActivity.class);
                intent.putExtra("token", ((IToken) getActivity()).getToken());
                intent.putExtra("id", ((IWeiboMsgInfo) getActivity()).getMsg().getId());
                startActivity(intent);

                break;
            case R.id.menu_refresh:
                if (task == null || task.getStatus() == MyAsyncTask.Status.FINISHED) {
                    task = new UpdateMsgTask();
                    task.execute();
                }
                break;
            case R.id.menu_share:

                buildShareActionMenu();
                return true;
            case R.id.menu_copy:
                ClipboardManager cm = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("sinaweibo", content.getText().toString()));
                Toast.makeText(getActivity(), getString(R.string.copy_successfully), Toast.LENGTH_SHORT).show();
                break;
            case R.id.menu_fav:
                if (favTask == null || favTask.getStatus() == MyAsyncTask.Status.FINISHED) {
                    favTask = new FavAsyncTask(((IToken) getActivity()).getToken(), msg.getId());
                    favTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
                }

                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private View.OnClickListener picOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String url = "";
            switch (v.getId()) {
                case R.id.content_pic:
                    url = msg.getOriginal_pic();
                    break;
                case R.id.repost_content_pic:
                    url = msg.getRetweeted_status().getOriginal_pic();
                    break;
            }
            if (!TextUtils.isEmpty(url)) {
                Intent intent = new Intent(getActivity(), BrowserBigPicActivity.class);
                intent.putExtra("url", url);
                startActivity(intent);
            }
        }


    };
}
