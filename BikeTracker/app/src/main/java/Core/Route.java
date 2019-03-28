package Core;

import android.location.Location;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import utils.GeometryUtils;

public class Route {

    /** The route ID. Ideally it should be labeled with the name of the roads on which the route runs */
    private String id;
    /** The corner points of the route */
	private Location routePoints[];
    /** True is the route is active */
    private boolean status;
    /** The segments of the route*/
    private Segment segments[];
    /** loop means that the route is a loop and the last corner-point connects with the first corner-point*/
    private boolean loop;
    /** Reference of current position*/
    private Location loc;
    /** Reference of the current segment */
    private Segment currentSegment;
    /** Reference to the Ghost's segement */
    private Segment ghostSegment;

    /** Reference to the closest position on route */
    private Location projectionOnRoute;
    /** Distance to the closest position on route */
    private float distanceToRoutePath;


    private static String TAG = "DEBUGGING";

    public Route(boolean loop){
        this.loop = loop;
    }

    public void setRoutePoints(Location routePoints[]){
        this.routePoints = routePoints;
        makeSegments();
    }

    /**
     * Private Makes segments out of routePoints
     */
    private void makeSegments(){
        if (this.loop){
            segments = new Segment[routePoints.length];
            for (int i = 0; i < routePoints.length - 1; i++) {
                segments[i] = new Segment(routePoints[i],routePoints[i+1]);
            }
            segments[segments.length-1] = new Segment(this.routePoints[routePoints.length-1],routePoints[0]);
        } else{
            segments = new Segment[routePoints.length-1];
            for (int i = 0; i < routePoints.length - 1; i++) {
                segments[i] = new Segment(routePoints[i],routePoints[i+1]);
            }
        }
    }

    /**
     * Determines wether a traveled distance to a position + 1 step is shorter or equal to the route length
     * @param position The position near to any of the route segments.
     * @param stepLength the step length in meters
     * @return true if still on route, else false.
     */
    private boolean stillOnRoute(Location position, float stepLength){
        // If the route is not a loop do the following, else the position will be on the route
        if (!this.loop){
            // accumulated traveled distance
            float traveledDistance = getTraveledDistanceToPosition(position) + stepLength;

            // evalute the distance traveled against the route length
            return traveledDistance <= getTotalLength();
        } else {
            // on route because it is a loop
            return true;
        }
    }
    /**
     Gets the distance on route traveled from the route origin to the start position
     of the nearest segment to a given point.
     * Gets the distance from the route start to the position
     * @param position The position near to any of the route segments.
     */
    private float getTraveledDistanceToPosition(Location position) {
        // get the index of the closest segment to the position
        int index = (int) getIndexAndProximityToClosestSegmentTo(position).get("index");

        // get the distance to the start cornerpoint of that segment
        float distanceToCorner = this.getAccDistanceUpToSegment(index, false);

        // calculate the distance on the segment
        float distanceOnSegment = this.segments[index].getDistanceOnSegment(position);

        // add the distance to the corner to the distance on the segment
        return distanceToCorner + distanceOnSegment;
        // return the distance
    }

    private float getTotalLength() {
        float totalLength = 0;

        for (int i = 0; i <= segments.length; i++) {
            totalLength = totalLength + segments[i].getLength();
        }

        return totalLength;
    }

    /**
     * Gets the distance traveled on the route path in meters from pointA to point B.
     * The returned value is positive when A is in front of B, else the distance is negative.
     * @param positionA First position on the route
     * @param positionB Second position on the route
     * @return The value of the distance between the two points.The returned value is
     positive when A is in front of B, else the distance is negative.
     */
    public float getAtoBDistance(Location positionA, Location positionB){
        loc = positionB;
        int indexA = (int) getIndexAndProximityToClosestSegmentTo(positionA).get("index");
        ghostSegment = segments[indexA];
        //int indexAndProxB = (int) getIndexAndProximityToClosestSegmentTo(positionB).get("index");
        Map<String, Object> indexAndProxB = getIndexAndProximityToClosestSegmentTo(positionB);
        currentSegment = segments[(int)indexAndProxB.get("index")];
        distanceToRoutePath = (float)indexAndProxB.get("proximity");
        projectionOnRoute = (Location)indexAndProxB.get("projection");

        float distanceToA = getTraveledDistanceToPosition(positionA);
        float distanceToB = getTraveledDistanceToPosition(positionB);

        return (distanceToA - distanceToB);
    }


