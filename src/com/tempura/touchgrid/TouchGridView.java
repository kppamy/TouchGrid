package com.tempura.touchgrid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

public class TouchGridView extends SurfaceView implements SurfaceHolder.Callback {

	private static final String TAG = "TouchGrid:View";
	private static final int MAX_TOUCHPOINTS = 10;
	private static final String START_TEXT = "Touch To Start Test";
	private static final int INVALID_TOUCH_ID = -1;
	
	private GridThread drawThread = null;
	
	private Paint textPaint = new Paint();
	private Paint touchPaints[] = new Paint[MAX_TOUCHPOINTS];
	private int colors[] = new int[MAX_TOUCHPOINTS];
	
	private int width, height;
	private int offset = 20; //!< The size of block
	private float scale = 1.0f;
	private int colSize, rowSize;  //!< The w*h size always in portrait perspective
	
	private int myCanvas_w, myCanvas_h;
	private Bitmap myCanvasBitmap = null;
	private Canvas myCanvas = null;
	private Matrix identityMatrix;	
	
	//! A 2d array holding the status of block
	/**
	 * 		col
	 * row   0    1    2    3    ... w/10
	 *  0
	 *  1
	 *  2
	 *  .
	 *  .
	 *  h/10 
	 */
	private List<List<BlockData>> gridMatrix;
			
	private class BlockData {
		public int touchId; 	//!< The id of touch point (in multitouch case). 0 if not touched
		public Point pos = new Point();  		//!< The actual touch point (x, y) 
		public float pressure;	//!< The pressure applied to the pos
		public int size;  		//!< The size of touch event
		public Point loc = new Point();  		//!< The location in matrix (col, row) to check back
		public boolean bChanged = false;
		
		public BlockData(int col, int row) {
			loc.set(col, row);
			touchId = INVALID_TOUCH_ID;
			pos.set(0, 0);			
			pressure = 0.0f;
			size = 0;
			bChanged = false;
		}		
	}
	
	public TouchGridView(Context context) {
		super(context); 
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		drawThread = new GridThread(getHolder(), this);
		
		setFocusableInTouchMode(true); // make sure we get touch events
		
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		initGrid(display.getWidth(), display.getHeight());		
	}
	
	protected void onDraw(Canvas canvas) {

	    // TODO: Update whatever change to the draw parameters
		// Draw my own's canvasbitmap to the surface's canvas
		synchronized(this) {
			try {
				canvas.drawBitmap(myCanvasBitmap, identityMatrix, null);
			} catch (Exception ex) {
				Log.d(TAG, "Exception: " + ex.getMessage());				
			}
			
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int pointerCount = event.getPointerCount();
		if (pointerCount > MAX_TOUCHPOINTS) {
			pointerCount = MAX_TOUCHPOINTS;
		}
		
		for (int i = 0; i < pointerCount; i++) {
			int id = event.getPointerId(i);
			int x = (int) event.getX(i);
			int y = (int) event.getY(i);
			// TODO: Need to transform the coordinate based on the orientation
			
			// detect which block 
			int col = x/offset;
			int row = y/offset;
			if (row < rowSize && col < colSize) {
				BlockData b = gridMatrix.get(row).get(col);
				if (b != null)
				{
					b.pos.x = x; b.pos.y = y;
					if (b.touchId != id) {
						b.touchId = id;
						b.bChanged = true;
					}
					
					b.pressure = event.getPressure(i);
					b.size = (int)event.getSize(i);
					// Draw just the block to private canvas					
					if (myCanvas != null && b.bChanged) {
						// Draw only changed block to avoid flickering
						myCanvas.drawRect(col*offset, row*offset, (col+1)*offset, (row+1)*offset, touchPaints[b.touchId]);
						b.bChanged = false; 
					}					
				}			
			}
			else {
				Log.d(TAG, "(" + x + ", " + y + ") -> " + "(" + col + ", " + row + ")");
			}				
		}			
		
		return true;
	}	

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		this.width = width;
		this.height = height;		
		if (width > height) { // landscape position
			this.scale = width / 480f;
		} else { // portrait position
			this.scale = height / 480f;		
		}
		Log.d(TAG, "surfaceChanged: w="+this.width+", h="+this.height+", s="+this.scale);
		textPaint.setTextSize(14 * scale);
		Canvas c = getHolder().lockCanvas();
		if (c != null) {
			drawGrid(c, textPaint); 
			getHolder().unlockCanvasAndPost(c);
		}
	}	

