package com.example.new2;

import android.animation.Animator;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.TextView;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.listeners.OnClickListener;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity implements StartupCallback {

    private static String TAG = MainActivity.class.getSimpleName();
    RecyclerView recyclerView;

    private FastAdapter fastAdapter;
    LayoutInflater layoutInflater;
    String command = " pm list packages | grep -v com.google.* | grep -v com.android.* | grep -v  com.quic.*";

    private List<String> packageNames = new ArrayList<>();

    private List<String> finalPackageNames = new ArrayList<>();
    private List<String> finalPermissionList = new ArrayList<>();


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

        public Startup setListener(StartupCallback callback) {
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
                suResult = Shell.SU.run(new String[]{
                        params[0]

                });
            }

            // This is just so you see we had a progress dialog,
            // don't do this in production code
            try {
                Thread.sleep(200);
            } catch (Exception e) {
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            dialog.dismiss();

            // output
            StringBuilder sb = (new StringBuilder()).
                    append("Root? ").append(suAvailable ? "Yes" : "No").append((char) 10).
                    append("Version: ").append(suVersion == null ? "N/A" : suVersion).append((char) 10).
                    append("Version (internal): ").append(suVersionInternal == null ? "N/A" : suVersionInternal).append((char) 10).
                    append((char) 10);
            if (suResult != null) {
                for (String line : suResult) {
                    sb.append(line).append((char) 10);
                }
            }


            callback.rootCallback(sb.toString(), suResult);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler);

        final PackageManager pm = getPackageManager();
//get a list of installed apps.
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo x : packages) {
            packageNames.add(x.packageName);

        }


        List<SimpleItem> items = new ArrayList<>();
        String command = "";

        for (String x : packageNames) {
            SimpleItem item = new SimpleItem(x);
            items.add(item);

            //Show perms for each pkg

        }

        setRecyclerViewAdapter(items);
    }

    @Override
    public void rootCallback(String text, List<String> outputList) {
//        showPermDialog(outputList);
        this.finalPermissionList.add(text);
//        getPermList(text);
        Log.d(TAG, "rootCallback: Perm list size " + outputList.size() + "Package list size " + this.packageNames.size());
    }

    List<ApplicationInfo> getPackageNames() {
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

    void setRecyclerViewAdapter(List<SimpleItem> ITEMS) {

        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

        FastItemAdapter<SimpleItem> fastAdapter = new FastItemAdapter<>();
        recyclerView.setAdapter(fastAdapter);
        fastAdapter.add(ITEMS);
        fastAdapter.withSelectable(true);
        fastAdapter.withOnClickListener(new OnClickListener<SimpleItem>() {
            @Override
            public boolean onClick(@Nullable View v, IAdapter<SimpleItem> adapter, SimpleItem item, int position) {
//                List<String> permissionList = new ArrayList<>();
////                permissionList = getPermsFromPackage(MainActivity.this, item.name);
//
//
//                if(!(permissionList.size() == 0 || permissionList == null)) {
//                    Toast.makeText(MainActivity.this, permissionList.toString(), Toast.LENGTH_SHORT).show();
//                    showPermDialog(permissionList);
//                }
//                else {
//                    Toast.makeText(MainActivity.this, "No perms set for this app", Toast.LENGTH_SHORT).show();
//                }

                getPermsFromPackage(item.name);
                showPermDialog(null, item.name);


                return false;
            }
        });

//set our adapters to the RecyclerView
        recyclerView.setAdapter(fastAdapter);

    }

    private void showPermDialog(@Nullable  final List<String> permissionList, String packageName) {

        layoutInflater = MainActivity.this.getLayoutInflater();
        final View content = layoutInflater.inflate(R.layout.dialog_layout, null, false);

        final TextView permissionText = content.findViewById(R.id.permissionList);

        String permText = "";

//        for (String x : permissionList) {
//            permText += x + "\n";
//        }

        List<String> x = getGrantedPermissions(packageName);
        StringBuilder sb = new StringBuilder();
        for(int i = 0 ; i < x.size() ; i ++){
            if(x.get(i).contains("android.permission")) {
                sb.append(x.get(i) + (char) 10);
            }
        }
        permText = sb.toString();
        permissionText.setText(permText);
        permissionText.setMovementMethod(new ScrollingMovementMethod());


        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setView(content);

        AlertDialog dialog = builder.create();
        // get the center for the clipping circle

        final View view = dialog.getWindow().getDecorView();

        view.post(new Runnable() {
            @Override
            public void run() {
                final int centerX = view.getWidth() / 2;
                final int centerY = view.getHeight() / 2;
                // TODO Get startRadius from FAB
                // TODO Also translate animate FAB to center of screen?
                float startRadius = 20;
                float endRadius = view.getHeight();
                Animator animator = ViewAnimationUtils.createCircularReveal(view, centerX, centerY, startRadius, endRadius);
                animator.setDuration(500);
                animator.start();
            }
        });

        dialog.show();


    }

    void getPermsFromPackage(String packageName) {
//        String command = "dumpsys package " + packageName + " | grep -i granted=";
        String command = "dumpsys package " + packageName;
        callAsync(command);
    }

    void callAsync(String... command) {
        new Startup().setContext(MainActivity.this).setListener(MainActivity.this).execute(command);
    }

    List<String> getGrantedPermissions(final String appPackage) {
        List<String> granted = new ArrayList<String>();
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(appPackage, PackageManager.GET_PERMISSIONS);
            for (int i = 0; i < pi.requestedPermissions.length; i++) {
                if ((pi.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                    granted.add(pi.requestedPermissions[i]);
                }
            }
        } catch (Exception e) {
        }
        return granted;
    }
}