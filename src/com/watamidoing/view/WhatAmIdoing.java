package com.watamidoing.view;

import java.util.LinkedList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.NativeCameraView;
import org.opencv.android.OpenCVLoader;
import org.scribe.model.Token;

import twitter4j.auth.AccessToken;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Messenger;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Session;
import com.waid.R;
import com.watamidoing.Login;
import com.watamidoing.accepted.GetInvitedTask;
import com.watamidoing.chat.ChatDialogFragment;
import com.watamidoing.chat.xmpp.receivers.ChatEnabledReceiver;
import com.watamidoing.chat.xmpp.receivers.ChatMessageReceiver;
import com.watamidoing.chat.xmpp.receivers.ChatParticipantReceiver;
import com.watamidoing.chat.xmpp.receivers.XMPPServiceStoppedReceiver;
import com.watamidoing.chat.xmpp.service.Participant;
import com.watamidoing.chat.xmpp.service.XMPPService;
import com.watamidoing.chat.xmpp.service.XMPPServiceConnection;
import com.watamidoing.contentproviders.Authentication;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.contentproviders.LinkedInAuthenticationToken;
import com.watamidoing.contentproviders.TwitterAuthenticationToken;
import com.watamidoing.invite.email.InviteListTask;
import com.watamidoing.invite.facebook.FaceBookInviteDialogFragment;
import com.watamidoing.invite.facebook.FacebookPostTask;
import com.watamidoing.invite.linkedin.LinkedInAuthorization;
import com.watamidoing.invite.linkedin.SendLinkedInInviteTask;
import com.watamidoing.invite.twitter.SendTwitterInviteTask;
import com.watamidoing.invite.twitter.TwitterAuthorization;
import com.watamidoing.reeiver.callbacks.TotalWatchersController;
import com.watamidoing.reeiver.callbacks.XMPPConnectionController;
import com.watamidoing.reeiver.callbacks.ZeroMQController;
import com.watamidoing.tasks.WAIDLocationListener;
import com.watamidoing.total.receivers.TotalWatchersReceiver;
import com.watamidoing.total.service.TotalUsersWatchingTask;
import com.watamidoing.total.service.TotalWatchersService;
import com.watamidoing.transport.receivers.NetworkChangeReceiver;
import com.watamidoing.transport.receivers.ZeroMQServiceStoppedReceiver;
import com.watamidoing.transport.service.ZeroMQService;
import com.watamidoing.transport.service.ZeroMQServiceConnection;
import com.watamidoing.utils.Emoji;
import com.watamidoing.utils.ScreenDimension;
import com.watamidoing.utils.UtilsWhatAmIdoing;

