/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package hk.ust.gis;

import java.util.LinkedList;
import java.util.List;



/**
 *
 * @author bigstone
 */
public class RouteFinder {
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
    
    public static Segment findBestSegment(List<Segment> l, Point p){
        Segment sTmp = null;
        float disMin = Float.MAX_VALUE;
        for (Segment s : l) {
            float disTmp = s.distanceLine(p) + (float) s.length();            
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
    
    public static List<Segment> findRouteSameFloor(List<Segment> rList, Point sPoint, Point ePoint){

    	List<Segment> buffer = new LinkedList<Segment>();
        List<Segment> route = new LinkedList<Segment>();
        int origin = rList.size();
        Segment cRoute = findNearestSegment(rList, sPoint);
        Segment eRoute = findNearestSegment(rList, ePoint);
        route.add(cRoute);
        
        rList.remove(cRoute);
        
        while (!rList.isEmpty()) {
            if (cRoute.equals(eRoute)) {
                break;
            }
            List<Segment> chainedRoutes = extractChainedRoutes(rList, cRoute);
            if (chainedRoutes.isEmpty()) {
            	if (!route.isEmpty()){
            		cRoute = route.remove(route.size() - 1);
                    buffer.add(cRoute);
            	}
            	
                if (route.isEmpty()) {
                	cRoute = findNearestSegment(rList, sPoint);
                	rList.remove(cRoute);
                	route.add(cRoute);
                    continue;
                }
                
                //this part is trick, avoid retrieving segment from empty route list
                cRoute = route.get(route.size() - 1);
            } else {
                Segment bestRoute = findNearestSegment(chainedRoutes, ePoint);
                chainedRoutes.remove(bestRoute);
                
                rList.addAll(chainedRoutes);
                
                if (route.size() >= 2) {
					Segment previous = route.get(route.size() - 2);
					
					if (previous.isChained(bestRoute)) {
						buffer.add(cRoute);
						route.remove(cRoute);
					}
				}
                
                route.add(bestRoute);
               
                cRoute = bestRoute;
                
            }
        }
        
        if (!route.get(route.size()-1).isChained(eRoute)) {
        	buffer.addAll(route);
			route.clear();
		}
        
        
        //return segments to rList
        rList.addAll(buffer);
        rList.addAll(route);

        int after = rList.size();
        trimRoute(route, sPoint, ePoint);
        
        
        return route;        
    }
    
}
