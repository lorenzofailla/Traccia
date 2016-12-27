package com.apps.lorenzofailla.vocalpacer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

/**
 * Created by Lorenzo Failla on 11/gen/2016.
 */

public class MainService extends Service implements TextToSpeech.OnInitListener, LocationListener {
    /**
     * Questa classe rappresenta il servizio che gira in background per tutta la durata della sessione
     *
     */

    private NotificationCompat.Builder mBuilder;
    private NotificationManager notification_manager;

    private DBManager database_manager;

    private TextToSpeech local_tts;
    private boolean is_tts_available;

    private Handler tasks_handler = new Handler();

    // flag per l'interblocco di alcune operazioni
    private boolean is_advice_distance_allowed;
    private boolean is_advice_time_allowed;
    private boolean is_advise_end_of_session_allowed;
    private boolean is_advise_target_distance_fulfilment_allowed;

    private GPS_STATUS gps_status = GPS_STATUS.paused;
    private SESSION_STATUS session_status = SESSION_STATUS.stopped;
    private PACE_STATUS pace_status = null;
    private PACE_STATUS previous_pace_status = null;

    // parametri di configurazione - configurabili dall'utente
    private boolean advise_on_time_base;
    private boolean advise_on_distance_base;
    private boolean advise_distance_on_time_base;
    private boolean advise_time_on_distance_base;
    private boolean advise_remaining_time_on_time_base;
    private boolean advise_finish_distance_on_time_base;
    private boolean advise_remaining_time_on_distance_base;
    private boolean advise_finish_distance_on_distance_base;
    private boolean use_session_duration;
    private boolean use_target_distance;
    private boolean advise_target_distance_fulfilment_on_time_base;
    private boolean advise_target_distance_fulfilment_on_distance_base;
    private boolean advise_on_pace_change;

    private int time_interval;
    private double distance_interval;
    private long session_duration;
    private boolean approximate_distance;
    private double target_distance;

    // parametri di configurazione - non configurabili dall'utente
    private final long gps_standby_time_milliseconds = 300000;
    private final int gps_min_update_time = 15;

    private final long[] vibration_pattern_start = {0,50,50,50,50,50,50,50,50,50};
    private final long[] vibration_pattern_stop = {0,50,50,50,50,50,50,50,50,50};

    // parametri di funzionamento - calcolati dal servizio
    private double distance = 0.0;
    private long elapsed_time = 0L;
    private long remaining_time;
    private double expected_distance;
    private double actual_average_pace;
    private long time_start;
    private long next_advice_time = 0L;
    private double next_advice_distance = 0.0;
    private long gps_last_fix = System.currentTimeMillis()-3600000L;

    private long session_id;

    private Location current_location = null;
    private Location previous_location = null;

    private SharedPreferences shared_preferences;

    private LocalBroadcastManager local_broadcastmanager;

    private LocationManager location_manager;

