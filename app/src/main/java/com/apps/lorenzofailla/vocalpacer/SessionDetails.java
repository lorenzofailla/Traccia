package com.apps.lorenzofailla.vocalpacer;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SessionDetails extends AppCompatActivity implements View.OnClickListener {

    private long session_uid;

    ImageButton btn_delete_session;
    ImageButton btn_export_session;
    ImageButton btn_recalculate;

    FloatingActionButton fab_showinmap;

    private class create_gpx_file extends AsyncTask<Long, Void, String> {

        protected String doInBackground(Long... session_id) {

            File sd_extstorage = new File(Environment.getExternalStorageDirectory().toString()+"/vocalpacer");
            File output_file = new File(sd_extstorage, "session_"+session_id[0].toString()+".gpx");

            if (!sd_extstorage.exists()){

                sd_extstorage.mkdir();

            }

            try {

                FileWriter filewriter = new FileWriter(output_file, false);
                BufferedWriter bufferedwriter = new BufferedWriter(filewriter);

                bufferedwriter.write(compose_gpx_content());

                bufferedwriter.close();
                filewriter.close();

            } catch (IOException e) {

                e.printStackTrace();

            }

            return "session_"+session_id[0].toString()+".gpx";
        }

        protected void onPostExecute(String name_of_created_file) {

            // show an alert with a snackbar
            Snackbar.make(findViewById(R.id.btn___session_details___export_session), getString(R.string.session_details__session_exported)+ String.format("\n%s", name_of_created_file), Snackbar.LENGTH_LONG).show();

        }

    };

    private class recalculate_session extends AsyncTask<Long, Void, Void>  {

        protected Void doInBackground(Long... session_id){

            DBManager database_manager = new DBManager(getApplicationContext()).open();

            double distance = 0;

            double current_speed,
                    max_speed = -1;

            Location current_location = null,
                     previous_location = null;

            // inizializza un cursore con i dati della sessione
            Cursor session_samples = database_manager.get_session_samples(session_uid);

            if (session_samples!=null) {

                session_samples.moveToFirst();

                while (!session_samples.isAfterLast()) {

                    // registra la location attuale
                    if(current_location!=null){

                        previous_location = current_location;

                    }

                    current_location = new Location("VocalPacer");
                    current_location.setLatitude(session_samples.getDouble(session_samples.getColumnIndex(DBManager.KEY_TRACKSAMPLES_LATITUDE)));
                    current_location.setLongitude(session_samples.getDouble(session_samples.getColumnIndex(DBManager.KEY_TRACKSAMPLES_LONGITUDE)));
                    current_location.setAltitude(session_samples.getDouble(session_samples.getColumnIndex(DBManager.KEY_TRACKSAMPLES_ELEVATION)));
                    current_speed = session_samples.getDouble(session_samples.getColumnIndex(DBManager.KEY_TRACKSAMPLES_SPEED));
                    current_location.setSpeed((float) current_speed);

                    if (previous_location!=null){

                        distance += current_location.distanceTo(previous_location);

                    }

                    if (current_speed > max_speed){
                        // è stata registrata una velocità più alta di quella attuale

                        // la nuova velocità viene registrata come valore massimo.
                        max_speed = current_speed;

                    }

                    session_samples.moveToNext();

                }

                Log.i("@VOCALPACER", String.format("Distance calculation finished - result = %1.3f", distance/1000.0));
                Log.i("@VOCALPACER", String.format("Max. speed - result = %1.3f", max_speed));


            }



            return null;
        }

        protected void onPostExecute() {


        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_details);
    }

    @Override
    protected void onResume() {
        super.onResume();

        btn_delete_session=(ImageButton) findViewById(R.id.btn___session_details___delete_session);
        btn_export_session=(ImageButton) findViewById(R.id.btn___session_details___export_session);
        btn_recalculate=(ImageButton) findViewById(R.id.btn___session_details___recalculate);

        fab_showinmap=(FloatingActionButton) findViewById(R.id.fab___session_details___showinmap);

        btn_delete_session.setOnClickListener(this);
        btn_export_session.setOnClickListener(this);
        btn_recalculate.setOnClickListener(this);

        fab_showinmap.setOnClickListener(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            session_uid = extras.getLong("_session_uid");

            if (session_uid!=-1){
                retrieve_session(session_uid);

            }

        }

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private void retrieve_session(long uid){

        DBManager database_manager= new DBManager(this).open();

        Cursor session_data = database_manager.get_session(uid);

        TextView txv_session_date = (TextView) findViewById(R.id.txv___session_details___session_date);
        TextView txv_session_time = (TextView) findViewById(R.id.txv___session_details___session_time);
        TextView txv_session_distance = (TextView) findViewById(R.id.txv___session_details___session_distance);
        TextView txv_session_duration = (TextView) findViewById(R.id.txv___session_details___session_duration);
        TextView txv_n_of_samples = (TextView) findViewById(R.id.txv___session_details___number_of_samples);
        TextView txv_session_id = (TextView) findViewById(R.id.txv___session_details___session_id);

        txv_session_date.setText(session_data.getString(session_data.getColumnIndex(DBManager.KEY_SESSION_DATE)));
        txv_session_time.setText(session_data.getString(session_data.getColumnIndex(DBManager.KEY_SESSION_TIME)));
        txv_session_distance.setText(String.format("%1.2f", session_data.getDouble(session_data.getColumnIndex(database_manager.KEY_SESSION_DISTANCE)) / 1000));
        txv_session_duration.setText(SharedFunctions.format_time_string(session_data.getLong(session_data.getColumnIndex(database_manager.KEY_SESSION_DURATION))));
        txv_session_id.setText(getString(R.string.session_details__id_of_session) + String.format("%d", session_data.getInt(session_data.getColumnIndex(database_manager.KEY_SESSION_UID))));
        txv_n_of_samples.setText(getString(R.string.session_details__n_of_samples)+String.format("%d", database_manager.count_session_samples(session_uid)));

        database_manager.close();
    }

    @Override
    public void onClick(View v) {

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        DBManager database_manager= new DBManager(getApplicationContext()).open();

                        if (database_manager.delete_session(session_uid)) {
                            finish();
                        }

                        database_manager.close();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        switch (v.getId()){

            case R.id.btn___session_details___delete_session:

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.session_details__confirm_delete)
                        .setPositiveButton(R.string.yes, dialogClickListener)
                        .setNegativeButton(R.string.no, dialogClickListener)
                        .show();

                break;

            case R.id.btn___session_details___export_session:

                new create_gpx_file().execute(session_uid);

                break;

            case R.id.fab___session_details___showinmap:

                Intent show_in_map = new Intent(SessionDetails.this, ShowSessionInMap.class);
                show_in_map.putExtra("_session_uid", session_uid);
                startActivity(show_in_map);
                break;

            case R.id.btn___session_details___recalculate:

                new recalculate_session().execute(session_uid);

                break;
        }

    }

    private String compose_gpx_content(){

        DBManager database_manager= new DBManager(this).open();

        Cursor cursor = database_manager.get_session_samples(session_uid);
        String gpx_content = "";

        gpx_content += SharedFunctions.gpx_header+
                SharedFunctions.gpx_metadata+
                SharedFunctions.track_header+
                SharedFunctions.track_segment_header;

        if (cursor!=null) {
            cursor.moveToFirst();

            while (!cursor.isAfterLast()) {

                gpx_content += SharedFunctions.track_point(
                        cursor.getDouble(cursor.getColumnIndex(DBManager.KEY_TRACKSAMPLES_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(DBManager.KEY_TRACKSAMPLES_LONGITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(DBManager.KEY_TRACKSAMPLES_ELEVATION)),
                        cursor.getString(cursor.getColumnIndex(DBManager.KEY_TRACKSAMPLES_DATE)),
                        cursor.getString(cursor.getColumnIndex(DBManager.KEY_TRACKSAMPLES_TIME))
                );

                cursor.moveToNext();

            }

        }

        database_manager.close();

        gpx_content += SharedFunctions.track_segment_footer+
                SharedFunctions.track_footer+
                SharedFunctions.gpx_footer;

        return gpx_content;

    }

}
