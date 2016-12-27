package com.apps.lorenzofailla.vocalpacer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Arrays;

public class Settings extends AppCompatActivity
        implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    SharedPreferences shared_preferences;

    int selectable_time_intervals[];
    int selectable_distance_intervals[];
    int selectable_session_durations[];
    int selectable_target_distances[];

    CheckBox chk_advise_on_time_base;
    CheckBox chk_advise_on_distance_base;
    CheckBox chk_advise_distance_on_time_base;
    CheckBox chk_advise_time_on_distance_base;
    CheckBox chk_advise_remaining_time_on_time_base;
    CheckBox chk_advise_finish_distance_on_time_base;
    CheckBox chk_advise_remaining_time_on_distance_base;
    CheckBox chk_advise_finish_distance_on_distance_base;
    CheckBox chk_session_duration;
    CheckBox chk_approximate_distances;
    CheckBox chk_use_target_distance;
    CheckBox chk_advise_target_distance_fulfilment_on_time_base;
    CheckBox chk_advise_target_distance_fulfilment_on_distance_base;
    CheckBox chk_advise_on_pace_change;

    Spinner spn_time_interval;
    Spinner spn_distance_interval;
    Spinner spn_session_duration;
    Spinner spn_target_distance;

    boolean advise_on_time_base;
    boolean advise_on_distance_base;
    boolean advise_distance_on_time_base;
    boolean advise_time_on_distance_base;
    boolean advise_remaining_time_on_time_base;
    boolean advise_finish_distance_on_time_base;
    boolean advise_remaining_time_on_distance_base;
    boolean advise_finish_distance_on_distance_base;
    boolean use_session_duration;
    boolean approximate_distance_to_quarters_of_km;
    boolean use_target_distance;
    boolean advise_target_distance_fulfilment_on_time_base;
    boolean advise_target_distance_fulfilment_on_distance_base;
    boolean advise_on_pace_change;

    double distance_interval;
    long time_interval;
    long session_duration;
    double target_distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    protected void onResume() {

        super.onResume();

        // obtain an handler to shared preferences
        shared_preferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        // recupera gli handlers dei vari componenti
        selectable_time_intervals = getResources().getIntArray(R.array.selectable_times_values);
        selectable_distance_intervals = getResources().getIntArray(R.array.selectable_distances_values);
        selectable_session_durations = getResources().getIntArray(R.array.selectable_expected_session_duration_values);
        selectable_target_distances = getResources().getIntArray(R.array.selectable_target_distance_values);

        chk_advise_on_distance_base = (CheckBox) findViewById(R.id.chk_advise_on_distance_base);
        chk_advise_on_time_base = (CheckBox) findViewById(R.id.chk_advise_on_time_base);
        chk_advise_distance_on_time_base = (CheckBox) findViewById(R.id.chk_advise_distance_on_time_base);
        chk_advise_time_on_distance_base = (CheckBox) findViewById(R.id.chk_advise_time_on_distance_base);
        chk_advise_remaining_time_on_time_base = (CheckBox) findViewById(R.id.chk___settings___advise_remaining_time_on_time_base);
        chk_advise_finish_distance_on_time_base = (CheckBox) findViewById(R.id.chk___settings___advise_finish_distance_on_time_base);
        chk_advise_remaining_time_on_distance_base = (CheckBox) findViewById(R.id.chk___settings___advise_remaining_time_on_distance_base);
        chk_advise_finish_distance_on_distance_base = (CheckBox) findViewById(R.id.chk___settings___advise_finish_distance_on_distance_base);
        chk_session_duration = (CheckBox) findViewById(R.id.chk___settings___expected_session_duration);
        chk_approximate_distances = (CheckBox) findViewById(R.id.chk___settings___approximate_distances_to_quarters_of_km);
        chk_use_target_distance =(CheckBox) findViewById(R.id.chk___settings___use_target_distance) ;
        chk_advise_target_distance_fulfilment_on_time_base=(CheckBox) findViewById(R.id.chk___settings___advise_target_distance_fulfilment_on_time_base) ;
        chk_advise_target_distance_fulfilment_on_distance_base=(CheckBox) findViewById(R.id.chk___settings___advise_target_distance_fulfilment_on_distance_base) ;
        chk_advise_on_pace_change=(CheckBox) findViewById(R.id.chk___settings___advise_on_pace_change) ;

        spn_time_interval = (Spinner) findViewById(R.id.spn_time);
        spn_distance_interval = (Spinner) findViewById(R.id.spn_distance);
        spn_session_duration = (Spinner) findViewById(R.id.spn___settings___expected_session_duration);
        spn_target_distance =(Spinner) findViewById(R.id.spn___settings___target_distance);

        load_preferences();
        update_view();

        // set the listeners to the drawable components
        chk_advise_on_distance_base.setOnClickListener(this);
        chk_advise_on_time_base.setOnClickListener(this);
        chk_advise_distance_on_time_base.setOnClickListener(this);
        chk_advise_time_on_distance_base.setOnClickListener(this);
        chk_advise_remaining_time_on_time_base.setOnClickListener(this);
        chk_advise_finish_distance_on_time_base.setOnClickListener(this);
        chk_advise_remaining_time_on_distance_base.setOnClickListener(this);
        chk_advise_finish_distance_on_distance_base.setOnClickListener(this);
        chk_session_duration.setOnClickListener(this);
        chk_approximate_distances.setOnClickListener(this);
        chk_use_target_distance.setOnClickListener(this);
        chk_advise_target_distance_fulfilment_on_time_base.setOnClickListener(this);
        chk_advise_target_distance_fulfilment_on_distance_base.setOnClickListener(this);
        chk_advise_on_pace_change.setOnClickListener(this);

        spn_time_interval.setOnItemSelectedListener(this);
        spn_distance_interval.setOnItemSelectedListener(this);
        spn_session_duration.setOnItemSelectedListener(this);
        spn_target_distance.setOnItemSelectedListener(this);

    }

    protected void onPause() {
        super.onPause();
        transmit_configuration();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.menu_settings, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            case R.id.chk_advise_on_time_base:

                advise_on_time_base = chk_advise_on_time_base.isChecked();

                if (!advise_on_time_base){

                    advise_distance_on_time_base = false;
                    advise_finish_distance_on_time_base=false;
                    advise_remaining_time_on_time_base=false;
                    advise_target_distance_fulfilment_on_time_base=false;

                }

                break;

            case R.id.chk_advise_on_distance_base:

                advise_on_distance_base = chk_advise_on_distance_base.isChecked();

                if (!advise_on_distance_base){

                    advise_time_on_distance_base = false;
                    advise_finish_distance_on_distance_base=false;
                    advise_remaining_time_on_distance_base=false;
                    advise_target_distance_fulfilment_on_distance_base=false;

                }

                break;

            case R.id.chk_advise_time_on_distance_base:

                advise_time_on_distance_base = chk_advise_time_on_distance_base.isChecked();

                break;

            case R.id.chk_advise_distance_on_time_base:

                advise_distance_on_time_base = chk_advise_distance_on_time_base.isChecked();

                break;

            case R.id.chk___settings___advise_remaining_time_on_distance_base:

                advise_remaining_time_on_distance_base = chk_advise_remaining_time_on_distance_base.isChecked();

                break;

            case R.id.chk___settings___advise_finish_distance_on_distance_base:

                advise_finish_distance_on_distance_base = chk_advise_finish_distance_on_distance_base.isChecked();

                break;

            case R.id.chk___settings___advise_remaining_time_on_time_base:

                advise_remaining_time_on_time_base = chk_advise_remaining_time_on_time_base.isChecked();

                break;

            case R.id.chk___settings___advise_finish_distance_on_time_base:

                advise_finish_distance_on_time_base = chk_advise_finish_distance_on_time_base.isChecked();

                break;

            case R.id.chk___settings___expected_session_duration:

                use_session_duration = chk_session_duration.isChecked();

                if (!use_session_duration) {

                    advise_finish_distance_on_distance_base=false;
                    advise_remaining_time_on_distance_base=false;
                    advise_target_distance_fulfilment_on_distance_base=false;

                    advise_finish_distance_on_time_base=false;
                    advise_remaining_time_on_time_base=false;
                    advise_target_distance_fulfilment_on_time_base=false;

                    advise_on_pace_change = false;

                }

                break;

            case R.id.chk___settings___approximate_distances_to_quarters_of_km:

                approximate_distance_to_quarters_of_km = chk_approximate_distances.isChecked();

                break;

            case R.id.chk___settings___use_target_distance:

                use_target_distance = chk_use_target_distance.isChecked();

                if(!use_target_distance){

                    advise_target_distance_fulfilment_on_distance_base=false;
                    advise_target_distance_fulfilment_on_time_base=false;
                    advise_on_pace_change = false;

                }

                break;

            case R.id.chk___settings___advise_target_distance_fulfilment_on_distance_base:

                advise_target_distance_fulfilment_on_distance_base=chk_advise_target_distance_fulfilment_on_distance_base.isChecked();

                break;

            case R.id.chk___settings___advise_target_distance_fulfilment_on_time_base:

                advise_target_distance_fulfilment_on_time_base=chk_advise_target_distance_fulfilment_on_time_base.isChecked();

                break;

            case R.id.chk___settings___advise_on_pace_change:

                advise_on_pace_change=chk_advise_on_pace_change.isChecked();

                break;

        }

        save_preferences();
        update_view();

    }

    private void update_view() {

        // set the checked property of the checkboxes
        chk_advise_on_time_base.setChecked(advise_on_time_base);
        chk_advise_on_distance_base.setChecked(advise_on_distance_base);
        chk_advise_distance_on_time_base.setChecked(advise_distance_on_time_base);
        chk_advise_time_on_distance_base.setChecked(advise_time_on_distance_base);
        chk_advise_remaining_time_on_time_base.setChecked(advise_remaining_time_on_time_base);
        chk_advise_finish_distance_on_time_base.setChecked(advise_finish_distance_on_time_base);
        chk_advise_remaining_time_on_distance_base.setChecked(advise_remaining_time_on_distance_base);
        chk_advise_finish_distance_on_distance_base.setChecked(advise_finish_distance_on_distance_base);
        chk_approximate_distances.setChecked(approximate_distance_to_quarters_of_km);
        chk_advise_on_pace_change.setChecked(advise_on_pace_change);

        // set the selected index of the spinners
        spn_time_interval.setSelection(Arrays.binarySearch(selectable_time_intervals, (int) time_interval));
        spn_distance_interval.setSelection(Arrays.binarySearch(selectable_distance_intervals, (int) distance_interval));
        spn_session_duration.setSelection(Arrays.binarySearch(selectable_session_durations, (int) session_duration));
        spn_target_distance.setSelection(Arrays.binarySearch(selectable_target_distances, (int) target_distance));

        chk_advise_distance_on_time_base.setEnabled(advise_on_time_base);
        spn_time_interval.setEnabled(advise_on_time_base);

        chk_advise_time_on_distance_base.setEnabled(advise_on_distance_base);
        spn_distance_interval.setEnabled(advise_on_distance_base);

        chk_advise_remaining_time_on_time_base.setChecked(advise_remaining_time_on_time_base);
        chk_advise_finish_distance_on_time_base.setChecked(advise_finish_distance_on_time_base);
        chk_advise_remaining_time_on_distance_base.setChecked(advise_remaining_time_on_distance_base);
        chk_advise_finish_distance_on_distance_base.setChecked(advise_finish_distance_on_distance_base);

        chk_session_duration.setChecked(use_session_duration);
        spn_session_duration.setEnabled(use_session_duration);

        chk_advise_remaining_time_on_time_base.setEnabled(use_session_duration && advise_on_time_base);
        chk_advise_finish_distance_on_time_base.setEnabled(use_session_duration && advise_on_time_base);
        chk_advise_remaining_time_on_distance_base.setEnabled(use_session_duration && advise_on_distance_base);
        chk_advise_finish_distance_on_distance_base.setEnabled(use_session_duration && advise_on_distance_base);

        chk_use_target_distance.setChecked(use_target_distance);
        spn_target_distance.setEnabled(use_target_distance);

        chk_advise_target_distance_fulfilment_on_time_base.setEnabled(use_session_duration && advise_on_time_base && use_target_distance);
        chk_advise_target_distance_fulfilment_on_distance_base.setEnabled(use_session_duration && advise_on_distance_base && use_target_distance);
        chk_advise_on_pace_change.setEnabled(use_session_duration && use_target_distance);

        chk_advise_target_distance_fulfilment_on_time_base.setChecked(advise_target_distance_fulfilment_on_time_base);
        chk_advise_target_distance_fulfilment_on_distance_base.setChecked(advise_target_distance_fulfilment_on_distance_base);
        chk_advise_on_pace_change.setChecked(advise_on_pace_change);

        update_calculated_pace();

    }


    private void save_preferences() {

        SharedPreferences.Editor shared_preferences_editor = shared_preferences.edit();

        shared_preferences_editor.putBoolean(getString(R.string.preferences__advise_on_time_base), advise_on_time_base);
        shared_preferences_editor.putBoolean(getString(R.string.preferences__advise_on_distance_base), advise_on_distance_base);
        shared_preferences_editor.putBoolean(getString(R.string.preferences__advise_time_on_distance_base), advise_time_on_distance_base);
        shared_preferences_editor.putBoolean(getString(R.string.preferences__advise_distance_on_time_base), advise_distance_on_time_base);
        shared_preferences_editor.putLong(getString(R.string.preferences__time_interval), time_interval);
        shared_preferences_editor.putFloat(getString(R.string.preferences__distance_interval), (float) distance_interval);
        shared_preferences_editor.putLong(getString(R.string.preferences__target_duration), session_duration);
        shared_preferences_editor.putBoolean(getString(R.string.preferences__advise_remaining_time_on_time_base), advise_remaining_time_on_time_base);
        shared_preferences_editor.putBoolean(getString(R.string.preferences__advise_finish_distance_on_time_base), advise_finish_distance_on_time_base);
        shared_preferences_editor.putBoolean(getString(R.string.preferences__advise_remaining_time_on_distance_base), advise_remaining_time_on_distance_base);
        shared_preferences_editor.putBoolean(getString(R.string.preferences__advise_finish_distance_on_distance_base), advise_finish_distance_on_distance_base);
        shared_preferences_editor.putBoolean(getString(R.string.preferences__use_session_duration), use_session_duration);
        shared_preferences_editor.putBoolean(getString(R.string.preferences__approximate_distances_to_quarters_of_km), approximate_distance_to_quarters_of_km);
        shared_preferences_editor.putBoolean(getString(R.string.preferences__use_target_distance), use_target_distance);
        shared_preferences_editor.putFloat(getString(R.string.preferences__target_distance), (float) target_distance);
        shared_preferences_editor.putBoolean(getString(R.string.preferences__advise_target_distance_fulfilment_on_time_base), advise_target_distance_fulfilment_on_time_base);
        shared_preferences_editor.putBoolean(getString(R.string.preferences__advise_target_distance_fulfilment_on_distance_base), advise_target_distance_fulfilment_on_distance_base);
        shared_preferences_editor.putBoolean(getString(R.string.preferences__advise_on_pace_change), advise_on_pace_change);

        shared_preferences_editor.commit();

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
        boolean default_approximate_distances = getResources().getBoolean(R.bool.pref_default__approximate_distance_to_quarters_of_km);
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

        time_interval = shared_preferences.getLong(
                getString(R.string.preferences__time_interval),
                default_time_interval);

        distance_interval = (double) shared_preferences.getFloat(
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

        approximate_distance_to_quarters_of_km = shared_preferences.getBoolean(
                getString(R.string.preferences__approximate_distances_to_quarters_of_km),
                default_approximate_distances
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

    }

    private void transmit_configuration() {

        Intent intent = new Intent("@VOCALPACER:::CONFIGURATION_UPDATE_REPLY");

        /*
        intent.putExtra("__advise_on_time_base", advise_on_time_base);
        intent.putExtra("__advise_on_distance_base", advise_on_distance_base);
        intent.putExtra("__advise_time_on_distance_base", advise_time_on_distance_base);
        intent.putExtra("__advise_distance_on_time_base", advise_distance_on_time_base);
        intent.putExtra("__time_interval", time_interval);
        intent.putExtra("__distance_interval", distance_interval);
        intent.putExtra("__target_duration", session_duration);
        intent.putExtra("__advise_remaining_time_on_time_base", advise_remaining_time_on_time_base);
        intent.putExtra("__advise_finish_distance_on_time_base", advise_finish_distance_on_time_base);
        intent.putExtra("__advise_remaining_time_on_distance_base", advise_remaining_time_on_distance_base);
        intent.putExtra("__advise_finish_distance_on_distance_base", advise_finish_distance_on_distance_base);
        intent.putExtra("__use_session_duration", use_session_duration);
        intent.putExtra("__approximate_distances", approximate_distance_to_quarters_of_km);
        intent.putExtra("__use_target_distance", use_target_distance);
        intent.putExtra("__target_distance", target_distance);
        intent.putExtra("__advise_target_distance_fulfilment_on_time_base", advise_target_distance_fulfilment_on_time_base);
        intent.putExtra("__advise_target_distance_fulfilment_on_distance_base", advise_target_distance_fulfilment_on_distance_base);
        intent.putExtra("__advise_on_pace_change", advise_on_pace_change);
        */

        LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(intent);

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        switch (parent.getId()) {

            case R.id.spn_time:
                time_interval = (long) (selectable_time_intervals[position]);
                break;

            case R.id.spn_distance:
                distance_interval = (float) (selectable_distance_intervals[position]);
                break;

            case R.id.spn___settings___expected_session_duration:
                session_duration = (long) (selectable_session_durations[position]);
                update_calculated_pace();
                break;

            case R.id.spn___settings___target_distance:
                target_distance = (float) (selectable_target_distances[position]);
                update_calculated_pace();
                break;
        }

        save_preferences();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private void update_calculated_pace(){

        TextView txv_calculated_pace = (TextView) findViewById(R.id.txv___settings___selected_pace);

        if(use_target_distance && use_session_duration){

            txv_calculated_pace.setText(String.format("pace: %1.1f min/km", (double) session_duration/target_distance*16.66666667));

        } else {

            txv_calculated_pace.setText(getString(R.string.main_label_blank));

        }


    }
}