    private Runnable check_triggers_for_speech = new Runnable() {

        public void run() {
/**
 *
 */
            // update the elapsed time
            if (session_status == SESSION_STATUS.started) {

                Intent intent;

                // calculate elapsed time and remaining time
                long current_time_ms = System.currentTimeMillis();
                elapsed_time = current_time_ms - time_start;

                // compose the intent to transmit data about timebeat
                // the intent is broadcast at the end of the present 'if' block
                intent = new Intent("@VOCALPACER:::TIMEBEAT");
                intent.putExtra("_Elapsed_Time", elapsed_time);

                if (use_session_duration) {

                    // calculates the remaining time
                    remaining_time = session_duration * 1000 - elapsed_time;

                    if (remaining_time > 0) {
                        // if session duration is still valid :
                        // : calculates expected distance
                        actual_average_pace = (double) distance / elapsed_time * 1000.0;
                        expected_distance = distance + (actual_average_pace * remaining_time / 1000.0);

                    } else {
                        // duration has been overcame :
                        // : it's not possible to calculate the expected distance, so the acual distance is kept for reference
                        expected_distance = distance;

                        if (is_tts_available && is_advise_end_of_session_allowed) {

                            local_tts.speak(getString(R.string.speech_finish_reached) + " " + advise_distance(distance, false), TextToSpeech.QUEUE_ADD, null);
                            is_advise_end_of_session_allowed = false;

                        }

                    }

                    intent.putExtra("_Remaining_Time", remaining_time);
                    intent.putExtra("_Expected_Distance", expected_distance);

                }

                ////////////////////////////////////////////////////////////////////////////////////
                // se l'utente ha selezionato una distanza obiettivo, vengono effettuate le seguenti
                // operazioni:
                // - avvisa del superamento della distanza obiettivo
                ////////////////////////////////////////////////////////////////////////////////////

                if (use_target_distance){

                    if(target_distance-distance <= 0){

                        // avvisa dell'avvenuto raggiungimento del target
                        if (is_tts_available && is_advise_target_distance_fulfilment_allowed) {

                            local_tts.speak(getString(R.string.speech_target_distance_reached) + " " + advise_time(elapsed_time), TextToSpeech.QUEUE_ADD, null);
                            is_advise_target_distance_fulfilment_allowed = false;

                        }

                    }

                }

                ////////////////////////////////////////////////////////////////////////////////////
                // se l'utente ha selezionato di essere avvisato sui cambi di passo durante la
                // corsa, vengono effettuate le segenti operazioni:
                //  // TODO: 10/07/2016  inserire descrizione della funzione
                ////////////////////////////////////////////////////////////////////////////////////

                if (advise_on_pace_change){

                    /*
                    ////////////////////////////////////////////////////////////////////////////////////////////
                    // In questa funzione vengono valutate le condizioni per generare un avviso di cambio di
                    // passo durante la corsa.
                    // Se il valore precedente del passo era buono, controlla che la distanza all'arrivo non
                    // sia sceso sotto un valore di soglia, altrimenti genera l'avviso di aumentare il passo.
                    // Se il valore precedente del passo non era buono, controlla che la distanza all'arrivo non
                    // sia salito sopra un valore di soglia, altrimenti genera l'avviso di diminuire il passo.
                    ////////////////////////////////////////////////////////////////////////////////////////////
                    */

                    // aggiorna il valore di previous_pace_status
                    if (pace_status!=null) {
                        previous_pace_status = pace_status;
                    }

                    // aggiorna il valore di pace_status

                    double PACE_CHANGE_UPPER_BOUND = 1.05;
                    double PACE_CHANGE_LOWER_BOUND = 1;
                    if (expected_distance >= target_distance* PACE_CHANGE_UPPER_BOUND){
                        // pace is good
                        pace_status=PACE_STATUS.keep;

                        // valuta se generare l'avviso che il passo è tornato buono
                        if (previous_pace_status!= null) {
                            // controllo necessario per evitare un java.lang.NullPointerException

                            if (is_tts_available && previous_pace_status.equals(PACE_STATUS.increase)) {
                                // genera l'avviso
                                local_tts.speak(getString(R.string.speech_insight_keep_this_pace), TextToSpeech.QUEUE_ADD, null);
                            }
                        }


                    } else if (expected_distance <= target_distance* PACE_CHANGE_LOWER_BOUND){
                        // pace is not good, has to be increased
                        pace_status=PACE_STATUS.increase;

                        // valuta se generare l'avviso che il passo deve incrementare
                        if (previous_pace_status!= null) {
                            // controllo necessario per evitare un java.lang.NullPointerException

                            if (is_tts_available && previous_pace_status.equals(PACE_STATUS.keep)) {
                                // genera l'avviso
                                local_tts.speak(getString(R.string.speech_insight_increase_the_pace), TextToSpeech.QUEUE_ADD, null);
                            }

                        }

                    }

                }

                ////////////////////////////////////////////////////////////////////////////////////
                // check triggers to speech on DISTANCE base
                ////////////////////////////////////////////////////////////////////////////////////

                int distance_to_advice = (int) (distance - next_advice_distance);
                int time_to_advice = (int) (elapsed_time / 1000 - next_advice_time);

                if (distance_to_advice > 0) {

                    if (advise_on_distance_base && is_tts_available && is_advice_distance_allowed) {

                        // advise on distance
                        local_tts.speak(advise_distance(distance, approximate_distance), TextToSpeech.QUEUE_ADD, null);

                        // if enabled, advise on time
                        if (advise_time_on_distance_base) {

                            local_tts.speak(advise_time(elapsed_time), TextToSpeech.QUEUE_ADD, null);
                        }

                        if (use_session_duration && (remaining_time > 0)) {

                            if (advise_remaining_time_on_distance_base) {

                                local_tts.speak(advice_remaining_time(remaining_time), TextToSpeech.QUEUE_ADD, null);

                            }

                            if (advise_finish_distance_on_distance_base) {

                                local_tts.speak(advice_expected_finish(expected_distance), TextToSpeech.QUEUE_ADD, null);
                            }

                            if (advise_target_distance_fulfilment_on_distance_base){

                                // determina se la distanza attesa all'arrivo è maggiore o minore della distanza obiettivo, e manda un avviso vocale di conseguenza
                                local_tts.speak(advise_target_distance_fulfilment_insight(target_distance, expected_distance), TextToSpeech.QUEUE_ADD, null);

                            }

                        }

                        // update the interlock flag
                        is_advice_distance_allowed = false;

                        // recalculate the next advice distance
                        recalculate_next_advice_distance();

                    }

                } else {

                    // Set
                    is_advice_distance_allowed = true;

                }

                ////////////////////////////////////////////////////////////////////////////////////
                // verifica se ci sono le condizioni per generare avvisi vocali su base tempo
                ////////////////////////////////////////////////////////////////////////////////////

                if (time_to_advice > 0) {

                    // check if speech info has to be triggered according to time interval
                    if (advise_on_time_base && is_tts_available && is_advice_time_allowed) {

                        // advise on time
                        local_tts.speak(advise_time(elapsed_time), TextToSpeech.QUEUE_ADD, null);

                        // if enabled, advise on distance
                        if (advise_distance_on_time_base) {
                            local_tts.speak(advise_distance(distance, approximate_distance), TextToSpeech.QUEUE_ADD, null);
                        }

                        if (use_session_duration && (remaining_time > 0)) {

                            if (advise_remaining_time_on_time_base) {

                                local_tts.speak(advice_remaining_time(remaining_time), TextToSpeech.QUEUE_ADD, null);

                            }

                            if (advise_finish_distance_on_time_base) {

                                local_tts.speak(advice_expected_finish(expected_distance), TextToSpeech.QUEUE_ADD, null);

                            }

                            if (use_target_distance && advise_target_distance_fulfilment_on_time_base){

                                // determina se la distanza attesa all'arrivo è maggiore o minore della distanza obiettivo, e manda un avviso vocale di conseguenza
                                local_tts.speak(advise_target_distance_fulfilment_insight(target_distance, expected_distance), TextToSpeech.QUEUE_ADD, null);

                            }

                        }

                        // update the interlock flag
                        is_advice_time_allowed = false;

                        // recalculate the next advice time
                        recalculate_next_advice_time();

                    }

                } else {

                    is_advice_time_allowed = true;

                }

                local_broadcastmanager.sendBroadcast(intent);

            } else {

            }

            tasks_handler.postAtTime(this, SystemClock.uptimeMillis() + 1000);
        }
    };

