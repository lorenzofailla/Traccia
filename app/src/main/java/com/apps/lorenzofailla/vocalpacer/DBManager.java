package com.apps.lorenzofailla.vocalpacer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by 105053228 on 25/feb/2016.
 */

public class DBManager {

    static final String KEY_SESSION_UID = "_id";
    static final String KEY_SESSION_DATE = "session_date_start";
    static final String KEY_SESSION_TIME = "session_time_start";
    static final String KEY_SESSION_DISTANCE = "session_distance";
    static final String KEY_SESSION_DURATION = "session_duration";

    static final String TAG = "Traccia_DBManager";
    static final String DB_NAME = "Traccia_DB";
    static final String DB_TABLE_SESSIONS_NAME = "sessions";
    static final int DATABASE_VERSIONE = 1;

    static final String DB_TABLE_TRACKSAMPLES_NAME ="tracksamples";
    static final String KEY_TRACKSAMPLES_UID ="_id";
    static final String KEY_TRACKSAMPLES_SESSION_ID ="session";
    static final String KEY_TRACKSAMPLES_DATE="date";
    static final String KEY_TRACKSAMPLES_TIME="time";
    static final String KEY_TRACKSAMPLES_LATITUDE="latitude";
    static final String KEY_TRACKSAMPLES_LONGITUDE="longitude";
    static final String KEY_TRACKSAMPLES_SPEED="speed";
    static final String KEY_TRACKSAMPLES_ELEVATION="elevation";

    // this string contains the SQL statement to create the databse
    static final String SESSIONS_DB_CREATION = "CREATE TABLE " + DB_TABLE_SESSIONS_NAME + " (" +
            KEY_SESSION_UID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            KEY_SESSION_DATE + " TEXT, " +
            KEY_SESSION_TIME + " TEXT, " +
            KEY_SESSION_DISTANCE + " NUMERIC, " +
            KEY_SESSION_DURATION + " NUMERIC " +
            ");";

    static final String TRACKSAMPLES_DB_CREATION = "CREATE TABLE " + DB_TABLE_TRACKSAMPLES_NAME + " (" +
            KEY_TRACKSAMPLES_UID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            KEY_TRACKSAMPLES_SESSION_ID + " INTEGER, " +
            KEY_TRACKSAMPLES_DATE + " TEXT, " +
            KEY_TRACKSAMPLES_TIME + " TEXT, " +
            KEY_TRACKSAMPLES_LATITUDE + " NUMERIC, " +
            KEY_TRACKSAMPLES_LONGITUDE + " NUMERIC, " +
            KEY_TRACKSAMPLES_SPEED + " NUMERIC, " +
            KEY_TRACKSAMPLES_ELEVATION + " NUMERIC" +
            ");";

    static final String DB_UPDATE = "";

    final Context context;
    DatabaseHelper DBHelper;

    SQLiteDatabase db;

    // constructor of the class
    public DBManager(Context context)
    {
        this.context = context;
        DBHelper = new DatabaseHelper(context);
    }

