package com.example.dani.biketracker;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private Button sign_in_button;
    private TextView mStatusTextView;
    private Logic logic;
    private FirebaseAuth mAuth;
    private static String TAG = "DEBUGGING";
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    // User is signed in
                    //redirect
                    // Name, email address, and profile photo Url
                    String name = user.getDisplayName();
                    String email = user.getEmail();
                    logic.setUser(user);
                    startActivity(new Intent(MainActivity.this, RecordingActivity.class));


                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    //updateUI(null);
                }

            }
        };

        /* TODO Google sign in
       // Set the dimensions of the sign-in button.
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);

        sign_in_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
            }
        });

    */
        logic = Logic.getInstance();
    }



    @Override
    public void onStart() {
        super.onStart();

        mAuth = FirebaseAuth.getInstance();
        mAuth.addAuthStateListener(mAuthListener);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {


            // Check if user's email is verified
            boolean emailVerified = user.isEmailVerified();

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getToken() instead.
            String uid = user.getUid();
        }
        else {
            signIn("d4svlab1@gmail.com", "viscosity");
        }
    }

    private void signIn(final String email, String password) {
        Log.d(TAG, "signIn:" + email);


        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                Log.d(TAG, " Verification : signIn With Email:onComplete:" + task.isSuccessful());
                //  If sign in succeeds i.e if task.isSuccessful(); returns true then the auth state listener will be notified and logic to handle the
                // signed in user can be handled in the listener.


                // If sign in fails, display a message to the user.
                if (!task.isSuccessful()) {
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthInvalidUserException e) {
                        mStatusTextView.setError("Invalid Emaild Id");
                        mStatusTextView.requestFocus();
                    } catch (FirebaseAuthInvalidCredentialsException e) {
                        Log.d(TAG, "email :" + email);
                        mStatusTextView.setError("Invalid Password");
                        mStatusTextView.requestFocus();
                    } catch (FirebaseNetworkException e) {
                        showErrorToast("error_message_failed_sign_in_no_network");
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                    Log.w(TAG, "signInWithEmail:failed", task.getException());
                    Toast.makeText(MainActivity.this, R.string.login_error,
                            Toast.LENGTH_SHORT).show();
                    //TODO pdateUI(null);
                }
            }

        });
    }

        @Override
        public void onStop () {
            super.onStop();
            if (mAuthListener != null) {
                mAuth.removeAuthStateListener(mAuthListener);
            }
        }

        private void showErrorToast (String error_message){
            Toast.makeText(MainActivity.this, R.string.login_error,
                    Toast.LENGTH_SHORT).show();
    }
}
