package sensors;

import android.util.Log;

import java.util.Observable;

public class Synchro extends Observable {

    public Synchro(){

    }

    public void updateChange(){
        setChanged();
        notifyObservers();
    }

}
