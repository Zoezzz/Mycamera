package com.example.mycamera;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.example.mycamera.camera.CameraPreview;
import com.example.mycamera.camera.FocusView;
import com.example.mycamera.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;

/**
 * @Class: TakePhoteActivity
 * @Description: 拍照界面
 */
public class TakePhoteActivity extends Activity implements CameraPreview.OnCameraStatusListener,
        SensorEventListener {
    private static final String TAG = "TakePhoteActivity";
    public static final Uri IMAGE_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    public static final String PATH = Environment.getExternalStorageDirectory()
            .toString() + "/MyCamera/";  //AndroidMedia
    
    CameraPreview mCameraPreview;
    
    RelativeLayout mTakePhotoLayout;
    LinearLayout mCropperLayout;
    FocusView focusView;
	
    /** 缩放级别拖动条 */ 
	private SeekBar mZoomSeekBar;
	private Handler mHandler;
	private TempImageView mTempImageView;
	
	/** 数据保存 */ 
	public byte[] data_save = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // 设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_take_photo);
        
        // Initialize components of the Application   
        mCameraPreview = (CameraPreview) findViewById(R.id.cameraPreview);
        focusView = (FocusView) findViewById(R.id.view_focus);
        mTempImageView=(TempImageView) findViewById(R.id.tempImageView);
   
        mTakePhotoLayout = (RelativeLayout) findViewById(R.id.take_photo_layout);
        
        mCameraPreview.setFocusView(focusView);
        mCameraPreview.setOnCameraStatusListener(this);
        
        mSensorManager = (SensorManager) getSystemService(Context.
                SENSOR_SERVICE);
        mAccel = mSensorManager.getDefaultSensor(Sensor.
                TYPE_ACCELEROMETER);
        
        mHandler=new Handler();
        mZoomSeekBar = (SeekBar) findViewById(R.id.zoomSeekBar);
        
		//获取当前照相机支持的最大缩放级别，值小于0表示不支持缩放。当支持缩放时，加入拖动条。
		int maxZoom = mCameraPreview.getMaxZoom();
		if(maxZoom>0){
			mZoomSeekBar.setMax(maxZoom);
			mZoomSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		}
        //setOnTouchListener 手势调焦距
		mCameraPreview.setOnTouchListener(new TouchListener());
		
		/*int margin = (mCameraPreview.getHeight()-mCameraPreview.getWidth()*9/16)/2;
		Log.v("Supported preview", "margin " + margin);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
               RelativeLayout.LayoutParams.MATCH_PARENT,
               RelativeLayout.LayoutParams.MATCH_PARENT);
	    lp.setMargins(0,10, 0, 10);
	    mCameraPreview.setLayoutParams(lp);*/
    }

    boolean isRotated = false;

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.e(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }

    //button 拍照
    public void takePhoto(View view) {
        if(mCameraPreview != null) {
        	shootSound();
            mCameraPreview.takePicture();
        }
    }
    
    //button 关闭
    public void close(View view) {
        finish();
    }
    
    /**
     * 拍照成功后回调
     * 存储图片//<并显示截图界面>
     */
    
    @Override
    public void onCameraStopped(byte[] data) {
        Log.i("TAG", "==onCameraStopped==");
        
        /* 
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);  //创建图像
        long dateTaken = System.currentTimeMillis();                           //系统时间
        String filename = DateFormat.format("jpg_yyyy-MM-dd kk.mm.ss", dateTaken).toString() + ".jpg";    //图像名称
        Uri source = insertImage(getContentResolver(), filename, dateTaken, PATH,filename, bitmap, data); //存储图像（PATH目录）
        */
        
        //----------------------------------------------------------------------
        data_save = data;
        Thread newThread; //声明一个子线程
        newThread = new Thread(new Runnable() {
        	@Override
        	public void run() 
        	{
                Bitmap bitmap = BitmapFactory.decodeByteArray(data_save, 0, data_save.length);
                long dateTaken = System.currentTimeMillis();
                String filename = DateFormat.format("jpg_yyyy-MM-dd kk.mm.ss", dateTaken)
                        .toString() + ".jpg";
                Uri source = insertImage(getContentResolver(), filename, dateTaken, PATH,filename, bitmap, data_save);
                
                bitmap.recycle();  // 回收bitmap空间
                
        	}
         });
        newThread.start(); //启动线程
        //----------------------------------------------------------------------
        
        Bitmap bitmap_small = byteToBitmap(data);  //生成缩略图
        mTempImageView.setImageBitmap(bitmap_small);
	    mTempImageView.startAnimation(R.anim.tempview_show);
        mCameraPreview.start();
        
    }

    /**
     * 存储图像并将信息添加入媒体数据库
     */  
    private Uri insertImage(ContentResolver cr, String name, long dateTaken,
                            String directory, String filename, Bitmap source, byte[] jpegData) {
        OutputStream outputStream = null;
        String filePath = directory + filename;
        try {
            File dir = new File(directory);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(directory, filename);
            if (file.createNewFile()) {
                outputStream = new FileOutputStream(file);
                if (source != null) {
                    source.compress(Bitmap.CompressFormat.JPEG,90, outputStream);  //压缩的问题 100 
                } else {
                    outputStream.write(jpegData);
                }
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return null;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable t) {
                }
            }
        }
        ContentValues values = new ContentValues(7);
        values.put(MediaStore.Images.Media.TITLE, name);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.DATE_TAKEN, dateTaken);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.DATA, filePath);
        return cr.insert(IMAGE_URI, values);
    }
    
    private float mLastX = 0;
    private float mLastY = 0;
    private float mLastZ = 0;
    private boolean mInitialized = false;
    private SensorManager mSensorManager;
    private Sensor mAccel;
   
    @Override
    public void onSensorChanged(SensorEvent event) {

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        if (!mInitialized){
            mLastX = x;
            mLastY = y;
            mLastZ = z;
            mInitialized = true;
        }
        float deltaX  = Math.abs(mLastX - x);
        float deltaY = Math.abs(mLastY - y);
        float deltaZ = Math.abs(mLastZ - z);

        if(deltaX > 0.8 || deltaY > 0.8 || deltaZ > 0.8){
            mCameraPreview.setFocus();
        }
        mLastX = x;
        mLastY = y;
        mLastZ = z;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    
    //------------------------------------onSeekBarChangeListener
	private final OnSeekBarChangeListener onSeekBarChangeListener=new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
			mCameraPreview.setZoom(progress);
			mHandler.removeCallbacksAndMessages(mZoomSeekBar);
			//ZOOM模式下 在结束两秒后隐藏seekBar 设置token为mZoomSeekBar用以在连续点击时移除前一个定时任务
			mHandler.postAtTime(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					mZoomSeekBar.setVisibility(View.GONE);
				}
			}, mZoomSeekBar,SystemClock.uptimeMillis()+2000);
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub

		}
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
		}
	};
	
	//-------------------------------------------OnTouchListener
	private final class TouchListener implements OnTouchListener {
		
		/** 记录是拖拉照片模式还是放大缩小照片模式 */
		private static final int MODE_INIT = 0;
		/** 放大缩小照片模式 */
		private static final int MODE_ZOOM = 1;
		
		private int mode = MODE_INIT;// 初始状态 

		/** 用于记录拖拉图片移动的坐标位置 */
		private float startDis;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			/** 通过与运算保留最后八位 MotionEvent.ACTION_MASK = 255 */
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			
			// 手指压下屏幕
			case MotionEvent.ACTION_DOWN:
				mode = MODE_INIT;
				
				//手动对焦
				int width = focusView.getWidth();
				int height = focusView.getHeight();
				focusView.setX(event.getX() - (width / 2));
				focusView.setY(event.getY() - (height / 2));
				focusView.beginFocus();
				//mCameraPreview.setFocusView(focusView);
				break;
				
			case MotionEvent.ACTION_POINTER_DOWN:
				//如果mZoomSeekBar为null 表示该设备不支持缩放 直接跳过设置mode Move指令也无法执行
				if(mZoomSeekBar==null) return true;
				//移除token对象为mZoomSeekBar的延时任务
				mHandler.removeCallbacksAndMessages(mZoomSeekBar);
				mZoomSeekBar.setVisibility(View.VISIBLE);

				mode = MODE_ZOOM;
				/** 计算两个手指间的距离 */
				startDis = distance(event);
				break;
				
			case MotionEvent.ACTION_MOVE:
				if (mode == MODE_ZOOM) {
					//只有同时触屏两个点的时候才执行
					if(event.getPointerCount()<2) return true;
					float endDis = distance(event);// 结束距离
					//每变化10f zoom变1
					int scale=(int) ((endDis-startDis)/10f);
					if(scale>=1||scale<=-1){
						int zoom = mCameraPreview.getZoom()+scale;
						//zoom不能超出范围
						if(zoom > mCameraPreview.getMaxZoom()) zoom = mCameraPreview.getMaxZoom();
						if(zoom<0) zoom=0;
						mCameraPreview.setZoom(zoom);
						mZoomSeekBar.setProgress(zoom);
						//将最后一次的距离设为当前距离
						startDis=endDis;
					}
				}
				break;
				// 手指离开屏幕
			case MotionEvent.ACTION_UP:
				if(mode!=MODE_ZOOM){
					//设置聚焦
					mCameraPreview.focusOnTouch(event);
					//Point point=new Point((int)event.getX(), (int)event.getY());
					//mCameraPreview.onFocus(point,autoFocusCallback);
					//mFocusImageView.startFocus(point);
				}else {
					//ZOOM模式下 在结束两秒后隐藏seekBar 设置token为mZoomSeekBar用以在连续点击时移除前一个定时任务
					mHandler.postAtTime(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							mZoomSeekBar.setVisibility(View.GONE);
						}
					}, mZoomSeekBar,SystemClock.uptimeMillis()+800);
				}
				break;
			}
			return true;
		}
		/** 计算两个手指间的距离 */
		private float distance(MotionEvent event) {
			float dx = event.getX(1) - event.getX(0);
			float dy = event.getY(1) - event.getY(0);
			/** 使用勾股定理返回两点之间的距离 */
			return (float) Math.sqrt(dx * dx + dy * dy);
		}
	}
	
	public void shootSound()  
	{  
	    AudioManager meng = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);  
	    int volume = meng.getStreamVolume( AudioManager.STREAM_NOTIFICATION); 
	    MediaPlayer shootMP = null;
	  
	    if (volume != 0)  
	    {  
	        if (shootMP == null)  
	            shootMP = MediaPlayer.create(this, Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));  
	        if (shootMP != null)  
	            shootMP.start();  
	    }  
	}  

    //------------------------------------------bitmap 缩略图
	public static Bitmap byteToBitmap(byte[] imgByte) {  
        InputStream input = null;  
        Bitmap bitmap = null;  
        BitmapFactory.Options options = new BitmapFactory.Options();  
        options.inSampleSize = 32;  
        input = new ByteArrayInputStream(imgByte);  
        SoftReference softRef = new SoftReference(BitmapFactory.decodeStream(input, null, options));  
        bitmap = (Bitmap) softRef.get();  
        if (imgByte != null) {  
            imgByte = null;  
        }  
        try {  
            if (input != null) {  
                input.close();  
            }  
        } catch (IOException e) {  
            // TODO Auto-generated catch block  
            e.printStackTrace();  
        }  
        return bitmap;  
    }  
}

