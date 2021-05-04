package si.uni_lj.fri.pbd.stkp;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.google.android.gms.common.util.IOUtils;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.channels.AsynchronousFileChannel;
import java.util.ArrayList;

public class DrawingGpxPoints implements  Runnable {
    private final String KTfileName = "KT.gpx";
    private Context context;
    private GoogleMap map;
    private String[] fileNamesToDraw;
    private Boolean DrawKT;

    // Pass the google map in constructor
    public DrawingGpxPoints(GoogleMap map, String[] fileNamesToDraw, Context context)  {
        this.map = map;
        this.fileNamesToDraw = fileNamesToDraw;
        this.context = context;
    }


    public void drawKT() throws IOException, XmlPullParserException {
        // =========== Check if we should draw KT ===========
        boolean contains = false;
        for (int i = 0; i < this.fileNamesToDraw.length; i++) {
            if (this.fileNamesToDraw[i].equals(this.KTfileName)) {
                contains = true;
                break;
            }
        }

        if (!contains) {
            Toast.makeText(this.context, "KT file not in arr", Toast.LENGTH_LONG).show();
            return;
        }
        // ===========/ Check if we should draw KT ===========

        // =========== Opening file, preparing parser ===========
        // try to open the file
        InputStream inputStream = context.getAssets().open(this.KTfileName);
        // TODO: check if inputStream is empty
        // set up parser
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,false);
        xpp.setInput(inputStream,null);
        // ===========/ Opening file, preparing parser ===========

        //
        LatLng position = null;
        String name = null;
        String elevation = null;
        int eventType = xpp.getEventType();
        //
        // ===========/ Drawing in a loop ===========
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if(eventType == XmlPullParser.START_DOCUMENT) {
                //Log.d("gpx","Start document");
            } else if(eventType == XmlPullParser.START_TAG ) {
                String tag = xpp.getName();
                if (tag.equals("wpt")) {
                    position = new LatLng(Double.parseDouble(xpp.getAttributeValue(null, "lat")), Double.parseDouble(xpp.getAttributeValue(null, "lon")));
                } else if (tag.equals("ele")) {
                    xpp.next();
                    elevation = xpp.getText();
                } else if ( tag.equals("name")) {
                    xpp.next();
                    name = xpp.getText();
                }

            } else if(eventType == XmlPullParser.END_TAG) {
                //Log.d("gpx","End tag "+xpp.getName());
            } else if(eventType == XmlPullParser.TEXT) {
                //Log.d("gpx","Text "+xpp.getText());
            }

            //Log.d("gpx", "position: " + position + ", name: " + name + ", elevation: " + elevation);

            if (position != null && name != null && elevation != null) {
                addMarkerOnMap(position, name);
                position = null;
                name = null;
                elevation = null;
            }
            eventType = xpp.next();
        }
        // ===========/ Drawing in a loop ===========
    }

    private void addMarkerOnMap(LatLng position, String locationName) {
        Log.d("gpx", position + " name -> " + locationName);
        map.addMarker(new MarkerOptions()
                .position(position)
                .title(locationName));

    }

    public void drawET() throws XmlPullParserException, IOException {
        // create
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,false);

        ArrayList<LatLng> points = new ArrayList<LatLng>();
        PolylineOptions polyLineOptions =  new PolylineOptions();

        // =========== loop over all files needed to be drawn ===========
        for (int i = 0; i < this.fileNamesToDraw.length; i++) {
            // Draw only etape
            if (!this.fileNamesToDraw[i].equals(this.KTfileName)) {
                // open file
                InputStream inputStream = context.getAssets().open(this.fileNamesToDraw[i]);
                xpp.setInput(inputStream,null);

                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if(eventType == XmlPullParser.START_DOCUMENT) {
                        //Log.d("gpx","Start document");
                    } else if(eventType == XmlPullParser.START_TAG ) {
                        String tag = xpp.getName();
                        if (tag.equals("trkpt")) {
                            points.add(new LatLng(Double.parseDouble(xpp.getAttributeValue(null, "lat")), Double.parseDouble(xpp.getAttributeValue(null, "lon"))));
                            //drawLine(new LatLng(Double.parseDouble(xpp.getAttributeValue(null, "lat")), Double.parseDouble(xpp.getAttributeValue(null, "lon"))));
                        }

                    } else if(eventType == XmlPullParser.END_TAG) {
                        //Log.d("gpx","End tag "+xpp.getName());
                    } else if(eventType == XmlPullParser.TEXT) {
                        //Log.d("gpx","Text "+xpp.getText());
                    }


                    eventType = xpp.next();
                }

                polyLineOptions.addAll(points);
                polyLineOptions.width(6);
                polyLineOptions.color(Color.RED);
                map.addPolyline(polyLineOptions);

                // Add finish marker on single "courses"
                // TODO: change to len == 1
                if (this.fileNamesToDraw.length != 1) {
                    // Create smaller bitmap than original
                    Bitmap b = BitmapFactory.decodeResource(context.getResources(), R.drawable.finish_flag);
                    Bitmap smallMarker = Bitmap.createScaledBitmap(b, b.getWidth()/2, b.getHeight()/2, false);
                    BitmapDescriptor smallMarkerIcon = BitmapDescriptorFactory.fromBitmap(smallMarker);
                    // add to the map as marker
                    map.addMarker(new MarkerOptions()
                    .position(points.get(points.size() - 1))
                    .icon(smallMarkerIcon));
                }

                points.clear();

            }
        }
        // ===========/ loop over all files needed to be drawn ===========
    }



    @Override
    public void run() {
        try {
            this.drawKT();
            this.drawET();
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }
}
