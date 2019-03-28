package Core;

import android.location.Location;
import android.util.Log;

import utils.GeometryUtils;

public class Segment {

    private Location start, end;
    private float length;
    private float bearing;
    private static String TAG = "DEBUGGING";


    /**
     * Utility class used to make distance operations on a route
     * @param  start The beginig of the segment.
     * @param  end The end of the segment.
     */
    public Segment(Location start, Location end){
        this.start = start;
        this.end = end;
        this.length = start.distanceTo(end);
        this.bearing = start.bearingTo(end);

    }

    /**
     * Gets the point based on a distance from the start point of a segment
     *@param distance distance Distance in meters
     */
    public Location getIntermediatePointFromDistance(float distance){
        // estimate the fraction
        float fraction = distance / length;

        if (fraction <= 1){
            return GeometryUtils.getIntermediatePoint(start, end, fraction);
        } else {
            Log.d(TAG, "The distance given "+ distance + " is larger than this segment " + this.length + ". Undefined returned");
            return null;
        }
    }

    /**
     Returns the distance from the segment start to the position. It assumes that the vehicle is subscribed to to route
     @param  position between the start and end of this segment
     @return {Number} distance Distance in meters
     */
    public float getDistanceOnSegment(Location position){
        return start.distanceTo(position);
    }

    /**
     * Determines if the bearing of a vector defined by two positions is aligned with the bearing of the current segment.
     It is useful to know if the cyclist rides aligned to the segment direction
     * @param startPos
     * @param endPos
     * @param range In radians. Half the range of elignment evaluation. Could be omited. It should not be greater than PI/2.
     * By default it is PI/2, meaning that the range of evaluation if the segment bearing +/- 90 degrees.
     */
    public boolean isBearingAligned(Location startPos, Location endPos, float range){
        double range2 = Math.PI/2;
        // replace with new range
        if (range < Math.PI/2){
            range2 = range;
        }
        // get bearing of two points
        double tmpBearing = startPos.bearingTo(endPos);
        // get angle between bearings
        double angleBetweenBearings = GeometryUtils.relativeBearing(this.bearing, tmpBearing);

        if (Math.abs(angleBetweenBearings) <= range2) return true;
        else return false;
    }

    public float getLength() {
        return length;
    }

    public Location getStart() {
        return start;
    }

    public Location getEnd() {
        return end;
    }
}
