package com.example.coolweather;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.coolweather.activity.WeatherActivity;
import com.example.coolweather.db.City;
import com.example.coolweather.db.County;
import com.example.coolweather.db.Province;
import com.example.coolweather.util.HttpUtil;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joy on 2016/12/21.
 */

public class AreaFragment extends Fragment {

    private ListView mListview;
    private List<String> dataList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private TextView mTitleText;
    private Button mBackBtn;
    private Province mSelectedProvince;

    private static final int LEVEL_PROVINCE = 0;
    private static final int LEVEL_CITY = 1;
    private static final int LEVEL_COUNTY = 2;
    private int currentLevel = 0;
    private List<Province> mProvinceList;
    private List<City> mCityList;
    private City mSelectedCity;
    private List<County> mListCounty;
    private ProgressDialog mProgressDialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        mListview = (ListView) view.findViewById(R.id.list_view);
        mTitleText = (TextView) view.findViewById(R.id.title_text);
        mBackBtn = (Button) view.findViewById(R.id.back_button);

        adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, dataList);
        mListview.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            private County mCounty;

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    mSelectedProvince = mProvinceList.get(position);
                    currentLevel = LEVEL_CITY;
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    mSelectedCity = mCityList.get(position);
                    currentLevel = LEVEL_COUNTY;
                    queryCounty();
                } else if (currentLevel == LEVEL_COUNTY) {
                    mCounty = mListCounty.get(position);
                    String weatherId = mCounty.getWeatherId();
                    if (getActivity() instanceof MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                    } else if (getActivity() instanceof WeatherActivity) {
                        WeatherActivity weatherActivity = (WeatherActivity) getActivity();
                        weatherActivity.mDrawerLayout.closeDrawers();
                        weatherActivity.mSrl_update_weather.setRefreshing(true);
                        weatherActivity.requestWeather(weatherId);
//                        weatherActivity.mSrl_update_weather.setRefreshing(false);

                    }
                }
            }
        });

        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_CITY) {
                    queryProvince();
                } else if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                }
            }
        });
        queryProvince();
    }

    private void queryCounty() {
        mTitleText.setText(mSelectedCity.getCityName());
        mListCounty = DataSupport.where("cityid=?", mSelectedCity.getId() + "").find(County.class);
        dataList.clear();
        if (mListCounty.size() > 0) {
            for (County county : mListCounty) {
                dataList.add(county.getCountyName());

            }
            adapter.notifyDataSetChanged();
        } else {
            String address = "http://guolin.tech/api/china/" + mSelectedProvince.getProvinceCode() + "/" + mSelectedCity.getCityCode();
            queryFromServer(address);
        }


    }

    private void queryCities() {
        mTitleText.setText(mSelectedProvince.getProvinceName());
        mBackBtn.setVisibility(View.VISIBLE);
        currentLevel = LEVEL_CITY;

        mCityList = DataSupport.where("provinceId = ?", mSelectedProvince.getId() + "").find(City.class);
        if (mCityList.size() > 0) {
            dataList.clear();
            for (City city : mCityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            mListview.setSelection(0);
        } else {
            String address = "http://guolin.tech/api/china/" + mSelectedProvince.getProvinceCode();
            queryFromServer(address);
        }

    }

    private void queryProvince() {
        mTitleText.setText("中国");
        mBackBtn.setVisibility(View.GONE);
        mProvinceList = DataSupport.findAll(Province.class);
        currentLevel = LEVEL_PROVINCE;
        if (mProvinceList.size() > 0) {
            dataList.clear();
            for (Province province : mProvinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address);
        }
    }

    private void queryFromServer(String address) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }


            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    JSONArray array = new JSONArray(response.body().string());
                    if (currentLevel == LEVEL_PROVINCE) {

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject jsonObject = array.getJSONObject(i);
                            Province province = new Province();
                            province.setProvinceName(jsonObject.getString("name"));
                            province.setProvinceCode(jsonObject.getInt("id"));
                            province.save();
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                queryProvince();
                            }
                        });
                    } else if (currentLevel == LEVEL_CITY) {
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject jsonObject = array.getJSONObject(i);
                            City city = new City();
                            city.setCityName(jsonObject.getString("name"));
                            city.setCityCode(jsonObject.getInt("id"));
                            city.setProvinceId(mSelectedProvince.getId());
                            city.save();
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                queryCities();
                            }
                        });

                    } else if (currentLevel == LEVEL_COUNTY) {
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject jsonObject = array.getJSONObject(i);
                            County county = new County();
                            county.setCityId(mSelectedCity.getId());
                            county.setCountyName(jsonObject.getString("name"));
                            county.setWeatherId(jsonObject.getString("weather_id"));
                            county.save();
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                queryCounty();
                            }
                        });
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();


                } finally {
                    closeProgressDialog();
                }

            }

        });
    }

    private void closeProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    private void showProgressDialog() {
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setMessage("正在加载中");
        mProgressDialog.show();
    }

}
