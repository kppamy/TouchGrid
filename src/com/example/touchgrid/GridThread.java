package com.example.touchgrid;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GridThread extends Thread {
	private SurfaceHolder sh;
	private TouchGridView view;

	private Canvas canvas;

	private boolean run = false;

	public GridThread(SurfaceHolder _holder, TouchGridView _view) {
	    sh = _holder;
	    view = _view;
	}

	public void setRunnable(boolean _run) {
	    run = _run;
	}

	public void run() {
	    while(run) {
	        canvas = null;
	        try {
	            canvas = sh.lockCanvas(null);
	            synchronized(sh) {
	                //view.onDraw(canvas);
	            }
	        } finally {
	            if(canvas != null) {
	                sh.unlockCanvasAndPost(canvas);
	            }
	        }
	    }
	}

	public Canvas getCanvas() {
	    if(canvas != null) {
	        return canvas;
	    } else {
	        return null;
	    }
	}
}
