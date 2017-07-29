package com.punuo.sys.app.xungeng.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.punuo.sys.app.R;
import com.punuo.sys.app.xungeng.adapter.PoiAdapter;
import com.punuo.sys.app.xungeng.model.MyPoiItem;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by acer on 2016/11/2.
 */

public class SendLocation extends Activity implements LocationSource, AMapLocationListener, GeocodeSearch.OnGeocodeSearchListener,View.OnClickListener {
    @Bind(R.id.mapview)
    MapView mapview;
    @Bind(R.id.poiloc)
    ListView poiloc;
    @Bind(R.id.back)
    ImageButton back;
    @Bind(R.id.title)
    TextView title;
    @Bind(R.id.send)
    Button send;

    private AMap aMap = null;
    public AMapLocationClient mapLocationClient;
    public AMapLocationClientOption mapLocationClientOption;
    private OnLocationChangedListener mListener;
    private double latit;
    private double longit;
    GeocodeSearch search;
    private PoiSearch.Query query;
    private PoiAdapter poiAdapter;
    private List<MyPoiItem> myPoiItems = new ArrayList<>();
    private Handler handler = new Handler();
    RegeocodeAddress address;
    List<PoiItem> poiItems = new ArrayList<>();
    int lastposition = -1;
    MarkerOptions marker = new MarkerOptions();
    private Marker lastmark = null;
    private String city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sendlocation);
        ButterKnife.bind(this);
        mapview.onCreate(savedInstanceState);
        title.setText("位置信息");
        send.setOnClickListener(this);
        back.setOnClickListener(this);
        if (aMap == null) {
            aMap = mapview.getMap();
        }

        setUpMap();
        search = new GeocodeSearch(this);
        search.setOnGeocodeSearchListener(this);
        poiAdapter = new PoiAdapter(SendLocation.this, myPoiItems);
        poiloc.setAdapter(poiAdapter);
        poiloc.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (lastposition != -1 && lastposition != position) {
                    myPoiItems.get(lastposition).setIsselect(false);
                }
                myPoiItems.get(position).setIsselect(true);
                Log.d("111", myPoiItems.get(position).getPoiItem().getSnippet());
                lastposition = position;
                LatLonPoint point = myPoiItems.get(position).getPoiItem().getLatLonPoint();
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(point.getLatitude(), point.getLongitude()), 17f));
                if (lastmark != null) {
                    lastmark.remove();
                }
                lastmark = aMap.addMarker(marker.position(new LatLng(point.getLatitude(), point.getLongitude())));
                poiAdapter.notifyDataSetChanged();

            }
        });
    }

    private void setUpMap() {
        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.moveCamera(CameraUpdateFactory.zoomTo(17f));
        // aMap.setMyLocationType()
    }

    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mapLocationClient == null) {
            mapLocationClient = new AMapLocationClient(this);
            mapLocationClientOption = new AMapLocationClientOption();
            //设置定位监听
            mapLocationClientOption.setOnceLocation(true);
            mapLocationClientOption.setNeedAddress(true);
            mapLocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mapLocationClient.setLocationOption(mapLocationClientOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mapLocationClient.startLocation();
        }
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mapLocationClient != null) {
            mapLocationClient.stopLocation();
            mapLocationClient.onDestroy();
        }
        mapLocationClient = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapview.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapview.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapview.onSaveInstanceState(outState);
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (mListener != null && aMapLocation != null) {
            if (aMapLocation != null
                    && aMapLocation.getErrorCode() == 0) {
                mListener.onLocationChanged(aMapLocation);// 显示系统小蓝点
                latit = aMapLocation.getLatitude();
                longit = aMapLocation.getLongitude();
                lastmark = aMap.addMarker(marker.position(new LatLng(latit, longit)));
                RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(latit, longit), 200, GeocodeSearch.AMAP);
                search.getFromLocationAsyn(query);
            } else {
                String errText = "定位失败," + aMapLocation.getErrorCode() + ": " + aMapLocation.getErrorInfo();
                Log.e("AmapErr", errText);
            }
        }
    }


    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
        address = regeocodeResult.getRegeocodeAddress();
        Log.e("1111", address.getFormatAddress());
        poiItems = address.getPois();
        myPoiItems.clear();
        String s = address.getProvince() + address.getCity() + address.getDistrict();
        PoiItem poiItem = new PoiItem("local", new LatLonPoint(latit, longit),
                address.getFormatAddress(), address.getFormatAddress());
        poiItem.setAdName(s);
        MyPoiItem mypoiItem = new MyPoiItem();
        mypoiItem.setPoiItem(poiItem);
        mypoiItem.setIsselect(true);
        lastposition = 0;
        myPoiItems.add(mypoiItem);
        for (int j = 0; j < poiItems.size(); j++) {
            MyPoiItem myPoiItem = new MyPoiItem();
            poiItems.get(j).setAdName(s);
            myPoiItem.setPoiItem(poiItems.get(j));
            myPoiItems.add(myPoiItem);
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                poiAdapter.notifyDataSetChanged();
            }
        });

    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.send:
                if (lastposition!=-1) {
                    Log.d("111", myPoiItems.get(lastposition).getPoiItem().getSnippet());
                }
                String address=myPoiItems.get(lastposition).getPoiItem().getTitle();
                Intent intent=new Intent();
                intent.putExtra("location",address);
                setResult(RESULT_OK,intent);
                finish();
                break;
            case R.id.back:
                setResult(RESULT_CANCELED);
                finish();
                break;
        }
    }
}
