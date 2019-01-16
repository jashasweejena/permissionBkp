package com.example.new2;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity implements StartupCallback{

    private static String TAG = MainActivity.class.getSimpleName();
    TextView rootText;
    String command = "pm list packages | grep -v com.google.* | grep -v com.android.* | grep -v  com.quic.* | grep -v android.*";


    private class Startup extends AsyncTask<String, Void, Void> {
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
        protected Void doInBackground(String... params) {
            // Let's do some SU stuff
            suAvailable = Shell.SU.available();
            if (suAvailable) {
                suVersion = Shell.SU.version(false);
                suVersionInternal = Shell.SU.version(true);
                suResult = Shell.SU.run(new String[] {
                        params[0]

                });
            }

            // This is just so you see we had a progress dialog,
            // don't do this in production code
            try { Thread.sleep(1000); } catch(Exception e) { }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            List<String> packages = new ArrayList<>();
            dialog.dismiss();

            // output
            StringBuilder sb = (new StringBuilder()).
                    append("Root? ").append(suAvailable ? "Yes" : "No").append((char)10).
                    append("Version: ").append(suVersion == null ? "N/A" : suVersion).append((char)10).
                    append("Version (internal): ").append(suVersionInternal == null ? "N/A" : suVersionInternal).append((char)10).
                    append((char)10);
            if (suResult != null) {
                for (String line : suResult) {
                    String x = line.substring(("package").length() + 1);
                    packages.add(x);
                    sb.append(x).append((char)10);
                }
            }

            callback.rootCallback(sb.toString(), packages);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootText =(TextView) findViewById(R.id.rootText);
        rootText.setMovementMethod(new ScrollingMovementMethod());
        //Background stuff
        new Startup().setContext(this).setListener(this).execute(command);

    }

    @Override
    public void rootCallback(String text, List<String> packages) {
        this.rootText.setText(text);

    }

    List<ApplicationInfo> getPackageNames(){
        final PackageManager pm = getPackageManager();
//get a list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            Log.d(TAG, "Installed package :" + packageInfo.packageName);
            Log.d(TAG, "Source dir : " + packageInfo.sourceDir);
            Log.d(TAG, "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));
        }

        return packages;
    }
}
