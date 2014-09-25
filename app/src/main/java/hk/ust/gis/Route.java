package hk.ust.gis;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * Created by bigstone on 9/26/14.
 */
public class Route {
    public LinkedList<Point> routePoints = new LinkedList<Point>();
    public double routeLength = 0;
    private Route parentRoute = null;

    public Route(Route parentRoute) {
        this.parentRoute = parentRoute;
        this.routeLength = parentRoute.routeLength;
    }
    public Route(){

    }

    public Point getCurrentPoint(){
        return routePoints.getLast();
    }

    public Point getLastPoint(){
        return routePoints.get(routePoints.size() - 2);
    }

    public double calculateSegmentLength(){
        double length = 0;
        if(routePoints.size() > 1){
            Point lPoint = routePoints.getFirst();
            for (int i = 1 ; i < routePoints.size(); i++){
                Point cPoint = routePoints.get(i);
                length += lPoint.distance(cPoint);
                lPoint = cPoint;
            }
        }

        return length;
    }

    public void pushPoint(Point p){
        if (!routePoints.isEmpty()){
            Point lastPoint = routePoints.getLast();
            routeLength += lastPoint.distance(p);
        }

        routePoints.addLast(p);
    }

    public Route branch(Point nextPoint) {
        Route newBranch = new Route(this);
        newBranch.pushPoint(this.getCurrentPoint());
        newBranch.pushPoint(nextPoint);
        return newBranch;
    }

    public List<Segment> extractWholeRoute(){
        List<Segment> routeSegment = new ArrayList<Segment>();
        Stack<Route> routeStack = new Stack<Route>();
        Route cRoute = this;
        routeStack.push(cRoute);

        while(cRoute.parentRoute != null){
            cRoute = cRoute.parentRoute;
            routeStack.push(cRoute);
        }

        Point lastPoint = null;
        while (!routeStack.isEmpty()) {
            Route route = routeStack.pop();
            while (!route.routePoints.isEmpty()) {
                Point cPoint = route.routePoints.remove(0);
                if(lastPoint == null || cPoint.equals(lastPoint)){
                    lastPoint = cPoint;
                    continue;
                }
                routeSegment.add(new Segment(lastPoint, cPoint));
                lastPoint = cPoint;
            }

        }
        return routeSegment;
    }
}
