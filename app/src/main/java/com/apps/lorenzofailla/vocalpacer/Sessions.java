package com.apps.lorenzofailla.vocalpacer;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Sessions extends AppCompatActivity
        implements View.OnClickListener,
        AdapterView.OnItemClickListener {

    ListView lvw_sessions;
    DBManager database_manager;
    CursorAdapter cursor_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sessions);
    }

    protected void onResume(){
        super.onResume();

        // initialize the database manager
        database_manager = new DBManager(this).open();

        // update the main info on the top of the page
        update_main_info();

        // initialize the handler to the drawable objects in the activity
        lvw_sessions = (ListView) findViewById(R.id.lst_sessions);

        Cursor database_cursor = database_manager.get_all_activities();

        cursor_adapter = new CursorAdapter(this, database_cursor, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER)  {

            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {

                View v=getLayoutInflater().inflate(R.layout.session_row, null);
                return v;

            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {

                TextView txv_session_date;
                TextView txv_session_time ;
                TextView txv_session_distance;
                TextView txv_session_duration;

                txv_session_date = (TextView) view.findViewById(R.id.txv__session_row__session_date);
                txv_session_time = (TextView) view.findViewById(R.id.txv__session_row__session_time);
                txv_session_distance = (TextView) view.findViewById(R.id.txv__session_row__session_distance);
                txv_session_duration = (TextView) view.findViewById(R.id.txv__session_row__session_duration);

                txv_session_date.setText(cursor.getString(cursor.getColumnIndex(database_manager.KEY_SESSION_DATE)));
                txv_session_time.setText(cursor.getString(cursor.getColumnIndex(database_manager.KEY_SESSION_TIME)));
                txv_session_distance.setText(String.format("%1.2f", cursor.getDouble(cursor.getColumnIndex(database_manager.KEY_SESSION_DISTANCE))/1000 ));
                txv_session_duration.setText(SharedFunctions.format_time_string(cursor.getLong(cursor.getColumnIndex(database_manager.KEY_SESSION_DURATION))));

            }
        };

        lvw_sessions.setAdapter(cursor_adapter);
        lvw_sessions.setOnItemClickListener(this);

    }

    protected void onPause() {
        super.onPause();

        database_manager.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_sessions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        switch (parent.getId()){
            case R.id.lst_sessions:

                long u_id=cursor_adapter.getItemId(position);
                Intent intent=new Intent(Sessions.this, SessionDetails.class);
                intent.putExtra("_session_uid", u_id);
                startActivity(intent);

                break;

        }

    }

    private void update_main_info(){

        TextView txv_nofsessions = (TextView) findViewById(R.id.txv___sessions___nofsessions_value);
        TextView txv_totaltime = (TextView) findViewById(R.id.txv___sessions___totaltime_value);
        TextView txv_totaldistance = (TextView) findViewById(R.id.txv___sessions___totaldistance_value);

        txv_nofsessions.setText(""+ database_manager.count_sessions());
        txv_totaltime.setText(SharedFunctions.format_time_string(database_manager.count_total_duration()));
        txv_totaldistance.setText(String.format("%1.2f", database_manager.count_total_distance()/1000 ));
    }

}
