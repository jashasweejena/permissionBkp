package com.example.new2;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity implements StartupCallback{

    private static String TAG = MainActivity.class.getSimpleName();
    TextView rootText;


    private class Startup extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog = null;
        private Context context = null;
        private boolean suAvailable = false;
        private String suVersion = null;
        private String suVersionInternal = null;
        private List<String> suResult = null;
        private StartupCallback callback = null;

        public Startup setContext(Context context) {
            this.context = context;
            return this;
        }

        public Startup setListener(StartupCallback callback){
            this.callback = callback;
            return this;
        }

        @Override
        protected void onPreExecute() {
            // We're creating a progress dialog here because we want the user to wait.
            // If in your app your user can just continue on with clicking other things,
            // don't do the dialog thing.

            dialog = new ProgressDialog(context);
            dialog.setTitle("Some title");
            dialog.setMessage("Doing something interesting ...");
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            // Let's do some SU stuff
            suAvailable = Shell.SU.available();
            if (suAvailable) {
                suVersion = Shell.SU.version(false);
                suVersionInternal = Shell.SU.version(true);
                suResult = Shell.SU.run(new String[] {
                        "pm list packages | grep -v com.google.* | grep -v com.android.* | grep -v  com.quic.* "
                });
            }

            // This is just so you see we had a progress dialog,
            // don't do this in production code
            try { Thread.sleep(5000); } catch(Exception e) { }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            dialog.dismiss();

            // output
            StringBuilder sb = (new StringBuilder()).
                    append("Root? ").append(suAvailable ? "Yes" : "No").append((char)10).
                    append("Version: ").append(suVersion == null ? "N/A" : suVersion).append((char)10).
                    append("Version (internal): ").append(suVersionInternal == null ? "N/A" : suVersionInternal).append((char)10).
                    append((char)10);
            if (suResult != null) {
                for (String line : suResult) {
                    sb.append(line).append((char)10);
                }
            }

            callback.rootCallback(sb.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootText = findViewById(R.id.rootText);
        rootText.setMovementMethod(new ScrollingMovementMethod());
        //Background stuff
        new Startup().setContext(this).setListener(this).execute();

    }

    @Override
    public void rootCallback(String text) {
        this.rootText.setText(text);
    }


}
