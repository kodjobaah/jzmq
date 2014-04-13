package com.watamidoing.camera;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import com.watamidoing.transport.service.WebsocketService;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

public class ImageHandler extends Handler {
	
	private String TAG = "ImageHandler";
	
	private int compressionQuality = 70;
	
	public static final int PUSH_MESSAGE_TO_QUEUE = 23456789;
	private Messenger mService;
	public ImageHandler() {
		
	}
	public ImageHandler(Messenger mService) {
		this.mService = mService;
	}
	@Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case PUSH_MESSAGE_TO_QUEUE:
            	


        		byte[] uncompressedFrame = msg.getData().getByteArray("frame");
			int previewFormat = msg.getData().getInt("previewFormat");
			int width = msg.getData().getInt("width");
			int height = msg.getData().getInt("height");
			int imageWidth = msg.getData().getInt("imageWidth");
			int imageHeight = msg.getData().getInt("imageHeight");
			
        		YuvImage image = new YuvImage(uncompressedFrame,
						previewFormat, width, height,
						null);
        		
			
				// Put the camera preview frame right into the yuvIplimage
				// object
				// yuvIplimage.getByteBuffer().put(data);
				android.graphics.Rect previewRect = new android.graphics.Rect(
						0, 0, imageWidth, imageHeight);
				ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();
			
				image.compressToJpeg(previewRect,compressionQuality, jpegOutputStream);

				
				byte[] jdata = jpegOutputStream.toByteArray();
				BitmapFactory.Options bitmapFatoryOptions = new BitmapFactory.Options();
	              bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
	              Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length, bitmapFatoryOptions);
		
	              ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
	              
	              bmp.compress(CompressFormat.PNG, 80, pngOutputStream);
	              
				// byte[] jdata = resizeImage(baos.toByteArray());
				byte[] frame = jpegOutputStream.toByteArray();

            		//byte[] frame = msg.getData().getByteArray("frame");
            		
            		String res = Base64.encodeToString(frame, Base64.DEFAULT);
            		Log.i(TAG,"SIZE BEFORE ENCODING:"+res.length());
            		if (res.length() > 10000) {
            			compressionQuality = compressionQuality - 2; 
            		}
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
