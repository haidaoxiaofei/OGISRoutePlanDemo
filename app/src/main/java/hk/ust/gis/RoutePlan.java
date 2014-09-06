package hk.ust.gis;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteLine;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;

import java.util.List;

import cn.o.android.map.Graphic;
import cn.o.android.map.GraphicsLayer;
import cn.o.android.map.MapView;
import cn.o.android.map.OcnMap2D5Layer;
import cn.o.android.map.geometry.OPath;
import cn.o.android.map.geometry.OPoint;
import cn.o.android.map.geometry.OPolyline;
import cn.o.android.symbol.SimpleLineSymbol;
import cn.o.android.utils.ProjConvert;


public class RoutePlan extends Activity implements OnGetRoutePlanResultListener {



    MapView oMap;
    GraphicsLayer gLayer = new GraphicsLayer();
    SimpleLineSymbol symbol = new SimpleLineSymbol(Color.RED);
    final OPolyline oPolyline = new OPolyline(new OPath());

    RouteLine route = null;
    RoutePlanSearch mSearch = null;    // 搜索模块，也可去掉地图模块独立使用


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        // 隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 设置成全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_route_plan);



        oMap = (MapView) findViewById(R.id.omap);
        OcnMap2D5Layer baseLayer = new OcnMap2D5Layer();
        oMap.addLayer(baseLayer);
        oMap.addLayer(gLayer);

        mSearch = RoutePlanSearch.newInstance();
        mSearch.setOnGetRoutePlanResultListener(this);

        symbol.setAlpha(150);
        symbol.setWidth(10);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.route_plan, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void SearchButtonProcess(View v) {



        //设置起终点信息，对于tranist search 来说，城市名无意义
        LatLng startPoint = new LatLng(22.541222,113.978804);// LatLng(latitude, longitude)
        LatLng endPoint = new LatLng(22.541201,113.980628);

        routePlan(startPoint, endPoint);

    }

    public void routePlan(LatLng baiduStartPoint, LatLng baiduEndPoint){
        PlanNode stNode = PlanNode.withLocation(baiduStartPoint);
        PlanNode enNode = PlanNode.withLocation(baiduEndPoint);

        mSearch.walkingSearch((new WalkingRoutePlanOption())
        .from(stNode)
        .to(enNode));
    }


    @Override
    public void onGetWalkingRouteResult(WalkingRouteResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(RoutePlan.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
        }
        if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
            //起终点或途经点地址有岐义，通过以下接口获取建议查询信息
            //result.getSuggestAddrInfo()
            return;
        }
        if (result.error == SearchResult.ERRORNO.NO_ERROR) {

            route = result.getRouteLines().get(0);
            List<WalkingRouteLine.WalkingStep> steps = route.getAllStep();


            for(WalkingRouteLine.WalkingStep r : steps){
                List<LatLng> points = r.getWayPoints();

                if (points.size() >= 2){
                    for(LatLng p : points){

                       OPoint o = ProjConvert.baiduToOcn2(new OPoint((float)p.longitude,  (float)p.latitude));
                       oPolyline.addPoint(o);
                    }
                }
            }

            drawRoute();


        }
    }

    private void drawRoute(){
        Graphic g = new Graphic(oPolyline, symbol);
        OPoint[] opoints = oPolyline.getPaths()[0].getPoints();
        gLayer.addGraphic(g);

        oMap.centerAt(opoints[0], false);
        oMap.refresh();
    }

    @Override
    public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {

    }

    @Override
    public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {

    }


}
