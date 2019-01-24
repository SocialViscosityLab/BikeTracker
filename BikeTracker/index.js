
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

// The ghost movement's speed 
const ghostSpeed =  5; // meters per seconds

/**
 * This database triggered function will check for child nodes that are older than the
 * cut-off time. Each child needs to have a `timestamp` attribute.
 */
exports.ghostMovements = functions.database.ref('/biketracker-e494b/routes/{C08jnZCLwq8GJmi0m053}').onUpdate(async (change) => {
  const ref = change.after.ref.parent; // reference to the parent
  const now = Date.now();
  const initialPoint = ref.get
  const cutoff = now - CUT_OFF_TIME;
  const oldItemsQuery = ref.orderByChild('timestamp').endAt(cutoff);
  const snapshot = await oldItemsQuery.once('value');
  // create a map with all children that need to be removed
  const updates = {};
  snapshot.forEach(child => {
    updates[child.key] = null;
  });
  // execute all updates in one go and return the result to end the function
  return ref.update(updates);
});

function calculateLocation(positionInitial, positionFinal, speed, time){
  var latI = positionInitial.coords.latitude;
  var lonI = positionInitial.coords.longitude;
  var latF = positionFinal.coords.latitude;
  var lonF = positionFinal.coords.longitude;

  var travelledDistance = speed * time;


    var R = 6371e3; // earth's radio in metres
    var pi = Math.PI;
    rLat1 = 
}

function isInLocation(lat, lon, maxDist, position){
//Distance code taken from: https://www.movable-type.co.uk/scripts/latlong.html

  var lat1 = lat;
  var lon1 = lon;
  var lat2 = position.coords.latitude;
  var lon2 = position.coords.longitude;

  var R = 6371e3; // metres
  
  var fi1 = Math.sin((lat1 * Math.PI) / 180);     
  var fi2 = Math.sin((lat2 * Math.PI) / 180);     

  var deltaFi = Math.sin((lat2-lat1) * Math.PI / 180);
  var deltaLambda = Math.sin((lon2-lon1)*Math.PI / 180);

  var a = Math.sin(deltaFi/2) * Math.sin(deltaFi/2) + Math.cos(fi1) * Math.cos(fi2) * Math.sin(deltaLambda/2) * Math.sin(deltaLambda/2);
  var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

  var d = R * c;

  if(d<= maxDist){
    console.log("entro");
    return true;
  }
  else{
    console.log("no entro");
    return false;
  }
}

var tid = setInterval( function () {
    if ( document.readyState !== 'complete' ) return;
    clearInterval( tid );      
    getLocation();
 
    // do your work
}, 100 );
