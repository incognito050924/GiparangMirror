package richslide.com.giparangmirror;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IRadarDataSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import richslide.com.giparangmirror.view.Preview;
import richslide.com.giparangmirror.view.Preview2;

public class MainActivity extends BaseActivity {

    private TextureView mCameraTextureView;
    private Preview2 mPreview;
    Activity mainActivity = this;
    LinearLayout layoutPreview, layoutChart, layoutProgress;
    private static final String TAG = "MAINACTIVITY";
    public static final int REQUEST_CAMERA = 1;
    private RadarChart mChart;

    TextView mErythema,mEmotion,mPigmentation,mPore,mWrinkle,mTotal;
    TextView mToday;

    private Handler handler;
    private Runnable runnable;
    //private ProgressBar bar;
    TextView mTxtDays, mTxtTimes;
    Timer mTimer;
    private Handler mHandler = new Handler();
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            Date rightNow = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat(
                    "yyyy.MM.dd EE§a hh:mm");
            String dateString = formatter.format(rightNow);
            String[] dateStrings = dateString.split("§");
            mTxtDays.setText(dateStrings[0]+"요일");
            mTxtTimes.setText(dateStrings[1]);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 안드로이드 6.0 이상 버전에서는 CAMERA 권한 허가를 요청한다.
        mTxtDays = (TextView) findViewById(R.id.txtDays);
        mTxtTimes = (TextView) findViewById(R.id.txtTimes);
        MainTimerTask timerTask = new MainTimerTask();
        mTimer = new Timer();
        mTimer.schedule(timerTask, 500, 1000);

        mTotal = (TextView) findViewById(R.id.txtTotal);
        mErythema = (TextView) findViewById(R.id.txtErythema);
        mEmotion = (TextView) findViewById(R.id.txtEmotion);
        mPigmentation = (TextView) findViewById(R.id.txtPigmentation);
        mPore = (TextView) findViewById(R.id.txtPore);
        mWrinkle = (TextView) findViewById(R.id.txtWrinkle);
        mToday = (TextView) findViewById(R.id.txtToday);

        layoutProgress = (LinearLayout) this.findViewById(R.id.layoutProgress);

        layoutPreview = (LinearLayout)findViewById(R.id.layoutPreview);
        layoutChart = (LinearLayout)findViewById(R.id.layoutChart);
        mChart = (RadarChart) findViewById(R.id.chart1);
        //mChart.setBackgroundColor(Color.BLACK);
        mChart.setBackgroundResource(R.drawable.img_chart_back);
        mChart.getDescription().setEnabled(false);
        mChart.setWebLineWidth(1f);
        mChart.setWebColor(Color.WHITE);
        mChart.setWebLineWidthInner(1f);
        mChart.setWebColorInner(Color.WHITE);
        mChart.setWebAlpha(100);
        MarkerView mv = new MarkerView(this, R.layout.radar_markerview);
        mv.setChartView(mChart); // For bounds control
        mChart.setMarker(mv); // Set the marker to the chart
        mChart.animateXY(
                1400, 1400,
                Easing.EasingOption.EaseInOutQuad,
                Easing.EasingOption.EaseInOutQuad);


        layoutPreview.setVisibility(View.VISIBLE);
        layoutChart.setVisibility(View.GONE);
        layoutProgress.setVisibility(View.GONE);

