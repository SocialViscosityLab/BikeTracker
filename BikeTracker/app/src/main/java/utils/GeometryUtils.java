package utils;

import android.location.Location;

import java.util.HashMap;
import java.util.Map;

/**
 Abstract utility class with Geometry methods
 */
public class GeometryUtils {

    public GeometryUtils(){ }

    public static Location getIntermediatePoint(Location start, Location end, float fraction){
        Location intermediatePoint = new Location("");

        // Transform all the coordinates to radians to do calculations
        double latO = Math.toRadians(start.getLatitude());
        double lonO = Math.toRadians(start.getLongitude());
        double latF = Math.toRadians(end.getLatitude());
        double lonF = Math.toRadians(end.getLongitude());

        double d = start.distanceTo(end);

        double a = Math.sin((1-fraction)*d)/Math.sin(d);
        double b = Math.sin(fraction*d)/Math.sin(d);

        double myX = a * Math.cos(latO) * Math.cos(lonO) + b * Math.cos(latF) * Math.cos(lonF);
        double myY = a * Math.cos(latO) * Math.sin(lonO) + b * Math.cos(latF) * Math.sin(lonF);
        double myZ = a * Math.sin(latO) + b * Math.sin(latF);

        double latI = Math.atan2(myZ,Math.sqrt(Math.pow(myX,2) + Math.pow(myY,2)));
        double lonI = Math.atan2(myY, myX);

        intermediatePoint.setLatitude(Math.toDegrees(latI));
        intermediatePoint.setLongitude(Math.toDegrees(lonI));

        return intermediatePoint;
    }

    /**
     * Gets the angle between two bearings
     * https://rosettacode.org/wiki/Angle_difference_between_two_bearings
     */
    public static double relativeBearing(double b1Rad, double b2Rad){
        double b1y = Math.cos(b1Rad);
        double b1x = Math.sin(b1Rad);
        double b2y = Math.cos(b2Rad);
        double b2x = Math.sin(b2Rad);
        double crossp = b1y * b2x - b2y * b1x;
        double dotp = b1x * b2x + b1y * b2y;

        if(crossp > 0.) {
            return Math.acos(dotp);
        }else {
            return -Math.acos(dotp);
        }
    }

    public static  Map<String, Object> distToSegment(Location position, Location start, Location end) {
        Map<String, Object> projection = new HashMap<>();
        // let's call our point p0 and the points that define the line as p1 and p2.
        double x = position.getLongitude();
        double y = position.getLatitude();
        double startX = start.getLongitude();
        double startY = start.getLatitude();
        double endX = end.getLongitude();
        double endY = end.getLatitude();
        // Then you get the vectors A = p0 - p1 and B = p2 - p1.
        double A = x - startX;
        double B = y - startY;
        double C = endX - startX;
        double D = endY - startY;
        double dot = A * C + B * D;
        double len_sq = C * C + D * D;
        // Param is the scalar value that when multiplied to B gives you the point on the line closest to p0.
        double param = -1;
        //in case of 0 length line
        if (len_sq != 0)
            param = dot / len_sq;
        // XX and YY is then the closest point on the line segment
        double xx, yy;
        // If param <= 0, the closest point is p1.
        if (param < 0) {
            xx = startX;
            yy = startY;
            //If param >= 1, the closest point is p1.
        } else if (param > 1) {
            xx = endX;
            yy = endY;
            //If it's between 0 and 1, it's somewhere between p1 and p2 so we interpolate.
        } else {
            xx = startX + param * C;
            yy = startY + param * D;
        }
        // dx/dy is the vector from p0 to that point,
        double dx = x - xx;
        double dy = y - yy;

        Location projection_loc = new Location("");
        projection_loc.setLongitude(xx);
        projection_loc.setLatitude(yy);

        projection.put("location", projection_loc);
        projection.put("distance", position.distanceTo(projection_loc));

        //and finally we return the length that vector
        return projection;
    }
}
