package com.watamidoing.view;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.xmlpull.v1.XmlSerializer;

import twitter4j.auth.AccessToken;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Session;
import com.waid.R;
import com.watamidoing.Login;
import com.watamidoing.chat.ChatDialogFragment;
import com.watamidoing.contentproviders.Authentication;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.contentproviders.LinkedInAuthenticationToken;
import com.watamidoing.contentproviders.TwitterAuthenticationToken;
import com.watamidoing.invite.facebook.FaceBookInviteDialogFragment;
import com.watamidoing.invite.facebook.FacebookPostTask;
import com.watamidoing.invite.linkedin.LinkedInAuthorization;
import com.watamidoing.invite.linkedin.SendLinkedInInviteTask;
import com.watamidoing.invite.linkedin.ShareTag;
import com.watamidoing.invite.twitter.SendTwitterInviteTask;
import com.watamidoing.invite.twitter.TwitterAuthorization;
import com.watamidoing.tasks.DoLaterTask;
import com.watamidoing.tasks.GetInvitedTask;
import com.watamidoing.tasks.InviteListTask;
import com.watamidoing.tasks.TotalUsersWatchingTask;
import com.watamidoing.tasks.WAIDLocationListener;
import com.watamidoing.tasks.callbacks.WebsocketController;
import com.watamidoing.tasks.callbacks.XMPPConnectionController;
import com.watamidoing.transport.receivers.ChatEnabledReceiver;
import com.watamidoing.transport.receivers.ChatMessageReceiver;
import com.watamidoing.transport.receivers.ChatParticipantReceiver;
import com.watamidoing.transport.receivers.NetworkChangeReceiver;
import com.watamidoing.transport.receivers.NotAbleToConnectReceiver;
import com.watamidoing.transport.receivers.ServiceConnectionCloseReceiver;
import com.watamidoing.transport.receivers.ServiceStartedReceiver;
import com.watamidoing.transport.receivers.ServiceStoppedReceiver;
import com.watamidoing.transport.service.WebsocketService;
import com.watamidoing.transport.service.WebsocketServiceConnection;
import com.watamidoing.utils.ScreenDimension;
import com.watamidoing.utils.UtilsWhatAmIdoing;
import com.watamidoing.xmpp.service.XMPPService;
import com.watamidoing.xmpp.service.XMPPServiceConnection;

