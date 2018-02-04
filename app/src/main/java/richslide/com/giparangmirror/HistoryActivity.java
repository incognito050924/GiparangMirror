package richslide.com.giparangmirror;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import richslide.com.giparangmirror.common.GiparangConst;
import richslide.com.giparangmirror.service.HttpClient;
import richslide.com.giparangmirror.view.Preview;

public class HistoryActivity extends BaseActivity {

    private RadarChart mChart;
    Activity activity = this;
    private static final String TAG = "HistoryActivity";

    LinearLayout mLinearLayout;

    TextView mTxtDays, mTxtTimes;
    Timer mTimer;
    int defaultYear = 2017;
    int defaultMonth = 11;

    Spinner spinner;

    TextView mTotal, mSelectedTotal, mSelectedTotalText;

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

    private View.OnClickListener barChartOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            for (int i=0; i < mLinearLayout.getChildCount();i++) {
                FrameLayout fl = (FrameLayout) mLinearLayout.getChildAt(i);
                Button btn = (Button) fl.getChildAt(1);
                btn.setTextColor(getResources().getColorStateList(R.color.gray333));
                btn.setBackground(ContextCompat.getDrawable(activity,R.color.blank));
            }
            Button button = (Button)view;
            button.setTextColor(getResources().getColorStateList(R.color.black));
            button.setBackground(ContextCompat.getDrawable(activity,R.color.blue));
            String day = button.getText()+"";

            NetworkTaskDay networkTask = new NetworkTaskDay();
            networkTask.execute("/"+spinner.getSelectedItem()+"/"+day);

