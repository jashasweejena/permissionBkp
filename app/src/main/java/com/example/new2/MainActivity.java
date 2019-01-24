package com.example.new2;

import android.animation.Animator;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

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
                Thread.sleep(1000);
            } catch (Exception e) {
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            List<String> packages = new ArrayList<>();
            dialog.dismiss();

            // output
            StringBuilder sb = (new StringBuilder()).
                    append("Root? ").append(suAvailable ? "Yes" : "No").append((char) 10).
                    append("Version: ").append(suVersion == null ? "N/A" : suVersion).append((char) 10).
                    append("Version (internal): ").append(suVersionInternal == null ? "N/A" : suVersionInternal).append((char) 10).
                    append((char) 10);
            if (suResult != null) {
                for (String line : suResult) {
                    String x = line.substring(("package").length() + 1);
                    packages.add(x);
                    sb.append(x).append((char) 10);
                }
            }


            callback.rootCallback(sb.toString(), packages);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler);

        //Background stuff
        new Startup().setContext(this).setListener(this).execute(command);


    }

    @Override
    public void rootCallback(String text, List<String> packages) {

        List<SimpleItem> items = new ArrayList<>();
        String command = "";

        for (String x : packages) {
            SimpleItem item = new SimpleItem(x);
            items.add(item);

            //Show perms for each pkg

        }

        setRecyclerViewAdapter(items);




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
                List<String> permissionList = new ArrayList<>();
                permissionList = getPerms(MainActivity.this, item.name);

                if(!(permissionList.size() == 0 || permissionList == null)) {
                    Toast.makeText(MainActivity.this, permissionList.toString(), Toast.LENGTH_SHORT).show();
                    showPermDialog(permissionList);
                }
                else {
                    Toast.makeText(MainActivity.this, "No perms set for this app", Toast.LENGTH_SHORT).show();
                }



                return false;
            }
        });

//set our adapters to the RecyclerView
        recyclerView.setAdapter(fastAdapter);

    }

    private void showPermDialog(final List<String> permissionList) {

        layoutInflater = MainActivity.this.getLayoutInflater();
        final View content = layoutInflater.inflate(R.layout.dialog_layout, null, false);

        final TextView permissionText = content.findViewById(R.id.permissionList);

        String permText = "";
//        StringBuilder sb = new StringBuilder();

        for(String x : permissionList){
             permText += x + "\n";
        }
        permissionText.setText(permText);



        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setView(content)
        ;

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

    List<String> getPerms(Context context, String packageName){
        PackageManager p = context.getPackageManager();
        final List<PackageInfo> appinstall = p.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        List<String> permissionList = new ArrayList<>();

        try {
            PackageInfo info = p.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions != null) {
                for (String x : info.requestedPermissions) {
                    permissionList.add(x);
                    Log.d(TAG, "getPerms: " + x);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return permissionList;
    }
}