    // class  SQLiteOpenHelper is extended to manage connections with database
        private static class DatabaseHelper extends SQLiteOpenHelper
    {
        DatabaseHelper(Context context)
        {
            // invoco il costruttore della classe base
            super(context, DB_NAME, null, DATABASE_VERSIONE);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            try {
                db.execSQL(SESSIONS_DB_CREATION);
                db.execSQL(TRACKSAMPLES_DB_CREATION);
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            try {
                db.execSQL(DB_UPDATE);
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // open the connection to the database
    final public DBManager open() throws SQLException
    {
        // ottengo accesso al DB anche in scrittura
        db = DBHelper.getWritableDatabase();
        return this;
    }

    //
    final public void close()
    {
        // chiudo la connessione al DB
        DBHelper.close();
    }

    final public Cursor get_all_activities()
    {
        // applico il metodo query senza applicare nessuna clausola WHERE
        return db.query(DB_TABLE_SESSIONS_NAME, new String[] {"rowid _id", KEY_SESSION_DATE, KEY_SESSION_TIME, KEY_SESSION_DISTANCE, KEY_SESSION_DURATION}, null, null, null, null, null);
    }


    public final Cursor get_session(long session_id)
    {
        // apply the query method
        Cursor cursor = db.query(true, DB_TABLE_SESSIONS_NAME, new String[] {"rowid _id", KEY_SESSION_DATE, KEY_SESSION_TIME, KEY_SESSION_DISTANCE, KEY_SESSION_DURATION}, KEY_SESSION_UID + "=" + session_id, null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    // insert a record in the 'activities' table
    final public long insert_session(String activity_date_start, String activity_time_start)
    {
        // create a ContentValues object for the new record
        ContentValues initialValues = new ContentValues();

        initialValues.put(KEY_SESSION_DATE, activity_date_start);
        initialValues.put(KEY_SESSION_TIME, activity_time_start);

        // applico il metodo insert
        return db.insert(DB_TABLE_SESSIONS_NAME, null, initialValues);

    }

    final public long insert_sample(long session_id,
                                    String sample_date,
                                    String sample_time,
                                    double sample_latitude,
                                    double sample_longitude,
                                    double sample_speed,
                                    double sample_elevation
                                    ){

        // ContentValues object is created for the new record
        ContentValues record_value = new ContentValues();

        record_value.put(KEY_TRACKSAMPLES_SESSION_ID, session_id);
        record_value.put(KEY_TRACKSAMPLES_DATE, sample_date);
        record_value.put(KEY_TRACKSAMPLES_TIME, sample_time);
        record_value.put(KEY_TRACKSAMPLES_LATITUDE, sample_latitude);
        record_value.put(KEY_TRACKSAMPLES_LONGITUDE, sample_longitude);
        record_value.put(KEY_TRACKSAMPLES_SPEED, sample_speed);
        record_value.put(KEY_TRACKSAMPLES_ELEVATION, sample_elevation);

        return db.insert(DB_TABLE_TRACKSAMPLES_NAME, null, record_value);

    }

    final int count_session_samples(long session_uid){

        Cursor cursor= db.query(true, DB_TABLE_TRACKSAMPLES_NAME, new String[]{"rowid _id"}, KEY_TRACKSAMPLES_SESSION_ID + "=" + session_uid, null, null, null, null, null);
        return cursor.getCount();

    }

    final Cursor get_session_samples(long session_uid){

        return db.query(true, DB_TABLE_TRACKSAMPLES_NAME, new String[]{"rowid _id", KEY_TRACKSAMPLES_LATITUDE, KEY_TRACKSAMPLES_LONGITUDE, KEY_TRACKSAMPLES_ELEVATION, KEY_TRACKSAMPLES_DATE, KEY_TRACKSAMPLES_TIME, KEY_TRACKSAMPLES_SPEED}, KEY_TRACKSAMPLES_SESSION_ID + "=" + session_uid, null, null, null, null, null);

    }

    final public boolean delete_session(long session_uid)
    {

        int deleted_samples=db.delete(DB_TABLE_TRACKSAMPLES_NAME, KEY_TRACKSAMPLES_SESSION_ID+"="+session_uid, null);
        int deleted_sessions=db.delete(DB_TABLE_SESSIONS_NAME, KEY_SESSION_UID + "=" + session_uid, null);

        return (deleted_samples+deleted_sessions) > 0;
    }


    final public boolean update_session(long session_id, long duration, double distance)
    {
        // creo una mappa di valori
        ContentValues args = new ContentValues();
        args.put(KEY_SESSION_DURATION, duration);
        args.put(KEY_SESSION_DISTANCE, distance);

        return db.update(DB_TABLE_SESSIONS_NAME, args, KEY_SESSION_UID + "=" + session_id, null) > 0;
    }

    final public int count_sessions(){
        Cursor cursor= db.query(DB_TABLE_SESSIONS_NAME, new String[] {"rowid _id"}, null, null,null, null, null);
        return cursor.getCount();

    }

    final public long count_total_duration(){

        long result=0;

        Cursor cursor= db.query(DB_TABLE_SESSIONS_NAME, new String[] {"rowid _id", KEY_SESSION_DURATION}, null, null,null, null, null);

        if (cursor!=null){
            cursor.moveToFirst();

            while (!cursor.isAfterLast()){
                result+=cursor.getLong(cursor.getColumnIndex(KEY_SESSION_DURATION));
                cursor.moveToNext();
            }
        }


        return result;
    }

    final public double count_total_distance(){

        double result=0;

        Cursor cursor= db.query(DB_TABLE_SESSIONS_NAME, new String[] {"rowid _id", KEY_SESSION_DISTANCE}, null, null, null, null, null);

        if (cursor!=null){
            cursor.moveToFirst();

            while (!cursor.isAfterLast()){
                result+=cursor.getDouble(cursor.getColumnIndex(KEY_SESSION_DISTANCE));
                cursor.moveToNext();
            }
        }

        return result;
    }

}

