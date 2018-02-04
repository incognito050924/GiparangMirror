package richslide.com.giparangmirror.view;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import richslide.com.giparangmirror.AndroidUploader;
import richslide.com.giparangmirror.MainActivity;
import richslide.com.giparangmirror.common.GiparangConst;
import richslide.com.giparangmirror.impl.IUploader;
import richslide.com.giparangmirror.service.HttpClient;

/**
 * Created by BRB_LAB on 2016-06-07.
 */
public class Preview2 extends Thread implements IUploader {
    private final static String TAG = "Preview : ";

    private Size mPreviewSize;
    private Context mContext;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mCaptureSession;
    private TextureView mTextureView;
    CameraManager manager;
    StreamConfigurationMap map;
    String cameraId;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private ProgressDialog mProgressDialog;

    Preview2 preview;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 270);
        ORIENTATIONS.append(Surface.ROTATION_90, 180);
        ORIENTATIONS.append(Surface.ROTATION_180, 90);
        ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    public Preview2(Context context, TextureView textureView) {
        mContext = context;
        mTextureView = textureView;
        preview = this;
    }

    private String getBackFacingCameraId(CameraManager cManager) {
        try {
            for (final String cameraId : cManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cManager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                // CameraCharacteristics.LENS_FACING_FRONT 전면카메라, LENS_FACING_BACK : 후면카메라
                if (cOrientation == CameraCharacteristics.LENS_FACING_BACK) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
    private String getFrontFacingCameraId(CameraManager cManager) {
        try {
            for (final String cameraId : cManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cManager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                // CameraCharacteristics.LENS_FACING_FRONT 전면카메라, LENS_FACING_BACK : 후면카메라
                if (cOrientation == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
    private String getExternalFacingCameraId(CameraManager cManager) {
        try {
            for (final String cameraId : cManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cManager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                // CameraCharacteristics.LENS_FACING_FRONT 전면카메라, LENS_FACING_BACK : 후면카메라
                if (cOrientation == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void openCamera() {
        manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "openCamera E");
        try {
            cameraId = getBackFacingCameraId(manager);
            if (cameraId == null) {
                cameraId = getFrontFacingCameraId(manager);
                if (cameraId == null) getExternalFacingCameraId(manager);
            }
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];

            int permissionCamera = ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
            if(permissionCamera == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.CAMERA}, MainActivity.REQUEST_CAMERA);
            } else {
                manager.openCamera(cameraId, mStateCallback, null);
            }
        } catch (CameraAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener(){

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                              int width, int height) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onSurfaceTextureAvailable, width="+width+",height="+height);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                                int width, int height) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // TODO Auto-generated method stub
        }
    };

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onOpened");
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onDisconnected");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onError");
        }

    };

    protected void startPreview() {
        // TODO Auto-generated method stub
        if(null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            Log.e(TAG, "startPreview fail, return");
        }

        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        if(null == texture) {
            Log.e(TAG,"texture is null, return");
            return;
        }

        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(texture);

        try {
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mPreviewBuilder.addTarget(surface);

        try {
            mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession session) {
                    // TODO Auto-generated method stub
                    mCaptureSession = session;
                    updatePreview(session);
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    // TODO Auto-generated method stub
                    Toast.makeText(mContext, "onConfigureFailed", Toast.LENGTH_LONG).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void updatePreview(CameraCaptureSession session) {
        // TODO Auto-generated method stub
        if(null == mCameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }

        //mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        HandlerThread thread = new HandlerThread("CameraPreview");
        thread.start();
        Handler backgroundHandler = new Handler(thread.getLooper());

        try {
            mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setSurfaceTextureListener()
    {
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    public void onResume() {
        Log.d(TAG, "onResume");
        setSurfaceTextureListener();
    }

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    public void onPause() {
        // TODO Auto-generated method stub
        Log.d(TAG, "onPause");
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
                Log.d(TAG, "CameraDevice Close");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    public void takePicture(final Handler mHandler) {
        // Progress Bar Start
        mHandler.post(((MainActivity) mContext).new ProgressExecRunnable());

        if(null == mCameraDevice) {
            Log.e(TAG, "mCameraDevice is null, return");
            return;
        }

        int width = 1920;
        int height = 1080;
        try {
            Size[] jpegSizes = null;
            if (map != null) {
                jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
                Size optimizedSize = getOptimalPreviewSize(jpegSizes, mPreviewSize.getWidth(), mPreviewSize.getHeight());
                height = optimizedSize.getHeight();
                width = optimizedSize.getWidth();
            }

            /*if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }*/

            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(mTextureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            //captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);


            // Orientation
            int rotation = ((Activity)mContext).getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, sensorToDeviceRotation(false,rotation));

            //final File file = new File(Environment.getExternalStorageDirectory()+"/DCIM", "pic.jpg");


            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File (sdCard.getAbsolutePath() + "/camtest");
            dir.mkdirs();

            String fileName = String.format("%d.jpg", System.currentTimeMillis());
            final File file = new File(dir, fileName);

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                            reader.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {

                        output = new FileOutputStream(file);
                        output.write(bytes);

/*
                        NetworkTask2 networkTask = new NetworkTask2();

                        Map<String, Object> params = new HashMap<String,Object>();
                        params.put("image", bytes);
                        networkTask.execute(params);
*/
                        AndroidUploader uploader = new AndroidUploader(mContext);
                        uploader.uploadPicture(file.getAbsolutePath(), preview);
                        /*
                        if (code == AndroidUploader.ReturnCode.http201) {
                            Toast.makeText(mContext,"파일업로드 성공!",Toast.LENGTH_SHORT).show();
                            file.delete();
                        } else {
                            Toast.makeText(mContext,"파일업로드 실패!   " +code ,Toast.LENGTH_SHORT).show();
                            file.delete();
                        }
                        */
                    } finally {
                        if (null != output) {
                            output.close();
                            // Progress Bar Exit
                            mHandler.post(((MainActivity) mContext).new ProgressDoneRunnable());
                        }
                    }
                }
            };

            HandlerThread thread = new HandlerThread("CameraPicture");
            thread.start();
            final Handler backgroudHandler = new Handler(thread.getLooper());
            reader.setOnImageAvailableListener(readerListener, backgroudHandler);

            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                                               CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    startPreview();
                }

            };

            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, backgroudHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.e("test","test");
                }
            }, backgroudHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public int getSensorOrientation() throws CameraAccessException {
        return manager.getCameraCharacteristics(cameraId).get(
                CameraCharacteristics.SENSOR_ORIENTATION);
    }

    public int sensorToDeviceRotation(boolean mirror, int deviceOrientation) throws CameraAccessException {

        // Reverse device orientation for front-facing cameras
        if (mirror) {
            deviceOrientation = -deviceOrientation;
        }
        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int sensorOrientation =  getSensorOrientation();
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    @Override
    public void loadComplete(JSONObject jsonObject) {
        if (jsonObject != null) {
            try {
//                if (jsonObject.getString("msg") =="NoFaces") {
//                    Toast.makeText(mContext, "인식된 얼굴이 없습니다.", Toast.LENGTH_SHORT).show();
//                }
                String fileName = jsonObject.getString("image");
                File file = new File(fileName);
                if (file.exists()) {
                    file.delete();
                }
                ((MainActivity)mContext).goChart(jsonObject);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private Size getOptimalPreviewSize(Size[] sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;

        // Find Optimized Size
        for (Size size : sizes) {
            double ratio = (double) size.getWidth() / size.getHeight();
//            Log.v("##PictureSize##", "width: "+size.getWidth()+"("+w+")"+
//                    "height :"+size.getHeight()+"("+h+")"+"Ratio: "+ratio+"("+targetRatio+")"+String.valueOf(ratio-targetRatio));
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                if (optimalSize == null) optimalSize = size;
                else {
                    if (optimalSize.getHeight() < size.getHeight()) optimalSize = size;
                }
                minDiff = Math.abs(size.getHeight() - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
//                    optimalSize = size;
//                    minDiff = Math.abs(size.getHeight() - targetHeight);
                    if (optimalSize == null) optimalSize = size;
                    else {
                        if (optimalSize.getHeight() < size.getHeight()) optimalSize = size;
                    }
                    minDiff = Math.abs(size.getHeight() - targetHeight);
                }
            }
        }

        return optimalSize;
    }

    public class NetworkTask2 extends AsyncTask<Map<String, Object>, Integer, String> {


        @Override
        protected String doInBackground(Map<String, Object>... maps) { // 내가 전송하고 싶은 파라미터

            // Http 요청 준비 작업
            HttpClient.Builder http = new HttpClient.Builder("POST", GiparangConst.SERVER_URL+GiparangConst.SEND_IMAGE);
            // Parameter 를 전송한다.
            http.addAllParameters(maps[0]);

            //Http 요청 전송
            HttpClient post = http.create();
            post.request();
            // 응답 상태코드 가져오기
            int statusCode = post.getHttpStatusCode();
            // 응답 본문 가져오기
            String body = post.getBody();
            return body;
        }

        @Override
        protected void onPostExecute(String s) {
            mProgressDialog.dismiss();
        }
    }

}