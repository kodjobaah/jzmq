package com.watamidoing.view;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.YuvImage;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
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
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.watamidoing.R;
import com.watamidoing.contentproviders.Authentication;
import com.watamidoing.contentproviders.DatabaseHandler;
import com.watamidoing.tasks.GetInvitedTask;
import com.watamidoing.tasks.InviteListTask;
import com.watamidoing.tasks.callbacks.WebsocketController;
import com.watamidoing.transport.receivers.NetworkChangeReceiver;
import com.watamidoing.transport.receivers.NotAbleToConnectReceiver;
import com.watamidoing.transport.receivers.ServiceConnectionCloseReceiver;
import com.watamidoing.transport.receivers.ServiceStartedReceiver;
import com.watamidoing.transport.receivers.ServiceStoppedReceiver;
import com.watamidoing.transport.service.WebsocketService;
import com.watamidoing.transport.service.WebsocketServiceConnection;
import com.watamidoing.utils.ConnectionResult;
import com.watamidoing.utils.HttpConnectionHelper;
import com.watamidoing.utils.ScreenDimension;
import com.watamidoing.utils.UtilsWhatAmIdoing;

public class WhatAmIdoing extends Activity implements WebsocketController {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8274547292305156402L;
	private static final String START_CAMERA = "Start Camera";
	private static final String STOP_CAMERA = "Stop Camera";
	private static final String STOP_SHARING = "Stop Sharing";
	private static final String START_SHARING = "Start Sharing";
	private static final String LOG_TAG = "whatamidoing.oncreate";
	private InviteListTask mInviteListTask;
	private PowerManager.WakeLock mWakeLock;	
	private volatile FFmpegFrameRecorder recorder;
	private boolean recording = false;
	private long startTime = 0;

	private boolean videoStart = false;
	private boolean videoSharing = false;
	private int sampleAudioRateInHz = 44100;
	private int imageWidth = 320;
	private int imageHeight = 240;
	private int frameRate = 30;
	private CameraView cameraView;
	private IplImage yuvIplimage = null;
	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection;
	protected ServiceStartedReceiver serviceStartedReceiver;
	protected ServiceStoppedReceiver serviceStoppedReceiver;
	protected NetworkChangeReceiver networkChangeReceiver;
	protected ServiceConnectionCloseReceiver serviceConnectionCloseReceiver;

	protected boolean sharing = false;
	private boolean previewRunning = false;
	private int cameraId;
	private LinearLayout mainLayout;
	private NotAbleToConnectReceiver notAbleToConnectReceiver;
	private WhatAmIdoing activity;

	/** Messenger for communicating with service. */
	private Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;


	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			default:
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	public Camera camera;
	protected GetInvitedTask mInvitedTaskListView;



	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mConnection = new WebsocketServiceConnection(this, this);
		setContentView(R.layout.options);
		activity = this;