        mCameraTextureView = (TextureView) findViewById(R.id.cameraTextureView);
        // 안드로이드 6.0 이상 버전에서는 CAMERA 권한 허가를 요청한다.
        requestPermissionCheck();
    }

    public boolean requestPermissionCheck(){
        int sdkVersion = Build.VERSION.SDK_INT;
        if(sdkVersion >= Build.VERSION_CODES.M) {
            final String[] requiredPermissions = {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
            };
            final List<String> neededPermissions = new ArrayList<>();
            for (final String permission : requiredPermissions) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(),
                        permission) != PackageManager.PERMISSION_GRANTED) {
                    neededPermissions.add(permission);
                }
            }
            if (!neededPermissions.isEmpty()) {
                requestPermissions(neededPermissions.toArray(new String[]{}),
                        REQUEST_CAMERA);
            } else {
                setInit();
            }
        }else{  // version 6 이하일때
            setInit();
            return true;
        }

        return true;
    }

    private void setInit(){
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                mPreview = new Preview2(mainActivity, mCameraTextureView);
                mPreview.openCamera();
            }
        };
        handler.postDelayed(runnable,1000);
        /*
        TimerTask tt = new TimerTask() {
            public void run() {
                mPreview = new Preview(mainActivity, mCameraTextureView);
                mPreview.openCamera();
                Log.d(TAG,"mPreview set");
            }
        };
        Timer timer = new Timer();
        timer.schedule(tt, 1000);
        */

        /*mPreview = new Preview(mainActivity, mCameraTextureView);
        mPreview.openCamera();*/

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA:
                final String[] requiredPermissions = {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.INTERNET,
                        Manifest.permission.ACCESS_NETWORK_STATE
                };
                boolean checked = false;
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    int grantResult = grantResults[i];
                    if (permission.equals(Manifest.permission.CAMERA)) {
                        if(grantResult == PackageManager.PERMISSION_GRANTED) {
                            checked = true;
                        } else {
                            Toast.makeText(this,"카메라 접근권한이 승인되어야 합니다", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } else if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        if(grantResult == PackageManager.PERMISSION_GRANTED) {
                            checked = true;
                        } else {
                            Toast.makeText(this,"SD 카드 접근권한이 승인되어야 합니다", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } else {
                        if(grantResult == PackageManager.PERMISSION_GRANTED) {
                            checked = true;
                        } else {
                            finish();
                        }
                    }
                }

                if (checked) {
                    setInit();
                }
                break;
        }
    }

    public void goChart(JSONObject jsonObject) throws JSONException {
        setChartInit();
        layoutPreview.setVisibility(View.GONE);
        layoutChart.setVisibility(View.VISIBLE);

        ArrayList<RadarEntry> entries1 = new ArrayList<RadarEntry>();

        // NOTE: The order of the entries when being added to the entries array determines their position around the center of
        // the chart.
        float total = Float.parseFloat(jsonObject.getString("score_total"));
        float erythema = Float.parseFloat(jsonObject.getString("score_erythema"));
        float emotion  = Float.parseFloat(jsonObject.getString("score_emotion"));
        float pigmentation  = Float.parseFloat(jsonObject.getString("score_pigmentation"));
        float pore  = Float.parseFloat(jsonObject.getString("score_pore"));
        float wrinkle  = Float.parseFloat(jsonObject.getString("score_wrinkle"));

        mTotal.setText(Integer.toString(Math.round(total)));
        mErythema.setText(Integer.toString(Math.round(erythema)));
        mEmotion.setText(Integer.toString(Math.round(emotion)));
        mPigmentation.setText(Integer.toString(Math.round(pigmentation)));
        mPore.setText(Integer.toString(Math.round(pore)));
        mWrinkle.setText(Integer.toString(Math.round(wrinkle)));
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일 측정결과");
        mToday.setText(sdf.format(cal.getTime()));

        entries1.add(new RadarEntry(pore));
        entries1.add(new RadarEntry(wrinkle));
        entries1.add(new RadarEntry(pigmentation));
        entries1.add(new RadarEntry(emotion));
        entries1.add(new RadarEntry(erythema));

        RadarDataSet set1 = new RadarDataSet(entries1,"");
        set1.setColor(Color.rgb(0,187,212));
        set1.setFillColor(Color.rgb(0,187,212));
        set1.setDrawFilled(true);
        set1.setFillAlpha(130);
        set1.setLineWidth(2f);
        set1.setDrawHighlightCircleEnabled(true);
        set1.setDrawHighlightIndicators(false);
        set1.setValueTextSize(13);
        set1.setValueTypeface(Typeface.createFromAsset(getAssets(),"font/NanumSquareBold.ttf"));
        ArrayList<IRadarDataSet> sets = new ArrayList<IRadarDataSet>();
        sets.add(set1);

        RadarData data = new RadarData(sets);
        data.setValueTextSize(13);
        data.setDrawValues(false);
        data.setValueTextColor(Color.RED);
        data.setValueTypeface(Typeface.createFromAsset(getAssets(),"font/NanumSquareBold.ttf"));

        mChart.setData(data);
        mChart.invalidate();

    }

    private void setChartInit() {

        XAxis xAxis = mChart.getXAxis();
        xAxis.setTextSize(13);
        xAxis.setYOffset(0f);
        xAxis.setXOffset(0f);
        xAxis.setTypeface(Typeface.createFromAsset(getAssets(),"font/NanumSquareBold.ttf"));
        xAxis.setValueFormatter(new IAxisValueFormatter() {

            private String[] mActivities = new String[]{"모공", "주름", "반점", "감정상태", "홍반"};

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return mActivities[(int) value % mActivities.length];
            }
        });
        xAxis.setTextColor(Color.WHITE);

        YAxis yAxis = mChart.getYAxis();
        yAxis.setLabelCount(5, false);
        yAxis.setTextSize(13);
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(80f);
        yAxis.setDrawLabels(false);
        yAxis.setTypeface(Typeface.createFromAsset(getAssets(),"font/NanumSquareBold.ttf"));
        Legend l = mChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(true);
        l.setForm(Legend.LegendForm.NONE);
        l.setFormSize(0);
    }

    public void chartBackPressed(View view) {
        layoutPreview.setVisibility(View.VISIBLE);
        layoutChart.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {

        if (layoutPreview.getVisibility() == View.VISIBLE) {
            super.onBackPressed();
        } else {
            layoutPreview.setVisibility(View.VISIBLE);
            layoutChart.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        super.onDestroy();
        //mPreview.onPause();
    }

    @Override
    protected void onPause() {
        mTimer.cancel();
        mTimer = null;
        super.onPause();
    }

    @Override
    protected void onResume() {
        MainTimerTask timerTask = new MainTimerTask();
        mTimer = new Timer();
        mTimer.schedule(timerTask, 500, 3000);
        super.onResume();
        //mPreview.onResume();
    }

    public void onClickHandler(View view) {
        mPreview.takePicture(mHandler);
        //new ProgressTask().execute();
    }

    public void goHistoryHandler(View view) {
        Intent intent = new Intent(this,HistoryActivity.class);
        startActivity(intent);
        finish();
    }

    class MainTimerTask extends TimerTask {
        public void run() {
            mHandler.post(mUpdateTimeTask);
        }
    }

    public class ProgressExecRunnable implements Runnable {
        @Override
        public void run() {
            layoutProgress.setVisibility(View.VISIBLE);
        }
    }
    public class ProgressDoneRunnable implements Runnable {
        @Override
        public void run() {
            layoutProgress.setVisibility(View.GONE);
        }
    }

}


