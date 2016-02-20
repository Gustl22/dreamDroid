package net.reichholf.dreamdroid.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.reichholf.dreamdroid.DreamDroid;
import net.reichholf.dreamdroid.R;
import net.reichholf.dreamdroid.helpers.DateTime;
import net.reichholf.dreamdroid.helpers.ExtendedHashMap;
import net.reichholf.dreamdroid.helpers.NameValuePair;
import net.reichholf.dreamdroid.helpers.Python;
import net.reichholf.dreamdroid.helpers.enigma2.Event;
import net.reichholf.dreamdroid.helpers.enigma2.Picon;
import net.reichholf.dreamdroid.helpers.enigma2.Service;
import net.reichholf.dreamdroid.helpers.enigma2.URIStore;
import net.reichholf.dreamdroid.helpers.enigma2.requesthandler.AbstractListRequestHandler;
import net.reichholf.dreamdroid.helpers.enigma2.requesthandler.EpgNowNextListRequestHandler;
import net.reichholf.dreamdroid.helpers.enigma2.requesthandler.EventListRequestHandler;
import net.reichholf.dreamdroid.intents.IntentFactory;
import net.reichholf.dreamdroid.loader.AsyncListLoader;
import net.reichholf.dreamdroid.loader.LoaderResult;
import net.reichholf.dreamdroid.vlc.VLCPlayer;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by reichi on 16/02/16.
 */


public class VideoActivity extends AppCompatActivity implements IVLCVout.Callback {
	static float sOverlayAlpha = 0.85f;

	SurfaceView mVideoSurface;
	VLCPlayer mPlayer;
	VideoOverlayFragment mOverlayFragment;

