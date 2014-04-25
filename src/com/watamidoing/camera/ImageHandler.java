package com.watamidoing.camera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import com.watamidoing.transport.service.WebsocketService;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicYuvToRGB;
import android.support.v8.renderscript.Type;

public class ImageHandler extends Handler {

	private String TAG = "ImageHandler";

	private int compressionQuality = 70;

	public static final int PUSH_MESSAGE_TO_QUEUE = 23456789;
	private Messenger mService;

	private Context context;

	private RenderScript rs;

	private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;

	long start_time =0;
	double difference =0;
	private int count;
	public ImageHandler() {

	}
	public ImageHandler(Messenger mService, Context context) {
		this.mService = mService;
		this.context = context;
		rs =  RenderScript.create(context);
		count = 0;
	}

	public Bitmap convertYuvToBitmap(byte[] yuv,int w, int h) {




		Element ele = Element.U8_4(rs);

		yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, ele);


		Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(yuv.length);
		Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

		Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(w).setY(h);
		Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

		in.copyFrom(yuv);

		Bitmap bitmap =  Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

		yuvToRgbIntrinsic.setInput(in);
		yuvToRgbIntrinsic.forEach(out);

		out.copyTo(bitmap);
		return bitmap;
	}

	public Bitmap scaleBitMap(Bitmap bitmap) {
		final int MAX_DIM = 320;

		int decodeWidth = bitmap.getWidth();
		int decodeHeight = bitmap.getHeight();

		while (decodeWidth > MAX_DIM || decodeHeight > MAX_DIM) {
			decodeWidth >>= 1;
			decodeHeight >>= 1;
		}

		bitmap = Bitmap.createScaledBitmap(bitmap, decodeWidth, decodeHeight, false);

		return bitmap;
	}

	@Override
	public void handleMessage(Message msg) {
		switch (msg.what) {
		case PUSH_MESSAGE_TO_QUEUE:


			if (start_time == 0)
				start_time = System.nanoTime();
			else {
				long end_time = System.nanoTime();
				difference = (end_time - start_time)/1e6;
				start_time = end_time;
			}

			Log.i(TAG,"This is how long it takes to process each frame["+difference+"]");
			long measure_start = System.nanoTime();
			byte[] uncompressedFrame = msg.getData().getByteArray("frame");
			int previewFormat = msg.getData().getInt("previewFormat");
			int width = msg.getData().getInt("width");
			int height = msg.getData().getInt("height");
			int imageWidth = msg.getData().getInt("imageWidth");
			int imageHeight = msg.getData().getInt("imageHeight");

			YuvImage image = new YuvImage(uncompressedFrame,
					previewFormat, imageWidth, imageHeight,
					null);


			// Put the camera preview frame right into the yuvIplimage
			// object
			// yuvIplimage.getByteBuffer().put(data);
			android.graphics.Rect previewRect = new android.graphics.Rect(
					0, 0, imageWidth, imageHeight);
			ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();
			image.compressToJpeg(previewRect,100, jpegOutputStream);

			Bitmap b = BitmapFactory.decodeByteArray(jpegOutputStream.toByteArray(),0,jpegOutputStream.toByteArray().length);

			// original measurements
			int origWidth = b.getWidth();
			int origHeight = b.getHeight();

			final int destWidth = 352;//or the width you need

			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			byte[] frame = new byte[0];
			if(origWidth > destWidth){
				// picture is wider than we want it, we calculate its target height
				// int destHeight = origHeight/( origWidth / destWidth ) ;
				int destHeight = 288;
				// we create an scaled bitmap so it reduces the image, not just trim it
				Bitmap b2 = Bitmap.createScaledBitmap(b, destWidth, destHeight, false);

				// compress to the format you want, JPEG, PNG... 
				// 70 is the 0-100 quality percentage
				b2.compress(Bitmap.CompressFormat.JPEG,compressionQuality , outStream);
				/*
			    if(count < 4) {
			    			File sdCard = Environment.getExternalStorageDirectory();
							File dir = new File (sdCard.getAbsolutePath() + "/waid");
							dir.mkdirs();
							File file = new File(dir, "filename"+count+".jpg");
							try {
			    FileOutputStream fo = new FileOutputStream(file);
			    fo.write(outStream.toByteArray());
			    fo.close();
							} catch (Exception e) {
								e.printStackTrace();
							}
			    		}
				 */
				frame = outStream.toByteArray();
			} else {
				frame = jpegOutputStream.toByteArray();	
			}

			//byte[] frame = jpegOutputStream.toByteArray();

			String res = Base64.encodeToString(frame, Base64.DEFAULT);
			Log.d(TAG,"SIZE BEFORE ENCODING:"+res.length());
			if (res.length() > 8000) {
				compressionQuality = compressionQuality - 2; 
			} 


			if (res.length() < 17000) {
				ByteArrayOutputStream rstBao = new ByteArrayOutputStream();

				GZIPOutputStream zos = null;
				try {
					zos = new GZIPOutputStream(rstBao);
					zos.write(res.getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						zos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}


				long measure_end = System.nanoTime();

				double diff = (measure_end - measure_start)/1e6;
				Log.i(TAG,"This how long it took to shrink["+diff+"]");
				byte[] jdata = rstBao.toByteArray();

				Message msgObj = Message.obtain(null,
						WebsocketService.PUSH_MESSAGE_TO_QUEUE);
				Bundle bundle = new Bundle();
				bundle.putByteArray("frame", jdata);
				msgObj.setData(bundle);
				try {
					if (mService != null)
						mService.send(msgObj);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				Log.i(TAG, "--DROPING FILE");
			}
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			break;
		default:
			super.handleMessage(msg);
		}
	}

	public void setMessageService(Messenger mService) {
		this.mService = mService;
	}

}