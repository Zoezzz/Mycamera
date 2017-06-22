package com.example.mycamera.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.mycamera.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @Class: CameraPreview
 * @Description: �Զ������
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, AutoFocusCallback {
	
	private static final String TAG = "CameraPreview";

	private int viewWidth = 0;
	private int viewHeight = 0;

	/** �����ӿ� */
	private OnCameraStatusListener listener;

	private SurfaceHolder holder;
	private Camera camera;
	private FocusView mFocusView;
	
	/** ��ǰ���ż���  Ĭ��Ϊ0*/ 
	private int mZoom=0;

	//����һ��PictureCallback���󣬲�ʵ�����е�onPictureTaken����
	private PictureCallback pictureCallback = new PictureCallback() {

		// �÷������ڴ�����������Ƭ����
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// ֹͣ��Ƭ����
			try {
				camera.stopPreview();
			} catch (Exception e) {
			}
			// ���ý����¼�
			if (null != listener) {
				listener.onCameraStopped(data);
			}
		}
	};

	// Preview��Ĺ��췽��
	public CameraPreview(Context context, AttributeSet attrs) {
		super(context, attrs);
		// ���SurfaceHolder����
		holder = getHolder();
		// ָ�����ڲ�׽�����¼���SurfaceHolder.Callback����
		holder.addCallback(this);
		// ����SurfaceHolder���������
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		//setOnTouchListener(onTouchListener);
	}

	// ��surface����ʱ����
	public void surfaceCreated(SurfaceHolder holder) {
		Log.e(TAG, "==surfaceCreated==");
		if(!Utils.checkCameraHardware(getContext())) {
			Toast.makeText(getContext(), "����ͷ��ʧ�ܣ�", Toast.LENGTH_SHORT).show();
			return;
		}
		// ���Camera����
		camera = getCameraInstance();
		try {
			// ����������ʾ���������SurfaceHolder����
			camera.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
			// �ͷ��ֻ�����ͷ
			camera.release();
			camera = null;
		}
		updateCameraParameters();
		if (camera != null) {
			camera.startPreview();
		}
		setFocus();
	}

	// ��surface����ʱ����
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.e(TAG, "==surfaceDestroyed==");
		// �ͷ��ֻ�����ͷ
		camera.release();
		camera = null;
	}

	// ��surface�Ĵ�С�����ı�ʱ����
	public void surfaceChanged(final SurfaceHolder holder, int format, int w,
			int h) {
		// stop preview before making changes
		try {
			camera.stopPreview();
		} catch (Exception e){
			// ignore: tried to stop a non-existent preview
		}
		// set preview size and make any resize, rotate or reformatting changes here
		updateCameraParameters();
		// start preview with new settings
		try {
			camera.setPreviewDisplay(holder);
			camera.startPreview();

		} catch (Exception e){
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}
		setFocus();
	}

	/**
	 * �����ʾ��������
	 */
	/*OnTouchListener onTouchListener = new OnTouchListener() {
		@SuppressWarnings("deprecation")
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				int width = mFocusView.getWidth();
				int height = mFocusView.getHeight();
				mFocusView.setX(event.getX() - (width / 2));
				mFocusView.setY(event.getY() - (height / 2));
				mFocusView.beginFocus();
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				focusOnTouch(event);
			}
			return true;
		}
	};*/

	/**
	 * ��ȡ����ͷʵ��
	 * @return
	 */
	private Camera getCameraInstance() {
		Camera c = null;
		try {
			int cameraCount = 0;
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			cameraCount = Camera.getNumberOfCameras(); // get cameras number

			for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
				Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
				// ��������ͷ�ķ�λ��Ŀǰ�ж���ֵ�����ֱ�ΪCAMERA_FACING_FRONTǰ�ú�CAMERA_FACING_BACK����
				if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
					try {
						c = Camera.open(camIdx);   //�򿪺�������ͷ
					} catch (RuntimeException e) {
						Toast.makeText(getContext(), "����ͷ��ʧ�ܣ�", Toast.LENGTH_SHORT).show();
					}
				}
			}
			if (c == null) {
				c = Camera.open(0); // attempt to get a Camera instance
			}
		} catch (Exception e) {
			Toast.makeText(getContext(), "����ͷ��ʧ�ܣ�", Toast.LENGTH_SHORT).show();
		}
		return c;
	}
	
	/**  
	 *  ��ȡ������ż������Ϊ40
	 */
	public int getMaxZoom(){
		if(camera==null) return -1;		
		Camera.Parameters parameters=camera.getParameters();
		if(!parameters.isZoomSupported()) return -1;
		return parameters.getMaxZoom()>40?40:parameters.getMaxZoom();
	}
	/**  
	 *  ����������ż���  @param zoom
	 */
	public void setZoom(int zoom){
		if(camera==null) return;
		Camera.Parameters parameters;
		parameters=camera.getParameters();
		
		if(!parameters.isZoomSupported()) return;
		parameters.setZoom(zoom);
		camera.setParameters(parameters);
		mZoom=zoom;
	}
	
	public int getZoom(){
		return mZoom;
	}

	private void updateCameraParameters() {
		if (camera != null) {
			Camera.Parameters p = camera.getParameters();
			setParameters(p);
			try {
				camera.setParameters(p);
			} catch (Exception e) {
				/*Camera.Size previewSize = findBestPreviewSize(p);
				p.setPreviewSize(previewSize.width, previewSize.height);
				p.setPictureSize(previewSize.width, previewSize.height);*/
				Point camerapoint = findBestPictureResolution(p);
				p.setPictureSize(camerapoint.x, camerapoint.y);  //picture ͼ�̳ߴ�
				camerapoint = findBestPreviewResolution(p);
				p.setPreviewSize(camerapoint.x, camerapoint.y);  //preview Ԥ���ֱ���
				camera.setParameters(p);
			}
		}
	}

	/**
	 * @param p
	 */
	private void setParameters(Camera.Parameters p) {
		List<String> focusModes = p.getSupportedFocusModes();
		if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
			p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
		}
		long time = new Date().getTime();
		p.setGpsTimestamp(time);
		p.setPictureFormat(PixelFormat.JPEG);  //������Ƭ��ʽ
		
		//����Ԥ����ͼƬ�ߴ�-------------------------------------------------------
		/*
	 	Camera.Size previewSize = findPreviewSizeByScreen(p);
		p.setPreviewSize(previewSize.width, previewSize.height);   //Ԥ���ߴ�
		p.setPictureSize(previewSize.width, previewSize.height);   //ͼƬ�ߴ�
		*/
		//finding best preview and picture (supporting & sorting & time)
		/*
		List<Camera.Size> sizeList = p.getSupportedPreviewSizes();
		sizeList = p.getSupportedPictureSizes();
		if (sizeList.size()>0) 
		{
			Camera.Size cameraSize=sizeList.get(6);
			p.setPictureSize(cameraSize.width, cameraSize.height);
		}
		*/
		Point camerapoint = findBestPictureResolution(p);
		p.setPictureSize(camerapoint.x, camerapoint.y);  //picture ͼ�̳ߴ�
		camerapoint = findBestPreviewResolution(p);
		p.setPreviewSize(camerapoint.x, camerapoint.y);  //preview Ԥ���ֱ���
		
		//-----------------------------------------------------------------------
		
		p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);  //�۽�ģʽ	
		setZoom(mZoom);   //�������ż���
		if (getContext().getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
			camera.setDisplayOrientation(90);
			p.setRotation(90);
		}
	}

	// �������գ������������Ƭ����PictureCallback�ӿڵ�onPictureTaken����
	public void takePicture() {
		if (camera != null) {
			try {
				camera.takePicture(null, null, pictureCallback);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// ���ü����¼�
	public void setOnCameraStatusListener(OnCameraStatusListener listener) {
		this.listener = listener;
	}

	@Override
	public void onAutoFocus(boolean success, Camera camera) {

	}

	public void start() {
		if (camera != null) {
			camera.startPreview();
		}
	}

	public void stop() {
		if (camera != null) {
			camera.stopPreview();
		}
	}

	/**
	 * ������ռ����ӿ�
	 */
	public interface OnCameraStatusListener {
		// ������ս����¼�
		void onCameraStopped(byte[] data);
	}

	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {
		viewWidth = MeasureSpec.getSize(widthSpec);
		viewHeight = MeasureSpec.getSize(heightSpec);
		super.onMeasure(
				MeasureSpec.makeMeasureSpec(viewWidth, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(viewHeight, MeasureSpec.EXACTLY));
	}

	/**
	 * ��Ԥ����С����Ϊ��Ļ��С
	 * @param parameters
	 * @return
	 */
	private Camera.Size findPreviewSizeByScreen(Camera.Parameters parameters) {
		if (viewWidth != 0 && viewHeight != 0) {
			return camera.new Size(Math.max(viewWidth, viewHeight),
					Math.min(viewWidth, viewHeight));
		} else {
			return camera.new Size(Utils.getScreenWH(getContext()).heightPixels,
					Utils.getScreenWH(getContext()).widthPixels);
		}
	}

	/**
	 * �ҵ�����ʵ���ʾ�ֱ��� ����ֹԤ��ͼ����Σ�
	 * @param parameters
	 * @return
	 */
	private Camera.Size findBestPreviewSize(Camera.Parameters parameters) {

		// ϵͳ֧�ֵ�����Ԥ���ֱ���
		String previewSizeValueString = null;
		previewSizeValueString = parameters.get("preview-size-values");

		if (previewSizeValueString == null) {
			previewSizeValueString = parameters.get("preview-size-value");
		}

		if (previewSizeValueString == null) { // ��Щ�ֻ�����m9��ȡ����֧�ֵ�Ԥ����С ��ֱ�ӷ�����Ļ��С
			return camera.new Size(Utils.getScreenWH(getContext()).widthPixels,
					Utils.getScreenWH(getContext()).heightPixels);
		}
		float bestX = 0;
		float bestY = 0;

		float tmpRadio = 0;
		float viewRadio = 0;

		if (viewWidth != 0 && viewHeight != 0) {
			viewRadio = Math.min((float) viewWidth, (float) viewHeight)
					/ Math.max((float) viewWidth, (float) viewHeight);
		}

		String[] COMMA_PATTERN = previewSizeValueString.split(",");
		for (String prewsizeString : COMMA_PATTERN) {
			prewsizeString = prewsizeString.trim();

			int dimPosition = prewsizeString.indexOf('x');
			if (dimPosition == -1) {
				continue;
			}

			float newX = 0;
			float newY = 0;

			try {
				newX = Float.parseFloat(prewsizeString.substring(0, dimPosition));
				newY = Float.parseFloat(prewsizeString.substring(dimPosition + 1));
			} catch (NumberFormatException e) {
				continue;
			}

			float radio = Math.min(newX, newY) / Math.max(newX, newY);
			if (tmpRadio == 0) {
				tmpRadio = radio;
				bestX = newX;
				bestY = newY;
			} else if (tmpRadio != 0 && (Math.abs(radio - viewRadio)) < (Math.abs(tmpRadio - viewRadio))) {
				tmpRadio = radio;
				bestX = newX;
				bestY = newY;
			}
		}

		if (bestX > 0 && bestY > 0) {
			return camera.new Size((int) bestX, (int) bestY);
		}
		return null;
	}

	/**
	 * ���ý���Ͳ������
	 *
	 * @param event
	 */
	public void focusOnTouch(MotionEvent event) {

		int[] location = new int[2];
		RelativeLayout relativeLayout = (RelativeLayout)getParent();
		relativeLayout.getLocationOnScreen(location);

		Rect focusRect = Utils.calculateTapArea(mFocusView.getWidth(),
				mFocusView.getHeight(), 1f, event.getRawX(), event.getRawY(),
				location[0], location[0] + relativeLayout.getWidth(), location[1],
				location[1] + relativeLayout.getHeight());
		Rect meteringRect = Utils.calculateTapArea(mFocusView.getWidth(),
				mFocusView.getHeight(), 1.5f, event.getRawX(), event.getRawY(),
				location[0], location[0] + relativeLayout.getWidth(), location[1],
				location[1] + relativeLayout.getHeight());

		Camera.Parameters parameters = camera.getParameters();
		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

		if (parameters.getMaxNumFocusAreas() > 0) {
			List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
			focusAreas.add(new Camera.Area(focusRect, 1000));

			parameters.setFocusAreas(focusAreas);
		}

		if (parameters.getMaxNumMeteringAreas() > 0) {
			List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
			meteringAreas.add(new Camera.Area(meteringRect, 1000));

			parameters.setMeteringAreas(meteringAreas);
		}

		try {
			camera.setParameters(parameters);
		} catch (Exception e) {
		}
		camera.autoFocus(this);
	}

	/**
	 * ���þ۽���ͼƬ
	 * @param focusView
	 */
	public void setFocusView(FocusView focusView) {
		this.mFocusView = focusView;
	}

	/**
	 * �����Զ��۽������Ҿ۽���ȦȦ��ʾ����Ļ�м�λ��
	 */
	public void setFocus() {
		if(!mFocusView.isFocusing()) {
			try {
				camera.autoFocus(this);
				mFocusView.setX((Utils.getWidthInPx(getContext())-mFocusView.getWidth()) / 2);
				mFocusView.setY((Utils.getHeightInPx(getContext())-mFocusView.getHeight()) / 2);
				mFocusView.beginFocus();
			} catch (Exception e) {
			}
		}
	}
	
	//-------------------------------------------bestPictureResolution
	  private Point findBestPictureResolution(Camera.Parameters p) {
	        List<Camera.Size> supportedPicResolutions = p.getSupportedPictureSizes(); // ���ٻ᷵��һ��ֵ

	        //log supported picture resolutions
	        StringBuilder picResolutionSb = new StringBuilder();
	        for (Camera.Size supportedPicResolution : supportedPicResolutions)
	        {
	            picResolutionSb.append(supportedPicResolution.width).append('x').append(supportedPicResolution.height).append(" ");
	        }
	        Log.d(TAG, "Supported picture resolutions: " + picResolutionSb);

	        //log default resolutions
	        Camera.Size defaultPictureResolution = p.getPictureSize();
	        Log.d(TAG, "default picture resolution " + 
	        		defaultPictureResolution.width + "x" + defaultPictureResolution.height);

	        // ����
	        List<Camera.Size> sortedSupportedPicResolutions = new ArrayList<Camera.Size>(supportedPicResolutions);
	        Collections.sort(sortedSupportedPicResolutions, new Comparator<Camera.Size>() {
	            @Override
	            public int compare(Camera.Size a, Camera.Size b) {
	                int aPixels = a.height * a.width;
	                int bPixels = b.height * b.width;
	                if (bPixels < aPixels) {
	                    return -1;
	                }
	                if (bPixels > aPixels) {
	                    return 1;
	                }
	                return 0;
	            }
	        });
	        
	        Iterator<Camera.Size> it = sortedSupportedPicResolutions.iterator();
		      while (it.hasNext()) {
		          Camera.Size supportedPicResolution = it.next();
		          int width = supportedPicResolution.width;
		          int height = supportedPicResolution.height;
		          
		          double aspectRatio = (double) width / (double) height;
		          //double ratio = 16/9;  //double---1
		          //double distortion = Math.abs(aspectRatio - ratio);
		          //Log.d(TAG, "distortion " + aspectRatio);
		          if (aspectRatio < 1.4) {
		        	  it.remove();
		              continue;
		            }
		      }

	        if (!sortedSupportedPicResolutions.isEmpty()) {
	            Camera.Size largestPreview = sortedSupportedPicResolutions.get(1);  //3600 X 2160 4096 2304
	            Point largestSize = new Point(largestPreview.width, largestPreview.height);
	            Log.d(TAG, "using largest suitable picture resolution: " + largestSize);
	            return largestSize;
	        }

	        // û���ҵ����ʵģ��ͷ���Ĭ�ϵ�
	        Point defaultResolution = new Point(defaultPictureResolution.width, defaultPictureResolution.height);
	        Log.d(TAG, "No suitable picture resolutions, using default: " + defaultResolution);

	        return defaultResolution;
	    }
	  
	  //---------------------------------------find best Preview Resolution
	  private Point findBestPreviewResolution(Camera.Parameters p) {
		  Camera.Size defaultPreviewResolution = p.getPreviewSize();
		  Log.d(TAG, "camera default resolution " + defaultPreviewResolution.width +
				  "x" + defaultPreviewResolution.height);

	      List<Camera.Size> rawSupportedSizes = p.getSupportedPreviewSizes();
	      if (rawSupportedSizes == null) {
	    	  Log.w(TAG, "Device returned no supported preview sizes; using default");
	    	  return new Point(defaultPreviewResolution.width, defaultPreviewResolution.height);
	        }
	      
	      // ���շֱ��ʴӴ�С����
	      List<Camera.Size> supportedPreviewResolutions = new ArrayList<Camera.Size>(rawSupportedSizes);
	      Collections.sort(supportedPreviewResolutions, new Comparator<Camera.Size>() {
	    	  @Override
	          public int compare(Camera.Size a, Camera.Size b) {
	    		  int aPixels = a.height * a.width;
	              int bPixels = b.height * b.width;
	              if (bPixels < aPixels) {
	                  return -1;
	              }
	              if (bPixels > aPixels) {
	                  return 1;
	              }
	             return 0;
	          }
	      });
	      
	      //log supported preview resolution
	      StringBuilder previewResolutionSb = new StringBuilder();
	      for (Camera.Size supportedPreviewResolution : supportedPreviewResolutions) {
	    	  previewResolutionSb.append(supportedPreviewResolution.width)
	    	  	.append('x').append(supportedPreviewResolution.height).append(' ');
	        }
	      Log.v("Supported preview", "Supported preview resolutions: " + previewResolutionSb);

	      //Iterator
	      double screenAspectRatio = (double) Utils.getScreenWH(getContext()).widthPixels/ (double) Utils.getScreenWH(getContext()).heightPixels;
	      Iterator<Camera.Size> it = supportedPreviewResolutions.iterator();
	      while (it.hasNext()) {
	          Camera.Size supportedPreviewResolution = it.next();
	          int width = supportedPreviewResolution.width;
	          int height = supportedPreviewResolution.height;

	          boolean isCandidatePortrait = width > height;  //Portrait 
	          int maybeFlippedWidth = isCandidatePortrait ? height : width;
	          int maybeFlippedHeight = isCandidatePortrait ? width : height;
	          
	          double aspectRatio = (double) maybeFlippedHeight / (double) maybeFlippedWidth;
	          double distortion = Math.abs(aspectRatio - screenAspectRatio);
	          if (distortion > 0.15) {
	        	  it.remove();
	              continue;
	            }

	          //�ҵ�����Ļ�ֱ�����ȫƥ���Ԥ������ֱ���ֱ�ӷ���
	          if (maybeFlippedWidth == Utils.getScreenWH(getContext()).widthPixels && maybeFlippedHeight == Utils.getScreenWH(getContext()).heightPixels) {
	              Point exactPoint = new Point(width, height);
	              Log.d(TAG, "found preview resolution exactly matching screen resolutions: " + exactPoint);
	              return exactPoint;
	          }
	      }

	      //���û���ҵ����ʵģ����һ��к�ѡ�����أ������������������ģ��������ñȽϵ͵Ļ�����̫����
	      if (!supportedPreviewResolutions.isEmpty()) {
	          Camera.Size largestPreview = supportedPreviewResolutions.get(0);
	          Point largestSize = new Point(largestPreview.width, largestPreview.height);
	          Log.d("Supported preview", "using largest suitable preview resolution: " + largestSize);
	          return largestSize;
	        }

	      // û���ҵ����ʵģ��ͷ���Ĭ�ϵ�
	      Point defaultResolution = new Point(defaultPreviewResolution.width, defaultPreviewResolution.height);
	      Log.d(TAG, "No suitable preview resolutions, using default: " + defaultResolution);
	      return defaultResolution;
	    }
	
}
