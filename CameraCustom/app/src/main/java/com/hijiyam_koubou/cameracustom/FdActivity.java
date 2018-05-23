package com.hijiyam_koubou.cameracustom;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

public class FdActivity extends Activity implements CvCameraViewListener2 {

	private static final String TAG = "OCVSample::Activity";
	private static final Scalar FACE_RECT_COLOR = new Scalar(0 , 255 , 0 , 255);
	public static final int JAVA_DETECTOR = 0;
	public static final int NATIVE_DETECTOR = 1;

	private MenuItem mItemFace50;
	private MenuItem mItemFace40;
	private MenuItem mItemFace30;
	private MenuItem mItemFace20;
	private MenuItem mItemType;

	private Mat mRgba;
	private Mat mGray;
	private File mCascadeFile;
	private CascadeClassifier mJavaDetector;
	private DetectionBasedTracker mNativeDetector;

	private int mDetectorType = JAVA_DETECTOR;
	private String[] mDetectorName;

	private float mRelativeFaceSize = 0.2f;
	private int mAbsoluteFaceSize = 0;

	private CameraBridgeViewBase mOpenCvCameraView;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			final String TAG = "onManagerConnected[FdA]";
			String dbMsg = "開始";
			try {
				dbMsg = "status=" + status;
				switch ( status ) {
					case LoaderCallbackInterface.SUCCESS: {
						dbMsg += ",OpenCV loaded successfully";
						// Load native library after(!) OpenCV initialization
						System.loadLibrary("detection_based_tracker");

						try {
							dbMsg += ",load cascade file from application resources";
							InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
							File cascadeDir = getDir("cascade" , Context.MODE_PRIVATE);
							dbMsg += ",cascadeDir=" + cascadeDir.getName();
							mCascadeFile = new File(cascadeDir , "lbpcascade_frontalface.xml");
							FileOutputStream os = new FileOutputStream(mCascadeFile);

							byte[] buffer = new byte[4096];
							int bytesRead;
							while ( (bytesRead = is.read(buffer)) != -1 ) {
								os.write(buffer , 0 , bytesRead);
							}
							is.close();
							os.close();

							mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
							if ( mJavaDetector.empty() ) {
								dbMsg += ",Failed to load cascade classifier";
								mJavaDetector = null;
							} else
								dbMsg += ",Loaded cascade classifier from " + mCascadeFile.getAbsolutePath();
							mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath() , 0);

							cascadeDir.delete();

						} catch (IOException er) {
							myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
						}
						mOpenCvCameraView.enableView();
					}
					break;
					default: {
						super.onManagerConnected(status);
					}
					break;
				}
				myLog(TAG , dbMsg);
			} catch (Exception er) {
				myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
			}
		}
	};

	public FdActivity() {
		final String TAG = "FdActivity[FdA]";
		String dbMsg = "開始";
		try {
			mDetectorName = new String[2];
			mDetectorName[JAVA_DETECTOR] = "Java";
			mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";
			dbMsg = ",Instantiated new " + this.getClass();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}

	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		final String TAG = "onCreate[FdA]";
		String dbMsg = "開始";
		try {
			dbMsg = "called onCreate";
			super.onCreate(savedInstanceState);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

			setContentView(R.layout.face_detect_surface_view);

			mOpenCvCameraView = ( CameraBridgeViewBase ) findViewById(R.id.fd_activity_surface_view);
			mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
			mOpenCvCameraView.setCvCameraViewListener(this);
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		final String TAG = "onPause[FdA]";
		String dbMsg = "開始";
		try {
			if ( mOpenCvCameraView != null )
				mOpenCvCameraView.disableView();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	//OpenCV Managerを使用する　場合はこのマスクを解除
	static {
		System.loadLibrary("opencv_java3");
	}

	@Override
	public void onResume() {
		super.onResume();
		final String TAG = "onResume[FdA]";
		String dbMsg = "開始";
		try {
			//OpenCV Managerを使用する　場合はこのマスクを解除
			//	https://qiita.com/denjin-m/items/8b2f30b98ef4529b8f1f
//			dbMsg = ",initDebug=" + OpenCVLoader.initDebug();
//			if ( !OpenCVLoader.initDebug() ) {
//				dbMsg = "Internal OpenCV library not found. Using OpenCV Manager for initialization";
//				OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0 , this , mLoaderCallback);
//				//OPENCV_VERSION_3_0_0	;
//			} else {
				dbMsg = "OpenCV library found inside package. Using it!";
				mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}

	}

	public void onDestroy() {
		super.onDestroy();
		final String TAG = "onDestroy[FdA]";
		String dbMsg = "開始";
		try {
			mOpenCvCameraView.disableView();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public void onCameraViewStarted(int width , int height) {
		final String TAG = "onCameraViewStarted[FdA]";
		String dbMsg = "開始";
		try {
			mGray = new Mat();
			mRgba = new Mat();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public void onCameraViewStopped() {
		final String TAG = "onCameraViewStarted[FdA]";
		String dbMsg = "開始";
		try {
			mGray.release();
			mRgba.release();
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		final String TAG = "onCameraFrame[FdA]";
		String dbMsg = "開始";
		try {
			mRgba = inputFrame.rgba();
			mGray = inputFrame.gray();

			if ( mAbsoluteFaceSize == 0 ) {
				int height = mGray.rows();
				if ( Math.round(height * mRelativeFaceSize) > 0 ) {
					mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
				}
				mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
			}

			MatOfRect faces = new MatOfRect();

			if ( mDetectorType == JAVA_DETECTOR ) {
				if ( mJavaDetector != null )
					mJavaDetector.detectMultiScale(mGray , faces , 1.1 , 2 , 2 , // TODO: objdetect.CV_HAAR_SCALE_IMAGE
							new Size(mAbsoluteFaceSize , mAbsoluteFaceSize) , new Size());
			} else if ( mDetectorType == NATIVE_DETECTOR ) {
				if ( mNativeDetector != null )
					mNativeDetector.detect(mGray , faces);
			} else {
				Log.e(TAG , "Detection method is not selected!");
			}

			Rect[] facesArray = faces.toArray();
			for ( int i = 0 ; i < facesArray.length ; i++ )
				Imgproc.rectangle(mRgba , facesArray[i].tl() , facesArray[i].br() , FACE_RECT_COLOR , 3);

			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return mRgba;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final String TAG = "setDetectorType[FdA]";
		String dbMsg = "開始";
		try {
			dbMsg = "called onCreateOptionsMenu";
			mItemFace50 = menu.add("Face size 50%");
			mItemFace40 = menu.add("Face size 40%");
			mItemFace30 = menu.add("Face size 30%");
			mItemFace20 = menu.add("Face size 20%");
			mItemType = menu.add(mDetectorName[mDetectorType]);
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final String TAG = "setDetectorType[FdA]";
		String dbMsg = "開始";
		try {
				dbMsg = "called onOptionsItemSelected; selected item: " + item;
			if ( item == mItemFace50 )
				setMinFaceSize(0.5f);
			else if ( item == mItemFace40 )
				setMinFaceSize(0.4f);
			else if ( item == mItemFace30 )
				setMinFaceSize(0.3f);
			else if ( item == mItemFace20 )
				setMinFaceSize(0.2f);
			else if ( item == mItemType ) {
				int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
				item.setTitle(mDetectorName[tmpDetectorType]);
				setDetectorType(tmpDetectorType);
			}			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
		return true;
	}

	private void setMinFaceSize(float faceSize) {
		final String TAG = "setDetectorType[FdA]";
		String dbMsg = "開始";
		try {
			mRelativeFaceSize = faceSize;
			mAbsoluteFaceSize = 0;
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}
	}

	private void setDetectorType(int type) {
		final String TAG = "setDetectorType[FdA]";
		String dbMsg = "開始";
		try {
			dbMsg = "type="+type;
			if ( mDetectorType != type ) {
				mDetectorType = type;
				if ( type == NATIVE_DETECTOR ) {
					dbMsg += "Detection Based Tracker enabled";
					mNativeDetector.start();
				} else {
					dbMsg += "Cascade detector enabled";
					mNativeDetector.stop();
				}
			}
			myLog(TAG , dbMsg);
		} catch (Exception er) {
			myErrorLog(TAG , dbMsg + ";でエラー発生；" + er);
		}

	}
	/////////////////////////////////////////////
//public void messageShow(String titolStr, String mggStr) {
//	CS_Util UTIL = new CS_Util();
//	UTIL.messageShow(titolStr, mggStr, this);
//}

	public static void myLog(String TAG , String dbMsg) {
		CS_Util UTIL = new CS_Util();
		UTIL.myLog(TAG , dbMsg);
	}

	public static void myErrorLog(String TAG , String dbMsg) {
		CS_Util UTIL = new CS_Util();
		UTIL.myErrorLog(TAG , dbMsg);
	}
}


//https://blogs.osdn.jp/2017/02/10/opencv.html

/**
 Android StudioでOpenCVを使えるようにする
 OpenCV   Managerを使用しないで  B: 共有ライブラリをアプリケーションパッケージ(.apk)に含める
		 https://qiita.com/denjin-m/items/8b2f30b98ef4529b8f1f
 *     CMakeでAndroid向けのOpenCVを利用する
 *            //  https://qiita.com/ara_tack/items/d98bce625cb302e3714a
 * */