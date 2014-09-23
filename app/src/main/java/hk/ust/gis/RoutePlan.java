package hk.ust.gis;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.route.RoutePlanSearch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.o.android.map.Graphic;
import cn.o.android.map.GraphicsLayer;
import cn.o.android.map.Layer;
import cn.o.android.map.LayerEventListener;
import cn.o.android.map.MapView;
import cn.o.android.map.OcnMap2D5Layer;
import cn.o.android.map.geometry.OPath;
import cn.o.android.map.geometry.OPoint;
import cn.o.android.map.geometry.OPolyline;
import cn.o.android.symbol.ISymbol;
import cn.o.android.symbol.SimpleLineSymbol;
import cn.o.android.symbol.SimpleMarkerSymbol;
import hk.ust.gis.Point.PointType;

public class RoutePlan extends Activity {


    MapView oMap;
    GraphicsLayer gLayer = new GraphicsLayer();
    ISymbol symbol;
    OPolyline oPolyline = new OPolyline(new OPath());
    private OPoint sPoint = null;
    private OPoint ePoint = null;
    private OPoint cPoint = null;
    private Graphic sPointGraphic = null;
    private Graphic ePointGraphic = null;
    private Graphic aPointGraphic = null;
    private static int POINT_RADIUS = 5;
    private static int LINE_WIDTH = 5;
    RouteLine route = null;
    RoutePlanSearch mSearch = null;    // 搜索模块，也可去掉地图模块独立使用
    private LayerEventListener pointListener;
    private PointType pointType = PointType.EMPTY;

    public List<Segment> routesList = new ArrayList<Segment>();
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

        pointListener = new LayerEventListener() {
            @Override
            public boolean onTouch(Layer layer, MotionEvent event) {

                OPoint scrPt = new OPoint(event.getX(), event.getY());
                // 屏幕坐标转换为地图坐标
                cPoint = oMap.screenToProjPoint(scrPt);

                switch (pointType) {
                    case START:
                        sPoint = cPoint;
                        drawPoint(sPoint, PointType.START);
                        break;
                    case END:
                        ePoint = cPoint;

                        drawPoint(ePoint, PointType.END);

                        break;
                    case ATTACH:
                        OPoint roadPoint = pointAttachToRouteNet(cPoint);
                        drawPoint(roadPoint, PointType.ATTACH);

                        break;
                }

                pointType = PointType.EMPTY;

                return true;
            }

        };
        gLayer.addEventListener(pointListener);
        try {
            loadRouteRecord();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private OPoint pointAttachToRouteNet(OPoint originPoint){
        Point p = new Point(originPoint);
        Segment s = RouteFinder.findNearestSegment(routesList, p);
        p = s.projectPoint(p);

        return new OPoint((float)p.getX(), (float)p.getY());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.route_plan, menu);

        return true;
    }
    private void drawPoint(OPoint p, PointType type) {
        if (type == PointType.START) {
            symbol = new SimpleMarkerSymbol(Color.BLUE, POINT_RADIUS,
                    SimpleMarkerSymbol.STYLE.CIRCLE);
            if (sPointGraphic != null) {
                gLayer.removeGraphic(sPointGraphic);
            }

            sPointGraphic = new Graphic(p, symbol);
            gLayer.addGraphic(sPointGraphic);
        } else if (type == PointType.END){
            symbol = new SimpleMarkerSymbol(Color.RED, POINT_RADIUS,
                    SimpleMarkerSymbol.STYLE.CIRCLE);
            if (ePointGraphic != null) {
                gLayer.removeGraphic(ePointGraphic);
            }

            ePointGraphic = new Graphic(p, symbol);
            gLayer.addGraphic(ePointGraphic);
        } else if (type == PointType.ATTACH) {
            symbol = new SimpleMarkerSymbol(Color.BLACK, POINT_RADIUS + 5,
                    SimpleMarkerSymbol.STYLE.CIRCLE);
            if (aPointGraphic != null) {
                gLayer.removeGraphic(aPointGraphic);
            }

            aPointGraphic = new Graphic(p, symbol);
            gLayer.addGraphic(aPointGraphic);
        }

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


    public void onSearchButtonProcess(View v) {

        if (sPoint == null) {
            Toast.makeText(getApplicationContext(), "Please choose the start point!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (ePoint == null) {
            Toast.makeText(getApplicationContext(), "Please choose the end point!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        gLayer.removeAll();

        List<Segment> route = RouteFinder.findRoute(routesList,new Point(sPoint), new Point(ePoint));
        for(Segment s : route){
            drawRoute(s);
        }

        oMap.refresh();
    }


    public void onAttachPoint(View v) {
        pointType = PointType.ATTACH;

    }



    private void drawRoute(Segment s){
        symbol = new SimpleLineSymbol(Color.RED);
        // 设置线宽
        ((SimpleLineSymbol) symbol).setWidth(LINE_WIDTH);
        ((SimpleLineSymbol) symbol).setAlpha(100);
        oPolyline = new OPolyline(new OPath());
        oPolyline.addPoint(new OPoint(s.sPoint.x, s.sPoint.y));
        oPolyline.addPoint(new OPoint(s.ePoint.x, s.ePoint.y));
        Graphic g = new Graphic(oPolyline, symbol);

        gLayer.addGraphic(g);
    }
    public void onSPoint(View v) {
        pointType = PointType.START;

    }
    public void onEPoint(View v) {
        pointType = PointType.END;

    }


    private void loadRouteRecord() throws NumberFormatException, IOException {
//        ClassLoader classLoader = Thread.currentThread()
//                .getContextClassLoader();
//        InputStream input = classLoader
//                .getResourceAsStream("resources/OGIS_Route.txt");
//        BufferedReader br = new BufferedReader(new InputStreamReader(input));
//        String rtRecord = null;
//        while ((rtRecord = br.readLine()) != null) {
//            String rtInfo[] = rtRecord.split(" ");
//            Point point1 = new Point(rtInfo[0], rtInfo[1]);
//            Point point2 = new Point(rtInfo[2], rtInfo[3]);
//            routesList.add(new Segment(point1, point2));
//        }
//        br.close();
        String routeFilePath = "/sdcard/gmission_data/marker/OGIS_Route.txt";
        FileReader reader = new FileReader(routeFilePath);
        BufferedReader br = new BufferedReader(reader);
        String rtRecord = null;
        while ((rtRecord = br.readLine()) != null) {
            String rtInfo[] = rtRecord.split(" ");
            Point point1 = new Point(rtInfo[0], rtInfo[1]);
            Point point2 = new Point(rtInfo[2], rtInfo[3]);
            routesList.add(new Segment(point1, point2));
        }
        reader.close();
    }
}