	int mVideoWidth;
	int mVideoHeight;
	int mVideoVisibleWidth;
	int mVideoVisibleHeight;
	int mSarNum;
	int mSarDen;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setFullScreen();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video_player);
	}


	@Override
	protected void onResume() {
		super.onResume();
		mOverlayFragment = new VideoOverlayFragment();
		mOverlayFragment.setArguments(getIntent().getExtras());

		findViewById(R.id.overlay).setOnTouchListener(new View.OnTouchListener() {
			private static final int MAX_CLICK_DURATION = 200;
			private long startClickTime;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN: {
						startClickTime = Calendar.getInstance().getTimeInMillis();
						break;
					}
					case MotionEvent.ACTION_UP: {
						long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
						if (clickDuration < MAX_CLICK_DURATION) {
							mOverlayFragment.toggleViews();
						}
					}
				}
				return true;
			}
		});

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.replace(R.id.overlay, mOverlayFragment);
		ft.commit();

		mVideoSurface = (SurfaceView) findViewById(R.id.video_surface);
		mPlayer = new VLCPlayer();
		mPlayer.attach(mVideoSurface);
		VLCPlayer.getMediaPlayer().getVLCVout().addCallback(this);
		VLCPlayer.getMediaPlayer().setEventListener(mOverlayFragment);
		handleIntent(getIntent());
	}

	public void handleIntent(Intent intent) {
		setIntent(intent);
		if (intent.getAction() == Intent.ACTION_VIEW) {
			mPlayer.playUri(intent.getData());
		}
	}

	@Override
	protected void onPause() {
		mPlayer.detach();
		mPlayer = null;
		mVideoSurface = null;
		VLCPlayer.getMediaPlayer().getVLCVout().removeCallback(this);
		VLCPlayer.getMediaPlayer().setEventListener(null);

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.remove(mOverlayFragment);
		ft.commit();
		super.onPause();
	}

	protected void setupVideoSurface() {
		int surfaceWidth;
		int surfaceHeight;

		surfaceWidth = getWindow().getDecorView().getWidth();
		surfaceHeight = getWindow().getDecorView().getHeight();
		mPlayer.setWindowSize(surfaceWidth, surfaceHeight);

		if (mSarDen == mSarNum) {
			mSarDen = 1;
			mSarNum = 1;
		}

		double videoAspect, videoWith, displayAspect, displayWidth, displayHeight;
		displayWidth = surfaceWidth;
		displayHeight = surfaceHeight;

		videoWith = mVideoVisibleWidth * (double) mSarNum / mSarDen;
		videoAspect = videoWith / (double) mVideoVisibleHeight;
		displayAspect = displayWidth / displayHeight;

		if (displayAspect < videoAspect)
			displayHeight = displayWidth / videoAspect;
		else
			displayWidth = displayHeight * videoAspect;
		ViewGroup.LayoutParams layoutParams = mVideoSurface.getLayoutParams();
		layoutParams.width = (int) Math.floor(displayWidth * mVideoWidth / mVideoVisibleWidth);
		layoutParams.height = (int) Math.floor(displayHeight * mVideoHeight / mVideoVisibleHeight);
		mVideoSurface.setLayoutParams(layoutParams);
		mVideoSurface.invalidate();
	}

	public void setFullScreen() {
		int visibility = 0;
		visibility |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			visibility |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
			visibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		getWindow().getDecorView().setSystemUiVisibility(visibility);
	}

	@Override
	public void onNewLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
		mVideoWidth = width;
		mVideoHeight = height;
		mVideoVisibleWidth = visibleWidth;
		mVideoVisibleHeight = visibleHeight;
		mSarNum = sarNum;
		mSarDen = sarDen;

		setupVideoSurface();
	}

	@Override
	public void onSurfacesCreated(IVLCVout vlcVout) {
	}

	@Override
	public void onSurfacesDestroyed(IVLCVout vlcVout) {
	}

	public class VideoOverlayFragment extends Fragment implements MediaPlayer.EventListener,
			LoaderManager.LoaderCallbacks<LoaderResult<ArrayList<ExtendedHashMap>>> {
		private static final String TAG = "VideoOverlayFragment";
		private final int[] sOverlayViews = {R.id.service_detail_root};
		private final int[] sZapOverlayViews = {R.id.previous, R.id.next};

		public final String TITLE = "title";
		public final String SERVICE_INFO = "serviceInfo";
		public final String BOUQUET_REFERENCE = "bouquetRef";
		public final String SERVICE_REFERENCE = "serviceRef";
		protected String mServiceName;
		protected String mServiceRef;
		protected String mBouquetRef;

		protected ArrayList<ExtendedHashMap> mServiceList;
		protected ExtendedHashMap mServiceInfo;


		protected Handler mHandler;
		protected Runnable mAutoHideRunnable;
		protected Runnable mIssueReloadRunnable;

		@Override
		public void onCreate(@Nullable Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			mServiceName = getArguments().getString(TITLE);
			mServiceRef = getArguments().getString(SERVICE_REFERENCE);
			mBouquetRef = getArguments().getString(BOUQUET_REFERENCE);
			mServiceList = new ArrayList<>();
			HashMap<String, Object> serviceInfo = (HashMap<String, Object>) getArguments().get(SERVICE_INFO);
			if (serviceInfo != null)
				mServiceInfo = new ExtendedHashMap(serviceInfo);
			else
				mServiceInfo = null;
			mHandler = new Handler();
			mAutoHideRunnable = new Runnable() {
				@Override
				public void run() {
					hideOverlays();
				}
			};
			mIssueReloadRunnable = new Runnable() {
				@Override
				public void run() {
					reload();
				}
			};
			autohide();
			reload();
		}

		@Nullable
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.video_player_overlay, container, false);
			Button btnPrevious = (Button) view.findViewById(R.id.previous);
			btnPrevious.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					previous();
				}
			});
			Button btnNext = (Button) view.findViewById(R.id.next);
			btnNext.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					next();
				}
			});
			return view;
		}

		@Override
		public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			serviceInfoChanged();
		}

		@Override
		public Loader<LoaderResult<ArrayList<ExtendedHashMap>>> onCreateLoader(int id, Bundle args) {
			AbstractListRequestHandler handler;
			if (DreamDroid.featureNowNext())
				handler = new EpgNowNextListRequestHandler();
			else
				handler = new EventListRequestHandler(URIStore.EPG_NOW);
			return new AsyncListLoader(getActivity(), handler, true, args);
		}

		@Override
		public void onLoadFinished(Loader<LoaderResult<ArrayList<ExtendedHashMap>>> loader, LoaderResult<ArrayList<ExtendedHashMap>> data) {
			if (data.isError())
				return;
			mServiceList.clear();
			mServiceList.addAll(data.getResult());
			for (ExtendedHashMap service : mServiceList) {
				if (service.getString(Event.KEY_SERVICE_REFERENCE).equals(mServiceRef))
					mServiceInfo = service;
			}
			serviceInfoChanged();
		}

		@Override
		public void onLoaderReset(Loader<LoaderResult<ArrayList<ExtendedHashMap>>> loader) {

		}

		private void previous() {
			int index = getCurrentServiceIndex();
			if (index < 0)
				return;
			if (index == 0)
				index = mServiceList.size() - 1;
			else
				index--;
			mServiceInfo = mServiceList.get(index);
			mServiceRef = mServiceInfo.getString(Event.KEY_SERVICE_REFERENCE);
			mServiceName = mServiceInfo.getString(Event.KEY_SERVICE_NAME);
			if (Service.isMarker(mServiceRef)) {
				previous();
			} else {
				zap();
				serviceInfoChanged();
			}
		}

		private void zap() {
			Intent streamingIntent = IntentFactory.getStreamServiceIntent(getActivity(), mServiceRef, mServiceName, mBouquetRef, mServiceInfo);
			getArguments().putString(TITLE, mServiceRef);
			getArguments().getString(SERVICE_REFERENCE, mServiceRef);
			getArguments().getString(BOUQUET_REFERENCE, mBouquetRef);
			getArguments().putSerializable(SERVICE_INFO, mServiceInfo);
			((VideoActivity) getActivity()).handleIntent(streamingIntent);
		}

		private void next() {
			int index = getCurrentServiceIndex();
			if(index < 0)
				return;
			if(index >= mServiceList.size())
				index = 0;
			else
				index++;
			mServiceInfo = mServiceList.get(index);
			mServiceRef = mServiceInfo.getString(Event.KEY_SERVICE_REFERENCE);
			mServiceName = mServiceInfo.getString(Event.KEY_SERVICE_NAME);
			if(Service.isMarker(mServiceRef)) {
				next();
			} else {
				zap();
				serviceInfoChanged();
			}
		}

		private int getCurrentServiceIndex() {
			if(mServiceList == null || mServiceList.isEmpty())
				return -1;
			int idx = 0;
			for(ExtendedHashMap service : mServiceList) {
				if(service.getString(Event.KEY_SERVICE_REFERENCE).equals(mServiceRef))
					return idx;
				idx++;
			}
			return -1;
		}

		private void serviceInfoChanged() {
			Log.w(TAG, "service info changed!");
			showOverlays();
			if (mServiceInfo == null)
				return;
			long eventStart = Double.valueOf(mServiceInfo.getString(Event.KEY_EVENT_START)).longValue() * 1000;
			long eventEnd = eventStart + (Double.valueOf(mServiceInfo.getString(Event.KEY_EVENT_DURATION)).longValue() * 1000);
			long now = new Date().getTime();
			long updateAt = SystemClock.uptimeMillis() + eventEnd - now;

			mHandler.removeCallbacks(mIssueReloadRunnable);
			mHandler.postAtTime(mIssueReloadRunnable, +updateAt);
		}

		public void reload() {
			if ((mBouquetRef == null || mBouquetRef.isEmpty()) || getActivity() == null)
				return;
			ArrayList<NameValuePair> params = new ArrayList<>();
			params.add(new NameValuePair("bRef", mBouquetRef));
			Bundle args = new Bundle();
			args.putSerializable("params", params);
			getLoaderManager().restartLoader(1, args, this);
		}

		private void updateViews() {
			View view = getView();
			TextView serviceName = (TextView) view.findViewById(R.id.service_name);
			serviceName.setText(mServiceName);

			View parentNow = view.findViewById(R.id.event_now);
			View parentNext = view.findViewById(R.id.event_next);
			ProgressBar serviceProgress = (ProgressBar) view.findViewById(R.id.service_progress);

			if (mServiceInfo != null) {
				ImageView picon = (ImageView) view.findViewById(R.id.picon);
				Picon.setPiconForView(getActivity(), picon, mServiceInfo);

				TextView nowStart = (TextView) view.findViewById(R.id.event_now_start);
				TextView nowDuration = (TextView) view.findViewById(R.id.event_now_duration);
				TextView nowTitle = (TextView) view.findViewById(R.id.event_now_title);

				Event.supplementReadables(mServiceInfo); //update readable values

				nowStart.setText(mServiceInfo.getString(Event.KEY_EVENT_START_TIME_READABLE));
				nowTitle.setText(mServiceInfo.getString(Event.KEY_EVENT_TITLE));
				nowDuration.setText(mServiceInfo.getString(Event.KEY_EVENT_DURATION_READABLE));

				long max = -1;
				long cur = -1;
				String duration = mServiceInfo.getString(Event.KEY_EVENT_DURATION);
				String start = mServiceInfo.getString(Event.KEY_EVENT_START);

				if (duration != null && start != null && !Python.NONE.equals(duration) && !Python.NONE.equals(start)) {
					try {
						max = Double.valueOf(duration).longValue() / 60;
						cur = max - DateTime.getRemaining(duration, start);
					} catch (Exception e) {
						Log.e(DreamDroid.LOG_TAG, e.toString());
					}
				}

				serviceProgress.setVisibility(View.VISIBLE);
				if (max > 0 && cur >= 0) {
					serviceProgress.setMax((int) max);
					serviceProgress.setProgress((int) cur);
				}

				parentNow.setVisibility(View.VISIBLE);

				String next = mServiceInfo.getString(Event.PREFIX_NEXT.concat(Event.KEY_EVENT_TITLE));
				boolean hasNext = next != null && !"".equals(next);
				if (hasNext) {
					TextView nextStart = (TextView) view.findViewById(R.id.event_next_start);
					TextView nextDuration = (TextView) view.findViewById(R.id.event_next_duration);
					TextView nextTitle = (TextView) view.findViewById(R.id.event_next_title);

					nextStart.setText(mServiceInfo.getString(Event.PREFIX_NEXT.concat(Event.KEY_EVENT_START_TIME_READABLE)));
					nextTitle.setText(mServiceInfo.getString(Event.PREFIX_NEXT.concat(Event.KEY_EVENT_TITLE)));
					nextDuration.setText(mServiceInfo.getString(Event.PREFIX_NEXT.concat(Event.KEY_EVENT_DURATION_READABLE)));
					parentNext.setVisibility(View.VISIBLE);
				} else {
					parentNext.setVisibility(View.GONE);
				}
			} else {
				serviceProgress.setVisibility(View.GONE);
				parentNow.setVisibility(View.GONE);
				parentNext.setVisibility(View.GONE);
			}
		}

		@Override
		public void onResume() {
			super.onResume();


			showOverlays();
			reload();
		}

		@Override
		public void onPause() {
			mHandler.removeCallbacks(mAutoHideRunnable);
			mHandler.removeCallbacks(mIssueReloadRunnable);
			super.onPause();
		}

		public void autohide() {
			mHandler.postDelayed(mAutoHideRunnable, 10000);
		}

		public void showOverlays() {
			updateViews();
			for (int id : sOverlayViews)
				fadeInView(getView().findViewById(id));
			showZapOverlays();
			autohide();
		}

		public void hideOverlays() {
			mHandler.removeCallbacks(mAutoHideRunnable);
			for (int id : sOverlayViews)
				fadeOutView(getView().findViewById(id));
			hideZapOverlays();
		}

		private void showZapOverlays() {
			if(mServiceList == null || mServiceList.isEmpty()) {
				hideZapOverlays();
				return;
			}
			for(int id : sZapOverlayViews)
				fadeInView(getView().findViewById(id));
		}

		private void hideZapOverlays() {
			for(int id : sZapOverlayViews)
				fadeOutView(getView().findViewById(id));
		}

		private void fadeInView(final View v) {
			if (v.getVisibility() == View.VISIBLE)
				return;
			v.setVisibility(View.VISIBLE);
			v.setAlpha(0.0f);
			v.animate().alpha(sOverlayAlpha).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					v.setAlpha(sOverlayAlpha);
				}
			});
		}

		private void fadeOutView(final View v) {
			if (v.getVisibility() == View.GONE)
				return;
			v.animate().alpha(0.0f).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					v.setVisibility(View.GONE);
				}
			});
		}

		public void toggleViews() {
			View sdroot = getView().findViewById(R.id.service_detail_root);
			if (sdroot.getVisibility() == View.VISIBLE)
				hideOverlays();
			else
				showOverlays();
		}

		@Override
		public void onEvent(MediaPlayer.Event event) {
			switch (event.type) {
				case MediaPlayer.Event.Opening: {
					View progressView = getView().findViewById(R.id.video_load_progress);
					fadeInView(progressView);
					break;
				}
				case MediaPlayer.Event.Playing: {
					View progressView = getView().findViewById(R.id.video_load_progress);
					fadeOutView(progressView);
					break;
				}
				case MediaPlayer.Event.EncounteredError:
					Toast.makeText(getActivity(), R.string.playback_failed, Toast.LENGTH_LONG).show();
					break;
				case MediaPlayer.Event.EndReached:
				case MediaPlayer.Event.Stopped:
					getActivity().finish();
				default:
					break;
			}
		}
	}
}