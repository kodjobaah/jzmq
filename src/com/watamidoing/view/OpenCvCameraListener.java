package com.watamidoing.view;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.LinkedBlockingQueue;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.NativeCameraView;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.watamidoing.camera.ImageHandler;

public class OpenCvCameraListener implements CvCameraViewListener2 {

	public static final String TAG = "OpencCvCameraListener";

	private ImageHandler imageHandler;
	private Messenger mMessenger;
	private LinkedBlockingQueue<CameraViewData> dataQueue;
	private NativeCameraView cameraView;

	public OpenCvCameraListener() {
		dataQueue = new LinkedBlockingQueue<CameraViewData>();
		FrameConsumer consumer = new FrameConsumer();
		new Thread(consumer).start();
	}

	public void createMessengerService(Messenger mService, Context _context) {

		imageHandler = new ImageHandler(mService, _context);
		mMessenger = new Messenger(imageHandler);

	}

	class FrameConsumer implements Runnable {
		int fps = 4;
		private int compressQuality = 100;

		public void run() {
			try {
				while (true) {
					consume(dataQueue.take());
				}
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}

		void consume(CameraViewData cameraViewData) {
			Mat rgb = cameraViewData.getMat();

			if ((rgb.height() > 0) && (rgb.width() > 0)) {
				//Log.i(TAG, "FPS:" + fps);
				Message msgObj = Message.obtain(imageHandler,
						ImageHandler.PUSH_MESSAGE_TO_QUEUE);
				Bundle b = new Bundle();

				double fps = cameraView.getCurrentFps() == 0.0 ? 7.0
						: cameraView.getCurrentFps();
				int timeStamp = (int) (1000 / fps);
				Size dsize = new Size(352, 288);
				//Log.i(TAG,
				//		"--rgb-dims:" + rgb.dims() + ":height=" + rgb.height()
				//				+ ":width=" + rgb.width() + ":fps=" + fps);
				Mat rgbResize = rgb.clone();
				Imgproc.resize(rgb, rgbResize, dsize);
				rgb = null;

				//Log.i(TAG, "RESIZE--rgb-dims:" + rgbResize.dims() + ":height="
				//		+ rgbResize.height() + ":width=" + rgbResize.width()
				//		+ ":timestamp=" + timeStamp);
				Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf
																// types
				Bitmap bmp = Bitmap.createBitmap(352, 288, conf);
				Utils.matToBitmap(rgbResize, bmp);
				rgbResize = null;

				ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();
				bmp.compress(CompressFormat.JPEG, compressQuality,
						jpegOutputStream);
				bmp = null;

				if (jpegOutputStream.toByteArray().length < 17000) {
					if (jpegOutputStream.toByteArray().length < 8000) {
						compressQuality = compressQuality + 2;
					}

					Log.i(TAG, "current fps:" + cameraView.getCurrentFps());
					b.putByteArray("frame", jpegOutputStream.toByteArray());
					b.putInt("timeStamp", timeStamp);
					msgObj.setData(b);

					try {

						if (mMessenger != null) {
							mMessenger.send(msgObj);
							jpegOutputStream = null;
							b = null;
							msgObj = null;

						} else {
							Log.i(TAG, "Not transmitting messenger null");
						}
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				} else {
					compressQuality = compressQuality - 2;
				}

			}
		}
	}

	@Override
	public void onCameraViewStarted(int width, int height) {

	}

	@Override
	public void onCameraViewStopped() {
		// TODO Auto-generated method stub

	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

		CameraViewData cvd = new CameraViewData();

		Mat rgb = inputFrame.rgba();
		if ((rgb != null) && (rgb.width() > 0) && (rgb.height() > 0)) {
			cvd.setMat(rgb);
			dataQueue.add(cvd);
		}
		return rgb;
	}

	public void setCameraView(NativeCameraView cameraView) {
		this.cameraView = cameraView;

	}

	public void disableMessengerService() {

		if (imageHandler != null) {
			boolean hasMessages = imageHandler
					.hasMessages(ImageHandler.PUSH_MESSAGE_TO_QUEUE);
			if (hasMessages) {
				Log.d(TAG, "Messages on queue removing:");
				imageHandler.removeMessages(ImageHandler.PUSH_MESSAGE_TO_QUEUE);
			}
		}
		imageHandler = null;
		mMessenger = null;

	}

}
