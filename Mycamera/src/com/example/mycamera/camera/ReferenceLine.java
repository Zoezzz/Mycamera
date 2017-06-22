package com.example.mycamera.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.example.mycamera.utils.Utils;

/**
 * @Class: ReferenceLine
 * @Description: Íø¸ñ²Î¿¼Ïß
 */
public class ReferenceLine extends View {

	private Paint mLinePaint;

	public ReferenceLine(Context context) {
		super(context);
		init();
	}

	public ReferenceLine(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ReferenceLine(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	private void init() {
		mLinePaint = new Paint();
		mLinePaint.setAntiAlias(true);
		mLinePaint.setColor(Color.parseColor("#FF0000"));
		mLinePaint.setStrokeWidth(2);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int screenWidth = Utils.getScreenWH(getContext()).widthPixels;
		int screenHeight = Utils.getScreenWH(getContext()).heightPixels;

		int width = screenWidth/10;
		int line_width = width*7/9;
		
		int height = screenHeight/5;	
		
		
		for (int i = width*3/2+line_width/8, j = 0;i < screenWidth && j<9;i += line_width, j++) 
		{
			canvas.drawLine(i, height, i, height*2, mLinePaint);		
			canvas.drawLine(i,height*23/10, i, height*33/10, mLinePaint);
		}
		
		for (int i = width*3/2+line_width/8*7, j = 0;i < screenWidth && j<9;i += line_width, j++) {
			canvas.drawLine(i, height, i, height*2, mLinePaint);		
			canvas.drawLine(i, height*23/10, i, height*33/10, mLinePaint);
		}
		
		for (int j = height,i = 0;j < screenHeight && i < 2;j += height,i++) 
		{
			for(int n =0,m =0; m<9;n+=line_width,m++  )
			{
				canvas.drawLine(width*3/2+line_width/8+n, j, width*3/2+line_width/8*7+n, j, mLinePaint);
			}
		}
		
		for (int j = height*23/10,i = 0;j < screenHeight && i < 2;j += height,i++)   //2.3 height
		{
			for(int n =0,m =0; m<9;n+=line_width,m++  )
			{
				canvas.drawLine(width*3/2+line_width/8+n, j, width*3/2+line_width/8*7+n, j, mLinePaint);
			}
		}
		
			
		/*for (int j = height,i = 0;j < screenHeight && i < 2;j += height,i++) 
		{
			canvas.drawLine(width*3/2, j, width*3/2+9*line_width, j, mLinePaint);
		}
		
		for (int j = height*47/20,i = 0;j < screenHeight && i < 2;j += height,i++)   //2.35 height
		{
			canvas.drawLine(width*3/2, j, width*3/2+9*line_width, j, mLinePaint);
		}*/
	}

}