import org.jivesoftware.smack.SmackAndroid;
public class WhatAmIdoing extends FragmentActivity implements
		WebsocketController, XMPPConnectionController  {

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
	private static final String SHARE_LOCATION = "Share Location";
	private static final String LOG_TAG = "whatamidoing.oncreate";
	private InviteListTask mInviteListTask;
	private long startTime = 0;

	private boolean videoStart = false;
	private boolean videoSharing = false;
	private int imageWidth = 320;
	private int imageHeight = 240;
	private CameraView cameraView;
	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection;
	protected ServiceStartedReceiver serviceStartedReceiver;
	protected ServiceStoppedReceiver serviceStoppedReceiver;
	protected NetworkChangeReceiver networkChangeReceiver;
	protected ServiceConnectionCloseReceiver serviceConnectionCloseReceiver;

	protected boolean sharing = false;
	private int cameraId;
	private LinearLayout mainLayout;
	private NotAbleToConnectReceiver notAbleToConnectReceiver;
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
	
	private LinkedList<String> chatParticipantQueue = new LinkedList<String>();

	private IntentFilter chatParticipantReceiverFilter;

	private ChatParticipantReceiver chatParticipantReceiver;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		
		//Initializing the asmack libraries
		SmackAndroid.init(this);
		if (android.os.Build.VERSION.SDK_INT > 8) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

	   
		mConnection = new WebsocketServiceConnection(this, this);
		xmppServiceConnection = new XMPPServiceConnection(this,this);
		setContentView(R.layout.options);
		activity = this;


		
		// Setting up camera selection
		int cameraCount = 0;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		cameraCount = Camera.getNumberOfCameras();
		List<String> list = new ArrayList<String>();
		boolean foundFront = false;
		boolean foundBack = true;
		int frontCameraId = -1;
		int backCameraId = -1;
		for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
			Camera.getCameraInfo(camIdx, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				list.add("Front Camera");
				foundFront = true;
				frontCameraId = camIdx;
			} else {
				list.add("Back Camera");
				foundBack = true;
				backCameraId = camIdx;

			}
		}

		if (foundFront && foundBack) {
			cameraId = frontCameraId;
		} else if (foundFront) {
			cameraId = frontCameraId;
		} else {
			cameraId = backCameraId;
		}

		final Spinner cameraSelector = (Spinner) findViewById(R.id.selectCamera);
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		cameraSelector.setAdapter(dataAdapter);
		cameraSelector
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
					int iCurrentSelection = cameraSelector
							.getSelectedItemPosition();

					public void onItemSelected(AdapterView<?> adapterView,
							View view, int i, long l) {
						if (iCurrentSelection != i) {
							Log.d("WhatAmIdoing.cameralSelector.onItemSelected",
									"items selected index[" + i + "]");
							cameraId = i;
							if (videoStart) {
								camera.stopPreview();
								mainLayout.removeAllViews();
								cameraView = null;
								camera = null;
								startCamera();
							}

						}
						iCurrentSelection = i;
					}

					public void onNothingSelected(AdapterView<?> adapterView) {
						return;
					}
				});

		final Button shareButton = (Button) findViewById(R.id.viewSharers);
		shareButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				if (videoStart) {
					mInvitedTaskListView = new GetInvitedTask(activity);
					mInvitedTaskListView.execute((Void) null);
				} else {
					String message = activity
							.getString(R.string.camera_not_started);
					UtilsWhatAmIdoing.displayGenericMessageDialog(activity,
							message);
				}

			}
		});

		final Button shareLocation = (Button) findViewById(R.id.locationButton);
		shareLocation.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (videoStart) {

					Button locationButton = (Button) activity
							.findViewById(R.id.locationButton);
					String text = locationButton.getText().toString();

					if (SHARE_LOCATION.equalsIgnoreCase(text)) {

						runOnUiThread(new Thread(new Runnable() {
							public void run() {
								if (locationManager == null)
									locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

								if (waidLocationListener == null)
									waidLocationListener = new WAIDLocationListener(
											activity);
								
								List<String> providers = locationManager.getProviders(true);
						        for (String provider : providers) {
						            locationManager.requestLocationUpdates(provider, 1000L, 0.0f,
						                    new LocationListener() {
						                        public void onLocationChanged(Location location) {}

						                        public void onProviderDisabled(String provider) {
						                        }

						                        public void onProviderEnabled(String provider) {
						                        }

						                        public void onStatusChanged(String provider,
						                                int status, Bundle extras) {
						                        }
						            });
						        }
							


								Criteria criteria = new Criteria();
								String provider = locationManager
										.getBestProvider(criteria, false);
								Location location = locationManager
										.getLastKnownLocation(provider);

								if (location == null) {
									locationManager
									.getLastKnownLocation("network");
									
								}
								
								if (location == null) {
									locationManager
									.getLastKnownLocation("network");
								}
								if (location != null) {
									waidLocationListener.sendLocation(location);
									Log.i("WhatAmIdoing.onClick", "IS NOT NULL");
								} else {
									Log.i("WhatAmIdoing.onClick", "IS NULL");
									 Toast.makeText(getApplicationContext(), "Location Currently Not Available",Toast.LENGTH_SHORT).show();
								}
								// locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
								// 0, 0, waidLocationListener);
								// GPS location updates.
								// locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
								// 0, 0, waidLocationListener);
							}
						}));
					} else {
						locationButton.setText(SHARE_LOCATION);
						locationManager.removeUpdates(waidLocationListener);
						locationManager = null;
					}
					/*
					 * mInvitedTaskListView = new GetInvitedTask(activity);
					 * mInvitedTaskListView.execute((Void) null);
					 */
				} else {
					String message = activity
							.getString(R.string.camera_not_started);
					UtilsWhatAmIdoing.displayGenericMessageDialog(activity,
							message);
				}

			}
		});

		initializeButtons();

		mainLayout = (LinearLayout) this.findViewById(R.id.momemts_frame);
		ScreenDimension dimension = UtilsWhatAmIdoing
				.getScreenDimensions(activity);
		ViewGroup.LayoutParams params = mainLayout.getLayoutParams();

		// change width of the params e.g. 50dp
		params.width = Double.valueOf(dimension.getDpHeightPixels() * 0.8)
				.intValue();
		params.height = Double.valueOf(dimension.getDpWidthPixels() * 0.5)
				.intValue();
		// initialize new parameters for my element
		// mainLayout.setLayoutParams(new GridLayout.LayoutParams(params));

		// Making buttons equal width

		GridLayout gl = (GridLayout) this.findViewById(R.id.video_display);
		gl.requestLayout();

	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {

		Log.i(TAG, "onSaveInstanceState");
		if (videoStart) {

			Button startVideo = (Button) activity
					.findViewById(R.id.start_video);
			startVideo.setText(START_CAMERA);

			Button locationButton = (Button) activity
					.findViewById(R.id.locationButton);
			locationButton.setText(SHARE_LOCATION);
			mainLayout.removeAllViews();
			cameraView = null;
			videoStart = false;
			/*
			 * if (camera != null) { camera.release(); }
			 */
			camera = null;

			if (locationManager != null) {
				locationManager.removeUpdates(waidLocationListener);
				locationManager = null;
			}
		}

		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

	public void startVideo() {

		runOnUiThread(new Thread(new Runnable() {
			public void run() {

				Button startVideo = (Button) activity
						.findViewById(R.id.start_video);
				String text = startVideo.getText().toString();

				final Button transmissionButton = (Button) activity
						.findViewById(R.id.start_transmission);

				if (START_CAMERA.equalsIgnoreCase(text)) {
					transmissionButton.setEnabled(true);
					startVideo.setText(STOP_CAMERA);
					startCamera();
					videoStart = true;

				} else {
					camera.stopPreview();
					mainLayout.removeAllViews();
					startVideo.setText(START_CAMERA);
					cameraView = null;
					videoStart = false;
					camera = null;

					// camera.release();

					// NativeMethods.closeWebsocketConnection();
					// transmissionButton.setText(START_SHARING);
					// initializeButtons();
				}

			}
		}));

	}

	/**
	 * Used to start video
	 */
	public void startVideo(View view) {

		startVideo();
	}

	/**
	 * Used to start video transmission
	 */
	public void startTransmission(final View view) {

		runOnUiThread(new Thread(new Runnable() {

			private IntentFilter filterNotAbleToConnect;
			private IntentFilter filterServiceStartedReciever;
			private IntentFilter filterServiceStoppedReceiver;
			private IntentFilter filterRegisterReceiver;
			private IntentFilter filterServiceConnectionCloseReceiver;

			public void run() {

				Button startTransmission = (Button) activity
						.findViewById(R.id.start_transmission);
				String text = startTransmission.getText().toString();

				Intent msgIntent = new Intent(
						activity,
						com.watamidoing.transport.service.WebsocketService.class);

				if (START_SHARING.equalsIgnoreCase(text)) {
					startTransmission.setEnabled(true);

					if (isServiceRunning(WebsocketService.class.getName())) {
						StopReceivers stopReceivers = new StopReceivers(
								activity, false);
						stopReceivers.run();

					}
					startService(msgIntent);

					if (notAbleToConnectReceiver == null) {
						filterNotAbleToConnect = new IntentFilter(
								NotAbleToConnectReceiver.NOT_ABLE_TO_CONNECT);
						filterNotAbleToConnect
								.addCategory(Intent.CATEGORY_DEFAULT);
						notAbleToConnectReceiver = new NotAbleToConnectReceiver(
								activity);
						registerReceiver(notAbleToConnectReceiver,
								filterNotAbleToConnect);
					}

					if (serviceStartedReceiver == null) {
						filterServiceStartedReciever = new IntentFilter(
								ServiceStartedReceiver.SERVICE_STARTED);
						filterServiceStartedReciever
								.addCategory(Intent.CATEGORY_DEFAULT);
						serviceStartedReceiver = new ServiceStartedReceiver(
								activity);
						registerReceiver(serviceStartedReceiver,
								filterServiceStartedReciever);
					}

					if (serviceStoppedReceiver == null) {
						filterServiceStoppedReceiver = new IntentFilter(
								ServiceStoppedReceiver.SERVICE_STOPED);
						filterServiceStoppedReceiver
								.addCategory(Intent.CATEGORY_DEFAULT);
						serviceStoppedReceiver = new ServiceStoppedReceiver(
								activity);
						registerReceiver(serviceStoppedReceiver,
								filterServiceStoppedReceiver);
					}

					if (serviceConnectionCloseReceiver == null) {
						filterServiceConnectionCloseReceiver = new IntentFilter(
								ServiceConnectionCloseReceiver.SERVICE_CONNECTION_CLOSED);
						filterServiceConnectionCloseReceiver
								.addCategory(Intent.CATEGORY_DEFAULT);
						serviceConnectionCloseReceiver = new ServiceConnectionCloseReceiver(
								activity);
						registerReceiver(serviceConnectionCloseReceiver,
								filterServiceConnectionCloseReceiver);
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
					startTransmission.setText(STOP_SHARING);
					doBindService();
				} else {
					stopService(msgIntent);
					// Unregistering receivers

					videoSharing = false;

					doUnbindService();
					startTransmission.setText(START_SHARING);
				}

			}
		}));

	}

	private void initializeButtons() {

		runOnUiThread(new Thread(new Runnable() {
			public void run() {

				if (isServiceRunning(WebsocketService.class.getName())) {
					StopReceivers stopReceivers = new StopReceivers(activity,
							false);
					stopReceivers.run();
					final Button startTransmissionButton = (Button) activity
							.findViewById(R.id.start_transmission);
					startTransmissionButton.setEnabled(true);

					final Button startVideo = (Button) activity
							.findViewById(R.id.start_video);
					startVideo.setEnabled(true);

					final Button viewSharers = (Button) activity
							.findViewById(R.id.viewSharers);
					viewSharers.setEnabled(true);

					final Button shareLocation = (Button) activity
							.findViewById(R.id.locationButton);
					shareLocation.setEnabled(true);
					videoStart = true;
					videoSharing = true;
					startCamera();

				} else {

					final Button startTransmissionButton = (Button) activity
							.findViewById(R.id.start_transmission);
					startTransmissionButton.setEnabled(true);

					final Button startVideo = (Button) activity
							.findViewById(R.id.start_video);
					startVideo.setEnabled(true);

					final Button viewSharers = (Button) activity
							.findViewById(R.id.viewSharers);
					viewSharers.setEnabled(false);

					final Button shareLocation = (Button) activity
							.findViewById(R.id.locationButton);
					shareLocation.setEnabled(false);
					videoStart = false;
					videoSharing = false;
				}
			}
		}));

	}

	/**
	 * Called from the websocket tasks
	 */
	@Override
	public void websocketConnectionCompleted(final boolean results) {
		runOnUiThread(new Thread(new Runnable() {
			public void run() {

				final Button startTransmissionButton = (Button) activity
						.findViewById(R.id.start_transmission);
				if (results) {

					startTransmissionButton.setText(STOP_SHARING);
					startTransmissionButton.setEnabled(true);

					Button viewSharers = (Button) activity
							.findViewById(R.id.viewSharers);
					viewSharers.setEnabled(true);

					Button locationButton = (Button) activity
							.findViewById(R.id.locationButton);
					locationButton.setEnabled(true);

					videoSharing = true;
					if (cameraView != null) {
						cameraView.sharingHasStarted();
					}
					
					//totalUsersWatchingTask = new TotalUsersWatchingTask(activity);
					//totalUsersWatchingTask.execute((Void)null);
					
					
				}
			}
		}));

	}

  public void updateTotalViews(final String watchers) {
	  
	  
	  if (isVideoSharing()) {
		  runOnUiThread(new Thread(new Runnable() {
				public void run() {

					
					if (watchers != null) {
					final TextView totalWatchers = (TextView) activity
							.findViewById(R.id.totalWatchers);
					totalWatchers.setText(watchers);
					}
					totalUsersWatchingTask = new TotalUsersWatchingTask(activity);
					totalUsersWatchingTask.execute((Void)null);
				}
		  }));
	  }
  }
		
	
  
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		GridLayout gl = (GridLayout) this.findViewById(R.id.video_display);
		final Button viewSharers = (Button) activity
				.findViewById(R.id.viewSharers);
		int width = viewSharers.getWidth();

		final Button startTransmissionButton = (Button) activity
				.findViewById(R.id.start_transmission);
		startTransmissionButton.setWidth(width);

		final Button startVideo = (Button) activity
				.findViewById(R.id.start_video);
		startVideo.setWidth(width);

		final Button shareLocation = (Button) activity
				.findViewById(R.id.locationButton);
		shareLocation.setWidth(width);
		
		final TextView totalWatchers = (TextView) activity
				.findViewById(R.id.totalWatchers);
		totalWatchers.setWidth(width);
		gl.requestLayout();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (camera != null) {
			camera.stopPreview();
		}
		Log.i(TAG, "Paused called");
	}

	@Override
	public void onResume() {
		super.onResume();

		Log.i(TAG, "Resume called serviceRunning =["
				+ isServiceRunning(WebsocketService.class.getName())
				+ "] and share=[" + videoSharing + "]");
		final Button startTransmissionButton = (Button) activity
				.findViewById(R.id.start_transmission);
		boolean videoBeingShared = startTransmissionButton.getText() == STOP_SHARING;
		if (isServiceRunning(WebsocketService.class.getName())
				&& videoBeingShared) {
			videoSharing = true;
			callingFromResume = true;
			DoLaterTask dlt = new DoLaterTask(this);
			dlt.execute((Void) null);

		}

	}

	@Override
	public void websocketProblems(final boolean result) {

		runOnUiThread(new Thread(new Runnable() {
			public void run() {
				if (result) {
					stopSharingAndNotifyCamera();
					UtilsWhatAmIdoing.displayWebsocketProblemsDialog(activity);
				}

			}
		}));

	}

	@Override
	public void websocketServiceStop(final boolean serviceStopped) {
		runOnUiThread(new Thread(new Runnable() {
			public void run() {
				if (serviceStopped) {
					stopSharingAndNotifyCamera();
					UtilsWhatAmIdoing
							.displayWebsocketServiceStoppedDialog(activity);

				}

			}
		}));

	}

	synchronized private void stopSharingAndNotifyCamera() {
		final Button transmissionButton = (Button) activity
				.findViewById(R.id.start_transmission);

		videoSharing = false;
		Button viewSharers = (Button) activity.findViewById(R.id.viewSharers);
		viewSharers.setEnabled(false);
		doUnbindService();
		transmissionButton.setText(START_SHARING);
		if (cameraView != null) {
			cameraView.sharingHasStopepd();
		}

		Button locationButton = (Button) activity
				.findViewById(R.id.locationButton);
		locationButton.setText(SHARE_LOCATION);
		locationButton.setEnabled(false);
		if (locationManager != null) {
			locationManager.removeUpdates(waidLocationListener);
			locationManager = null;
		}
		final TextView totalWatchers = (TextView) activity
				.findViewById(R.id.totalWatchers);
		totalWatchers.setText("");
		
		
	}

	@Override
	public void websocketServiceConnectionClose(final boolean connectionClose) {
		runOnUiThread(new Thread(new Runnable() {
			public void run() {
				if (connectionClose) {
					stopSharingAndNotifyCamera();
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

	public void doBindService() {
		// Establish a connection with the service. We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		getApplicationContext()
				.bindService(
						new Intent(
								getApplicationContext(),
								com.watamidoing.transport.service.WebsocketService.class),
						mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
		Log.d("WhatAmidoingCamera.doBindService", "binding to service");

	}

	public void doUnbindService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with
			// it, then now is the time to unregister.
			// Detach our existing connection.
			if (mConnection != null) {
				getApplicationContext().unbindService(mConnection);
				mIsBound = false;
				mService = null;
			}

		}
	}

	@Override
	public void setMessengerService(Messenger mService) {
		this.mService = mService;
		Toast.makeText(getApplicationContext(), "sharing", Toast.LENGTH_LONG)
				.show();

	}

	@Override
	public void onDestroy() {

		super.onDestroy();
		stopChatService();
	}

	@Override
	public void sendFrame(String res) {
		// TODO Auto-generated method stub

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

			try {
				if (notAbleToConnectReceiver != null) {
					unregisterReceiver(notAbleToConnectReceiver);
				}
			} catch (IllegalArgumentException e) {
				notAbleToConnectReceiver = null;
			}

			try {
				if (serviceStartedReceiver != null) {
					unregisterReceiver(serviceStartedReceiver);
				}
			} catch (IllegalArgumentException e) {
				serviceStartedReceiver = null;
			}

			try {
				if (serviceStoppedReceiver != null) {
					unregisterReceiver(serviceStoppedReceiver);
				}
			} catch (IllegalArgumentException e) {
				serviceStoppedReceiver = null;
			}

			try {
				if (serviceConnectionCloseReceiver != null) {
					unregisterReceiver(serviceConnectionCloseReceiver);
				}
			} catch (IllegalArgumentException e) {
				serviceConnectionCloseReceiver = null;
			}

			try {
				if (networkChangeReceiver != null) {
					unregisterReceiver(networkChangeReceiver);
				}
			} catch (IllegalArgumentException e) {
				networkChangeReceiver = null;
			}
			doUnbindService();
			Intent msgIntent = new Intent(activity,
					com.watamidoing.transport.service.WebsocketService.class);
			stopService(msgIntent);
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
		}
	}

	public void callOriginalOnBackPressed() {
		super.onBackPressed();
	}

	@Override
	public void onBackPressed() {

		StopReceivers stopReceivers = new StopReceivers(this, true);
		runOnUiThread(new Thread(stopReceivers));

	}

	private void startCamera() {

		if (cameraView == null) {
			/*
			 * Changing the size of the screen
			 */
			ScreenDimension dimension = UtilsWhatAmIdoing
					.getScreenDimensions(activity);
			// get layout parameters for that element
			ViewGroup.LayoutParams params = mainLayout.getLayoutParams();

			// change width of the params e.g. 50dp
			params.width = Double.valueOf(dimension.getDpHeightPixels() * 0.8)
					.intValue();
			params.height = Double.valueOf(dimension.getDpWidthPixels() * 0.5)
					.intValue();

			imageHeight = params.height;
			imageWidth = params.width;

			cameraView = new CameraView(activity);
			LinearLayout.LayoutParams layoutParam = new LinearLayout.LayoutParams(
					imageWidth, imageHeight);
			mainLayout.addView(cameraView, layoutParam);
		}

		if (videoSharing) {
			cameraView.sharingHasStarted();
		}
	}

	class CameraView extends SurfaceView implements SurfaceHolder.Callback,
			PreviewCallback {

		private SurfaceHolder holder;
		long videoTimestamp = 0;

		Bitmap bitmap;
		Canvas canvas;

		private volatile boolean sharingStarted;

		public CameraView(Context _context) {
			super(_context);

			holder = this.getHolder();
			holder.addCallback(this);
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			Log.i(TAG, "cameraId [" + cameraId + "]");

			try {
				camera = Camera.open(cameraId);
			} catch (RuntimeException e) {
				e.printStackTrace();

				camera = Camera.open(Camera.getNumberOfCameras() - 1);
			}

			try {
				camera.setPreviewDisplay(holder);
				camera.setPreviewCallback(this);

				Camera.Parameters currentParams = camera.getParameters();
				Log.v(LOG_TAG,
						"Preview imageWidth: "
								+ currentParams.getPreviewSize().width
								+ " imageHeight: "
								+ currentParams.getPreviewSize().height);

				// Use these values
				imageWidth = currentParams.getPreviewSize().width;
				imageHeight = currentParams.getPreviewSize().height;

				bitmap = Bitmap.createBitmap(imageWidth, imageHeight,
						Bitmap.Config.ALPHA_8);
				camera.startPreview();

			} catch (IOException e) {
				Log.v(LOG_TAG, e.getMessage());
				e.printStackTrace();
			}

		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			Log.v(LOG_TAG, "Surface Changed: width " + width + " height: "
					+ height);

			// Get the current parameters
			Camera.Parameters currentParams = camera.getParameters();
			Log.v(LOG_TAG,
					"Preview imageWidth: "
							+ currentParams.getPreviewSize().width
							+ " imageHeight: "
							+ currentParams.getPreviewSize().height);

			// Use these values
			imageWidth = currentParams.getPreviewSize().width;
			imageHeight = currentParams.getPreviewSize().height;
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			if (camera != null) {
				camera.setPreviewCallback(null);
				camera.release();
			}
			Log.i(LOG_TAG, "onSurfaceDestroy");
		}

		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {

			Log.d("WhatAmIDoing.CameraView.onPreview", "sharing["
					+ sharingStarted + "]");
			if (sharingStarted) {

				Camera.Parameters parameters = camera.getParameters();
				Size size = parameters.getPreviewSize();
				YuvImage image = new YuvImage(data,
						parameters.getPreviewFormat(), size.width, size.height,
						null);
				videoTimestamp = 1000 * (System.currentTimeMillis() - startTime);

				// Put the camera preview frame right into the yuvIplimage
				// object
				// yuvIplimage.getByteBuffer().put(data);
				android.graphics.Rect previewRect = new android.graphics.Rect(
						0, 0, imageWidth, imageHeight);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				image.compressToJpeg(previewRect, 70, baos);

				// byte[] jdata = resizeImage(baos.toByteArray());
				byte[] jdata = baos.toByteArray();

				Message msgObj = Message.obtain(null,
						WebsocketService.PUSH_MESSAGE_TO_QUEUE);
				Bundle b = new Bundle();
				b.putByteArray("frame", jdata);
				msgObj.setData(b);
				try {
					if (mService != null)
						mService.send(msgObj);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Log.d("WhatAmiDoing.CameraView.onPreview", " transmit bytes["
						+ jdata.length + "]");
				// baos = null;
				jdata = null;
			}
		}

		byte[] resizeImage(byte[] input) {
			Bitmap original = BitmapFactory.decodeByteArray(input, 0,
					input.length);
			Camera.Parameters parameters = camera.getParameters();
			Size size = parameters.getPreviewSize();
			Log.i("WhatAmiDoing.CameraView.resizeImage", "original bitmap ["
					+ original + "]");
			Bitmap resized = Bitmap.createScaledBitmap(original,
					size.width / 5, size.height / 5, true);

			ByteArrayOutputStream blob = new ByteArrayOutputStream();
			resized.compress(Bitmap.CompressFormat.JPEG, 100, blob);

			return blob.toByteArray();
		}

		public void sharingHasStarted() {
			sharingStarted = true;
		}

		public void sharingHasStopepd() {
			sharingStarted = false;
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
				mInviteListTask = new InviteListTask(activity);
				mInviteListTask.execute((Void) null);
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
			}

		} else {
			String message = activity.getString(R.string.camera_not_started);
			UtilsWhatAmIdoing.displayGenericMessageDialog(activity, message);
		}

		return false;
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
			FacebookPostTask fpt = new FacebookPostTask(url,
					activity,facebookSession);
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
		
		
		//Registering the receiver of chat messages
		chatMessageReceiverFilter = new IntentFilter(
				ChatMessageReceiver.MESSAGE_RECIEVED);
		chatMessageReceiverFilter
				.addCategory(Intent.CATEGORY_DEFAULT);
		chatMessageReceiver = new ChatMessageReceiver(this);
		registerReceiver(chatMessageReceiver,
				chatMessageReceiverFilter);
		
		chatEnabledReceiverFilter = new IntentFilter(
				ChatEnabledReceiver.CHAT_ENABLED_RECEIVER);
		chatEnabledReceiverFilter
				.addCategory(Intent.CATEGORY_DEFAULT);
		chatEnabledReceiver = new ChatEnabledReceiver(this);
		registerReceiver(chatEnabledReceiver,
				chatEnabledReceiverFilter);
		
		
		chatParticipantReceiverFilter = new IntentFilter(
				ChatParticipantReceiver.CHAT_PARTICIPANT_RECEIVER);
		chatParticipantReceiverFilter
				.addCategory(Intent.CATEGORY_DEFAULT);
		chatParticipantReceiver = new ChatParticipantReceiver(this);
		registerReceiver(chatParticipantReceiver,
				chatParticipantReceiverFilter);
		
		Intent msgIntent = new Intent(
				this,
				com.watamidoing.xmpp.service.XMPPService.class);
	  if (WhatAmIdoing.isServiceRunning(XMPPService.class.getName())) {
		  Log.i(TAG,"--------STOPPIN SERVICE");
		  this.stopService(msgIntent);
	  }
		Log.i(TAG,"--------STARTING SERVICE");
		this.startService(msgIntent);

		
	}
	
	public void doBindChatService() {
		// Establish a connection with the service. We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		getApplicationContext()
				.bindService(
						new Intent(
								getApplicationContext(),
								com.watamidoing.xmpp.service.XMPPService.class),
						xmppServiceConnection, Context.BIND_AUTO_CREATE);
		chatServiceIsBound = true;
	
		Log.d("WhatAmidoingCamera.doBindService", "binding to service");

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
			chatDialogFragment = ChatDialogFragment.newInstance(
					true, activity,messenger);
			chatDialogFragment.show(fm, "chat_dialog_fragment");
			
			String oldMessage = chatMessageQueue.poll();
			while(oldMessage != null) {
				chatDialogFragment.addMessage(oldMessage);
				oldMessage = chatMessageQueue.poll();
			}
			
			oldMessage = chatParticipantQueue.poll();
			while(oldMessage != null) {
				chatDialogFragment.addParticipant(oldMessage);
				oldMessage = chatParticipantQueue.poll();
			}
			
			
		}
		
	}

	@Override
	public void messageReceived(String chatMessage) {
		
		if (chatDialogFragment == null) {
			chatMessageQueue.add(chatMessage);
		} else {
			chatDialogFragment.addMessage(chatMessage);
		}
	}

	@Override
	public void enableChat(boolean enable) {
		
		if (enable) {
			doBindChatService();
		}
		// TODO Auto-generated method stub
		
	}

	@Override
	public void newParticipant(String participant) {
		if (chatDialogFragment == null) {
			chatParticipantQueue.add(participant);
		} else {
			chatDialogFragment.addParticipant(participant);
		}
		
	}
	
	public void stopChatService() {
		
		

		//Registering the receiver of chat messages
		try {
			if (chatMessageReceiver == null) {
				unregisterReceiver(chatMessageReceiver);
			}
		} catch (IllegalArgumentException e) {
			chatMessageReceiver = null;
		}
		
		try {
			if (chatEnabledReceiver == null) {
				unregisterReceiver(chatEnabledReceiver);
			}
		} catch (IllegalArgumentException e) {
			chatEnabledReceiver = null;
		}
		
		try {
			if (chatParticipantReceiver == null) {
				unregisterReceiver(chatParticipantReceiver);
			}
		} catch (IllegalArgumentException e) {
			chatParticipantReceiver = null;
		}
		doUnbindChatService();
		Intent msgIntent = new Intent(activity,
				com.watamidoing.xmpp.service.XMPPService.class);
		stopService(msgIntent);
		
	}
}
