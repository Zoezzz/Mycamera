package com.example.mycamera;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;

/** 
 * @ClassName: TempImageView 
 * @Description: 临时ImageView，在执行完一个动画后自动隐藏
 *  
 */
public class TempImageView extends ImageView implements AnimationListener{

	public final static String TAG="TempImageView";
	
	public static final int NO_ID=-1;   //若动画不存在
	
	private int mAnimationID=NO_ID;

	public TempImageView(Context context) {
		// TODO Auto-generated constructor stub
		super(context);
	}

	public TempImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TempImageView);
		mAnimationID = a.getResourceId(R.styleable.TempImageView_animat_id, NO_ID);
		a.recycle();
	}

	@Override
	public void onAnimationStart(Animation animation) {
		// TODO Auto-generated method stub
		setVisibility(View.VISIBLE);
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		// TODO Auto-generated method stub
		setVisibility(View.GONE);
		
		/*Drawable drawable =  getDrawable();
		Bitmap bm = null;
		if(drawable!=null&&drawable instanceof BitmapDrawable)
			bm=((BitmapDrawable)drawable).getBitmap();
		//相机监听接口不为空，则执行拍照结束操作
		if (mListener!=null) mListener.onAnimtionEnd(bm,mIsVideo);*/
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
		// TODO Auto-generated method stub

	}
	/**  
	 * @Description: 开始动画  resourceID 动画资源的ID 
	 */
	public void startAnimation(){
		startAnimation(null);
	}

	public void startAnimation(int resourceID){
		mAnimationID=resourceID;
		startAnimation();
	}

	public void startAnimation(Animation animation){
		if(animation!=null){
			animation.setAnimationListener(this);
			super.startAnimation(animation);
			return;
		}
		if(mAnimationID!=NO_ID){
			animation=AnimationUtils.loadAnimation(getContext(), mAnimationID);
			animation.setAnimationListener(this);
			super.startAnimation(animation);
		}
	}
	
	@Override
	public void setImageBitmap(Bitmap bm) {
		// TODO Auto-generated method stub
		super.setImageBitmap(bm);
	}

}
