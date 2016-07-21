package activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coolweather.app.R;

import service.AutoUpdateService;
import util.HttpCallbackListener;
import util.HttpUtil;
import util.Utility;

/**
 * Created by Administrator on 2016/7/21.
 */
public class WeatherActivity extends AppCompatActivity implements View.OnClickListener{

    private LinearLayout layoutWeatherInfo;
    private TextView cityName;
    private TextView publishTime;
    private TextView weatherDesp;
    private TextView tvTemp1;
    private TextView tvTemp2;
    private TextView tvCurrentDate;

    private Button btnSwitchCity;
    private Button btnRefreshWeather;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.weather_layout);

        //初始化控件
        layoutWeatherInfo = (LinearLayout) findViewById(R.id.weather_info_layout);
        cityName = (TextView) findViewById(R.id.city_name);
        publishTime = (TextView) findViewById(R.id.publish_text);
        weatherDesp = (TextView) findViewById(R.id.weather_desp);
        tvTemp1 = (TextView) findViewById(R.id.temp1);
        tvTemp2 = (TextView) findViewById(R.id.temp2);
        tvCurrentDate = (TextView) findViewById(R.id.current_date);
        btnSwitchCity = (Button) findViewById(R.id.switch_city);
        btnRefreshWeather = (Button) findViewById(R.id.refresh_weather);
        btnSwitchCity.setOnClickListener(this);
        btnRefreshWeather.setOnClickListener(this);

        String countryCode = getIntent().getStringExtra("country_code");
        if (!TextUtils.isEmpty(countryCode)) {
            //根据县级代号查询天气
            publishTime.setText("同步中");
            layoutWeatherInfo.setVisibility(View.INVISIBLE);
            queryWeatherInfo(countryCode);
        } else {
            //没有县级代码时候显示本地天气
            showWeather();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.switch_city:
                Intent intent = new Intent(this, ChooseAreaActivity.class);
                intent.putExtra("from_weather_activity", true);
                startActivity(intent);
                finish();
                break;
            case R.id.refresh_weather:
                publishTime.setText("同步中。。。");
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                String countryCode = preferences.getString("country_code", "");
                if (!TextUtils.isEmpty(countryCode)) {
                    queryWeatherInfo(countryCode);

                }
                break;
            default:
                break;
        }
    }

    public void queryWeatherInfo(String countryCode) {
        String address = "http://www.weather.com.cn/data/cityinfo/" + countryCode + ".html";
        queryFromServer(address);
    }

    /**
     * 根据传入的地址去服务器查询天气信息
     */
    private void queryFromServer(final String address) {
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                //处理服务器返回的天气信息
                if (Utility.handleWeatherResponse(WeatherActivity.this, response)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showWeather();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            publishTime.setText("同步失败");
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        publishTime.setText("同步失败");
                    }
                });

            }
        });
    }

    private void showWeather() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        cityName.setText(preferences.getString("city_name", ""));
        tvTemp1.setText(preferences.getString("temp1", ""));
        tvTemp2.setText(preferences.getString("temp2", ""));
        weatherDesp.setText(preferences.getString("weather_desp", ""));
        publishTime.setText(preferences.getString("publish_time", ""));
        tvCurrentDate.setText(preferences.getString("current_date", ""));
        layoutWeatherInfo.setVisibility(View.VISIBLE);
        cityName.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);

    }
}
