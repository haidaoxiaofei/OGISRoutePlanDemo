/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hk.ust.gis;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 *
 * @author bigstone
 */
public class RouteFinderPoint {
    private static final int TOP_N_COUNT = 3;


    public static Segment findNearestSegment(List<Segment> l, Point p){
        Segment sTmp = null;
        float disMin = Float.MAX_VALUE;
        for (Segment s : l) {
            float disTmp = s.distanceLine(p);
            if (disTmp < disMin) {
                disMin = disTmp;
                sTmp = s;
            }
        }
        return sTmp;
    }
    

    private static List<Segment> extractChainedRoutes(List<Segment> rList, Segment cRoute){
        List<Segment> chainedRoutes = new LinkedList<Segment>();
        for (int i = rList.size() - 1; i >= 0; i--) {
            if (rList.get(i).isChained(cRoute)) {
                chainedRoutes.add(rList.remove(i));
            }
        }
        return chainedRoutes;
    }

    private static List<Point> findConectedPoints(List<Segment> rList, Point p){
        List<Point>chainedRoutes = new LinkedList<Point>();
        for (Segment s : rList){
            if (s.getsPoint().equals(p)) {
                chainedRoutes.add(s.getePoint());
            }

            if (s.getePoint().equals(p)) {
                chainedRoutes.add(s.getsPoint());
            }
        }

        return chainedRoutes;
    }

    public static List<Segment> findRoute(List<Segment> rList, Point sPoint, Point ePoint){
        if (rList.isEmpty()){
            return null;
        }
        List<Segment> routeList =  findRouteSameFloor(rList, sPoint, ePoint);

    	return routeList;
    }
    
    private static void trimRoute(List<Segment> route, Point sPoint, Point ePoint){
    	if (route.size() < 2) {
			return; 
		}
    	route.set(0, cutSegment(route.get(0), route.get(1), sPoint));
    	route.set(route.size() - 1, cutSegment(route.get(route.size() - 1), route.get(route.size() - 2), ePoint));
    	
    }
    private static Segment cutSegment(Segment so, Segment nearS, Point p){
    	Point projectedP = so.projectPoint(p);
    	
    	if (so.distanceLine(projectedP) > 1) {
			return so;
		}
    	
    	Segment s = new Segment(so);
    	if (nearS.distanceLine(s.sPoint) < nearS.distanceLine(s.ePoint)) {
			s.ePoint.x = projectedP.x;
			s.ePoint.y = projectedP.y;
		} else {
			s.sPoint.x = projectedP.x;
			s.sPoint.y = projectedP.y;
		}
    	return s;
    }
    
    public static float routeLength(List<Segment> route){
    	float length = 0;
    	for (Segment segment : route) {
			length += segment.length();
		}
    	return length;
    }

    public static List<Route> extractBestRoutes(List<Route> routeCandidates, Point target, int count){
        if (routeCandidates.size() < count){
            List<Route> copyCandidate = new ArrayList<Route>(routeCandidates);
            routeCandidates.clear();
            return copyCandidate;
        }

        List<Route> chosenRoutes = new ArrayList<Route>();

        for (int i = 0; i < count; i++) {
            double shortestLength = Double.MAX_VALUE;
            Route bestRoute = null;

            for (Route r : routeCandidates){
                double length = r.routeLength + r.getCurrentPoint().distance(target);
                if(length < shortestLength) {
                    bestRoute = r;
                    shortestLength = length;
                }
            }
            routeCandidates.remove(bestRoute);
            chosenRoutes.add(bestRoute);
        }
        return chosenRoutes;
    }


    public static List<Segment> findRouteSameFloor(List<Segment> rList, Point sPoint, Point ePoint){

        List<Route> routeCandidates = new LinkedList<Route>();


        List<Point> visitedPoints = new ArrayList<Point>();
        Segment startSegment =  findNearestSegment(rList, sPoint);

        Route startRoute1 = new Route();
        startRoute1.pushPoint(startSegment.projectPoint(sPoint));
        startRoute1.pushPoint(startSegment.getsPoint());
        visitedPoints.add(startSegment.getsPoint());

        Route startRoute2 = new Route();
        startRoute2.pushPoint(startSegment.projectPoint(sPoint));
        startRoute2.pushPoint(startSegment.getePoint());
        visitedPoints.add(startSegment.getePoint());

        routeCandidates.add(startRoute1);
        routeCandidates.add(startRoute2);


        Segment endSegment = findNearestSegment(rList, ePoint);

        Point esPoint = endSegment.getsPoint();
        Point eePoint = endSegment.getePoint();
        Point targetPoint = endSegment.projectPoint(ePoint);
        //explore target
        while(true){
            if (routeCandidates.isEmpty()){
                break;
            }
            //extract TOP_N_COUNT routes to expand
            List<Route> checkRouteList = extractBestRoutes(routeCandidates, targetPoint, TOP_N_COUNT);
            //iterate top routes
            for (int i = 0; i < checkRouteList.size(); i++){
                Route r = checkRouteList.get(i);
                List<Point> nextPoints = findConectedPoints(rList, r.getCurrentPoint());
                nextPoints.remove(r.getLastPoint());
                nextPoints.removeAll(visitedPoints);
                if (nextPoints.size() == 1) {
                    r.pushPoint(nextPoints.get(0));
                    routeCandidates.add(r);
                } else if (nextPoints.size() > 1) {
                    for (Point p : nextPoints){
                        routeCandidates.add(r.branch(p));
                    }
                }
                visitedPoints.addAll(nextPoints);
            }


            //check whether reach targets
            if (visitedPoints.contains(esPoint) || visitedPoints.contains(eePoint)){
                Route bestRoute = null;
                double shortestLength = Double.MAX_VALUE;
                for (Route r : routeCandidates) {
                    if(r.getCurrentPoint().equals(esPoint)
                            && r.routeLength + esPoint.distance(targetPoint) < shortestLength){
                        bestRoute = r;
                        shortestLength = r.routeLength + esPoint.distance(targetPoint);
                    }

                    if(r.getCurrentPoint().equals(eePoint)
                            && r.routeLength + eePoint.distance(targetPoint) < shortestLength){
                        bestRoute = r;
                        shortestLength = r.routeLength + eePoint.distance(targetPoint);
                    }
                }

                bestRoute.pushPoint(targetPoint);
                return bestRoute.extractWholeRoute();
            }
        }

        return new ArrayList<Segment>();
    }
    
}