	//! Initialize and draw the grid on surface
	private void initGrid(int w, int h) {
		Log.d(TAG, "Initialize the grid matrix");
		// Set the paint colors
		textPaint.setColor(Color.WHITE);
		colors[0] = Color.BLUE;
		colors[1] = Color.RED;
		colors[2] = Color.GREEN;
		colors[3] = Color.YELLOW;
		colors[4] = Color.CYAN;
		colors[5] = Color.MAGENTA;
		colors[6] = Color.DKGRAY;
		colors[7] = Color.WHITE;
		colors[8] = Color.LTGRAY;
		colors[9] = Color.GRAY;
		for (int i = 0; i < MAX_TOUCHPOINTS; i++) {
			touchPaints[i] = new Paint();
			touchPaints[i].setColor(colors[i]);
			touchPaints[i].setStrokeWidth(2);
			touchPaints[i].setAntiAlias(true);
			touchPaints[i].setDither(true);
			touchPaints[i].setStyle(Paint.Style.STROKE);
			touchPaints[i].setStrokeJoin(Paint.Join.ROUND);
			touchPaints[i].setStrokeCap(Paint.Cap.ROUND);
		}		
		
		// Initialize the grid matrix in accordance to w and h (assume landscape mode)
		// This should be done only once
		if (w > h) { // landscape
			rowSize = w/offset;
			colSize = h/offset;
		} else { // portrait
			rowSize = h/offset;
			colSize = w/offset;	
		}
		gridMatrix = new ArrayList<List<BlockData>>();
		for (int row = 0; row < rowSize; row++) {
			List<BlockData> rowData = new ArrayList<BlockData>();
			for (int col = 0; col < colSize; col++)
				rowData.add(new BlockData(col, row));
			gridMatrix.add(rowData);
		}
		
		// Draw the grid
		drawGrid(myCanvas, textPaint);
	}	
	
	private void drawGrid(Canvas c, Paint paint) {				
		if (c != null) {			

			// TODO: This is making the UI unresponsive and must be moved to a thread
			if (gridMatrix != null) {
				// Draw block							
				for (int ri = 0; ri < gridMatrix.size(); ri++) {					
					for (int ci = 0; ci < gridMatrix.get(ri).size(); ci++) {
						BlockData b = gridMatrix.get(ri).get(ci);
						//Log.d(TAG, "Block " + b.loc.x + ", " + b.loc.y);
						if (b.touchId != INVALID_TOUCH_ID && b.bChanged) {
							// c.drawCircle((float)b.pos.x, (float)b.pos.y, (float)b.size, touchPaints[b.touchId]);							
							c.drawRect(ci*offset, ri*offset, (ci+1)*offset, (ri+1)*offset, touchPaints[b.touchId]);
							b.bChanged = false;
						}
					}
				}					
			}
		}				
	}	

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub	
		myCanvas_w = getWidth();
		myCanvas_h = getHeight();
		myCanvasBitmap = Bitmap.createBitmap(myCanvas_w, myCanvas_h, Bitmap.Config.ARGB_8888);
		myCanvas = new Canvas();
		myCanvas.setBitmap(myCanvasBitmap);
		 
		identityMatrix = new Matrix();		
		if (drawThread == null) 
			drawThread = new GridThread(getHolder(), this);
		drawThread.setRunnable(true);
	    drawThread.start();		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	    boolean retry = true;
	    drawThread.setRunnable(false);

	    while(retry) {
	        try {
	            drawThread.join();
	            retry = false;
	        } catch(InterruptedException ie) {
	            //Try again and again and again
	        }
	        break;
	    }
	    drawThread = null;
	}	
}
