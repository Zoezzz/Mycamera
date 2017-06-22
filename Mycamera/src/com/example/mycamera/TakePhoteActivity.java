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
 * @Description: ���ս���
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
	
    /** ���ż����϶��� */ 
	private SeekBar mZoomSeekBar;
	private Handler mHandler;
	private TempImageView mTempImageView;
	
	/** ���ݱ��� */ 
	public byte[] data_save = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ���ú���
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // ����ȫ��
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
        
		//��ȡ��ǰ�����֧�ֵ�������ż���ֵС��0��ʾ��֧�����š���֧������ʱ�������϶�����
		int maxZoom = mCameraPreview.getMaxZoom();
		if(maxZoom>0){
			mZoomSeekBar.setMax(maxZoom);
			mZoomSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		}
        //setOnTouchListener ���Ƶ�����
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

    //button ����
    public void takePhoto(View view) {
        if(mCameraPreview != null) {
        	shootSound();
            mCameraPreview.takePicture();
        }
    }
    
    //button �ر�
    public void close(View view) {
        finish();
    }
    
    /**
     * ���ճɹ���ص�
     * �洢ͼƬ//<����ʾ��ͼ����>
     */
    
    @Override
    public void onCameraStopped(byte[] data) {
        Log.i("TAG", "==onCameraStopped==");
        
        /* 
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);  //����ͼ��
        long dateTaken = System.currentTimeMillis();                           //ϵͳʱ��
        String filename = DateFormat.format("jpg_yyyy-MM-dd kk.mm.ss", dateTaken).toString() + ".jpg";    //ͼ������
        Uri source = insertImage(getContentResolver(), filename, dateTaken, PATH,filename, bitmap, data); //�洢ͼ��PATHĿ¼��
        */
        
        //----------------------------------------------------------------------
        data_save = data;
        Thread newThread; //����һ�����߳�
        newThread = new Thread(new Runnable() {
        	@Override
        	public void run() 
        	{
                Bitmap bitmap = BitmapFactory.decodeByteArray(data_save, 0, data_save.length);
                long dateTaken = System.currentTimeMillis();
                String filename = DateFormat.format("jpg_yyyy-MM-dd kk.mm.ss", dateTaken)
                        .toString() + ".jpg";
                Uri source = insertImage(getContentResolver(), filename, dateTaken, PATH,filename, bitmap, data_save);
                
                bitmap.recycle();  // ����bitmap�ռ�
                
        	}
         });
        newThread.start(); //�����߳�
        //----------------------------------------------------------------------
        
        Bitmap bitmap_small = byteToBitmap(data);  //��������ͼ
        mTempImageView.setImageBitmap(bitmap_small);
	    mTempImageView.startAnimation(R.anim.tempview_show);
        mCameraPreview.start();
        
    }

    /**
     * �洢ͼ�񲢽���Ϣ�����ý�����ݿ�
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
                    source.compress(Bitmap.CompressFormat.JPEG,90, outputStream);  //ѹ�������� 100 
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
			//ZOOMģʽ�� �ڽ������������seekBar ����tokenΪmZoomSeekBar�������������ʱ�Ƴ�ǰһ����ʱ����
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
		
		/** ��¼��������Ƭģʽ���ǷŴ���С��Ƭģʽ */
		private static final int MODE_INIT = 0;
		/** �Ŵ���С��Ƭģʽ */
		private static final int MODE_ZOOM = 1;
		
		private int mode = MODE_INIT;// ��ʼ״̬ 

		/** ���ڼ�¼����ͼƬ�ƶ�������λ�� */
		private float startDis;

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			/** ͨ�������㱣������λ MotionEvent.ACTION_MASK = 255 */
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			
			// ��ָѹ����Ļ
			case MotionEvent.ACTION_DOWN:
				mode = MODE_INIT;
				
				//�ֶ��Խ�
				int width = focusView.getWidth();
				int height = focusView.getHeight();
				focusView.setX(event.getX() - (width / 2));
				focusView.setY(event.getY() - (height / 2));
				focusView.beginFocus();
				//mCameraPreview.setFocusView(focusView);
				break;
				
			case MotionEvent.ACTION_POINTER_DOWN:
				//���mZoomSeekBarΪnull ��ʾ���豸��֧������ ֱ����������mode Moveָ��Ҳ�޷�ִ��
				if(mZoomSeekBar==null) return true;
				//�Ƴ�token����ΪmZoomSeekBar����ʱ����
				mHandler.removeCallbacksAndMessages(mZoomSeekBar);
				mZoomSeekBar.setVisibility(View.VISIBLE);

				mode = MODE_ZOOM;
				/** ����������ָ��ľ��� */
				startDis = distance(event);
				break;
				
			case MotionEvent.ACTION_MOVE:
				if (mode == MODE_ZOOM) {
					//ֻ��ͬʱ�����������ʱ���ִ��
					if(event.getPointerCount()<2) return true;
					float endDis = distance(event);// ��������
					//ÿ�仯10f zoom��1
					int scale=(int) ((endDis-startDis)/10f);
					if(scale>=1||scale<=-1){
						int zoom = mCameraPreview.getZoom()+scale;
						//zoom���ܳ�����Χ
						if(zoom > mCameraPreview.getMaxZoom()) zoom = mCameraPreview.getMaxZoom();
						if(zoom<0) zoom=0;
						mCameraPreview.setZoom(zoom);
						mZoomSeekBar.setProgress(zoom);
						//�����һ�εľ�����Ϊ��ǰ����
						startDis=endDis;
					}
				}
				break;
				// ��ָ�뿪��Ļ
			case MotionEvent.ACTION_UP:
				if(mode!=MODE_ZOOM){
					//���þ۽�
					mCameraPreview.focusOnTouch(event);
					//Point point=new Point((int)event.getX(), (int)event.getY());
					//mCameraPreview.onFocus(point,autoFocusCallback);
					//mFocusImageView.startFocus(point);
				}else {
					//ZOOMģʽ�� �ڽ������������seekBar ����tokenΪmZoomSeekBar�������������ʱ�Ƴ�ǰһ����ʱ����
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
		/** ����������ָ��ľ��� */
		private float distance(MotionEvent event) {
			float dx = event.getX(1) - event.getX(0);
			float dy = event.getY(1) - event.getY(0);
			/** ʹ�ù��ɶ���������֮��ľ��� */
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

    //------------------------------------------bitmap ����ͼ
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

