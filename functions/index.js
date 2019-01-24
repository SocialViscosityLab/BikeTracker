/*
const firestore = new Firestore();
const settings = { timestampsInSnapshots: true};
firestore.settings(settings);
*/

const functions = require('firebase-functions');

const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);
var db = admin.firestore();



exports.myTestFunction = functions.firestore.document('routes/{routeId}').onUpdate((change, context) => {

  const routeId = context.params.routeId

  console.log("Aquí hubo un cambio: 0021");

  var route = change.after.data();
  var running = route.running;


  if(running === true){
    console.log("The ghost is running");
    createJourney(routeId);
  }else{
    console.log("The ghost is not running");
  }
  return '200';
});


function createJourney(id){
  var id_journey = 0;
  var tempId = -1;
  let ghost_session_id = '00000';
  let id_user = 'ghost';
  let time = new Date();
  let startTime = time.getFullYear()+"/"+time.getMonth()+"/"+time.getDate()+" - "+time.getHours()+":"+time.getMinutes()+":"+time.getSeconds();
  let ghost_speed = 5;
  let ghost_acceleration = 0;
  let refRoute = db.collection('routes').doc(id);
  var id_journey_filled = '';

  var position_points = [];
  var initialLocation = {};
  var finalLocation = {};

  var journeys = db.collection('journeys').get().then(snapshot => {
    snapshot.forEach(doc => {
      let id = parseInt(doc.id);
      if(id !== null){
        if (id > tempId){
          tempId = id;
        }
      }
    });

    id_journey = tempId + 1;
    let zero_filled = '00000';
    id_journey_filled = (zero_filled+id_journey).slice(-zero_filled.length);

    console.log("Id found:",id_journey_filled);

    return "200";
  })
  .catch(error => {
    console.log('Error getting collection');
    return "404";
  });


  var ruoute = refRoute.get().then(function(doc){
    if(doc.exists) {
      console.log("Docuemnt data:", doc.data());
      let pps = doc.data().position_points;
      let tempIndx = -1;
      pps.forEach(pp => {

        tempIndx ++;
        position_point = {
          latitude: pp._latitude,
          longitude: pp._longitude,
          acceleration: ghost_acceleration,
          speed: ghost_speed,
          time: tempIndx*1000,
          suggestion: 0
        }

        position_points[tempIndx] = position_point;

      });
      console.log("position_points:", position_points);


      //Temporal locations
      initialLocation = {
        latitude: 40.104407,
        longitude:  -88.23105
      };

      finalLocation = {
        latitude: 40.104407,
        longitude:  -88.23105
      };

    }else{
      console.log("No such a route!");
    }

    return "200";

  }).catch(function(error) {
    console.log('Error getting the route:',error);
    return "404";
  });

  Promise.all([journeys,ruoute]).then(function(){
    let ghostSession = {
      id_user: id_user,
      start_time: startTime,
      data_points: position_points
    }

    db.collection('journeys').doc(id_journey_filled).set({'reference_route':refRoute});
    db.collection('journeys').doc(id_journey_filled).collection('sessions').doc(ghost_session_id).set(ghostSession);

    return '200'

  }).catch(function(error) {
    console.log('Error updating database:',error);
    return "404";
  });

}


//Function to syncronize the followers sessions with the server time when the session is created
exports.setServerTime = functions.firestore.document('journeys/{journey_id}/sessions/{session_id}').onCreate((snapshot, context) => {

  const journeyId = context.params.journey_id
  const sessionId = context.params.session_id
  let time = new Date();
  let startTime = time.getFullYear()+"/"+time.getMonth()+"/"+time.getDate()+" - "+time.getHours()+":"+time.getMinutes()+":"+time.getSeconds();

  var session = snapshot.data();
  var session_time = session.start_time;

  db.collection('journeys').doc(journeyId).collection('sessions').doc(sessionId).update({"start_time":startTime});

  console.log("The session: "+sessionId+ ", of the journey: "+journeyId+" has been updated with the server's time");

  return "200";
});
