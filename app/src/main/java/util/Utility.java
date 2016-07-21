package util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.City;
import model.CoolWeatherDB;
import model.Country;
import model.Province;

/**
 * Created by Administrator on 2016/7/20.
 */
public class Utility {
    /**
     * 解析和处理服务器返回的省级数据
     * */
    public synchronized static boolean handleProvincesResponse(CoolWeatherDB coolWeatherDB, String response) {

        if (!TextUtils.isEmpty(response)) {


            String[] allProvinces = response.split(",");
            if (allProvinces != null && allProvinces.length > 0) {
                for (String p : allProvinces) {
                    String[] array = p.split(":");
                    Province province = new Province();
                    province.setProvinceCode(getCode(array[0]));
                    String name = array[1].substring(1, array[1].length() - 1);
                    province.setProvinceName(name);
                    //将数据存储到Province表
                    coolWeatherDB.saveProvince(province);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 解析和处理服务器返回的市级数据
     * */
    public static boolean handleCitiesResponse(CoolWeatherDB coolWeatherDB, String response, Province province) {


        if (!TextUtils.isEmpty(response)) {
            String[] allCities = response.split(",");
            for (String c : allCities) {
                String[] array = c.split(":");
                City city = new City();
                city.setCityCode(province.getProvinceCode() + getCode(array[0]));

                String cityName = array[1].replaceAll("}", "");
                Log.d("tag", cityName);
                String name = cityName.substring(1, cityName.length() - 1);
                city.setCityName(name);
                city.setProvinceId(province.getId());
                coolWeatherDB.saveCity(city);
            }
            return true;
        }
        return false;
    }

    /**
     * 处理服务器返回的县数据
     */
    public static boolean handleCountriesResponse(CoolWeatherDB coolWeatherDB, String response, City city) {

        if (!TextUtils.isEmpty(response)) {
            String[] allCountries = response.split(",");
            for (String cou : allCountries) {
                Country country = new Country();
                String[] array = cou.split(":");
                country.setCountryCode(city.getCityCode() + getCode(array[0]));
                String name = array[1].substring(1, array[1].length() - 1);
                country.setCountryName(name);
                country.setCityId(city.getId());
                coolWeatherDB.saveCountry(country);
            }
            return true;
        }

        return false;
    }

    public static String getCode(String code) {
        Pattern p = Pattern.compile("[\\d]+");//获得某种格式
        Matcher m = p.matcher(code);//将格式应用在code上获得一个匹配器
        if (m.find()) {
            return m.group();
        }
        return null;
    }

    /**
     * 解析服务器返回的JSON数据，并将解析出的数据存储到本地
     * */
    public static boolean handleWeatherResponse(Context context, String response) {
        if (!TextUtils.isEmpty(response)) {
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONObject weatherInfo = jsonObject.getJSONObject("weatherInfo");
                String cityName = weatherInfo.getString("city");
                String countryCode = weatherInfo.getString("cityid");
                String temp1 = weatherInfo.getString("temp1");
                String temp2 = weatherInfo.getString("temp2");
                String weatherDesp = weatherInfo.getString("weather");
                String publishTime = weatherInfo.getString("ptime");
                saveWeatherInfo(context, cityName, countryCode, temp1, temp2, weatherDesp,
                        publishTime);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * 将服务器返回的所有天气信息存储到SharedPreferences中
     */
    public static void saveWeatherInfo(Context context, String cityName, String countryCode, String temp1,
    String temp2, String weatherDesp, String publishTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.CHINA);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean("city_selected", true);
        editor.putString("city_name", cityName);
        editor.putString("country_code", countryCode);
        editor.putString("temp1", temp1);
        editor.putString("temp2", temp2);
        editor.putString("weather_desp", weatherDesp);
        editor.putString("publish_time", publishTime);
        editor.putString("current_date", sdf.format(new Date()));
        editor.commit();
    }
}

