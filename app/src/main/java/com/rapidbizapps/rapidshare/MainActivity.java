package com.rapidbizapps.rapidshare;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.rapidbizapps.rapidshare.docs.Profile;
import com.rapidbizapps.rapidshare.docs.Project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends ActionBarActivity implements  ProjectAdapter.OnItemClickListener {

    private static final String TAG = Application.TAG;
    private CharSequence mTitle;
    private DrawerLayout mDrawerLayout;
    private SwitchCompat mToggleGCM;
    private LiveQuery liveQuery;
    Application application ;
    private MyPref preferences;

    private String getCurrentProjectId() {
        String currentProjectId = preferences.getCurrentProjectId();
        if (currentProjectId == null) {
            try {
                QueryEnumerator enumerator = Project.getQuery(application.getDatabase()).run();
                if (enumerator.getCount() > 0) {
                    currentProjectId = enumerator.getRow(0).getDocument().getId();
                }
            } catch (CouchbaseLiteException e) { }
        }
        return currentProjectId;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        application = (Application) getApplication();
        this.preferences = new MyPref(getApplication());
        setContentView(R.layout.activity_main);
//        mToggleGCM = (SwitchCompat) findViewById(R.id.toggleGCM);

        Log.d(Application.TAG, "MainActivity State: onCreate()");
//
//        if (preferences.getCurrentUserId() != null && preferences.getCurrentUserPassword() != null) { // basic auth
//            application.setDatabaseForName(preferences.getCurrentUserId());
//            application.startReplicationSyncWithBasicAuth(preferences.getCurrentUserId(), preferences.getCurrentUserPassword());
//        } else 

         if (preferences.getLastReceivedFbAccessToken() != null) { // fb auth
            application.setDatabaseForName(preferences.getCurrentUserId());
            application.startReplicationSyncWithFacebookLogin(preferences.getLastReceivedFbAccessToken());
        } 
//         else if (preferences.getCurrentUserId() != null) { // cookie auth
//            application.setDatabaseForName(preferences.getCurrentUserId());
//            application.startReplicationSyncWithCustomCookie(preferences.getCurrentUserId());
//        } else if (preferences.getGuestBoolean()) {
//            application.setDatabaseForGuest();
//        }
        else {
            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            startActivityForResult(i, 0);
            finish();
        }

        setupTodoProjects();

        //((TextView) findViewById(R.id.name)).setText(preferences.getCurrentUserId());

        mTitle = getTitle();

        String currentProjectId = getCurrentProjectId();
        if (currentProjectId != null) {
            displayProjectContent(currentProjectId);
        }

        application.getOnSyncProgressChangeObservable().addObserver(new Observer() {
            @Override
            public void update(final Observable observable, final Object data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Application.SyncProgress progress = (Application.SyncProgress) data;
                        Log.d(TAG, "Sync progress changed.  Completed: %d Total: %d Status: %s", progress.completedCount, progress.totalCount, progress.status);

                        if (progress.status == Replication.ReplicationStatus.REPLICATION_ACTIVE) {
                            Log.d(TAG, "Turn on progress spinny");
                            setProgressBarIndeterminateVisibility(true);
                        } else {
                            Log.d(TAG, "Turn off progress spinny");
                            setProgressBarIndeterminateVisibility(false);
                        }
                    }
                });
            }
        });

        application.getOnSyncUnauthorizedObservable().addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Log.d(Application.TAG, "OnSyncUnauthorizedObservable called, show toast");

                        // clear the saved user id, since our session is no longer valid
                        // and we want to show the login button
                        preferences.setCurrentUserId(null);
                        preferences.setCurrentUserPassword(null);
                        invalidateOptionsMenu();

                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();

                        String msg = "Sync unable to continue due to invalid session/login";
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

                    }
                });

            }
        });

    }

    public void onSyncMethodToggleClicked(View view) {
        SwitchCompat syncToggle = (SwitchCompat) view;
        if (syncToggle.isChecked()) {
            // GCM
            getDeviceToken();
            application.startContinuousPushAndOneShotPull(preferences.getLastReceivedFbAccessToken());
        } else {
            application.startReplicationSyncWithFacebookLogin(preferences.getLastReceivedFbAccessToken());
        }
    }

    void getDeviceToken() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {

                GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(getApplicationContext());

                try {
                    String deviceToken = gcm.register("632113338862");
                    Log.i("GCM", "Device token : " + deviceToken);

                    // update user document
                    Document profile = Profile.getUserProfileById(application.getDatabase(), preferences.getCurrentUserId());
                    Map<String, Object> updatedProperties = new HashMap<String, Object>();
                    updatedProperties.putAll(profile.getProperties());

                    ArrayList<String> deviceTokens = (ArrayList<String>) profile.getProperty("device_tokens");
                    if (deviceTokens == null) {
                        deviceTokens = new ArrayList<String>();
                    }

                    deviceTokens.add(deviceToken);
                    updatedProperties.put("device_tokens", deviceTokens);

                    try {
                        profile.putProperties(updatedProperties);
                    } catch (CouchbaseLiteException e) {
                        e.printStackTrace();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(null, null, null);
    }

    void setupTodoProjects() {
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        liveQuery = Project.getQuery(application.getDatabase()).toLiveQuery();

        ProjectAdapter mAdapter = new ProjectAdapter(this, liveQuery);
        mAdapter.setOnItemClickListener(this);

        mRecyclerView.setAdapter(mAdapter);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
    }


    @Override
    public void onItemClick(View view, int position) {
        String listId = liveQuery.getRows().getRow(position).getDocumentId();

        displayProjectContent(listId);
        mDrawerLayout.closeDrawers();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(Application.TAG, "MainActivity State: onStart()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(Application.TAG, "MainActivity State: onRestart()");

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Application.TAG, "MainActivity State: onResume()");

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Log.d(Application.TAG, "MainActivity State: onPostResume()");

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(Application.TAG, "MainActivity State: onPause()");

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(Application.TAG, "MainActivity State: onStop()");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(Application.TAG, "MainActivity State: onDestroy()");

    }


    private void displayProjectContent(String listDocId) {
//        Document document = application.getDatabase().getDocument(listDocId);
//        getSupportActionBar().setSubtitle((String) document.getProperty("title"));
//
//        FragmentManager fragmentManager = getFragmentManager();
//        fragmentManager.beginTransaction()
//                .replace(R.id.container, TasksFragment.newInstance(listDocId))
//                .commit();
//
//        preferences.setCurrentProjectId(listDocId);
    }

//    public void restoreActionBar() {
//        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
//        actionBar.setDisplayShowTitleEnabled(true);
//        actionBar.setTitle(mTitle);
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    private void createNewProject() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Add new Project");

        final EditText input = new EditText(this);
        input.setMaxLines(1);
        input.setSingleLine(true);
        input.setHint("Project title");
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String title = input.getText().toString();
                if (title.length() == 0) {
                    // TODO: Show an error message.
                    return;
                }
                try {
                    String currentUserId = preferences.getCurrentUserId();
                    Document document = Project.createNewProject(application.getDatabase(), title, currentUserId);
                    displayProjectContent(document.getId());
                    invalidateOptionsMenu();
                } catch (CouchbaseLiteException e) {
                    Log.e(Application.TAG, "Cannot create a new list", e);
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) { }
        });

        alert.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == R.id.action_new_list) {
//            createNewProject();
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    private void startSyncWithCustomCookie(String cookieVal) {
        Application application = (Application) MainActivity.this.getApplication();
        application.startReplicationSyncWithCustomCookie(cookieVal);
    }

    private void startSyncWithBasicAuth(String username, String password) {
        Application application = (Application) MainActivity.this.getApplication();
        application.startReplicationSyncWithBasicAuth(username, password);
    }

}
