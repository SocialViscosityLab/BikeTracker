package com.example.dani.biketracker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class RecordingActivity extends AppCompatActivity {
    private Button trigger_session;
    private Logic logic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        logic = Logic.getInstance();
        logic.firestoreSetup();

        this.trigger_session = findViewById(R.id.trigger_session);
        this.trigger_session.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!logic.isAlive()){
                    logic.start();
                }
                if(logic.isInSession()){
                    trigger_session.setText("Start Session");
                    Log.d("debugging", "Session finished!");
                    logic.closeSession();

                    logic.readFirestore();
                    //logic.updateDataBase();

                    //Log.d("debugging", logic.getSessions().get(logic.getSessions().size()-1).getStringOfSession());

                }else{
                    trigger_session.setText("Stop Session");
                    logic.startNewSession();
                    logic.getFDB();
                }
                // TODO: startActivity(new Intent(MainActivity.this, RadarActivity.class));
            }
        });
    }
}