public class WhatAmIdoing extends FragmentActivity implements ZeroMQController,
		XMPPConnectionController, TotalWatchersController {

	public static final Uri FRIEND_PICKER = Uri.parse("picker://friend");

	/**
	 * 
	 */
	private static final String TAG = "WhatAmIdoing";
	private static final long serialVersionUID = 8274547292305156402L;
	private static final String START_CAMERA = "Start Camera";
	private static final String STOP_CAMERA = "Stop Camera";
	private static final String STOP_SHARING = "Stop Sharing";
	private static final String START_SHARING = "Start Sharing";
	
	private static final String VIDEO_START_STATE = "waid-video-start-state";

	private static final String VIDEO_SHARING_STATE = "waid-video-sharing-state";
	private InviteListTask mInviteListTask;

	private String startSharingState = START_SHARING;

	private boolean videoStart = false;
	private boolean videoSharing = false;
	private int imageWidth = 320;
	private int imageHeight = 240;
	private NativeCameraView cameraView;
	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection zeroMQServiceConnection;
	protected ZeroMQServiceStoppedReceiver serviceStoppedReceiver;
	protected NetworkChangeReceiver networkChangeReceiver;

	protected boolean sharing = false;
	private int cameraId;
	private LinearLayout mainLayout;
	private static volatile WhatAmIdoing activity;

	/** Messenger for communicating with service. */
	private Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;

	public Camera camera;
	protected GetInvitedTask mInvitedTaskListView;

	private LocationManager locationManager;
	private WAIDLocationListener waidLocationListener;
	private Session facebookSession;
	protected FaceBookInviteDialogFragment inviteDialogFragment;
	public boolean callingFromResume = false;

	protected TotalUsersWatchingTask totalUsersWatchingTask;

	private ChatDialogFragment chatDialogFragment;

	private XMPPServiceConnection xmppServiceConnection;

	private boolean chatServiceIsBound;

	private ChatMessageReceiver chatMessageReceiver;

	private IntentFilter chatMessageReceiverFilter;

	private IntentFilter chatEnabledReceiverFilter;

	private ChatEnabledReceiver chatEnabledReceiver;

	private LinkedList<String> chatMessageQueue = new LinkedList<String>();

	private LinkedList<Participant> chatParticipantQueue = new LinkedList<Participant>();

	private IntentFilter chatParticipantReceiverFilter;

	private ChatParticipantReceiver chatParticipantReceiver;

	protected TotalWatchersReceiver totalWatchersReceiver;

	private XMPPServiceStoppedReceiver xmppServiceStoppedReceiver;

	public Size previewSize;

	private int previewWindowHeight;

	private MenuItem sendLocationMenuItem;

	private MenuItem whosAcceptedMenuItem;

	public boolean cameraHasBeenStarted;

	private OpenCvCameraListener opencvCameraListener;

	private BaseLoaderCallback mLoaderCallback;

	static {
		if (!OpenCVLoader.initDebug()) {
			Log.i(TAG, "-------------------- UNABLE TO OPEN -- OPEN CV");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		/*
		 * WifiManager wifiManager =
		 * (WifiManager)this.getSystemService(Context.WIFI_SERVICE);
		 * Log.i(TAG,"THIS IS THE LINK SPEED:"
		 * +wifiManager.getConnectionInfo().getLinkSpeed());
		 */
		if (savedInstanceState != null) {
			final Boolean vidStart = savedInstanceState
					.getBoolean(VIDEO_START_STATE);
			if (vidStart != null) {
				videoStart = vidStart;
			}
			final Boolean vidSharing = savedInstanceState
					.getBoolean(VIDEO_SHARING_STATE);
			if (vidSharing != null) {
				videoSharing = vidSharing;
			}
		}
		super.onCreate(savedInstanceState);

		if (android.os.Build.VERSION.SDK_INT > 8) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		zeroMQServiceConnection = new ZeroMQServiceConnection(this, this);
		xmppServiceConnection = new XMPPServiceConnection(this, this);
		setContentView(R.layout.options);
		activity = this;

		// Setting up camera selection
		int cameraCount = 0;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		cameraCount = Camera.getNumberOfCameras();
		boolean foundFront = false;
		boolean foundBack = true;
		int frontCameraId = -1;
		int backCameraId = -1;
		for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				foundFront = true;
				frontCameraId = camIdx;
			} else {
				foundBack = true;
				backCameraId = camIdx;

			}
		}

		if (foundFront && foundBack) {
			cameraId = frontCameraId;
		} else if (foundFront && !foundBack) {
			cameraId = frontCameraId;
		} else {
			cameraId = backCameraId;
		}

		mainLayout = (LinearLayout) this.findViewById(R.id.momemts_frame);
		ScreenDimension dimension = UtilsWhatAmIdoing
				.getScreenDimensions(activity);
		ViewGroup.LayoutParams params = mainLayout.getLayoutParams();

		cameraView = (NativeCameraView) mainLayout
				.findViewById(R.id.opencvCameraView);
		cameraView.enableFpsMeter();
		opencvCameraListener = new OpenCvCameraListener();
		opencvCameraListener.setCameraView(cameraView);
		cameraView.setCvCameraViewListener(opencvCameraListener);
		cameraView.setVisibility(SurfaceView.VISIBLE);

		// change width of the params e.g. 50dp
		params.width = Double.valueOf(dimension.getDpHeightPixels() * 0.8)
				.intValue();
		params.height = Double.valueOf(dimension.getDpWidthPixels() * 0.5)
				.intValue();
		GridLayout gl = (GridLayout) this.findViewById(R.id.video_display);
		gl.requestLayout();

		mLoaderCallback = new BaseLoaderCallback(this) {
			@Override
			public void onManagerConnected(int status) {
				switch (status) {
				case LoaderCallbackInterface.SUCCESS: {
					Log.i(TAG, "OpenCV loaded successfully");
					// cameraView.enableView();
				}
					break;
				default: {
					super.onManagerConnected(status);
				}
					break;
				}
			}
		};
		cameraView.disableView();
		cameraView.disableFpsMeter();

	}

	public void switchCamera(View view) {

		if (Camera.getNumberOfCameras() > 1) {
			if (cameraId == 0) {
				cameraId = 1;
			} else {
				cameraId = 0;
			}
			if (videoStart) {
				camera.stopPreview();
				mainLayout.removeAllViews();
				cameraView = null;
				camera = null;
				startCamera();
			}
		}

	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Always call the superclass so it can restore the view hierarchy
		super.onRestoreInstanceState(savedInstanceState);

		// Restore state members from saved instance
		videoStart = savedInstanceState.getBoolean(VIDEO_START_STATE);
		videoSharing = savedInstanceState.getBoolean(VIDEO_SHARING_STATE);
		Log.i(TAG, "onRestoreInstanceState camera[" + videoStart
				+ "] videoSharing[" + videoSharing + "]");
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.i(TAG, "onConfigurationChanged camera[" + videoStart
				+ "] videoSharing[" + videoSharing + "]");

	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {

		Log.i(TAG, "onSaveInstanceState camera[" + videoStart
				+ "] videoSharing[" + videoSharing + "]");
		savedInstanceState.putBoolean(VIDEO_START_STATE, videoStart);
		savedInstanceState.putBoolean(VIDEO_SHARING_STATE, videoSharing);

		if (videoStart) {

			stopVideo(true);

			if (locationManager != null) {
				locationManager.removeUpdates(waidLocationListener);
				locationManager = null;
			}
			stopChatService();
		}

		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

	public void startVideo() {

		runOnUiThread(new Thread(new Runnable() {
			public void run() {

				// String text = startVideo.getText().toString();

				final ImageButton transmissionButton = (ImageButton) activity
						.findViewById(R.id.start_transmission);

				// if (START_CAMERA.equalsIgnoreCase(text)) {
				if (!videoStart) {
					transmissionButton.setEnabled(true);

					startCamera();
					videoStart = true;

				} else {

					stopVideo(false);

					// camera.release();

					// NativeMethods.closeWebsocketConnection();
					// transmissionButton.setText(START_SHARING);
					// initializeButtons();
				}

			}
		}));

	}

	private void stopVideo(boolean wasVideoStarted) {

		ImageButton startVideo = (ImageButton) activity
				.findViewById(R.id.start_video);

		cameraView.disableView();

		/*
		 * if (camera != null) { camera.stopPreview(); }
		 * 
		 * if (mainLayout != null) { mainLayout.removeAllViews(); }
		 */
		// startVideo.setText(START_CAMERA);

		if (startVideo != null) {
			startVideo.setImageResource(R.drawable.camera);
		}
		// cameraView = null;
		videoStart = wasVideoStarted;
		// camera = null;
	}

	public void whoHasAccepted(View view) {
		whoHasAccepted();
	}

	private void whoHasAccepted() {
		if (videoStart) {
			mInvitedTaskListView = new GetInvitedTask(activity);
			mInvitedTaskListView.execute((Void) null);
		} else {
			String message = activity
					.getString(R.string.can_not_see_whoes_watching);
			UtilsWhatAmIdoing.displayGenericToast(activity, message);
		}

	}

	public void shareLocation(View view) {
		shareLocation();
	}

	private void shareLocation() {

		if (videoSharing && videoStart) {

			runOnUiThread(new Thread(new Runnable() {
				public void run() {
					if (locationManager == null)
						locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

					if (waidLocationListener == null)
						waidLocationListener = new WAIDLocationListener(
								activity);

					List<String> providers = locationManager.getProviders(true);
					for (String provider : providers) {
						locationManager.requestLocationUpdates(provider, 1000L,
								0.0f, new LocationListener() {
									public void onLocationChanged(
											Location location) {
									}

									public void onProviderDisabled(
											String provider) {
									}

									public void onProviderEnabled(
											String provider) {
									}

									public void onStatusChanged(
											String provider, int status,
											Bundle extras) {
									}
								});
					}

					Criteria criteria = new Criteria();
					String provider = locationManager.getBestProvider(criteria,
							false);
					Location location = locationManager
							.getLastKnownLocation(provider);

					if (location == null) {
						locationManager.getLastKnownLocation("network");

					}

					if (location == null) {
						locationManager.getLastKnownLocation("network");
					}
					if (location != null) {
						waidLocationListener.sendLocation(location);
						Log.i("WhatAmIdoing.onClick", "IS NOT NULL");
					} else {
						Log.i("WhatAmIdoing.onClick", "IS NULL");
						Toast.makeText(getApplicationContext(),
								"Location Currently Not Available",
								Toast.LENGTH_SHORT).show();
					}
					// locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
					// 0, 0, waidLocationListener);
					// GPS location updates.
					// locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
					// 0, 0, waidLocationListener);
				}
			}));
		} else {
			if (locationManager != null) {
				locationManager.removeUpdates(waidLocationListener);
				locationManager = null;
			}
			String message = activity
					.getString(R.string.can_not_share_location);
			UtilsWhatAmIdoing.displayGenericToast(activity, message);
		}

	}

	/**
	 * Used to start video
	 */
	public void startVideo(View view) {

		startVideo();
	}

	public void restartTransmission() {
		runOnUiThread(new Thread(new Runnable() {
			private IntentFilter filterServiceStoppedReceiver;
			private IntentFilter filterTotalWatchersReceiver;

			public void run() {

				ImageButton startTransmission = (ImageButton) activity
						.findViewById(R.id.start_transmission);

				Intent msgIntent = new Intent(activity,
						com.watamidoing.transport.service.ZeroMQService.class);

				Intent totalWatchersIntent = new Intent(
						activity,
						com.watamidoing.total.service.TotalWatchersService.class);

				if (!isServiceRunning(TotalWatchersService.class.getName())) {
					startService(totalWatchersIntent);
				}

				if (totalWatchersReceiver == null) {
					filterTotalWatchersReceiver = new IntentFilter(
							TotalWatchersReceiver.TOTAL_WATCHERS_RECEIVER);
					filterTotalWatchersReceiver
							.addCategory(Intent.CATEGORY_DEFAULT);
					totalWatchersReceiver = new TotalWatchersReceiver(activity);
					registerReceiver(totalWatchersReceiver,
							filterTotalWatchersReceiver);
				}

				if (serviceStoppedReceiver == null) {
					filterServiceStoppedReceiver = new IntentFilter(
							ZeroMQServiceStoppedReceiver.SERVICE_STOPED);
					filterServiceStoppedReceiver
							.addCategory(Intent.CATEGORY_DEFAULT);
					serviceStoppedReceiver = new ZeroMQServiceStoppedReceiver(
							activity);
					try {
						unregisterReceiver(serviceStoppedReceiver);
					} catch (Exception e) {

					}
					registerReceiver(serviceStoppedReceiver,
							filterServiceStoppedReceiver);
				}

				doBindService();

			}
		}));

	}

	/**
	 * Used to start video transmission
	 */
	public void startTransmission(final View view) {

		runOnUiThread(new Thread(new Runnable() {

			private IntentFilter filterServiceStoppedReceiver;
			private IntentFilter filterRegisterReceiver;
			private IntentFilter filterTotalWatchersReceiver;

			public void run() {

				ImageButton startTransmission = (ImageButton) activity
						.findViewById(R.id.start_transmission);

				Intent msgIntent = new Intent(activity,
						com.watamidoing.transport.service.ZeroMQService.class);

				Intent totalWatchersIntent = new Intent(
						activity,
						com.watamidoing.total.service.TotalWatchersService.class);

				// UtilsWhatAmIdoing.displayGenericToast(activity,
				// "start transmission clicked"+videoSharing);
				if (!videoSharing) {

					if (videoStart) {
						cameraHasBeenStarted = true;
						stopVideo(false);
					} else {
						cameraHasBeenStarted = false;
					}
					if (isServiceRunning(ZeroMQService.class.getName())) {
						StopReceivers stopReceivers = new StopReceivers(
								activity, false);
						stopReceivers.run();

					}
					ComponentName comp = startService(msgIntent);

					if (isServiceRunning(TotalWatchersService.class.getName())) {
						StopReceivers stopReceivers = new StopReceivers(
								activity, false);
						stopReceivers.run();

					}

					startService(totalWatchersIntent);

					if (totalWatchersReceiver == null) {
						filterTotalWatchersReceiver = new IntentFilter(
								TotalWatchersReceiver.TOTAL_WATCHERS_RECEIVER);
						filterTotalWatchersReceiver
								.addCategory(Intent.CATEGORY_DEFAULT);
						totalWatchersReceiver = new TotalWatchersReceiver(
								activity);
						registerReceiver(totalWatchersReceiver,
								filterTotalWatchersReceiver);
					}

					if (serviceStoppedReceiver == null) {
						filterServiceStoppedReceiver = new IntentFilter(
								ZeroMQServiceStoppedReceiver.SERVICE_STOPED);
						filterServiceStoppedReceiver
								.addCategory(Intent.CATEGORY_DEFAULT);
						serviceStoppedReceiver = new ZeroMQServiceStoppedReceiver(
								activity);
						registerReceiver(serviceStoppedReceiver,
								filterServiceStoppedReceiver);
					}

					// networkChangeReceiver = new
					// NetworkChangeReceiver(activity);

					if (filterRegisterReceiver == null) {
						filterRegisterReceiver = new IntentFilter(
								"android.net.conn.CONNECTIVITY_CHANGE");
						filterRegisterReceiver
								.addCategory(Intent.CATEGORY_DEFAULT);
						registerReceiver(networkChangeReceiver,
								filterRegisterReceiver);
					}

					/*
					 * startWebSocketTask = new StartsWebSocketTask(
					 * webSocketController, activity);
					 * startWebSocketTask.execute((Void) null);
					 */
					// startTransmission.setText(STOP_SHARING);
					
					if (doBindService()) {
						startTransmission.setEnabled(false);
						startTransmission.setImageResource(R.drawable.share_red);	
					} else {
						StopReceivers stopReceivers = new StopReceivers(activity,
								false);
						stopReceivers.run();
						videoSharing = false;
						startSharingState = START_SHARING;
						startTransmission.setEnabled(true);
						startTransmission.setImageResource(R.drawable.share_blue);
						UtilsWhatAmIdoing.displayGenericToast(activity,"Problems binding to service");
					}
				} else {
					StopReceivers stopReceivers = new StopReceivers(activity,
							false);
					stopReceivers.run();
					videoSharing = false;
					startSharingState = START_SHARING;
					startTransmission.setEnabled(true);
					startTransmission.setImageResource(R.drawable.share_blue);
				}

			}
		}));

	}

	public void setCameraDisplayOrientation(Activity activity, int cameraId,
			android.hardware.Camera camera) {
		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(result);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width1 = size.x;
		int height1 = size.y;

		Log.i(TAG, "width[" + width1 + "] height[" + height1 + "]");
		LinearLayout holder = (LinearLayout) this
				.findViewById(R.id.main_holder);
		GridLayout gl = (GridLayout) this.findViewById(R.id.video_display);
		android.view.ViewGroup.LayoutParams glLayoutParams = gl
				.getLayoutParams();
		android.view.ViewGroup.LayoutParams hLayoutParams = holder
				.getLayoutParams();
		LinearLayout momentsLayout = (LinearLayout) this
				.findViewById(R.id.momemts_frame);
		previewWindowHeight = momentsLayout.getLayoutParams().height;
		android.view.ViewGroup.LayoutParams mLayoutParam = momentsLayout
				.getLayoutParams();

		boolean orientationIsPortrait = false;
		if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			hLayoutParams.height = (int) (height1 * 0.9);
			glLayoutParams.height = (int) (height1 * 0.88);
			mLayoutParam.height = (int) (height1 * 0.6);
			orientationIsPortrait = true;
		} else {
			hLayoutParams.width = (int) (width1 * 0.9);
			// glLayoutParams.width = (int)(width1 * 0.);
			mLayoutParam.width = (int) (width1 * 0.75);

			int paddingLeft = (int) (width1 * 0.1);
			int paddingRight = gl.getPaddingRight();
			int paddingBottom = gl.getPaddingBottom();
			int paddingTop = gl.getPaddingTop();

			gl.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

		}

		gl.setLayoutParams(glLayoutParams);
		holder.setLayoutParams(hLayoutParams);
		momentsLayout.setLayoutParams(mLayoutParam);

		LinearLayout optionsFrame = (LinearLayout) this
				.findViewById(R.id.options_frame);
		android.view.ViewGroup.LayoutParams optionsFrameLayout = optionsFrame
				.getLayoutParams();
		optionsFrameLayout.width = momentsLayout.getLayoutParams().width;
		optionsFrame.setLayoutParams(optionsFrameLayout);
		int localWidth = optionsFrameLayout.width / 3;

		final ImageButton startTransmissionButton = (ImageButton) activity
				.findViewById(R.id.start_transmission);
		android.view.ViewGroup.LayoutParams stLayoutParams = startTransmissionButton
				.getLayoutParams();
		stLayoutParams.width = localWidth;
		startTransmissionButton.setLayoutParams(stLayoutParams);

		// startTransmissionButton.setWidth(localWidth);

		final ImageButton startVideo = (ImageButton) activity
				.findViewById(R.id.start_video);
		android.view.ViewGroup.LayoutParams svLayoutParams = startVideo
				.getLayoutParams();
		svLayoutParams.width = localWidth;
		startVideo.setLayoutParams(svLayoutParams);
		// startVideo.setWidth(localWidth);

		final ImageButton chatButton = (ImageButton) activity
				.findViewById(R.id.send_messge);
		android.view.ViewGroup.LayoutParams cbLayoutParams = chatButton
				.getLayoutParams();
		cbLayoutParams.width = localWidth;
		chatButton.setLayoutParams(cbLayoutParams);

		final TextView totalWatchers = (TextView) activity
				.findViewById(R.id.totalWatchers);
		int paddingLeftTotalWatchers = totalWatchers.getPaddingLeft();
		int paddingRightTotalWatchers = totalWatchers.getPaddingRight();
		int paddingBottomTotalWatchers = totalWatchers.getPaddingBottom();
		int paddingTopTotalWatchers = totalWatchers.getPaddingTop();
		paddingRightTotalWatchers = (int) (width1 * 0.16);
		totalWatchers.setPadding(paddingLeftTotalWatchers,
				paddingTopTotalWatchers, paddingRightTotalWatchers,
				paddingBottomTotalWatchers);

		final ImageButton shareLocation = (ImageButton) activity
				.findViewById(R.id.shareLocation);
		int paddingLeftShareLocation = shareLocation.getPaddingLeft();
		int paddingRightShareLocation = shareLocation.getPaddingRight();
		int paddingBottomShareLocation = shareLocation.getPaddingBottom();
		int paddingTopShareLocation = shareLocation.getPaddingTop();
		paddingRightShareLocation = (int) (width1 * 0.16);
		shareLocation.setPadding(paddingLeftShareLocation,
				paddingTopShareLocation, paddingRightShareLocation,
				paddingBottomShareLocation);

		final ImageButton viewSharers = (ImageButton) activity
				.findViewById(R.id.viewSharers);
		int paddingLeftViewSharers = viewSharers.getPaddingLeft();
		int paddingRightViewSharers = viewSharers.getPaddingRight();
		int paddingBottomViewSharers = viewSharers.getPaddingBottom();
		int paddingTopViewSharers = viewSharers.getPaddingTop();
		paddingRightViewSharers = (int) (width1 * 0.16);
		viewSharers.setPadding(paddingLeftViewSharers, paddingTopViewSharers,
				paddingRightViewSharers, paddingBottomViewSharers);
		/*
		 * final ImageButton selectCamera =
		 * (ImageButton)activity.findViewById(R.id.selectCamera); int
		 * paddingLeftCamera = selectCamera.getPaddingLeft(); int
		 * paddingRightCamera = selectCamera.getPaddingRight(); int
		 * paddingBottomCamera = selectCamera.getPaddingBottom(); int
		 * paddingTopCamera = selectCamera.getPaddingTop(); paddingRightCamera =
		 * (int)(width1 * 0.25); selectCamera.setPadding(paddingLeftCamera,
		 * paddingTopCamera, paddingRightCamera, paddingBottomCamera);
		 */

		gl.requestLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		cameraView.disableView();
		Log.i(TAG, "Paused called");
	}

	@Override
	public void onResume() {
		super.onResume();

		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this,
				mLoaderCallback);
		Log.i(TAG, "Resume called serviceRunning =["
				+ isServiceRunning(ZeroMQService.class.getName())
				+ "] and share=[" + videoSharing + "] camera=[" + videoStart
				+ "]");

		if (isServiceRunning(ZeroMQService.class.getName())) {
			callingFromResume = true;
			restartTransmission();
		}

		if (videoStart) {
			startCamera();
		}

	}

	@Override
	public void zeroMQServiceStop(final boolean serviceStopped) {
		runOnUiThread(new Thread(new Runnable() {
			public void run() {
				if (serviceStopped) {
					StopReceivers stop = new StopReceivers(activity, false);
					stop.run();
					stopChatService();
					// UtilsWhatAmIdoing.displayGenericToast(activity,
					// "Sharing Service Stopped");

				}

			}
		}));

	}

	@Override
	public void networkStatusChange(boolean available) {

		/*
		 * if (!available && sharing) { Toast.makeText(getApplicationContext(),
		 * "Network connectivity Lost -- stopping sharing",
		 * Toast.LENGTH_LONG).show(); Intent msgIntent = new
		 * Intent(activity,com.
		 * watamidoing.transport.service.WebsocketService.class);
		 * stopService(msgIntent); Button startTransmission = (Button)
		 * activity.findViewById(R.id.start_transmission);
		 * startTransmission.setText(START_SHARING); doUnbindService();
		 * initializeButtons();
		 * 
		 * } else if (!sharing && available){
		 * Toast.makeText(getApplicationContext(), "Network available",
		 * Toast.LENGTH_SHORT).show(); initializeButtons(); }
		 */
	}

	public boolean doBindService() {
		// Establish a connection with the service. We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		boolean bound = getApplicationContext().bindService(
				new Intent(getApplicationContext(),
						com.watamidoing.transport.service.ZeroMQService.class),
				zeroMQServiceConnection, 0);
		
		if (bound) {
			mIsBound = true;
			Log.d("WhatAmidoingCamera.doBindService", "binding to service");
		} else {
			Log.d("WhatAmidoingCamera.doBindService", "Uable binding to service");	
		}
		return bound;

	}

	public void doUnbindService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with
			// it, then now is the time to unregister.
			// Detach our existing connection.
			if (zeroMQServiceConnection != null) {
				getApplicationContext().unbindService(zeroMQServiceConnection);
				mIsBound = false;
				mService = null;
			}

		}
	}

	@Override
	public void setMessengerService(Messenger mService) {

		ImageButton startTransmission = (ImageButton) activity
				.findViewById(R.id.start_transmission);
		if (mService != null) {

			if (whosAcceptedMenuItem != null) {
				whosAcceptedMenuItem.setEnabled(true);
				sendLocationMenuItem.setEnabled(true);
			}
			if (cameraView != null) {
				// TODO:
				// cameraView.sharingHasStarted();
			}
			videoSharing = true;
			this.mService = mService;
			startSharingState = STOP_SHARING;
			startTransmission.setImageResource(R.drawable.share_red);
			opencvCameraListener.createMessengerService(mService, this);
		} else {
			StopReceivers stopReceivers = new StopReceivers(activity, false);
			stopReceivers.run();
			startTransmission.setImageResource(R.drawable.share_blue);
		}
		startTransmission.setEnabled(true);
		if (cameraHasBeenStarted) {
			startCamera();
		}

	}

	@Override
	public void onDestroy() {

		super.onDestroy();
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				unregisterAllReceivers();
				stopChatService();

			}
		});
	}

	class StopReceivers implements Runnable {

		private WhatAmIdoing activity;
		private Boolean callOnBackPress = true;

		public StopReceivers(WhatAmIdoing activity, Boolean callOnBackPress) {
			this.activity = activity;
			this.callOnBackPress = callOnBackPress;
		}

		@Override
		public void run() {

			Intent msgIntent = new Intent(activity,
					com.watamidoing.transport.service.ZeroMQService.class);
			activity.stopService(msgIntent);

			Intent totalWatchersIntent = new Intent(activity,
					com.watamidoing.total.service.TotalWatchersService.class);
			activity.stopService(totalWatchersIntent);

			unregisterAllReceivers();
			opencvCameraListener.disableMessengerService();
			if (cameraView != null) {
				// TODO
				// cameraView.sharingHasStopepd();
			}

			doUnbindService();

			if (callOnBackPress) {
				Authentication auth = DatabaseHandler.getInstance(activity)
						.getDefaultAuthentication();
				if (auth != null) {

					DatabaseHandler.getInstance(activity).removeAuthentication(
							auth);
				}
			}
			if (locationManager != null) {
				locationManager.removeUpdates(waidLocationListener);
				locationManager = null;
			}

			if (callOnBackPress) {
				activity.callOriginalOnBackPressed();
			}

			startSharingState = START_SHARING;
			final ImageButton transmissionButton = (ImageButton) activity
					.findViewById(R.id.start_transmission);
			transmissionButton.setImageResource(R.drawable.share_blue);
			transmissionButton.setEnabled(true);
			videoSharing = false;

			if (sendLocationMenuItem != null) {
				sendLocationMenuItem.setEnabled(false);
				whosAcceptedMenuItem.setEnabled(false);
			}

			final TextView totalWatchers = (TextView) activity
					.findViewById(R.id.totalWatchers);
			totalWatchers.setText("");

		}
	}

	public void callOriginalOnBackPressed() {
		super.onBackPressed();
	}

	private void unregisterAllReceivers() {
		try {
			if (totalWatchersReceiver != null) {
				unregisterReceiver(totalWatchersReceiver);
			}
		} catch (IllegalArgumentException e) {
		}
		totalWatchersReceiver = null;

		try {
			if (serviceStoppedReceiver != null) {
				unregisterReceiver(serviceStoppedReceiver);
			}
		} catch (IllegalArgumentException e) {
		}
		serviceStoppedReceiver = null;
		try {
			if (networkChangeReceiver != null) {
				unregisterReceiver(networkChangeReceiver);
			}
		} catch (IllegalArgumentException e) {
		}

		networkChangeReceiver = null;
	}

	@Override
	public void onBackPressed() {

		Log.i(TAG, "ON BACK PRESSED");
		StopReceivers stopReceivers = new StopReceivers(this, true);
		runOnUiThread(new Thread(stopReceivers));

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				stopChatService();

			}
		});

	}

	private void startCamera() {

		if (cameraView != null) {
			/*
			 * Changing the size of the screen
			 */
			ScreenDimension dimension = UtilsWhatAmIdoing
					.getScreenDimensions(activity);
			// get layout parameters for that element
			if (mainLayout == null) {
				mainLayout = (LinearLayout) this
						.findViewById(R.id.momemts_frame);
			}
			ViewGroup.LayoutParams params = mainLayout.getLayoutParams();

			imageHeight = params.height;
			imageWidth = params.width;

			// cameraView = new CameraView(activity);
			// mainLayout.addView(cameraView, layoutParam);
			// mainLayout.addView(cameraView);
			cameraView.enableView();
			cameraView.enableFpsMeter();
			videoStart = true;
			final ImageButton startVideo = (ImageButton) activity
					.findViewById(R.id.start_video);
			startVideo.setImageResource(R.drawable.stop_camera);

		}

		if (videoSharing) {
			// TODO
			// cameraView.sharingHasStarted();
		}
	}

	public void resetPassword() {

	}

	public void displayFriendsPicker() {

	}

	public void setFacebookSession(Session session) {
		this.facebookSession = session;
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		Fragment toRemove = fm.findFragmentByTag("facebook_invite_dialog");
		ft.remove(toRemove);
		ft.commit();
		shareOnFacebook();

	}

	/*
	 * @Override public boolean onPrepareOptionsMenu(Menu menu) { if
	 * (menu.size() == 0) { shareOnFacebook =
	 * menu.add(R.string.share_on_facebook); shareUsingEmail =
	 * menu.add(R.string.share_using_email); sendTweet =
	 * menu.add(R.string.send_tweet); } return true; }
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);

		menu.findItem(R.id.linkedin_menu_item).setIcon(
				resizeImage(R.drawable.linkedin, 100, 95));
		menu.findItem(R.id.facebook_menu_item).setIcon(
				resizeImage(R.drawable.facebook, 108, 108));
		menu.findItem(R.id.twitter_menu_item).setIcon(
				resizeImage(R.drawable.twitter, 160, 160));
		menu.findItem(R.id.email_menu_item).setIcon(
				resizeImage(R.drawable.mail, 120, 120));
		menu.findItem(R.id.logout_menu_item).setIcon(
				resizeImage(R.drawable.logout, 120, 120));

		sendLocationMenuItem = menu.findItem(R.id.send_location_menu_item);
		whosAcceptedMenuItem = menu.findItem(R.id.whos_accepted_menu_item);

		if (videoSharing) {
			sendLocationMenuItem.setEnabled(true);
			whosAcceptedMenuItem.setEnabled(true);
		} else {
			sendLocationMenuItem.setEnabled(false);
			whosAcceptedMenuItem.setEnabled(false);
		}
		return true;
	}

	private Drawable resizeImage(int resId, int w, int h) {
		// load the origial Bitmap
		Bitmap BitmapOrg = BitmapFactory.decodeResource(getResources(), resId);
		int width = BitmapOrg.getWidth();
		int height = BitmapOrg.getHeight();
		int newWidth = w;
		int newHeight = h;
		// calculate the scale
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		// create a matrix for the manipulation
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,
				height, matrix, true);
		return new BitmapDrawable(resizedBitmap);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();

		boolean skip = false;
		if (itemId == R.id.logout_menu_item) {
			Authentication auth = DatabaseHandler.getInstance(activity)
					.getDefaultAuthentication();
			if (auth != null) {
				DatabaseHandler.getInstance(activity)
						.removeAuthentication(auth);

			}

			TwitterAuthenticationToken tat = DatabaseHandler.getInstance(
					activity).getDefaultTwitterAuthentication();
			if (tat != null) {
				DatabaseHandler.getInstance(activity).removeAuthentication(tat);

			}

			LinkedInAuthenticationToken la = DatabaseHandler.getInstance(
					activity).getDefaultLinkedinAuthentication();
			if (la != null) {
				DatabaseHandler.getInstance(activity).removeAuthentication(la);

			}

			Intent intent = new Intent(this, Login.class);
			startActivity(intent);

			StopReceivers stopReceivers = new StopReceivers(this, true);
			runOnUiThread(new Thread(stopReceivers));
			skip = true;

		}

		if (videoStart && !skip && videoSharing) {

			if (itemId == R.id.linkedin_menu_item) {
				LinkedInAuthorization ta = new LinkedInAuthorization(activity);
				Token accessToken = ta.getAccessToken();
				if (accessToken == null) {
					ta.authorizeLinkedIn();
				} else {
					shareOnLinkedIn();
				}
				return true;
			} else if (itemId == R.id.facebook_menu_item) {
				shareOnFacebook();
				return true;
			} else if (itemId == R.id.email_menu_item) {
				sendEmail();
				return true;
			} else if (itemId == R.id.twitter_menu_item) {
				TwitterAuthorization ta = new TwitterAuthorization(activity);
				AccessToken accessToken = ta.getAccessToken();
				Log.i(TAG, "--------------------------- should be tweeig");
				if (accessToken == null) {
					ta.authorizeTwitter();
				} else {
					tweetWhatIAmDoing();
				}
				return true;
			} else if (itemId == R.id.send_location_menu_item) {
				shareLocation();
			} else if (itemId == R.id.whos_accepted_menu_item) {
				whoHasAccepted();
			}

		} else {
			String message = activity.getString(R.string.camera_not_started);
			UtilsWhatAmIdoing.displayGenericMessageDialog(activity, message);
		}

		return false;
	}

	public void sendEmail() {
		mInviteListTask = new InviteListTask(activity);
		mInviteListTask.execute((Void) null);
	}

	public void shareOnLinkedIn() {
		LinkedInAuthorization la = new LinkedInAuthorization(activity);
		Token accessToken = la.getAccessToken();
		if (accessToken != null) {
			String inviteUrl = activity
					.getString(R.string.send_invite_linkedin_url);
			Authentication auth = DatabaseHandler.getInstance(activity)
					.getDefaultAuthentication();
			String url = inviteUrl + "?token=" + auth.getToken();
			SendLinkedInInviteTask stit = new SendLinkedInInviteTask(url,
					activity);
			stit.execute((Void) null);

		}

	}

	public void tweetWhatIAmDoing() {
		TwitterAuthorization ta = new TwitterAuthorization(activity);
		AccessToken accessToken = ta.getAccessToken();
		if (accessToken != null) {
			String inviteUrl = activity
					.getString(R.string.send_invite_twitter_url);
			Authentication auth = DatabaseHandler.getInstance(activity)
					.getDefaultAuthentication();
			String url = inviteUrl + "?token=" + auth.getToken();
			SendTwitterInviteTask stit = new SendTwitterInviteTask(url,
					activity);
			stit.execute((Void) null);

		}
	}

	public void shareOnFacebook() {
		Log.i(TAG, "------------------------------- on click sharine (1)");

		if ((facebookSession == null) || (facebookSession.isClosed())) {
			FragmentManager fm = getSupportFragmentManager();
			inviteDialogFragment = FaceBookInviteDialogFragment.newInstance(
					true, activity);
			inviteDialogFragment.show(fm, "facebook_invite_dialog");
		} else {

			String inviteUrl = activity
					.getString(R.string.send_invite_facebook_url);
			Authentication auth = DatabaseHandler.getInstance(activity)
					.getDefaultAuthentication();
			String url = inviteUrl + "?token=" + auth.getToken();
			FacebookPostTask fpt = new FacebookPostTask(url, activity,
					facebookSession);
			fpt.execute((Void) null);

		}

	}

	public static boolean isServiceRunning(String serviceClassName) {
		final ActivityManager activityManager = (ActivityManager) activity
				.getSystemService(Context.ACTIVITY_SERVICE);
		final List<android.app.ActivityManager.RunningServiceInfo> services = activityManager
				.getRunningServices(Integer.MAX_VALUE);

		for (android.app.ActivityManager.RunningServiceInfo runningServiceInfo : services) {
			if (runningServiceInfo.service.getClassName().equals(
					serviceClassName)) {
				return true;
			}
		}
		return false;
	}

	public boolean isVideoSharing() {
		return videoSharing;
	}

	public boolean isVideoStart() {
		return videoStart;
	}

	public void displayChat(final View view) {

		// Registering the receiver of chat messages

		if (isVideoSharing() && isVideoStart()) {
			ImageButton displayChatButton = (ImageButton) activity
					.findViewById(R.id.send_messge);
			displayChatButton.setEnabled(false);
			Log.i(TAG, "---display chat button should be disabled");

			if (xmppServiceStoppedReceiver != null) {
				unregisterReceiver(xmppServiceStoppedReceiver);
			}

			IntentFilter xmppServiceStoppedFilter = new IntentFilter(
					XMPPServiceStoppedReceiver.XMPP_SERVICE_STOPPED_RECIEVED);

			xmppServiceStoppedFilter.addCategory(Intent.CATEGORY_DEFAULT);
			xmppServiceStoppedReceiver = new XMPPServiceStoppedReceiver(this);
			registerReceiver(xmppServiceStoppedReceiver,
					xmppServiceStoppedFilter);

			if (chatMessageReceiver != null) {
				unregisterReceiver(chatMessageReceiver);
			}
			chatMessageReceiverFilter = new IntentFilter(
					ChatMessageReceiver.MESSAGE_RECIEVED);
			chatMessageReceiverFilter.addCategory(Intent.CATEGORY_DEFAULT);
			chatMessageReceiver = new ChatMessageReceiver(this);
			registerReceiver(chatMessageReceiver, chatMessageReceiverFilter);

			if (chatEnabledReceiver != null) {
				unregisterReceiver(chatEnabledReceiver);
			}
			chatEnabledReceiverFilter = new IntentFilter(
					ChatEnabledReceiver.CHAT_ENABLED_RECEIVER);
			chatEnabledReceiverFilter.addCategory(Intent.CATEGORY_DEFAULT);
			chatEnabledReceiver = new ChatEnabledReceiver(this);
			registerReceiver(chatEnabledReceiver, chatEnabledReceiverFilter);

			if (chatParticipantReceiver != null) {
				unregisterReceiver(chatParticipantReceiver);
			}
			chatParticipantReceiverFilter = new IntentFilter(
					ChatParticipantReceiver.CHAT_PARTICIPANT_RECEIVER);
			chatParticipantReceiverFilter.addCategory(Intent.CATEGORY_DEFAULT);
			chatParticipantReceiver = new ChatParticipantReceiver(this);
			registerReceiver(chatParticipantReceiver,
					chatParticipantReceiverFilter);

			Intent msgIntent = new Intent(this,
					com.watamidoing.chat.xmpp.service.XMPPService.class);
			if (WhatAmIdoing.isServiceRunning(XMPPService.class.getName())) {
				Log.i(TAG, "--------STOPPIN SERVICE");
				doUnbindChatService();
				this.stopService(msgIntent);
			}
			Log.i(TAG, "--------STARTING SERVICE");
			this.startService(msgIntent);
		} else {
			UtilsWhatAmIdoing.displayGenericToast(this,
					this.getString(R.string.video_not_started_view_sharers));
		}

	}

	public void doBindChatService() {
		// Establish a connection with the service. We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		getApplicationContext().bindService(
				new Intent(getApplicationContext(),
						com.watamidoing.chat.xmpp.service.XMPPService.class),
				xmppServiceConnection, 0);
		chatServiceIsBound = true;

		Log.i("WhatAmidoingCamera.doBindService", "binding to service");

	}

	public void doUnbindChatService() {

		if (chatServiceIsBound) {
			// If we have received the service, and hence registered with
			// it, then now is the time to unregister.
			// Detach our existing connection.
			if (xmppServiceConnection != null) {
				getApplicationContext().unbindService(xmppServiceConnection);
				chatServiceIsBound = false;
			}

		}
	}

	@Override
	public void xmppConnection(boolean results, Messenger messenger) {

		if (results) {
			FragmentManager fm = getSupportFragmentManager();
			chatDialogFragment = ChatDialogFragment.newInstance(true, activity,
					messenger);
			chatDialogFragment.show(fm, "chat_dialog_fragment");

			String oldMessage = chatMessageQueue.poll();
			while (oldMessage != null) {
				chatDialogFragment.addMessage(oldMessage);
				oldMessage = chatMessageQueue.poll();
			}

			Participant oldParticipant = chatParticipantQueue.poll();

			while (oldParticipant != null) {
				chatDialogFragment.addParticipant(oldParticipant);
				oldParticipant = chatParticipantQueue.poll();
			}

		} else {
			String message = activity
					.getString(R.string.unable_to_connect_to_chat);
			UtilsWhatAmIdoing.displayGenericToast(activity, message);
		}

	}

	@Override
	public void messageReceived(String chatMessageBefore) {

		String chatMessage = Emoji.replaceInText(chatMessageBefore);
		Log.i(TAG, "--------------NEW MESSAGE RECEIVED SIZE OF LIST:"
				+ chatMessageQueue.size());
		Log.i(TAG, "--------------NEW MESSAGE RECEIVED:" + chatMessage);

		if (isVideoSharing()) {
			if (chatDialogFragment == null) {
				chatMessageQueue.add(chatMessage);
			} else {
				chatDialogFragment.addMessage(chatMessage);
			}
		} else {
			stopChatService();
		}
	}

	@Override
	public void enableChat(boolean enable) {

		if (enable) {
			doBindChatService();
		}

	}

	@Override
	public void newParticipant(Participant participant) {
		Log.i(TAG, "newParticpant[" + participant.getFullnick() + "]");

		if (isVideoSharing()) {
			if (chatDialogFragment == null) {
				Log.i(TAG, "Fragment not created[" + participant.getFullnick()
						+ "]");
				chatParticipantQueue.add(participant);
			} else {
				Log.i(TAG, "Fragment created[" + participant.getFullnick()
						+ "]");
				chatDialogFragment.addParticipant(participant);
			}

		} else {
			stopChatService();
		}

	}

	public void stopChatService() {

		Log.i(TAG, "stopChatService -- should be enabling the button");
		ImageButton displayChatButton = (ImageButton) activity
				.findViewById(R.id.send_messge);
		displayChatButton.setEnabled(true);
		chatParticipantQueue = new LinkedList<Participant>();
		chatMessageQueue = new LinkedList<String>();
		// Registering the receiver of chat messages

		try {
			if (xmppServiceStoppedReceiver != null) {
				unregisterReceiver(xmppServiceStoppedReceiver);
			}
		} catch (IllegalArgumentException e) {
		}

		xmppServiceStoppedReceiver = null;
		try {
			if (chatMessageReceiver != null) {
				unregisterReceiver(chatMessageReceiver);
			}
		} catch (IllegalArgumentException e) {
		}
		chatMessageReceiver = null;

		try {
			if (chatEnabledReceiver != null) {
				unregisterReceiver(chatEnabledReceiver);
			}
		} catch (IllegalArgumentException e) {
		}
		chatEnabledReceiver = null;

		try {
			if (chatParticipantReceiver != null) {
				unregisterReceiver(chatParticipantReceiver);
			}
		} catch (IllegalArgumentException e) {
		}

		chatParticipantReceiver = null;
		doUnbindChatService();

		if (isServiceRunning(XMPPService.class.getName())) {
			Intent msgIntent = new Intent(activity,
					com.watamidoing.chat.xmpp.service.XMPPService.class);
			stopService(msgIntent);
		}

		if (chatDialogFragment != null) {
			chatDialogFragment.dismiss();
		}
		chatDialogFragment = null;

	}

	@Override
	public void xmppServiceStopped(boolean status) {
		if (status) {
			stopChatService();
			// String message =
			// activity.getString(R.string.chat_service_stopped);
			// UtilsWhatAmIdoing.displayGenericToast(this, message);
		}

	}

	@Override
	public void updateTotalWatchers(final String totalWatchers) {
		runOnUiThread(new Thread(new Runnable() {
			public void run() {
				final TextView tw = (TextView) activity
						.findViewById(R.id.totalWatchers);
				tw.setText(totalWatchers);
			}
		}));

	}
}
