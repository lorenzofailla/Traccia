package com.apps.lorenzofailla.vocalpacer;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class ShowSessionInMap extends FragmentActivity implements OnMapReadyCallback {

    private DBManager database_manager;
    private long session_uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_show_session_in_map);

    }

    @Override
    protected void onResume() {
        super.onResume();

        Toolbar toolbar = (Toolbar) findViewById(R.id.tbr___showsessioninmap___toolbar);
        toolbar.setTitle(R.string.title_activity_show_session_in_map);
        toolbar.setTitleTextColor(Color.BLACK);
        toolbar.setNavigationIcon(android.R.drawable.ic_dialog_map);
        //toolbar.setLogo(android.R.drawable.ic_dialog_map);

        database_manager = new DBManager(this).open();

        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {

            session_uid = extras.getLong("_session_uid");

        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        database_manager.close();

    }

    @Override
    public void onMapReady(GoogleMap map) {

        PolylineOptions session_polyline = new PolylineOptions();
        MarkerOptions start_point = new MarkerOptions();
        MarkerOptions end_point = new MarkerOptions();
        LatLngBounds.Builder session_bounds = new LatLngBounds.Builder();

        Cursor cursor = database_manager.get_session_samples(session_uid);
        Cursor session_data = database_manager.get_session(session_uid);

        if (cursor != null) {

            cursor.moveToFirst();

            start_point
                    .position(
                            new LatLng(
                                    cursor.getDouble(cursor.getColumnIndex(DBManager.KEY_TRACKSAMPLES_LATITUDE)),
                                    cursor.getDouble(cursor.getColumnIndex(DBManager.KEY_TRACKSAMPLES_LONGITUDE))
                            )
                    )
                    .title(String.format(
                            "%1.1f km - %s", session_data.getDouble(session_data.getColumnIndex(DBManager.KEY_SESSION_DISTANCE)) / 1000,
                            SharedFunctions.format_time_string(session_data.getLong(session_data.getColumnIndex(DBManager.KEY_SESSION_DURATION)))
                    ))
            ;

            while (!cursor.isAfterLast()) {

                session_polyline.add(
                        new LatLng(

                                cursor.getDouble(cursor.getColumnIndex(DBManager.KEY_TRACKSAMPLES_LATITUDE)),
                                cursor.getDouble(cursor.getColumnIndex(DBManager.KEY_TRACKSAMPLES_LONGITUDE))
                        )
                );

                session_bounds.include(new LatLng(

                        cursor.getDouble(cursor.getColumnIndex(DBManager.KEY_TRACKSAMPLES_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(DBManager.KEY_TRACKSAMPLES_LONGITUDE))
                ));

                cursor.moveToNext();

            }

            cursor.moveToLast();

            end_point.position(
                    new LatLng(
                            cursor.getDouble(cursor.getColumnIndex(DBManager.KEY_TRACKSAMPLES_LATITUDE)),
                            cursor.getDouble(cursor.getColumnIndex(DBManager.KEY_TRACKSAMPLES_LONGITUDE))
                    )
            );
            end_point.title("Fine");

            map.addMarker(start_point);
            map.addMarker(end_point);
            map.addPolyline(session_polyline);

            map.animateCamera(CameraUpdateFactory.newLatLngBounds(session_bounds.build(), 100));
        }

    }

}


