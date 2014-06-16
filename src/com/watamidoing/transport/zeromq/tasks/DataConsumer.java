package com.watamidoing.transport.zeromq.tasks;

import java.util.concurrent.LinkedBlockingQueue;

import android.util.Log;

public class DataConsumer<T> extends LinkedBlockingQueue<T> {

    private static final long serialVersionUID = 1L;
	private static final String TAG = "DataConsumer";
    private boolean done = false;

    public DataConsumer(int capacity) {  
    		super(capacity); 
    	}

    public void done() { 
    		done = true; 
    	}

    public boolean isDone() { 
    		return done; 
    	}

    /**
     * May return null if producer ends the production after consumer 
     * has entered the element-await state.
     */
    public T take() throws InterruptedException {
        T el;
        while ((el = super.poll()) == null && !done) {
            synchronized (this) {
            	Log.d(TAG,"---------------------WAITING----------");
                wait();
                Log.d(TAG,"---------------------WOKEN UP---------------");
            }
        }

        return el;
    }
}
