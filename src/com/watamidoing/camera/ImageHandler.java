package com.watamidoing.camera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import com.waid.R;
import com.watamidoing.transport.service.ZeroMQService;
import com.watamidoing.zeromq.tasks.ZeroMQTransportTask;

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

	private Bitmap oldImage;

	long start_time =0;
	double difference =0;
	private int count;
	private int imageCount;
	private int frameCounter = 0;

	public ImageHandler(Messenger mService, Context context) {
		this.mService = mService;
		this.context = context;
		rs =  RenderScript.create(context);
		count = 0;
	}

	public Bitmap showDifference(Bitmap im1, Bitmap im2)
	{
		Bitmap resultImage =  Bitmap.createBitmap(im1.getWidth(), im1.getHeight(), Bitmap.Config.ARGB_8888);


		double THR = 50;
		int area = 0;
		for(int h=0; h < im1.getHeight(); h++)
		{
			for(int w=0; w < im1.getWidth(); w++)
			{
				int pix1=0;
				int alpha1 = 0xff &(im1.getPixel(w, h)>>24);
				int red1 = 0xff &(im1.getPixel(w, h)>>16);
				int green1 = 0xff & (im1.getPixel(w, h)>>8);
				int blue1 = 0xff & im1.getPixel(w, h);  

				int pix2=0;
				int alpha2 = 0xff &(im2.getPixel(w, h)>>24);
				int red2 = 0xff &(im2.getPixel(w, h)>>16);
				int green2 = 0xff & (im2.getPixel(w, h)>>8);
				int blue2 = 0xff & im2.getPixel(w, h);  

				//euclidian distance to estimate the simil.
				double dist =0;
				dist = Math.sqrt(Math.pow((double)(red1-red2), 2.0) 
						+Math.pow((double)(green1-green2), 2.0)
						+Math.pow((double)(blue1-blue2), 2.0) );
				if(dist >THR)
				{
					resultImage.setPixel(w, h, im2.getPixel(w, h));
					area++;
				}
				else
				{
					resultImage.setPixel(w, h, 0);
				}
			}
		} 
		return resultImage;
	}


	public void saveBitmap(Bitmap bitmap) {


		File sd = Environment.getExternalStorageDirectory();
		File file = new File(sd,"disp"+imageCount+".jpg");

		OutputStream fOut = null;
		try {
			fOut = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
			fOut.flush();
			fOut.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


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

			byte[] byteframe = msg.getData().getByteArray("frame");
			int timeStamp = msg.getData().getInt("timeStamp");

			// original measurements
			byte[] frame = new byte[0];
			frame = byteframe;	

			//byte[] frame = jpegOutputStream.toByteArray();

			String res = Base64.encodeToString(frame, Base64.DEFAULT);
			Log.d(TAG,"SIZE BEFORE ENCODING:"+res.length());
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

			byte[] jdata = rstBao.toByteArray();

			Message msgObj = Message.obtain(null,
					ZeroMQService.PUSH_MESSAGE_TO_QUEUE);
			Bundle bundle = new Bundle();
			
			String frame64 = Base64.encodeToString(jdata,
					Base64.DEFAULT)+":"+timeStamp;
			bundle.putString("frame",frame64);
			bundle.putInt("timeStamp", timeStamp);
			msgObj.setData(bundle);
			try {
				if (mService != null){
					
					//task.sendMessge(res);
					mService.send(msgObj);
					Log.d(TAG,"SENT:"+res.length());
				}
			
				rstBao = null;
				jdata = null;
				msgObj = null;
				bundle = null;
				frame64 = null;
				frame=null;
				byteframe=null;
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