    private Runnable remove_gps_updates = new Runnable() {

        @Override
        public void run() {
            stop_gps_updates_request();
        }
    };

    // set a BroadcastReceiver to handle broadcasts from activities
    private BroadcastReceiver broadcast_receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            // runs whenever a broadcasted Intent is received
            switch (intent.getAction()) {

                case "@VOCALPACER:::REQUEST_TO_START_SESSION": // start request is received

                    // sets the session status to waiting_to_start
                    session_status = SESSION_STATUS.waiting_to_start;

                    // starts the gps
                    start_gps_updates_request();
                    if (gps_status != null)
                        set_gps_status(gps_status); // this line is needed to trigger the
                    // managing of the gps status
                    break;

                case "@VOCALPACER:::REQUEST_TO_IMMEDIATE_START_SESSION":
                    // start the session immediately

                    // start the session
                    toggle_recording_status(true);

                    break;

                case "@VOCALPACER:::REQUEST_TO_STOP_SESSION": // stop request is received
                    if (session_status == SESSION_STATUS.started) {
                        // stop the session immediately

                        toggle_recording_status(false);
                    }

                    break;

                case "@VOCALPACER:::PARAMETERS_UPDATE_REQUEST": // an activity requests the update of the parameters

                    send_session_status_update();
                    send_parameters_update();
                    send_gps_status_update();

                    break;

                case "@VOCALPACER:::CONFIGURATION_UPDATE_REPLY": // an activity sent the configuration parameters, most likely because they were changed

                    /*
                    time_interval = (int) (intent.getLongExtra("__time_interval", 300L));
                    distance_interval = (double) (intent.getFloatExtra("__distance_interval", 250));
                    session_duration = intent.getLongExtra("__target_duration", 3600L);

                    advise_on_time_base = intent.getBooleanExtra("__advise_on_time_base", true);
                    advise_on_distance_base = intent.getBooleanExtra("__advise_on_distance_base", true);
                    advise_time_on_distance_base = intent.getBooleanExtra("__advise_time_on_distance_base", true);
                    advise_distance_on_time_base = intent.getBooleanExtra("__advise_distance_on_time_base", true);
                    advise_remaining_time_on_time_base = intent.getBooleanExtra("__advise_remaining_time_on_time_base", true);
                    advise_finish_distance_on_time_base = intent.getBooleanExtra("__advise_finish_distance_on_time_base", true);
                    advise_remaining_time_on_distance_base = intent.getBooleanExtra("__advise_remaining_time_on_distance_base", true);
                    advise_finish_distance_on_distance_base = intent.getBooleanExtra("__advise_finish_distance_on_distance_base", true);
                    use_session_duration = intent.getBooleanExtra("__use_session_duration", true);
                    use_target_distance = intent.getBooleanExtra("__use_target_distance", getResources().getBoolean(R.bool.pref_default__use_target_distance));
                    target_distance = intent.getDoubleExtra("__target_distance", (float) getResources().getInteger(R.integer.pref_default__target_distance));
                    advise_target_distance_fulfilment_on_time_base = intent.getBooleanExtra("__advise_target_distance_fulfilment_on_time_base", getResources().getBoolean(R.bool.pref_default__advise_target_distance_fulfilment_on_time_base));
                    advise_target_distance_fulfilment_on_distance_base = intent.getBooleanExtra("__advise_target_distance_fulfilment_on_distance_base", getResources().getBoolean(R.bool.pref_default__advise_target_distance_fulfilment_on_distance_base));
                    advise_on_pace_change = intent.getBooleanExtra("__advise_advise_on_pace_change", getResources().getBoolean(R.bool.pref_default__advise_on_pace_change));
                    */

                    load_preferences();

                    // recalculate the time and the distance for the next advice to be triggered
                    recalculate_next_advice_distance();
                    recalculate_next_advice_time();

                    Toast.makeText(getApplicationContext(), getString(R.string.toast_configuration_loaded), Toast.LENGTH_LONG).show();

                    send_parameters_update();

                    break;

                case "@VOCALPACER:::REQUEST_TO_CANCEL_WAITING_STATUS":

                    // set the session status to stopped
                    session_status = SESSION_STATUS.stopped;

                    // schedule the switch off of the gps, to save energy
                    tasks_handler.postAtTime(remove_gps_updates, SystemClock.uptimeMillis() + gps_standby_time_milliseconds);

                    break;

                case "@VOCALPACER:::REPLY_PLEASE_KEEP_SESSION_IN_DB":
                    // comes when user selected to keep the session in memory

                    finalize_session_data_in_db();

                    break;

                case "@VOCALPACER:::REPLY_PLEASE_DISCARD_SESSION":
                    // comes when user selected to discard the session
                    delete_session_data_in_db();

                    break;

            }

        }

    };


    private GpsStatus.Listener gps_status_listener = new GpsStatus.Listener() {

        @Override
        public void onGpsStatusChanged(int i) {

            switch (i) {

                case GpsStatus.GPS_EVENT_FIRST_FIX:

                    // set the gps status to working
                    set_gps_status(GPS_STATUS.working);

                    // record the fix time
                    gps_last_fix = System.currentTimeMillis();

                    break;


                case GpsStatus.GPS_EVENT_STARTED:

                    // set the gps status to fixing
                    set_gps_status(GPS_STATUS.fixing);

                    break;


                case GpsStatus.GPS_EVENT_STOPPED:

                    // set the gps status to paused
                    set_gps_status(GPS_STATUS.paused);

                    break;

            }

        }

    };

    private Runnable update_notification_text = new Runnable() {

        public void run() {

            String output_message;

            if (session_status == SESSION_STATUS.started) {
                output_message = getString(R.string.notification_sesson_running) + " " + SharedFunctions.format_time_string(elapsed_time);
            } else {
                output_message = getString(R.string.notification_sesson_stop);
            }

            mBuilder.setContentText(output_message);
            notification_manager.notify(1, mBuilder.build());

            tasks_handler.postAtTime(this, SystemClock.uptimeMillis() + 10000);
        }
    };

    @Override
    public void onLocationChanged(Location location) {

        ////////////////////////////////////////////////////////////////////////////////////////////
        // NEW LOCATION
        ////////////////////////////////////////////////////////////////////////////////////////////

        Intent intent;

        // record the fix time
        gps_last_fix = System.currentTimeMillis();

        // set the gps status to working, only if the current status is different
        if (gps_status != GPS_STATUS.working) set_gps_status(GPS_STATUS.working);

        if (current_location != null) {
            previous_location = current_location;

        }

        current_location = location;  // register the new current location

        if (session_status == SESSION_STATUS.started) {

            if (previous_location != null) {

                // UPDATE THE ACTUAL DISTANCE
                distance += current_location.distanceTo(previous_location);

            }

            if (elapsed_time > 0) {

                actual_average_pace = distance / elapsed_time * 1000.0;

            }

            database_manager.open();
            database_manager.insert_sample(session_id,
                    SharedFunctions.get_current_date_YYYY_MM_DD(),
                    SharedFunctions.get_current_time_HH_mm_SS(),
                    current_location.getLatitude(),
                    current_location.getLongitude(),
                    current_location.getSpeed(),
                    current_location.getAltitude()
            );
            database_manager.close();
        }

        // send an intent so that all the activities can take an action
        intent = new Intent("@VOCALPACER:::NEW_LOCATION");
        intent.putExtra("_current_speed", current_location.getSpeed());
        intent.putExtra("_distance", distance);

        local_broadcastmanager.sendBroadcast(intent);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle bundle) {

        if (provider.equals(LocationManager.GPS_PROVIDER)) {

            switch (status) {

                case LocationProvider.AVAILABLE:

                    // set the gps status to fixing or working, according to the result of the function
                    // check_gpx_fix()
                    set_gps_status(check_gpx_fix());

                    break;

                case LocationProvider.OUT_OF_SERVICE:

                    // set the gps status to disabled
                    set_gps_status(GPS_STATUS.disabled);

                    break;

                case LocationProvider.TEMPORARILY_UNAVAILABLE:

                    // set the gps status to disabled
                    set_gps_status(GPS_STATUS.disabled);

                    break;

            }

        }

    }

    @Override
    public void onProviderEnabled(String provider) {

        if (provider.equals(LocationManager.GPS_PROVIDER)) {

            // set the gps status to fixing
            set_gps_status(GPS_STATUS.fixing);

        }

    }

    @Override
    public void onProviderDisabled(String provider) {

        if (provider.equals(LocationManager.GPS_PROVIDER)) {

            // set the gps status to disabled
            set_gps_status(GPS_STATUS.disabled);

        }

    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = local_tts.setLanguage(Locale.getDefault());
            is_tts_available = !(result == TextToSpeech.LANG_MISSING_DATA) || (result == TextToSpeech.LANG_NOT_SUPPORTED);

        } else {

            is_tts_available = false;

        }

    }

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        database_manager = new DBManager(this.getApplicationContext());

        notification_manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        IntentFilter intent_filter = new IntentFilter();
        intent_filter.addAction("@VOCALPACER:::REQUEST_TO_START_SESSION");
        intent_filter.addAction("@VOCALPACER:::REQUEST_TO_STOP_SESSION");
        intent_filter.addAction("@VOCALPACER:::PARAMETERS_UPDATE_REQUEST");
        intent_filter.addAction("@VOCALPACER:::CONFIGURATION_UPDATE_REPLY");
        intent_filter.addAction("@VOCALPACER:::REQUEST_TO_IMMEDIATE_START_SESSION");
        intent_filter.addAction("@VOCALPACER:::REQUEST_TO_CANCEL_WAITING_STATUS");
        intent_filter.addAction("@VOCALPACER:::REPLY_PLEASE_KEEP_SESSION_IN_DB");
        intent_filter.addAction("@VOCALPACER:::REPLY_PLEASE_DISCARD_SESSION");

        local_broadcastmanager = LocalBroadcastManager.getInstance(this);
        local_broadcastmanager.registerReceiver(broadcast_receiver, intent_filter);

        // initialize the TextToSpeech
        local_tts = new TextToSpeech(this, this);

        // set the session status to stopped
        session_status = SESSION_STATUS.stopped;
        send_session_status_update();
        send_gps_status_update();

        // obtain an handler to shared preferences
        shared_preferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        load_preferences();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // create the notification
        mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.votrac)
                        .setContentTitle("Traccia")
                        .setContentText("is running... tap to open!");

        PendingIntent content_intent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        mBuilder.setContentIntent(content_intent);

        Notification notification = mBuilder.build();

        // start the service in foreground
        startForeground(1, notification);

        // initialize the tasks
        tasks_handler.removeCallbacks(update_notification_text);
        tasks_handler.postDelayed(update_notification_text, 0);

        tasks_handler.removeCallbacks(check_triggers_for_speech);
        tasks_handler.postDelayed(check_triggers_for_speech, 0);

        // initialize the LocationManager
        location_manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        location_manager.addGpsStatusListener(gps_status_listener);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {

        local_tts.speak(getString(R.string.speech_service_terminated), TextToSpeech.QUEUE_ADD, null);

        tasks_handler.removeCallbacks(update_notification_text);
        tasks_handler.removeCallbacks(check_triggers_for_speech);

        local_tts.shutdown();

        stop_gps_updates_request();

        local_broadcastmanager.unregisterReceiver(broadcast_receiver);

        stopForeground(true);

    }

    private void toggle_recording_status(boolean flag_value) {

        if (!(session_status == SESSION_STATUS.started) && flag_value) {
            // starts the recording

            String start_date = SharedFunctions.get_current_date_YYYY_MM_DD();
            String start_time = SharedFunctions.get_current_time_HH_mm_SS();

            // set the session status to started
            session_status = SESSION_STATUS.started;

            // Reset distance
            distance = 0;

            // Reset the last fix time
            gps_last_fix = System.currentTimeMillis()-3600000L;

            // initialize the value of some control variable
            recalculate_next_advice_time();
            recalculate_next_advice_distance();

            // initialize the value of some interlock flag
            is_advise_end_of_session_allowed = true;
            is_advise_target_distance_fulfilment_allowed = true;

            // keep record the current time
            time_start = System.currentTimeMillis();

            // creates a new record in the database
            database_manager.open();
            session_id = database_manager.insert_session(start_date, start_time);
            database_manager.close();

            local_tts.speak(getString(R.string.speech_recording_start), TextToSpeech.QUEUE_ADD, null);
            vibrate(vibration_pattern_start);

        } else if (session_status == SESSION_STATUS.started && !flag_value) {
            // stop the recording

            // set the session status to stopped
            session_status = SESSION_STATUS.stopped;

            if (conditions_to_keep_session_in_db()) {
                // >>>
                // <<< le condizioni per mantenere la traccia sono verificate
                // <<< so, updates the session record in the database with duration and distance

                finalize_session_data_in_db();

                // >>>

            } else {
                // >>>
                // <<< le condizioni per mantenere la traccia non sono verificate
                // <<< viene chiesto all'utente se mantenere la traccia oppure no.

                Intent intent = new Intent("@VOCALPACER:::REQUEST_KEEP_SESSION_IN_DB");
                local_broadcastmanager.sendBroadcast(intent);

                // >>>

            }


            local_tts.speak(getString(R.string.speech_recording_stop), TextToSpeech.QUEUE_ADD, null);
            vibrate(vibration_pattern_stop);

            // remove the gps periodical updates in 10 s, to save energy
            tasks_handler.postAtTime(remove_gps_updates, SystemClock.uptimeMillis() + 10000);

        }

        send_session_status_update();

    }

    private void delete_session_data_in_db(){

        // open the database to update the session record with with duration and distance
        database_manager.open();

        // elimino la traccia dal database
        database_manager.delete_session(session_id);

        // close the database
        database_manager.close();
    }

    private void finalize_session_data_in_db(){

        // open the database to update the session record with with duration and distance
        database_manager.open();

        // finalizzo i dati della traccia sul database
        database_manager.update_session(session_id, elapsed_time, distance);
        Toast.makeText(this, R.string.toast_session_saved_in_db, Toast.LENGTH_LONG).show();

        // close the database
        database_manager.close();

    }

    private void send_gps_status_update() {

        Intent intent;

        intent = new Intent("@VOCALPACER:::GPS_STATUS_UPDATE")
                .putExtra("_gps_status", gps_status);

        local_broadcastmanager.sendBroadcast(intent);

    }

    private void send_parameters_update() {
        Intent intent;
        intent = new Intent("@VOCALPACER:::PARAMETERS_CHANGED")
                .putExtra("_use_session_duration", use_session_duration)
                .putExtra("_use_target_distance", use_target_distance)
                .putExtra("_target_distance", target_distance)
                .putExtra("_Elapsed_Time", elapsed_time)
                .putExtra("_distance", distance);

        if (current_location!=null){
            intent.putExtra("_current_speed", current_location.getSpeed());
        }

        local_broadcastmanager.sendBroadcast(intent);

    }

    private void send_session_status_update(){

        Intent intent;
        intent = new Intent("@VOCALPACER:::SESSION_STATUS_CHANGED")
                .putExtra("_session_status", session_status);

        local_broadcastmanager.sendBroadcast(intent);

    }

    private void load_preferences() {

        boolean default_advise_on_time_base = getResources().getBoolean(R.bool.pref_default__advise_on_time_base);
        boolean default_advise_on_distance_base = getResources().getBoolean(R.bool.pref_default__advise_on_distance_base);
        boolean default_advise_distance_on_time_base = getResources().getBoolean(R.bool.pref_default__advise_distance_on_time_base);
        boolean default_advise_time_on_distance_base = getResources().getBoolean(R.bool.pref_default__advise_time_on_distance_base);
        float default_distance_interval = (float) getResources().getInteger(R.integer.pref_default__distance_interval);
        long default_time_interval = (long) (getResources().getInteger(R.integer.pref_default__time_interval));
        long default_session_duration = (long) getResources().getInteger(R.integer.pref_default__target_duration);
        boolean default_advise_remaining_time_on_time_base = getResources().getBoolean(R.bool.pref_default__advise_remaining_time_on_time_base);
        boolean default_advise_finish_distance_on_time_base = getResources().getBoolean(R.bool.pref_default__advise_finish_distance_on_time_base);
        boolean default_advise_remaining_time_on_distance_base = getResources().getBoolean(R.bool.pref_default__advise_remaining_time_on_distance_base);
        boolean default_advise_finish_distance_on_distance_base = getResources().getBoolean(R.bool.pref_default__advise_finish_distance_on_distance_base);
        boolean default_use_session_duration = getResources().getBoolean(R.bool.pref_default__use_session_duration);
        boolean default_approximate_distance = getResources().getBoolean(R.bool.pref_default__approximate_distance_to_quarters_of_km);
        boolean default_use_target_distance = getResources().getBoolean(R.bool.pref_default__use_target_distance);
        float default_target_distance = (float) getResources().getInteger(R.integer.pref_default__target_distance);
        boolean default_advise_target_distance_fulfilment_on_time_base = getResources().getBoolean(R.bool.pref_default__advise_target_distance_fulfilment_on_time_base);
        boolean default_advise_target_distance_fulfilment_on_distance_base = getResources().getBoolean(R.bool.pref_default__advise_target_distance_fulfilment_on_distance_base);
        boolean default_advise_on_pace_change = getResources().getBoolean(R.bool.pref_default__advise_on_pace_change);

        // retrieve the preferences
        advise_on_time_base = shared_preferences.getBoolean(
                getString(R.string.preferences__advise_on_time_base),
                default_advise_on_time_base);

        advise_on_distance_base = shared_preferences.getBoolean(
                getString(R.string.preferences__advise_on_distance_base),
                default_advise_on_distance_base);

        advise_time_on_distance_base = shared_preferences.getBoolean(
                getString(R.string.preferences__advise_time_on_distance_base),
                default_advise_time_on_distance_base);

        advise_distance_on_time_base = shared_preferences.getBoolean(
                getString(R.string.preferences__advise_distance_on_time_base),
                default_advise_distance_on_time_base);

        time_interval = (int) shared_preferences.getLong(
                getString(R.string.preferences__time_interval),
                default_time_interval);

        distance_interval = shared_preferences.getFloat(
                getString(R.string.preferences__distance_interval),
                default_distance_interval);

        session_duration = shared_preferences.getLong(
                getString(R.string.preferences__target_duration),
                default_session_duration);

        advise_remaining_time_on_time_base = shared_preferences.getBoolean(
                getString(R.string.preferences__advise_remaining_time_on_time_base),
                default_advise_remaining_time_on_time_base);

        advise_finish_distance_on_time_base = shared_preferences.getBoolean(
                getString(R.string.preferences__advise_finish_distance_on_time_base),
                default_advise_finish_distance_on_time_base);

        advise_remaining_time_on_distance_base = shared_preferences.getBoolean(
                getString(R.string.preferences__advise_remaining_time_on_distance_base),
                default_advise_remaining_time_on_distance_base);

        advise_finish_distance_on_distance_base = shared_preferences.getBoolean(
                getString(R.string.preferences__advise_finish_distance_on_distance_base),
                default_advise_finish_distance_on_distance_base);

        use_session_duration = shared_preferences.getBoolean(
                getString(R.string.preferences__use_session_duration),
                default_use_session_duration);

        approximate_distance = shared_preferences.getBoolean(
                getString(R.string.preferences__approximate_distances_to_quarters_of_km),
                default_approximate_distance
        );

        use_target_distance = shared_preferences.getBoolean(
                getString(R.string.preferences__use_target_distance),
                default_use_target_distance
        );

        target_distance = (double) shared_preferences.getFloat(
                getString(R.string.preferences__target_distance),
                default_target_distance
        );

        advise_target_distance_fulfilment_on_time_base=shared_preferences.getBoolean(
                getString(R.string.preferences__advise_target_distance_fulfilment_on_time_base),
                default_advise_target_distance_fulfilment_on_time_base
        );

        advise_target_distance_fulfilment_on_distance_base=shared_preferences.getBoolean(
                getString(R.string.preferences__advise_target_distance_fulfilment_on_distance_base),
                default_advise_target_distance_fulfilment_on_distance_base
        );

        advise_on_pace_change=shared_preferences.getBoolean(
                getString(R.string.preferences__advise_on_pace_change),
                default_advise_on_pace_change
        );

        // recalculate the time and the distance for the next advice to be triggered
        recalculate_next_advice_distance();
        recalculate_next_advice_time();

        send_parameters_update();

    }

    private void start_gps_updates_request() {
        /**
         *   Avvia la richiesta per ricevere le notifiche dal LocationListener implementato.
         *   Prima, rimuove per sicurezza il callback al Runnable remove_gps_updates, che si occupa
         *   invece di revocare la richiesta per ricezione notifiche dal LocationListener
        */

        // start the request for gps periodical updates

        // remove the task to stop the gps updates
        tasks_handler.removeCallbacks(remove_gps_updates);

        float gps_min_update_distance = 15.0f;
        location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, gps_min_update_time, gps_min_update_distance, this);

    }

    private void stop_gps_updates_request() {
        // stop the request for gps periodical updates

        location_manager.removeUpdates(this);

    }

    private void set_gps_status(GPS_STATUS new_status) {

        // store the current (before change) gps status
        GPS_STATUS previous_gps_status = gps_status;

        // set the new status
        gps_status = new_status;

        if (gps_status != previous_gps_status) {

            // send a broadcast to inform that the gps status has changed
            send_gps_status_update();

        }

        // perform some actions depending on gps and session status

        // check the status of gps
        switch (gps_status) {

            case working:

                // check the status of session
                switch (session_status) {

                    case waiting_to_start:

                        // start the session immediately
                        toggle_recording_status(true);

                        break;

                    case stopped:

                        // send a broadcast to inform that the session is ready to start immediately
                        local_broadcastmanager.sendBroadcast(new Intent("@VOCALPACER:::READY_TO_START"));

                        // schedule the switch off of the gps to save energy
                        tasks_handler.postAtTime(remove_gps_updates, SystemClock.uptimeMillis() + gps_standby_time_milliseconds);

                        break;

                }

                break;

            case fixing:

                // check the status of session
                switch (session_status) {

                    case waiting_to_start:

                        // start the session immediately
                        local_broadcastmanager.sendBroadcast(new Intent("@VOCALPACER:::ASK_FOR_START_WITHOUT_FIX"));

                        break;

                    case stopped:

                        // schedule the switch off of the gps to save energy
                        tasks_handler.postAtTime(remove_gps_updates, SystemClock.uptimeMillis() + gps_standby_time_milliseconds);

                        break;

                }

                break;

            case disabled:

                // check the status of session
                switch (session_status) {

                    case waiting_to_start:

                        // start the session immediately
                        local_broadcastmanager.sendBroadcast(new Intent("@VOCALPACER:::ASK_FOR_START_WITHOUT_GPS"));

                        break;

                }

                break;

        }

    }

    private void recalculate_next_advice_distance() {
        next_advice_distance = ((int) (distance / distance_interval) + 1) * distance_interval;

    }

    private void recalculate_next_advice_time() {
        next_advice_time = ((int) (elapsed_time / 1000 / time_interval) + 1) * time_interval;

    }

    private boolean conditions_to_keep_session_in_db() {

        return (distance > 100.0);

    }

    private GPS_STATUS check_gpx_fix(){

        long now = System.currentTimeMillis();
        int seconds_since_last_fix = (int) (now-gps_last_fix)/1000;

        if (seconds_since_last_fix < gps_min_update_time){

            return GPS_STATUS.working;

        } else {

            return GPS_STATUS.fixing;

        }

    }

    private void vibrate(long[] pattern){

        Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(getApplicationContext().VIBRATOR_SERVICE);

        vibrator.vibrate(pattern, -1);

    }

    public enum SESSION_STATUS {
        stopped,
        started,
        waiting_to_start
    }

    public enum GPS_STATUS {
        disabled,
        paused,
        fixing,
        working
    }

    public enum PACE_STATUS {
        increase,
        keep
    }

    private String advise_target_distance_fulfilment_insight(double target_distance, double expected_distance) {

        double KEEP_PACE_LOWER_BOUND = 1;
        double KEEP_PACE_UPPER_BOUND = 1.1;

        if (expected_distance < target_distance * KEEP_PACE_LOWER_BOUND) {

            // "increase pace" insight is returned
            return getString(R.string.speech_insight_increase_the_pace);

        } else if ((expected_distance >= target_distance * KEEP_PACE_LOWER_BOUND) && (expected_distance < target_distance * KEEP_PACE_UPPER_BOUND)) {

            // "keep pace" insight is returned
            return getString(R.string.speech_insight_keep_this_pace);

        } else {

            // "lower pace" insight is returned
            return getString(R.string.speech_insight_decrease_the_pace);
        }

    }

    private String advise_time(long time_ms) {

        String str_hours;
        String str_minutes;
        String conjunction;

        int elapsed_minutes = (int) (time_ms / 60000);
        int elapsed_hours = (int) (time_ms / 3600000);

        // perform modular division by 60 of seconds and minutes
        elapsed_minutes = elapsed_minutes % 60;

        if (elapsed_minutes == 0) {
            str_minutes = "";
            conjunction = "";
        } else if (elapsed_minutes == 1) {

            str_minutes = getString(R.string.speech_one) + " " + getString(R.string.speech_minute);

            if (elapsed_hours > 0) {
                conjunction = " " + getString(R.string.speech_and) + " ";
            } else {
                conjunction = "";
            }
        } else {
            str_minutes = String.format("%d ", elapsed_minutes) + getString(R.string.speech_minutes);
            if (elapsed_hours > 0) {
                conjunction = " " + getString(R.string.speech_and) + " ";
            } else {
                conjunction = "";
            }
        }

        if (elapsed_hours == 0) {
            str_hours = "";
        } else if (elapsed_hours == 1) {
            str_hours = getString(R.string.speech_one) + " " + getString(R.string.speech_hour);
        } else {
            str_hours = String.format("%d ", elapsed_hours) + getString(R.string.speech_hours);
        }

        return (str_hours + conjunction + str_minutes);

    }

    private String advise_distance(double distance_meters, boolean approximate) {

        String str_kilometers = "";
        String str_meters = "";
        String conjunction = "";

        int kilometers = (int) (distance_meters / 1000);
        int meters = (int) (distance_meters - kilometers * 1000);

        if (!approximate) {
            if (meters == 0) {

                str_meters = "";
                conjunction = "";

            } else if (meters == 1) {

                str_meters = getString(R.string.speech_one) + " " + getString(R.string.speech_meter);

                if (kilometers > 0) {
                    conjunction = " " + getString(R.string.speech_and) + " ";
                } else {
                    conjunction = "";
                }

            } else {

                str_meters = String.format("%d ", meters) + getString(R.string.speech_meters);

                if (kilometers > 0) {
                    conjunction = " " + getString(R.string.speech_and) + " ";
                } else {
                    conjunction = "";
                }
            }
        } else {
            // approssima la distanza a quarti di kilometro

            switch ((int) (meters / 250.0)) {

                case 0:
                    // meno di 250 metri

                    str_meters = "";
                    conjunction = "";

                    break;

                case 1:
                    // tra 250 e 499 metri

                    if (kilometers > 0) {

                        conjunction = " " + getString(R.string.speech_and) + " ";
                        str_meters = getString(R.string.speech_one_quarter);

                    } else {

                        conjunction = "";
                        str_meters = "250 " + getString(R.string.speech_meters);

                    }

                    break;

                case 2:
                    // tra 500 e 749 metri

                    if (kilometers > 0) {

                        conjunction = " " + getString(R.string.speech_and) + " ";
                        str_meters = getString(R.string.speech_one_half);

                    } else {

                        conjunction = "";
                        str_meters = "500 " + getString(R.string.speech_meters);

                    }

                    break;

                case 3:
                    // tra 750 e 999 metri

                    if (kilometers > 0) {

                        conjunction = " " + getString(R.string.speech_and) + " ";
                        str_meters = getString(R.string.speech_three_quarters);

                    } else {

                        conjunction = "";
                        str_meters = "750 " + getString(R.string.speech_meters);

                    }

                    break;

            }

        }

        if (kilometers == 0) {

            str_kilometers = "";

        } else if (kilometers == 1) {

            str_kilometers = getString(R.string.speech_one) + " " + getString(R.string.speech_kilometer);

        } else {

            str_kilometers = String.format("%d ", kilometers) + getString(R.string.speech_kilometers);

        }

        return (str_kilometers + conjunction + str_meters);

    }

    private String advice_remaining_time(long remaining_time_ms) {

        String phrase = advise_time(remaining_time_ms);

        if (!phrase.equals("")) {

            return (getString(R.string.speech_togo_time_1) + " " + phrase + " " + getString(R.string.speech_togo_time_2));

        } else {

            return "";

        }

    }

    private String advice_expected_finish(double distance_meters) {

        String phrase = advise_distance(distance_meters, false);

        if (!phrase.equals("")) {
            return (getString(R.string.speech_togo_distance_1) + " " + phrase + " " + getString(R.string.speech_togo_distance_2) + " ");
        } else {
            return "";
        }
    }

}