		// Setting up camera selection
		int cameraCount = 0;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		cameraCount = Camera.getNumberOfCameras();
		List<String> list = new ArrayList<String>();
		boolean foundFront  = false;
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
				backCameraId= camIdx;

			}
		}

		if (foundFront && foundBack ) {
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
		cameraSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			int iCurrentSelection = cameraSelector.getSelectedItemPosition();
			public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) { 
				if (iCurrentSelection != i){
					Log.d("WhatAmIdoing.cameralSelector.onItemSelected","items selected index["+i+"]");
					cameraId = i;
					if (videoStart) {
						camera.stopPreview();
						previewRunning = false;
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


		// Setting up the share momements button
		final Button button = (Button) findViewById(R.id.inviteButton);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				final Button startVideo = (Button) activity
						.findViewById(R.id.start_video);
				if (videoStart) {
					mInviteListTask = new InviteListTask(activity);
					mInviteListTask.execute((Void) null);
				} else {
					String message = activity.getString(R.string.video_not_started);
					UtilsWhatAmIdoing.displayGenericMessageDialog(activity, message);
				}
	
			}
		});
		
		final Button shareButton = (Button) findViewById(R.id.viewSharers);
		shareButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				final Button startVideo = (Button) activity
						.findViewById(R.id.start_video);
				if (videoStart) {
					mInvitedTaskListView = new GetInvitedTask(activity);
					mInvitedTaskListView.execute((Void) null);	
				} else {
					String message = activity.getString(R.string.video_not_started_view_sharers);
					UtilsWhatAmIdoing.displayGenericMessageDialog(activity, message);
				}
				
			}
		});
		
		initializeButtons();

		mainLayout = (LinearLayout) this.findViewById(R.id.momemts_frame);
		ScreenDimension dimension = UtilsWhatAmIdoing.getScreenDimensions(activity);
		ViewGroup.LayoutParams params = mainLayout.getLayoutParams();

		// change width of the params e.g. 50dp
		params.width = Double.valueOf(dimension.getDpHeightPixels()*0.8).intValue();
		params.height = Double.valueOf(dimension.getDpWidthPixels()*0.5).intValue();
		// initialize new parameters for my element
		// mainLayout.setLayoutParams(new GridLayout.LayoutParams(params));

		//Making buttons equal width  

		GridLayout gl = (GridLayout) this.findViewById(R.id.video_display);
		gl.requestLayout();  

	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
	 
		if (videoSharing) {
			doUnbindService();
		}
		
		if (videoStart) {
			camera.stopPreview();
			previewRunning = false;
			mainLayout.removeAllViews();
			cameraView = null;
			videoStart = false;
			camera = null;
		}
		
		 Authentication auth =  DatabaseHandler.getInstance(this).getDefaultAuthentication();
		 String invalidateTokenUrl = activity.getString(R.string.invalidate_token_url);
		 HttpConnectionHelper connectionHelper = new HttpConnectionHelper();
			try {
				String urlVal = invalidateTokenUrl+"?token="+auth.getToken();
				ConnectionResult connectionResult = connectionHelper.connect(urlVal);
			
				if ((connectionResult != null) && (connectionResult.getStatusCode() ==  HttpURLConnection.HTTP_OK)) {
					 Log.d("WhatAmIDoing.onSaveInstanceState","results from invalidate_token["+connectionResult.getResult()+"]");
				}
			
			} finally {
				connectionHelper.closeConnection();
			}
	    // Always call the superclass so it can save the view hierarchy state
	    super.onSaveInstanceState(savedInstanceState);
	}
	/**
	 * Used to start video
	 */
	public void startVideo(View view) {

		runOnUiThread(new Thread(new Runnable() {
			public void run() {

				Button startVideo = (Button) activity
						.findViewById(R.id.start_video);
				String text = startVideo.getText().toString();

				final Button transmissionButton = (Button) activity.findViewById(R.id.start_transmission);

				if (START_CAMERA.equalsIgnoreCase(text)) {
					transmissionButton.setEnabled(true);
					startVideo.setText(STOP_CAMERA);
					startCamera();
					videoStart = true;

				} else {
					camera.stopPreview();
					previewRunning = false;
					mainLayout.removeAllViews();
					startVideo.setText(START_CAMERA);
					cameraView = null;
					videoStart = false;
					camera = null;
					//NativeMethods.closeWebsocketConnection();
					//transmissionButton.setText(START_SHARING);
					//initializeButtons();
				}

			}
		}));

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

				Button startTransmission = (Button) activity.findViewById(R.id.start_transmission);

				final Button startVideoButton = (Button) activity.findViewById(R.id.start_video);
				String text = startTransmission.getText().toString();

				Intent msgIntent = new Intent(activity,com.watamidoing.transport.service.WebsocketService.class);

				if (START_SHARING.equalsIgnoreCase(text)) {
					startTransmission.setEnabled(true);
					startService(msgIntent);

					if (filterNotAbleToConnect != null) {
						filterNotAbleToConnect = new IntentFilter(NotAbleToConnectReceiver.NOT_ABLE_TO_CONNECT);
						filterNotAbleToConnect.addCategory(Intent.CATEGORY_DEFAULT);
						notAbleToConnectReceiver = new NotAbleToConnectReceiver(activity);
						registerReceiver(notAbleToConnectReceiver, filterNotAbleToConnect);
					}

					if (filterServiceStartedReciever == null) {
						filterServiceStartedReciever = new IntentFilter(ServiceStartedReceiver.SERVICE_STARTED);
						filterServiceStartedReciever.addCategory(Intent.CATEGORY_DEFAULT);
						serviceStartedReceiver = new ServiceStartedReceiver(activity);
						registerReceiver(serviceStartedReceiver, filterServiceStartedReciever);
					}

					if (filterServiceStoppedReceiver == null) {
						filterServiceStoppedReceiver = new IntentFilter(ServiceStoppedReceiver.SERVICE_STOPED);
						filterServiceStoppedReceiver.addCategory(Intent.CATEGORY_DEFAULT);
						serviceStoppedReceiver = new ServiceStoppedReceiver(activity);
						registerReceiver(serviceStoppedReceiver, filterServiceStoppedReceiver);
					}


					if (filterServiceConnectionCloseReceiver == null) {
						filterServiceConnectionCloseReceiver = new IntentFilter(ServiceConnectionCloseReceiver.SERVICE_CONNECTION_CLOSED);
						filterServiceConnectionCloseReceiver.addCategory(Intent.CATEGORY_DEFAULT);
						serviceConnectionCloseReceiver = new ServiceConnectionCloseReceiver(activity);
						registerReceiver(serviceConnectionCloseReceiver, filterServiceConnectionCloseReceiver);
					}


//					networkChangeReceiver = new NetworkChangeReceiver(activity);

					if (filterRegisterReceiver == null) {
						filterRegisterReceiver = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
						filterRegisterReceiver.addCategory(Intent.CATEGORY_DEFAULT);
						registerReceiver(networkChangeReceiver,filterRegisterReceiver);
					}


					/*
					startWebSocketTask = new StartsWebSocketTask(
							webSocketController, activity);
					startWebSocketTask.execute((Void) null);
					 */
					startTransmission.setText(STOP_SHARING);
					doBindService();
				} else {
					videoSharing = false;
					stopService(msgIntent);
					doUnbindService();
					startTransmission.setText(START_SHARING);
				}

			}
		}));

	}

	private void initializeButtons() {

		runOnUiThread(new Thread(new Runnable() {
			public void run() {

				final Button startTransmissionButton = (Button) activity
						.findViewById(R.id.start_transmission);
				startTransmissionButton.setEnabled(true);

				final Button inviteButton = (Button) activity
						.findViewById(R.id.inviteButton);
				inviteButton.setEnabled(false);

				final Button startVideo = (Button) activity
						.findViewById(R.id.start_video);
				startVideo.setEnabled(true);

				final Button viewSharers = (Button) activity
						.findViewById(R.id.viewSharers);
				viewSharers.setEnabled(false);
				
				videoStart = false;
				videoSharing = false;
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

				final Button startVideoButton = (Button) activity
						.findViewById(R.id.start_video);
				final Button startTransmissionButton = (Button) activity
						.findViewById(R.id.start_transmission);
				if (results) {

					startTransmissionButton.setText(STOP_SHARING);
					startTransmissionButton.setEnabled(true);
					Button shareMomement = (Button) activity.findViewById(R.id.inviteButton);
					shareMomement.setEnabled(true);

					Button viewSharers = (Button) activity.findViewById(R.id.viewSharers);
					viewSharers.setEnabled(true);
					videoSharing = true;
					if (cameraView != null ) {
						cameraView.sharingHasStarted();
					}

				}
			}
		}));

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

		final Button inviteButton = (Button) activity
				.findViewById(R.id.inviteButton);
		inviteButton.setWidth(width);

		final Button startVideo = (Button) activity
				.findViewById(R.id.start_video);
		startVideo.setWidth(width);

		gl.requestLayout();  
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();

	}

	@Override
	public void websocketProblems(final boolean result) {

		runOnUiThread(new Thread(new Runnable() {
			public void run() {
				Button startVideo = (Button) activity
						.findViewById(R.id.start_video);

				final Button transmissionButton = (Button) activity
						.findViewById(R.id.start_transmission);
				if (result) {
					videoSharing = false;
					Button shareMomement = (Button) activity.findViewById(R.id.inviteButton);
					shareMomement.setEnabled(false);
					Button viewSharers = (Button) activity.findViewById(R.id.viewSharers);
					viewSharers.setEnabled(false);

					doUnbindService();
					startVideo.setText(START_CAMERA);
					startVideo.setEnabled(false);
					transmissionButton.setText(START_SHARING);				
					final Button startTransmissionButton = (Button) activity
							.findViewById(R.id.start_transmission);
					startTransmissionButton.setEnabled(true);
					UtilsWhatAmIdoing.displayWebsocketProblemsDialog(activity);
					if (cameraView != null ) {
						cameraView.sharingHasStopepd();
					}

				}

			}
		}));

	}

	@Override
	public void websocketServiceStop(final boolean serviceStopped) {
		runOnUiThread(new Thread(new Runnable() {
			public void run() {
				Button startVideo = (Button) activity
						.findViewById(R.id.start_video);

				final Button transmissionButton = (Button) activity
						.findViewById(R.id.start_transmission);
				if (serviceStopped) {
					videoSharing = false;
					Button shareMomement = (Button) activity.findViewById(R.id.inviteButton);
					shareMomement.setEnabled(false);
					Button viewSharers = (Button) activity.findViewById(R.id.viewSharers);
					viewSharers.setEnabled(false);
					doUnbindService();
					transmissionButton.setText(START_SHARING);
					if (cameraView != null ) {
						cameraView.sharingHasStopepd();
					}
					UtilsWhatAmIdoing.displayWebsocketServiceStoppedDialog(activity);

				}

			}
		}));

	}

	@Override
	public void websocketServiceConnectionClose(final boolean connectionClose) {
		runOnUiThread(new Thread(new Runnable() {
			public void run() {
				Button startVideo = (Button) activity
						.findViewById(R.id.start_video);

				final Button transmissionButton = (Button) activity
						.findViewById(R.id.start_transmission);
				if (connectionClose) {
					videoSharing = false;
					Button shareMomement = (Button) activity.findViewById(R.id.inviteButton);
					shareMomement.setEnabled(false);
					Button viewSharers = (Button) activity.findViewById(R.id.viewSharers);
					viewSharers.setEnabled(false);

					doUnbindService();
					transmissionButton.setText(START_SHARING);
					if (cameraView != null ) {
						cameraView.sharingHasStopepd();
					}
					UtilsWhatAmIdoing.displayWebsocketServiceConnectionClosedDialog(activity);

				}

			}
		}));

	}

	@Override
	public void networkStatusChange(boolean available) {

		/*
		if (!available && sharing) { 
			Toast.makeText(getApplicationContext(), "Network connectivity Lost -- stopping sharing", Toast.LENGTH_LONG).show();
			Intent msgIntent = new Intent(activity,com.watamidoing.transport.service.WebsocketService.class);
			stopService(msgIntent);
			Button startTransmission = (Button) activity.findViewById(R.id.start_transmission);
			startTransmission.setText(START_SHARING);
			doUnbindService();
			initializeButtons();

		} else if (!sharing && available){
			Toast.makeText(getApplicationContext(), "Network available", Toast.LENGTH_SHORT).show();
			initializeButtons();
		}
		*/
	}


	public void doBindService() {
		// Establish a connection with the service.  We use an explicit
		// class name because there is no reason to be able to let other
		// applications replace our component.
		getApplicationContext().bindService(new Intent(getApplicationContext(),com.watamidoing.transport.service.WebsocketService.class), mConnection, Context.BIND_AUTO_CREATE);
		mIsBound = true;
		Log.d("WhatAmidoingCamera.doBindService","binding to service");

	}

	public void doUnbindService() {
		if (mIsBound) {
			// If we have received the service, and hence registered with
			// it, then now is the time to unregister.
			// Detach our existing connection.
			if(mConnection != null) {
				getApplicationContext().unbindService(mConnection);
				mIsBound = false;
				mService = null;
			}

		}
	}

	@Override
	public void setMessengerService(Messenger mService) {
		this.mService = mService;
		Toast.makeText(getApplicationContext(), "sharing",Toast.LENGTH_LONG ).show();

	}

	@Override
	public void sendFrame(String res) {
		// TODO Auto-generated method stub

	}

	private void startCamera() {

		if (cameraView == null) {
			/*
			 * Changing the size of the screen
			 */
			ScreenDimension dimension = UtilsWhatAmIdoing.getScreenDimensions(activity);
			// get layout parameters for that element
			ViewGroup.LayoutParams params = mainLayout.getLayoutParams();

			// change width of the params e.g. 50dp
			params.width = Double.valueOf(dimension.getDpHeightPixels()*0.8).intValue();
			params.height = Double.valueOf(dimension.getDpWidthPixels()*0.5).intValue();

			imageHeight =params.height;
			imageWidth = params.width;

			cameraView = new CameraView(activity);
			LinearLayout.LayoutParams layoutParam = new LinearLayout.LayoutParams(imageWidth, imageHeight);        
			mainLayout.addView(cameraView, layoutParam);
		}


		if (videoSharing) {
			cameraView.sharingHasStarted();
		}
	} 


	class CameraView extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback {



		private SurfaceHolder holder;

		private byte[] previewBuffer;

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
			camera = Camera.open(cameraId);

			try {
				camera.setPreviewDisplay(holder);
				camera.setPreviewCallback(this);

				Camera.Parameters currentParams = camera.getParameters();
				Log.v(LOG_TAG,"Preview Framerate: " + currentParams.getPreviewFrameRate());
				Log.v(LOG_TAG,"Preview imageWidth: " + currentParams.getPreviewSize().width + " imageHeight: " + currentParams.getPreviewSize().height);

				// Use these values
				imageWidth = currentParams.getPreviewSize().width;
				imageHeight = currentParams.getPreviewSize().height;
				frameRate = currentParams.getPreviewFrameRate();				

				bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ALPHA_8);


				/*
				Log.v(LOG_TAG,"Creating previewBuffer size: " + imageWidth * imageHeight * ImageFormat.getBitsPerPixel(currentParams.getPreviewFormat())/8);
	        	previewBuffer = new byte[imageWidth * imageHeight * ImageFormat.getBitsPerPixel(currentParams.getPreviewFormat())/8];
				camera.addCallbackBuffer(previewBuffer);
	            camera.setPreviewCallbackWithBuffer(this);
				 */				
				camera.startPreview();
				previewRunning = true;

			}
			catch (IOException e) {
				Log.v(LOG_TAG,e.getMessage());
				e.printStackTrace();
			}	
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			Log.v(LOG_TAG,"Surface Changed: width " + width + " height: " + height);

			// We would do this if we want to reset the camera parameters
			/*
            if (!recording) {
    			if (previewRunning){
    				camera.stopPreview();
    			}

    			try {
    				//Camera.Parameters cameraParameters = camera.getParameters();
    				//p.setPreviewSize(imageWidth, imageHeight);
    			    //p.setPreviewFrameRate(frameRate);
    				//camera.setParameters(cameraParameters);

    				camera.setPreviewDisplay(holder);
    				camera.startPreview();
    				previewRunning = true;
    			}
    			catch (IOException e) {
    				Log.e(LOG_TAG,e.getMessage());
    				e.printStackTrace();
    			}	
    		}            
			 */

			// Get the current parameters
			Camera.Parameters currentParams = camera.getParameters();
			Log.v(LOG_TAG,"Preview Framerate: " + currentParams.getPreviewFrameRate());
			Log.v(LOG_TAG,"Preview imageWidth: " + currentParams.getPreviewSize().width + " imageHeight: " + currentParams.getPreviewSize().height);

			// Use these values
			imageWidth = currentParams.getPreviewSize().width;
			imageHeight = currentParams.getPreviewSize().height;
			frameRate = currentParams.getPreviewFrameRate();

			// Create the yuvIplimage if needed
			yuvIplimage = IplImage.create(imageWidth, imageHeight, IPL_DEPTH_8U, 2);
			//yuvIplimage = IplImage.create(imageWidth, imageHeight, IPL_DEPTH_32S, 2);
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			try {
				camera.setPreviewCallback(null);

				previewRunning = false;
				camera.release();

			} catch (RuntimeException e) {
				Log.v(LOG_TAG,e.getMessage());
				e.printStackTrace();
			}
		}


		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {

			Log.d("WhatAmIDoing.CameraView.onPreview","sharing["+sharingStarted+"]");
			if (sharingStarted) {


				Camera.Parameters parameters = camera.getParameters();
				Size size = parameters.getPreviewSize();
				YuvImage image = new YuvImage(data, parameters.getPreviewFormat(),
						size.width, size.height, null);
				videoTimestamp = 1000 * (System.currentTimeMillis() - startTime);

				// Put the camera preview frame right into the yuvIplimage object
				//yuvIplimage.getByteBuffer().put(data);
				android.graphics.Rect previewRect = new android.graphics.Rect(0, 0, imageWidth, imageHeight);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				image.compressToJpeg(previewRect, 10, baos);

				byte[] jdata = baos.toByteArray();

				Message msgObj = Message.obtain(null, WebsocketService.PUSH_MESSAGE_TO_QUEUE);
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
				Log.d("WhatAmiDoing.onCameraFrame"," transmit bytes["+jdata.length+"]");
				baos = null;
				jdata = null;
			}
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






}
