package activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.app.R;

import java.util.ArrayList;
import java.util.List;

import model.City;
import model.CoolWeatherDB;
import model.Country;
import model.Province;
import util.HttpCallbackListener;
import util.HttpUtil;
import util.Utility;

/**
 * Created by Administrator on 2016/7/20.
 */
/**
 * 遍历省市县
 * */
public class ChooseAreaActivity extends AppCompatActivity {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTRY = 2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> arrayAdapter;
    private CoolWeatherDB coolWeatherDB;
    private List<String> dataList = new ArrayList<String>();
    /**
     * 省列表
     * */
    private List<Province> provinceList;
    /**
     * 市列表
     * */
    private List<City> cityList;
    /**
     * 县列表
     * */
    private List<Country> countryList;
    /**
     * 选中的省份
     * */
    private Province selectedProvince;
    /**
    * 选中的城市
     * */
    private City selectedCity;
    /**
     * 选中的级别
     * */
    private int currentLevel;

    /**
     * 是否是从WeatherActivity中跳转过来
     */
    private boolean isFromWeatherActivity;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("city_selected", false) && !isFromWeatherActivity) {
            Intent intent = new Intent (this, WeatherActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);
        listView = (ListView) findViewById(R.id.list_view);
        titleText = (TextView) findViewById(R.id.title_text);
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(arrayAdapter);
        coolWeatherDB = CoolWeatherDB.getInstance(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCountries();
                } else if (currentLevel == LEVEL_COUNTRY) {

                    String countryCode = countryList.get(position).getCountryCode();
                    Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
                    intent.putExtra("country_code", countryCode);
                    startActivity(intent);
                    finish();
                }
            }

        });
        queryProvinces();//加载省级数据
    }

    /**
     * 查询全国的省，优先从数据库查询，没有的话去服务器上查询
     */
    private void queryProvinces() {
        provinceList = coolWeatherDB.loadProvinces();
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            arrayAdapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("中国");
            currentLevel = LEVEL_PROVINCE;
        } else {
            queryFromServer(null, LEVEL_PROVINCE);
        }
    }

    /**
     * 查询选中省内所有的市，优先从数据库查询，没有的话去服务器上查询
     * */
    private void queryCities() {
        cityList = coolWeatherDB.loadCities(selectedProvince.getId());
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            arrayAdapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
        } else {
            queryFromServer(selectedProvince.getProvinceCode(), LEVEL_CITY);
        }

    }

    /**
     * 查询选中市内所有的县，优先从数据库查询，如果没有的话去服务器上查询
     */
    private void queryCountries() {
        countryList = coolWeatherDB.loadCounties(selectedCity.getId());
        if (countryList.size() > 0) {
            dataList.clear();
            for (Country country : countryList) {
                dataList.add(country.getCountryName());
            }
            arrayAdapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTRY;
        } else {
            queryFromServer(selectedCity.getCityCode(), LEVEL_COUNTRY);
        }
    }

    /**
     * 根据传入的代码和类型从服务器上查询省市县数据
     * */
    private void queryFromServer(final String code, final int type) {
        String address = "";

            switch (type) {
                case LEVEL_PROVINCE:
                    address = "http://www.weather.com.cn/data/city3jdata/china.html";
                    break;
                case LEVEL_CITY:
                    address = "http://www.weather.com.cn/data/city3jdata/provshi/" + code + ".html";
                    break;
                case LEVEL_COUNTRY:
                    address = "http://www.weather.com.cn/data/city3jdata/station/" + code +".html";

                    break;
            }



        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {

            @Override
            public void onFinish(String response) {
                boolean result = false;
                switch (type) {
                    case LEVEL_PROVINCE:
                        result = Utility.handleProvincesResponse(coolWeatherDB, response);
                        break;
                    case LEVEL_CITY:
                        result = Utility.handleCitiesResponse(coolWeatherDB, response, selectedProvince);
                        break;
                    case LEVEL_COUNTRY:
                        result = Utility.handleCountriesResponse(coolWeatherDB, response, selectedCity);

                        break;
                }

                if (result) {
                    //通过runOnUiThread回到主线程
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            closeProgressDialog();
                            if (LEVEL_PROVINCE == type) {
                                queryProvinces();
                            } else if (LEVEL_CITY == type) {
                                queryCities();
                            } else  {
                                queryCountries();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                //还是通过runOnUiThread（)回到主线程
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();

                    }
                });

            }
        });
    }

    /**
     * 显示进度对话框
     * */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载。。。");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     * */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    /**
     * 捕获Back键，根据当前的级别来判断，此时应该返回市列表，省列表，还是直接退出*/
    @Override
    public void onBackPressed() {
        if (currentLevel == LEVEL_COUNTRY) {
            queryCities();
        } else if (currentLevel == LEVEL_CITY) {
            queryProvinces();
        } else {
            if (isFromWeatherActivity) {
                Intent intent = new Intent(this, WeatherActivity.class);
            startActivity(intent);
        }
            finish();//关闭活动
        }
    }
}