            //setData();
        }
    };

    private AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener(){
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String params = (String)spinner.getItemAtPosition(position);
            calMonthData(params);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        mTxtDays = (TextView) findViewById(R.id.txtDays);
        mTxtTimes = (TextView) findViewById(R.id.txtTimes);
        spinner = (Spinner)findViewById(R.id.spinnerYearMonth);

        mTotal = (TextView) findViewById(R.id.txtTotal);
        mSelectedTotal = (TextView) findViewById(R.id.txtSelectedTotal);
        mSelectedTotalText = (TextView) findViewById(R.id.txtSelectedTotalText);
        //event listener
        spinner.setOnItemSelectedListener(onItemSelectedListener);

        mLinearLayout = (LinearLayout) findViewById(R.id.layoutBarchart);
        HistoryActivity.MainTimerTask timerTask = new HistoryActivity.MainTimerTask();
        mTimer = new Timer();
        mTimer.schedule(timerTask, 500, 1000);

        ArrayList<String> list = new ArrayList<String>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM");
        int toYear = calendar.get(Calendar.YEAR);
        int toMonth = calendar.get(Calendar.MONTH);
        int mm = (toYear - defaultYear)*12 + (toMonth - defaultMonth);

        for (int i=mm; 0 <= i; i--) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, defaultYear );
            cal.set(Calendar.MONTH, defaultMonth);
            cal.add(Calendar.MONTH, i);
            list.add(sdf.format(cal.getTime()));
        }

        ArrayAdapter spinnerAdapter;
        spinnerAdapter = new ArrayAdapter(this, R.layout.support_simple_spinner_dropdown_item, list);
        spinner.setAdapter(spinnerAdapter);

        mChart = (RadarChart) findViewById(R.id.chart1);
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
    }

    private void calMonthData(String params) {
        mLinearLayout.removeAllViews();
        NetworkTask2 networkTask = new NetworkTask2();
        networkTask.execute("/"+params);
    }

    public void setData() {
        setChartInit();
        float mult = 80;
        float min = 20;
        int cnt = 5;

        ArrayList<RadarEntry> entries1 = new ArrayList<RadarEntry>();
        ArrayList<RadarEntry> entries2 = new ArrayList<RadarEntry>();

        // NOTE: The order of the entries when being added to the entries array determines their position around the center of
        // the chart.
        for (int i = 0; i < cnt; i++) {
            float val1 = (float) (Math.random() * mult) + min;
            entries1.add(new RadarEntry(val1));

            float val2 = (float) (Math.random() * mult) + min;
            entries2.add(new RadarEntry(val2));
        }

        RadarDataSet set1 = new RadarDataSet(entries1,"");
        set1.setColor(Color.rgb(103, 110, 129));
        set1.setFillColor(Color.rgb(103, 110, 129));
        set1.setDrawFilled(true);
        set1.setFillAlpha(180);
        set1.setLineWidth(2f);
        set1.setDrawHighlightCircleEnabled(true);
        set1.setDrawHighlightIndicators(false);

        RadarDataSet set2 = new RadarDataSet(entries2,"");
        set2.setColor(Color.rgb(121, 162, 175));
        set2.setFillColor(Color.rgb(121, 162, 175));
        set2.setDrawFilled(true);
        set2.setFillAlpha(180);
        set2.setLineWidth(2f);
        set2.setDrawHighlightCircleEnabled(true);
        set2.setDrawHighlightIndicators(false);

        ArrayList<IRadarDataSet> sets = new ArrayList<IRadarDataSet>();
        sets.add(set1);
        sets.add(set2);

        RadarData data = new RadarData(sets);
        data.setValueTextSize(8f);
        data.setDrawValues(false);
        data.setValueTextColor(Color.WHITE);

        mChart.setData(data);
        mChart.invalidate();
    }

    public void goChart(JSONArray jsonArray) throws JSONException {
        setChartInit();
        ArrayList<IRadarDataSet> sets = new ArrayList<IRadarDataSet>();

        for (int i=0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            ArrayList<RadarEntry> entries1 = new ArrayList<RadarEntry>();

            // NOTE: The order of the entries when being added to the entries array determines their position around the center of
            // the chart.
            float total = Float.parseFloat(jsonObject.getString("score_total"));
            float erythema = Float.parseFloat(jsonObject.getString("score_erythema"));
            float emotion  = Float.parseFloat(jsonObject.getString("score_emotion"));
            float pigmentation  = Float.parseFloat(jsonObject.getString("score_pigmentation"));
            float pore  = Float.parseFloat(jsonObject.getString("score_pore"));
            float wrinkle  = Float.parseFloat(jsonObject.getString("score_wrinkle"));
            String measured_at  = jsonObject.getString("measured_at");
            String[] times = measured_at.split("T");
            String day = times[0].split("-")[2];

            Calendar cal = Calendar.getInstance();

            entries1.add(new RadarEntry(pore));
            entries1.add(new RadarEntry(wrinkle));
            entries1.add(new RadarEntry(pigmentation));
            entries1.add(new RadarEntry(emotion));
            entries1.add(new RadarEntry(erythema));

            RadarDataSet set1 = new RadarDataSet(entries1,"");
            if (i != 0) {
                set1.setColor(Color.rgb(233,29,98));
                set1.setFillColor(Color.rgb(233,29,98));
                mSelectedTotal.setText(Integer.toString(Math.round(total)));
                mSelectedTotalText.setText(day+"일 종합점수");
            }else {
                mTotal.setText(Integer.toString(Math.round(total)));
                String selectedDay  = jsonObject.getString("measured_at");
                set1.setColor(Color.rgb(0,187,212));
                set1.setFillColor(Color.rgb(0
                        ,187,212));
            }

            set1.setDrawFilled(true);
            set1.setFillAlpha(130);
            set1.setLineWidth(2f);
            set1.setDrawHighlightCircleEnabled(true);
            set1.setDrawHighlightIndicators(false);
            set1.setValueTextSize(13);
            set1.setValueTypeface(Typeface.createFromAsset(getAssets(),"font/NanumSquareBold.ttf"));
            sets.add(set1);
        }

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
        l.setDrawInside(false);
        l.setForm(Legend.LegendForm.NONE);
        l.setFormSize(0);
    }

    /**
     * Barchar 생성
     * @param total
     * @param day
     * @param isClick
     * @return
     */
    private FrameLayout createChartLayout (int total, int day, boolean isClick) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setLayoutParams(new FrameLayout.LayoutParams(Math.round(53 * dm.density),FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout linearLayout = new LinearLayout(this);
        FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.MATCH_PARENT));
        llp.gravity = Gravity.CENTER_HORIZONTAL;

        linearLayout.setLayoutParams(llp);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(0,0,0,Math.round(26 * dm.density));

        int imageWeight = 100-total;
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,Math.round(0 * dm.density),imageWeight)));
        imageView.setImageResource(R.drawable.img_black);

        LinearLayout chartLinearLayout = new LinearLayout(this);
        chartLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,Math.round(0 * dm.density),total)));
        chartLinearLayout.setOrientation(LinearLayout.VERTICAL);

        ImageView imageView1 = new ImageView(this);
        imageView1.setLayoutParams(new LinearLayout.LayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT)));
        imageView1.setImageResource(R.drawable.img_chart_bar_top);

        ImageView imageView2 = new ImageView(this);
        imageView2.setLayoutParams(new LinearLayout.LayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,Math.round(0 * dm.density),100)));
        imageView2.setImageResource(R.drawable.img_chart_bar);
        imageView2.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView2.setAdjustViewBounds(false);

        chartLinearLayout.addView(imageView1);
        chartLinearLayout.addView(imageView2);

        linearLayout.addView(imageView);
        linearLayout.addView(chartLinearLayout);



        Button button = new Button(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,Math.round(26 * dm.density)));
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.setMargins(0,0,0,Math.round(2 * dm.density));

        button.setText(day+"");
        button.setPadding(0,0,0,0);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP,10);
        button.setTextColor(getResources().getColorStateList(R.color.gray333));
        button.setBackground(ContextCompat.getDrawable(this,R.color.blank));
        button.setLayoutParams(lp);
        button.setOnClickListener(barChartOnClickListener);
        //button.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);

        frameLayout.addView(linearLayout);
        frameLayout.addView(button);

        if (isClick) {
            button.performClick();
        }
        return frameLayout;
    }

    @Override
    protected void onDestroy() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        mTimer.cancel();
        mTimer = null;
        super.onPause();
    }

    @Override
    protected void onResume() {
        HistoryActivity.MainTimerTask timerTask = new HistoryActivity.MainTimerTask();
        mTimer = new Timer();
        mTimer.schedule(timerTask, 500, 3000);
        super.onResume();
    }


    public void goMainHandler(View view) {
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
        finish();
    }

    class MainTimerTask extends TimerTask {
        public void run() {
            mHandler.post(mUpdateTimeTask);
        }
    }

    public class NetworkTask2 extends AsyncTask<String , Integer, String> {
        @Override
        protected String doInBackground(String... uriParam) { // 내가 전송하고 싶은 파라미터

            // Http 요청 준비 작업
            HttpClient.Builder http = new HttpClient.Builder("GET", GiparangConst.SERVER_URL+GiparangConst.SEND_RESULT+uriParam[0]);
            // Parameter 를 전송한다.
            //http.addAllParameters(maps[0]);

            //Http 요청 전송
            HttpClient post = http.create();
            post.request();
            // 응답 상태코드 가져오기
            int statusCode = post.getHttpStatusCode();
            // 응답 본문 가져오기
            String body = null;
            if (statusCode == 200 || statusCode == 201) {
                body = post.getBody();
            } else {

            }

            return body;
        }

        @Override
        protected void onPostExecute(String s) {
            if (s != null) {
                try {
                    JSONArray jsonArray = new JSONArray(s);
                    for (int i=0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String measured_at = jsonObject.getString("measured_at");
                        String[] times = measured_at.split("T");
                        String day = times[0].split("-")[2];
                        float total = Float.parseFloat(jsonObject.getString("score_total"));
                        boolean isClick = false;
                        if (i == jsonArray.length()-1) {
                            isClick = true;
                        }
                        FrameLayout frameLayout = createChartLayout(Math.round(total),Integer.parseInt(day),isClick);
                        mLinearLayout.addView(frameLayout);
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public class NetworkTaskDay extends AsyncTask<String , Integer, String> {
        @Override
        protected String doInBackground(String... uriParam) { // 내가 전송하고 싶은 파라미터

            // Http 요청 준비 작업
            HttpClient.Builder http = new HttpClient.Builder("GET", GiparangConst.SERVER_URL+GiparangConst.SEND_RESULT+uriParam[0]);
            // Parameter 를 전송한다.
            //http.addAllParameters(maps[0]);

            //Http 요청 전송
            HttpClient post = http.create();
            post.request();
            // 응답 상태코드 가져오기
            int statusCode = post.getHttpStatusCode();
            // 응답 본문 가져오기
            String body = null;
            if (statusCode == 200 || statusCode == 201) {
                body = post.getBody();
            } else {

            }

            return body;
        }

        @Override
        protected void onPostExecute(String s) {
            if (s != null) {
                try {
                    JSONArray jsonArray = new JSONArray(s);
                    goChart(jsonArray);
                }catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
