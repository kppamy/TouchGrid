package com.example.touchgrid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
	
	private Paint textPaint = new Paint();
	private Paint touchPaints[] = new Paint[MAX_TOUCHPOINTS];
	private int colors[] = new int[MAX_TOUCHPOINTS];
	
	private int width, height;
	private int offset = 20; //!< The size of block
	private float scale = 1.0f;
	private int colSize, rowSize;  //!< The w*h size always in portrait perspective 
	
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
		
		public BlockData(int col, int row) {
			loc.set(col, row);
			touchId = INVALID_TOUCH_ID;
			pos.set(0, 0);			
			pressure = 0.0f;
			size = 0;
		}		
	}
	
	public TouchGridView(Context context) {
		super(context); 
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		setFocusableInTouchMode(true); // make sure we get touch events
		
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		initGrid(display.getWidth(), display.getHeight());		
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
					b.touchId = id;
					b.pressure = event.getPressure(i);
					b.size = (int)event.getSize(i);
				}			
			}
			else {
				Log.d(TAG, "(" + x + ", " + y + " -> " + "(" + col + ", " + row + ")");
			}
				
		}
		
		Canvas c = getHolder().lockCanvas();
		if (c != null) {
			
			/*
			c.drawColor(Color.BLACK);
			if (event.getAction() == MotionEvent.ACTION_UP) {
				// clear everything
			} else {
				// draw crosshairs first then circles as a second pass
				for (int i = 0; i < pointerCount; i++) {
					int id = event.getPointerId(i);
					int x = (int) event.getX(i);
					int y = (int) event.getY(i);
					drawCrosshairs(x, y, touchPaints[id], i, id, c);
				}
				
				for (int i = 0; i < pointerCount; i++) {
					int id = event.getPointerId(i);
					int x = (int) event.getX(i);
					int y = (int) event.getY(i);
					drawCircle(x, y, touchPaints[id], c);
				}
			}*/
			drawGrid(c, textPaint);
			getHolder().unlockCanvasAndPost(c);
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
			/* // clear screen
			c.drawColor(Color.BLACK);
			float tWidth = textPaint.measureText(START_TEXT);
			c.drawText(START_TEXT, width / 2 - tWidth / 2, height / 2, textPaint);			
			*/
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
			touchPaints[i].setStrokeWidth(0);
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
		Canvas c = getHolder().lockCanvas();
		if (c != null) {
			drawGrid(c, textPaint);
			getHolder().unlockCanvasAndPost(c);
		}
	}	
	
	private void drawGrid(Canvas c, Paint paint) {				
		if (c != null) {			
			int rowLen = rowSize * offset;
			int colLen = colSize * offset;
			/*
			// Draw horizontal lines
			for (int y = offset; y < rowLen; y += offset)				
				c.drawLine(0, y, colLen, y, paint);
			// Draw vertical lines
			for (int x = offset; x < colLen; x += offset)
				c.drawLine(x, 0, x, rowLen, paint);*/
			
			// TODO: This is making the UI unresponsive and must be moved to a thread
			if (gridMatrix != null) {
				// Draw block							
				for (int ri = 0; ri < gridMatrix.size(); ri++) {					
					for (int ci = 0; ci < gridMatrix.get(ri).size(); ci++) {
						BlockData b = gridMatrix.get(ri).get(ci);
						//Log.d(TAG, "Block " + b.loc.x + ", " + b.loc.y);
						if (b.touchId != INVALID_TOUCH_ID) {
							// c.drawCircle((float)b.pos.x, (float)b.pos.y, (float)b.size, touchPaints[b.touchId]);							
							c.drawRect(ci*offset, ri*offset, (ci+1)*offset, (ri+1)*offset, touchPaints[b.touchId]);
						}
					}
				}					
			}
		}				
	}	
	
	private void drawCrosshairs(int x, int y, Paint paint, int ptr, int id, Canvas c) {
		c.drawLine(0, y, width, y, paint);
		c.drawLine(x, 0, x, height, paint);
		int textY = (int)((15 + 20 * ptr) * scale);
		c.drawText("x" + ptr + "=" + x, 10 * scale, textY, textPaint);
		c.drawText("y" + ptr + "=" + y, 70 * scale, textY, textPaint);
		c.drawText("id" + ptr + "=" + id, width - 55 * scale, textY, textPaint);
	}

	private void drawCircle(int x, int y, Paint paint, Canvas c) {
		c.drawCircle(x, y, 40 * scale, paint);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}	
}
