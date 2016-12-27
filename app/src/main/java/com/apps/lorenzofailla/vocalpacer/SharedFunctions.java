package com.apps.lorenzofailla.vocalpacer;

import java.util.Calendar;

/**
 * Created by 105053228 on 25/mar/2016.
 */
public final class SharedFunctions {

    public final static String gpx_header = "" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n" +
            "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\" creator=\"OruxMaps v.6.5.0\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n";

    public final static String gpx_footer = "</gpx>";

    public final static String gpx_metadata = "" +
            "<metadata>\n" +
            "<name>Pacer Vocale</name>\n" +
            "<desc>-</desc>\n" +
            "<link href=\"\">\n" +
            "<text>-</text>\n" +
            "</link>\n" +
            "</metadata>\n";

    public final static String track_header = "" +
            "<trk>\n" +
            "<name>Pacer Vocale</name>\n" +
            "<desc>-</desc>\n";

    public final static String track_footer = "</trk>\n";

    public final static String track_segment_header = "<trkseg>\n";

    public final static String track_segment_footer = "</trkseg>\n";

    public final static String track_point(double lat, double lon, double ele, String dateT, String timeZ){

        return String.format(
                "<trkpt lat=\"%1.7f\" lon=\"%1.7f\">\n" +
                "<ele>%1.2f</ele>\n" +
                "<time>%sT%sZ</time>\n" +
                "</trkpt>\n", lat, lon, ele, dateT, timeZ).replace(',','.');

    }

    public final static String format_time_string(long time_ms) {

        String header;
        String footer;

        if (time_ms < 0) {

            time_ms = time_ms * -1;
            header = "[";
            footer = "]";

        } else {

            header = "";
            footer = "";

        }

        int elapsed_seconds = (int) (time_ms / 1000);
        int elapsed_minutes = elapsed_seconds / 60;
        int elapsed_hours = elapsed_seconds / 3600;

        // perform modular division by 60 of seconds and minutes
        elapsed_seconds = elapsed_seconds % 60;
        elapsed_minutes = elapsed_minutes % 60;

        return String.format(header + "%02d:%02d:%02d" + footer, elapsed_hours, elapsed_minutes, elapsed_seconds);

    }

    public final static String get_current_date_YYYY_MM_DD() {

        Calendar now = Calendar.getInstance();
        return String.format("%04d-%02d-%02d", now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

    }

    public final static String get_current_time_HH_mm_SS() {

        Calendar now = Calendar.getInstance();
        return String.format("%02d:%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND));

    }
}
