package com.apps.lorenzofailla.vocalpacer;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    AlertDialog.Builder builder;
    AlertDialog alertdialog_no_gps;
    AlertDialog alertdialog_no_fix;
    AlertDialog alertdialog_ask_for_session_save_in_db;

    private boolean show_remaining_time = false;
    private boolean show_expected_distance = false;
    private boolean use_session_duration;
    private boolean use_target_distance;
    private double target_distance;

    private IntentFilter intent_filter = new IntentFilter();

    private FloatingActionButton btn_startstop;
    private TextView txv_elapsedtime;
    private TextView txv_distance;
    private TextView txv_speed;
    private TextView txv_elapsedtime_label;
    private TextView txv_distance_label;

    private Intent main_service;

    private long elapsed_time_ms;
    private float speed;
    private double distance;
    private double expected_distance;
    private long remaining_time;

    private MainService.GPS_STATUS gps_status;
    private MainService.SESSION_STATUS session_status;
    private MainService.SESSION_STATUS previous_session_status;

    private LocalBroadcastManager local_broadcastmanager;

    private static SoundPool sound_pool;

    private static int SOUND___GPS_ASK_WAIT_FOR_FIX;
    private static int SOUND___GPS_FIX_RECEIVED;
    private static int SOUND___GPS_NOT_AVAILABLE;

    DialogInterface.OnClickListener dialogclicklistener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {

            if(dialog.equals(alertdialog_no_fix)||dialog.equals(alertdialog_no_gps)){

                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        // positive button clicked

                        local_broadcastmanager.sendBroadcast(new Intent("@VOCALPACER:::REQUEST_TO_IMMEDIATE_START_SESSION"));

                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        // negative button clicked

                        local_broadcastmanager.sendBroadcast(new Intent("@VOCALPACER:::REQUEST_TO_CANCEL_WAITING_STATUS"));

                        break;
                }

            }

            else if (dialog.equals(alertdialog_ask_for_session_save_in_db)){


                switch (which) {

                    case DialogInterface.BUTTON_POSITIVE:
                        // positive button clicked

                        local_broadcastmanager.sendBroadcast(new Intent("@VOCALPACER:::REPLY_PLEASE_KEEP_SESSION_IN_DB"));

                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        // negative button clicked

                        local_broadcastmanager.sendBroadcast(new Intent("@VOCALPACER:::REPLY_PLEASE_DISCARD_SESSION"));

                        break;
                }

            }

        }
    };

    private Handler tasks_handler = new Handler();

    private View.OnClickListener click_listener = new View.OnClickListener() {

        public void onClick(View v) {

            Intent intent;

            switch (v.getId()) {

                case R.id.fab___main___startstop:

                    if (session_status == MainService.SESSION_STATUS.started) {
                        // recording is running, so it will be stopped

                        intent = new Intent("@VOCALPACER:::REQUEST_TO_STOP_SESSION");

                    } else {
                        // recording is stopped, so it will be started

                        intent = new Intent("@VOCALPACER:::REQUEST_TO_START_SESSION");

                    }

                    local_broadcastmanager.sendBroadcast(intent);

                    break;

                case R.id.txv_distance_label:case R.id.txv_distance:

                    if (use_session_duration) {

                        show_expected_distance = !show_expected_distance;

                        update_distance_label(show_expected_distance);
                        update_values();

                    }

                    break;

                case R.id.txv_time_label:case R.id.txv_time:

                    if (use_session_duration) {

                        show_remaining_time = !show_remaining_time;

                        update_time_label(show_remaining_time);
                        update_time();

                    }

                    break;

            }

        }
    };

    private Runnable blink_gps_icon = new Runnable() {

        @Override
        public void run() {

            ImageView img_gpsstatus = (ImageView) findViewById(R.id.img___main___gpsstatus);

            switch (img_gpsstatus.getVisibility()) {

                case View.VISIBLE:
                    img_gpsstatus.setVisibility(View.INVISIBLE);
                    break;

                case View.INVISIBLE:
                    img_gpsstatus.setVisibility(View.VISIBLE);
                    break;
            }

            // reschedule the same task to one second later
            tasks_handler.postDelayed(this, 1000);

        }

    };

    // declare a new BroadcastReceiver class
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            // runs whenever a broadcasted Intent is received

            switch (intent.getAction()) {

                case "@VOCALPACER:::NEW_LOCATION":

                    // recupera gli extra dall'intent
                    speed = intent.getFloatExtra("_current_speed", (float) 0.0);
                    distance = intent.getDoubleExtra("_distance", 0.0);

                    update_values();
                    break;

                case "@VOCALPACER:::TIMEBEAT":

                    // recupera gli extra dall'intent
                    elapsed_time_ms = intent.getLongExtra("_Elapsed_Time", 0L);
                    remaining_time = intent.getLongExtra("_Remaining_Time", 0L);
                    expected_distance = intent.getDoubleExtra("_Expected_Distance", 0.0);

                    update_values();
                    update_time();

                    break;

                case "@VOCALPACER:::GPS_STATUS_UPDATE":

                    // recupera gli extra dall'intent
                    gps_status = (MainService.GPS_STATUS) intent.getSerializableExtra("_gps_status");

                    update_gps_status();

                    break;

                case "@VOCALPACER:::PARAMETERS_CHANGED":

                    // recupera gli Extra dall'Intent
                    use_session_duration = intent.getBooleanExtra("_use_session_duration", false);
                    use_target_distance = intent.getBooleanExtra("_use_target_distance", false);
                    target_distance = intent.getDoubleExtra("_target_distance",-1.0);
                    elapsed_time_ms = intent.getLongExtra("_Elapsed_Time", 0L);
                    speed = intent.getFloatExtra("_current_speed", 0.0f);
                    distance = intent.getDoubleExtra("_distance", 0.0);

                    if (!use_session_duration) {
                        show_remaining_time = false;
                        show_expected_distance = false;
                        update_time_label(show_remaining_time);
                        update_distance_label(show_expected_distance);

                    }

                    break;

                case "@VOCALPACER:::SESSION_STATUS_CHANGED":

                    // registra lo stato precedente della sessione
                    if (session_status!=null) {

                        previous_session_status = session_status;

                    }

                    // recupera gli Extra dall'Intent
                    session_status = (MainService.SESSION_STATUS) intent.getSerializableExtra("_session_status");

                    if (session_status!=null){

                        if (session_status.equals(MainService.SESSION_STATUS.started)){

                            // dismiss the alertdialogs
                            if (alertdialog_no_fix.isShowing()) {
                                alertdialog_no_fix.dismiss();
                            }

                            if (alertdialog_no_gps.isShowing()) {
                                alertdialog_no_gps.dismiss();
                            }

                            if (previous_session_status == MainService.SESSION_STATUS.stopped){

                            // display a snackbar
                            Snackbar.make(findViewById(R.id.fab___main___startstop), getString(R.string.main_userinfo_session_started), Snackbar.LENGTH_SHORT).show();

                            }

                        } else if(session_status.equals(MainService.SESSION_STATUS.stopped)){
                            // la sessione non è attiva

                            // se la sessione era attiva, mostra una snackbar
                            if (previous_session_status == MainService.SESSION_STATUS.started) {
                                // la sessione era attiva

                                // mostra una snackbar
                                Snackbar.make(findViewById(R.id.fab___main___startstop), getString(R.string.main_userinfo_session_stopped), Snackbar.LENGTH_SHORT).show();

                            }

                        }

                        update_session_status();
                        update_button_label();
                        update_values();
                        update_time();

                    }

                    break;

                case "@VOCALPACER:::ASK_FOR_START_WITHOUT_FIX":


                    // play a sound
                    play_sound(SOUND___GPS_ASK_WAIT_FOR_FIX);

                    // show a dialog
                    alertdialog_no_fix.show();

                    break;

                case "@VOCALPACER:::ASK_FOR_START_WITHOUT_GPS":

                    // play a sound
                    play_sound(SOUND___GPS_NOT_AVAILABLE);

                    // shows a dialog
                    alertdialog_no_gps.show();

                    break;

                case "@VOCALPACER:::READY_TO_START":

                    // play a sound
                    play_sound(SOUND___GPS_FIX_RECEIVED);

                    // show an alert with a snackbar
                    Snackbar.make(findViewById(R.id.fab___main___startstop), getString(R.string.main_userinfo_session_ready), Snackbar.LENGTH_LONG).show();

                    break;

                case "@VOCALPACER:::REQUEST_KEEP_SESSION_IN_DB":

                    alertdialog_ask_for_session_save_in_db.show();

                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    protected void onPause() {
        super.onPause();

        // se lo stato della sessione non è SESSION_STATUS.started, allora ferma il servizio

        if (!(session_status == MainService.SESSION_STATUS.started)) {
            // lo stato della sessione non è SESSION_STATUS.started

            // ferma il servizio
            stopService(main_service);

        }

        local_broadcastmanager.unregisterReceiver(mReceiver);
        tasks_handler.removeCallbacks(blink_gps_icon);


    }

    protected void onDestroy() {
        super.onDestroy();
    }

    protected void onResume() {

        super.onResume();

        // assign the handler to each drawable of the view
        btn_startstop = (FloatingActionButton) findViewById(R.id.fab___main___startstop);
        txv_elapsedtime = (TextView) findViewById(R.id.txv_time);
        txv_distance = (TextView) findViewById(R.id.txv_distance);
        txv_speed = (TextView) findViewById(R.id.txv_speed);

        txv_distance_label = (TextView) findViewById(R.id.txv_distance_label);
        txv_elapsedtime_label = (TextView) findViewById(R.id.txv_time_label);

        // assign the onClickListener to each handler
        btn_startstop.setOnClickListener(click_listener);
        txv_elapsedtime_label.setOnClickListener(click_listener);
        txv_elapsedtime.setOnClickListener(click_listener);
        txv_distance_label.setOnClickListener(click_listener);
        txv_distance.setOnClickListener(click_listener);

        // define an IntentFilter to be registered with mReceiver on mLocalBroadcastManager
        intent_filter = new IntentFilter();
        intent_filter.addAction("@VOCALPACER:::NEW_LOCATION");
        intent_filter.addAction("@VOCALPACER:::TIMEBEAT");
        intent_filter.addAction("@VOCALPACER:::SESSION_STATUS_CHANGED");
        intent_filter.addAction("@VOCALPACER:::PARAMETERS_CHANGED");
        intent_filter.addAction("@VOCALPACER:::GPS_STATUS_UPDATE");
        intent_filter.addAction("@VOCALPACER:::PARAMETERS_UPDATE_REPLY");
        intent_filter.addAction("@VOCALPACER:::ASK_FOR_START_WITHOUT_FIX");
        intent_filter.addAction("@VOCALPACER:::ASK_FOR_START_WITHOUT_GPS");
        intent_filter.addAction("@VOCALPACER:::READY_TO_START");
        intent_filter.addAction("@VOCALPACER:::REQUEST_KEEP_SESSION_IN_DB");

        local_broadcastmanager = LocalBroadcastManager.getInstance(this);
        local_broadcastmanager.registerReceiver(mReceiver, intent_filter);

        // creates the alertdialog
        builder = new AlertDialog.Builder(this);

        alertdialog_no_gps = builder.setMessage(R.string.main_userinfo_forcestart_nogps)
                .setPositiveButton(R.string.start, dialogclicklistener)
                .setNegativeButton(R.string.cancel, dialogclicklistener)
                .create();

        alertdialog_no_fix = builder.setMessage(R.string.main_userinfo_forcestart_gpsfix)
                .setPositiveButton(R.string.start, dialogclicklistener)
                .setNegativeButton(R.string.cancel, dialogclicklistener)
                .create();

        alertdialog_ask_for_session_save_in_db = builder.setMessage(R.string.ask_for_save_session)
                .setPositiveButton(R.string.keep, dialogclicklistener)
                .setNegativeButton(R.string.discard, dialogclicklistener)
                .create();


        // instantiate the Intent class to the MainService service
        main_service = new Intent(this, MainService.class);

        // controlla che il servizio non sia in esecuzione, e nel caso lo avvia
        if(!is_service_running()) {
            // il servizio non è in esecuzione

            // avvia il servizio
            startService(main_service);

        }

        local_broadcastmanager.sendBroadcast(new Intent("@VOCALPACER:::PARAMETERS_UPDATE_REQUEST"));

        // Inizializza il soundpool e carica i suoni in memoria
        sound_pool = new SoundPool(1, AudioManager.STREAM_MUSIC, 100);

        SOUND___GPS_ASK_WAIT_FOR_FIX = sound_pool.load(this, R.raw.gps_ask_wait_for_fix,0);
        SOUND___GPS_FIX_RECEIVED = sound_pool.load(this, R.raw.gps_fix_received,0);
        SOUND___GPS_NOT_AVAILABLE = sound_pool.load(this, R.raw.gps_not_available,0);

        // chiama il metodo per aggiornare i drawable
        update_values();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent start_settings = new Intent(MainActivity.this, Settings.class);
            startActivity(start_settings);

            //Toast.makeText(getApplicationContext(), getString(R.string.info_label), Toast.LENGTH_LONG).show();
            return true;

        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_sessions) {

            Intent start_sessions = new Intent(MainActivity.this, Sessions.class);
            startActivity(start_sessions);

            //Toast.makeText(getApplicationContext(), getString(R.string.info_label), Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void update_gps_status() {

        ImageView img_gpsstatus = (ImageView) findViewById(R.id.img___main___gpsstatus);
        tasks_handler.removeCallbacks(blink_gps_icon);

        switch (gps_status) {

            case disabled:case paused:

                // hide the gps icon
                img_gpsstatus.setVisibility(View.INVISIBLE);

                break;

            case fixing:

                // make the gps icon blink
                tasks_handler.post(blink_gps_icon);

                break;

            case working:

                // show the gps icon
                img_gpsstatus.setVisibility(View.VISIBLE);

                break;
        }

        update_values();

    }

    private void update_session_status() {

        ImageView img_sessionstatus = (ImageView) findViewById(R.id.img___main___sessionstatus);

        switch (session_status) {

            case started:
                img_sessionstatus.setImageResource(android.R.drawable.ic_media_play);
                break;

            case waiting_to_start:
                img_sessionstatus.setImageResource(android.R.drawable.ic_media_next);
                break;

            case stopped:
                img_sessionstatus.setImageResource(android.R.drawable.ic_media_pause);
                break;

        }

    }

    private void update_button_label() {

        switch (session_status) {

            case started:
                btn_startstop.setImageResource(android.R.drawable.ic_media_pause);
                break;

            case stopped:
            case waiting_to_start:
                btn_startstop.setImageResource(android.R.drawable.ic_media_play);
                break;

        }

    }

    private void update_values() {

        RelativeLayout rly_speed = (RelativeLayout) findViewById(R.id.rly___main___speed );
        int rly_speed_backcolor;
        int rly_speed_visibility; // può essere VISIBLE, INVISIBLE oppure GONE

        String distance_label;
        String speed_label;

        if (session_status==MainService.SESSION_STATUS.started) {

            if (show_expected_distance) {

                distance_label = String.format("%1.2f", expected_distance / 1000);

            } else {

                distance_label = String.format("%1.2f", distance / 1000);

            }

            txv_distance.setText(distance_label);

            if (gps_status==MainService.GPS_STATUS.working) {

                speed_label = String.format("%1.1f", speed * 3.6);

                if (use_target_distance){

                    if(expected_distance>=target_distance){

                        rly_speed_backcolor = Color.GREEN;

                    } else {

                        rly_speed_backcolor = Color.RED;

                    }

                } else {

                    rly_speed_backcolor = Color.TRANSPARENT;

                }

                rly_speed_visibility = View.VISIBLE;

            } else if (gps_status==MainService.GPS_STATUS.fixing) {

                speed_label =getString(R.string.blank_value);
                rly_speed_backcolor =Color.CYAN;
                rly_speed_visibility = View.VISIBLE;

            } else {

                speed_label =getString(R.string.blank_value);
                rly_speed_backcolor =Color.TRANSPARENT;
                rly_speed_visibility = View.INVISIBLE;

            }



        } else {

            rly_speed_backcolor = Color.TRANSPARENT;

            if (gps_status==MainService.GPS_STATUS.working) {


                rly_speed_visibility = View.VISIBLE;
                speed_label = String.format("%1.1f", speed * 3.6);

            } else if (gps_status==MainService.GPS_STATUS.fixing) {

                speed_label =getString(R.string.blank_value);
                rly_speed_visibility = View.VISIBLE;

            } else {

                speed_label =getString(R.string.blank_value);
                rly_speed_visibility = View.INVISIBLE;

            }

            // session status not running or GPS status not working
            distance_label= getString(R.string.blank_value);

        }

        txv_speed.setText(speed_label);
        rly_speed.setBackgroundColor(rly_speed_backcolor);
        rly_speed.setVisibility(rly_speed_visibility);

        txv_distance.setText(distance_label);

    }

    private void update_time() {

        if (session_status == MainService.SESSION_STATUS.started) {

            String time_label;

            if (show_remaining_time) {
                time_label = SharedFunctions.format_time_string(remaining_time);

            } else {
                time_label = SharedFunctions.format_time_string(elapsed_time_ms);

            }

            txv_elapsedtime.setText(time_label);

        } else {

            txv_elapsedtime.setText(getString(R.string.blank_value_time));

        }

    }

    private void update_distance_label(boolean flag) {

        if (flag) {

            txv_distance_label.setText(getString(R.string.main_label_expected_distance));

        } else {

            txv_distance_label.setText(getString(R.string.main_label_distance));

        }

    }

    private void update_time_label(boolean flag) {

        if (flag) {

            txv_elapsedtime_label.setText(getString(R.string.main_label_residual_time));

        } else {

            txv_elapsedtime_label.setText(getString(R.string.main_label_time));

        }

    }

    private boolean is_service_running() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.apps.lorenzofailla.votrac.MainService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void play_sound(int sound_id){

        sound_pool.play(1, 1f, 1f, 1, 0, 1f);

    }



}