    /**
     * Gets the accumulated distance up to the first corner-point of the segment identified by its
     * index on the route's collection of segments. If inclusive parameter is true the accumulated
     * distance extends to the last cornerPoint of the segment
     @param index The index of the segment
     @param inclusive If true,  The index of the segment
     */
    private float getAccDistanceUpToSegment(int index, boolean inclusive){
        float accDistance = 0;
        if (inclusive){
            for (int i = 0; i <= index; i++) {
                accDistance += segments[i].getLength();
            }
        } else {
            for (int i = 0; i < index; i++) {
                accDistance += segments[i].getLength();
            }
        }
        return accDistance;
    }

    /**
     Find the closest segment index to a position
     @param position The position in space
     @return {Object} {index,proximity} to the closest segment of this route
     */
    private Map<String, Object> getIndexAndProximityToClosestSegmentTo(Location position){
         Map<String, Object> indexAndProximity = new HashMap<>();

            // get all segments
            // store the distance to the first segment
            float currentD = (float) GeometryUtils.distToSegment(position,this.segments[0].getStart(),this.segments[0].getEnd()).get("distance");
            Location projection = (Location) GeometryUtils.distToSegment(position,this.segments[0].getStart(),this.segments[0].getEnd()).get("location");
            // set return value
            int index = 0;
            // Go over all other the segments
            for (int i = 1; i < segments.length; i++) {
                // Calculate the distance to each one of them
                float nextD = (float) GeometryUtils.distToSegment(position,this.segments[i].getStart(),this.segments[i].getEnd()).get("distance");

                // Store the segment position of the shortest distances
                if (currentD > nextD){
                    currentD = nextD;
                    index = i;
                }
            }

         indexAndProximity.put("index", index);
         indexAndProximity.put("proximity", currentD);
         indexAndProximity.put("projection", projection);

         // Return the id of the closest segment
         return indexAndProximity;
    }

    /**
     * Calculates the position on route for a traveled distance starting from a given position.
     Process: to DETERMINE TRAVELED DISTANCE the idea is to get the distance to the current point and then add the step distance
     * @param position The insertion position on the route
     * @param stepLength The distance traveled for the duration of a sample rate at a given speed. It is defined in meters.
     * @return Returns the position where the cyclsist should be after traveling the stepLength on the route.
     * If the position falls beyond the last cornerpoint of this route, then the last cornerpoint is returned.
     */
    public Location getPosition (Location position, float stepLength){
        // Validate if the position is on the route path
        if (validatePosition(position)){
            // get the index of the closest segment to position
            int index = (int)getIndexAndProximityToClosestSegmentTo(position).get("index");

            // retrieve the segment for that index
            Segment currentSegment = this.segments[index];
            // The traveled distance on the segment
            float distanceOnSegment = currentSegment.getDistanceOnSegment(position);

            // The distance on segment plus the step distance
            float accumDistanceOnSegment = distanceOnSegment + stepLength;

            // Validate is the distance on segment falls within the boundaries of this segment
            if (accumDistanceOnSegment < currentSegment.getLength()){
                // if true get and return the NEW POSITION
                return currentSegment.getIntermediatePointFromDistance((float) accumDistanceOnSegment);
            } else {
                // Validate that traveled distance is less or equal to route's length
                if (this.stillOnRoute(position,stepLength)){
                    // Recalculate accumulated distance on the next segment
                    float remainingDistForNextSegment = accumDistanceOnSegment - currentSegment.getLength();
                    // retrieve the next segment
                    index = index + 1;
                    // if the index is greater than the number of segments
                    if (index >= this.segments.length){
                        // reset index if this route is a loop
                        if (this.loop){
                            index = 0;
                        } else {
                            // Route completed
                            return this.segments[this.segments.length-1].getEnd();
                        }
                    }
                    // retrieve current segment
                    currentSegment = this.segments[index];
                    // return the NEW POSITION
                    return currentSegment.getIntermediatePointFromDistance(remainingDistForNextSegment);

                } else {
                    // Route completed
                    Log.w(TAG, "WARNING!!! Route completed");
                    // Return the last point if the route
                    return this.segments[this.segments.length-1].getEnd();
                }
            }
        } else {
            return this.segments[this.segments.length-1].getEnd();
        }
    }

    /**
     * Returns true if the given position is not the end of the route.
     * @param position The position to be validated
     */
    private boolean validatePosition(Location position) {
        double distance = this.getAtoBDistance(segments[segments.length - 1].getEnd(), position);
        return loop || (distance >= 0);
    }

    /**
     * Return the last segment of the route as reference
     */
    public Segment getLastSegment(){
        return segments[segments.length-1];
    }
    public Segment getCurrentSegment() { return currentSegment; }
    public Map<String, Object> getProjection(){
        Map<String, Object> projection = new HashMap<>();
        projection.put("distance",distanceToRoutePath);
        projection.put("projected_loc", projectionOnRoute);
        return projection;
    }
    public Segment getGhostSegment() { return ghostSegment; }

    public boolean isLooped(){
     return loop;
    }
}
