package com.sbbs.me.android.utils;

import java.io.File;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.rarnu.utils.DownloadUtils;
import com.rarnu.utils.ImageUtils;
import com.sbbs.me.android.api.SbbsMeSinaUser;
import com.weibo.sdk.android.Oauth2AccessToken;
import com.weibo.sdk.android.Weibo;
import com.weibo.sdk.android.WeiboAuthListener;
import com.weibo.sdk.android.WeiboDialogError;
import com.weibo.sdk.android.WeiboException;
import com.weibo.sdk.android.api.AccountAPI;
import com.weibo.sdk.android.api.UsersAPI;
import com.weibo.sdk.android.keep.AccessTokenKeeper;
import com.weibo.sdk.android.net.RequestListener;

public class SinaOAuth {

	public interface SinaUserCallback {
		void onGetSinaUser(SbbsMeSinaUser user);
	}

	private Context mContext;
	private Weibo mWeibo;
	private final String CONSUMER_KEY = "259989602";
	private final String REDIRECT_URL = "http://sbbs.me/weibo/callback";
	public Oauth2AccessToken accessToken;
	public SbbsMeSinaUser sinaUser = null;
	private SinaUserCallback callback;

	public SinaOAuth(Context context, SinaUserCallback callback) {
		this.mContext = context;
		this.callback = callback;
	}

	public void sendSinaOauth() {
		mWeibo = Weibo.getInstance(CONSUMER_KEY, REDIRECT_URL);
		mWeibo.authorize(mContext, new WeiboAuthListener() {

			@Override
			public void onWeiboException(WeiboException error) {

			}

			@Override
			public void onError(WeiboDialogError error) {

			}

			@Override
			public void onComplete(Bundle result) {
				String token = result.getString("access_token");
				String expires_in = result.getString("expires_in");
				accessToken = new Oauth2AccessToken(token, expires_in);
				Config.setAccountType(mContext, 2);
				if (accessToken.isSessionValid()) {
					AccessTokenKeeper.keepAccessToken(mContext, accessToken);
				}
				getSinaAccountInfo();
			}

			@Override
			public void onCancel() {

			}
		});
	}

	public void getSinaAccountInfo() {
		accessToken = AccessTokenKeeper.readAccessToken(mContext);
		AccountAPI api = new AccountAPI(accessToken);
		api.getUid(new RequestListener() {

			@Override
			public void onIOException(IOException error) {

			}

			@Override
			public void onError(WeiboException error) {

			}

			@Override
			public void onComplete(String result) {
				long uid = 0L;
				try {
					JSONObject json = new JSONObject(result);
					uid = json.getLong("uid");
					Config.setSinaUserId(mContext, uid);
					getSinaUserInfo(uid);
				} catch (JSONException e) {

				}

			}
		});
	}

	public void getSinaUserInfo(long uid) {
		accessToken = AccessTokenKeeper.readAccessToken(mContext);
		UsersAPI api = new UsersAPI(accessToken);
		api.show(uid, new RequestListener() {

			@Override
			public void onIOException(IOException error) {

			}

			@Override
			public void onError(WeiboException error) {

			}

			@Override
			public void onComplete(String result) {
				Log.e("getSinaUserInfo", result);
				try {
					sinaUser = SbbsMeSinaUser.fromJson(new JSONObject(result));
				} catch (Exception e) {

				}
				if (callback != null) {
					callback.onGetSinaUser(sinaUser);
				}
			}
		});
	}

	public Drawable getUserHead(String url) {
		String headLocalPath = Environment.getExternalStorageDirectory()
				.getPath() + "/.sbbs/";
		if (!new File(headLocalPath).exists()) {
			new File(headLocalPath).mkdirs();
		}
		String headLocalName = headLocalPath + "mysinahead.jpg";
		if (!new File(headLocalName).exists()) {
			DownloadUtils.downloadFile(url, headLocalName, null);
		}

		Drawable d = Drawable.createFromPath(headLocalName);
		d = ImageUtils.zoomDrawable(d, 256, 256);
		return d;
	}
}