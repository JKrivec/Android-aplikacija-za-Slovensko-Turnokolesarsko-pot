package si.uni_lj.fri.pbd.stkp;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class DrawingGpxPoints {
    private final String ktFileName = "KT.gpx";
    private Context context;
    private GoogleMap map;
    private String[] fileNamesToDraw;
    private Boolean drawInternal;
    private String downloadsPath;

    // Pass the google map in constructor
    public DrawingGpxPoints(GoogleMap map, String[] fileNamesToDraw, boolean drawInternal, Context context)  {
        this.map = map;
        this.fileNamesToDraw = fileNamesToDraw;
        this.context = context;
        this.drawInternal = drawInternal;
        this.downloadsPath = context.getFilesDir().getAbsolutePath() + File.separator + context.getResources().getString(R.string.download_directory);
    }


    private void drawKT() throws IOException, XmlPullParserException {
        // =========== Check if we should draw KT ===========
        boolean contains = false;
        for (int i = 0; i < this.fileNamesToDraw.length; i++) {
            if (this.fileNamesToDraw[i].equals(ktFileName)) {
                contains = true;
                break;
            }
        }
        if (!contains) {
            return;
        }
        // ===========/ Check if we should draw KT ===========

        // =========== Opening file, preparing parser ===========
        InputStream inputStream;
        if (drawInternal) {
            inputStream = new FileInputStream(downloadsPath + File.separator + ktFileName);
        } else {
            inputStream = context.getAssets().open(ktFileName);
        }
        // TODO: check if inputStream is empty
        // set up parser
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,false);
        xpp.setInput(inputStream,null);
        // ===========/ Opening file, preparing parser ===========

        LatLng position = null;
        String name = null;
        String elevation = null;
        int eventType = xpp.getEventType();
        // =========== Drawing in a loop ===========
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if(eventType == XmlPullParser.START_TAG ) {
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
            }
            // elevation is currently not used, maybe in the future
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

    private void addMarkerOnMap(final LatLng position, final String locationName) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                map.addMarker(new MarkerOptions()
                        .position(position)
                        .title(context.getResources().getString(R.string.control_point))
                        .snippet(locationName));
            }
        });

    }

    private void drawET() throws XmlPullParserException, IOException {
        int polyLineColor;
        // create new XmlPullParser
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,false);

        ArrayList<LatLng> points = new ArrayList<>();
        // =========== loop over all files needed to be drawn ===========
        for (int i = 0; i < this.fileNamesToDraw.length; i++) {
            // Draw only etape
            if (!this.fileNamesToDraw[i].equals(ktFileName)) {
                // open the .gpx file
                InputStream inputStream;
                if (drawInternal) {
                    inputStream = new FileInputStream(downloadsPath + File.separator + fileNamesToDraw[i]);
                } else {
                    inputStream = context.getAssets().open(fileNamesToDraw[i]);
                }

                Log.d("drawing", "drawET: " + fileNamesToDraw[i]);

                xpp.setInput(inputStream,null);

                int eventType = xpp.getEventType();
                // Surround with try-catch in case some of the .gpx files are corrupt
                try {
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if(eventType == XmlPullParser.START_TAG ) {
                            String tag = xpp.getName();
                            if (tag.equals("trkpt")) {
                                points.add(new LatLng(Double.parseDouble(xpp.getAttributeValue(null, "lat")), Double.parseDouble(xpp.getAttributeValue(null, "lon"))));
                                //drawLine(new LatLng(Double.parseDouble(xpp.getAttributeValue(null, "lat")), Double.parseDouble(xpp.getAttributeValue(null, "lon"))));
                            }
                        }
                        eventType = xpp.next();
                    }
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }

                // Color the trails with different colors to visually separate them
                if (i % 2 == 0) {
                    polyLineColor = Color.RED;
                } else {
                    polyLineColor = Color.MAGENTA;
                }

                // Create polyline options to later draw onto the map
                final PolylineOptions polyLineOptions =  new PolylineOptions();
                polyLineOptions.addAll(points);
                polyLineOptions.width(10);
                polyLineOptions.color(polyLineColor);

                // Add finish marker on single "courses" (When viewing a single "etapa")
                final LatLng endPoint = points.get(points.size() - 1);
                final BitmapDescriptor finishFlag;
                if (this.fileNamesToDraw.length == 1) {
                    // Create smaller bitmap than original
                    Bitmap b = BitmapFactory.decodeResource(context.getResources(), R.drawable.flag_checkered_solid);
                    Bitmap smallMarker = Bitmap.createScaledBitmap(b, b.getWidth()/2, b.getHeight()/2, false);
                    finishFlag = BitmapDescriptorFactory.fromBitmap(smallMarker);
                } else {
                    finishFlag = null;
                }
                // Clear the arrayList for each new "etapa"
                points.clear();

                // Finally draw on the main thread
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        map.addPolyline(polyLineOptions);
                        // add to the map as marker
                        if (finishFlag != null) {
                            map.addMarker(new MarkerOptions()
                                .position(endPoint)
                                .icon(finishFlag));
                        }
                    }
                });
                // TODO: Add on click listener to polyline and show its name
            }
        }
        // ===========/ loop over all files needed to be drawn ===========
    }

    // Run on a new, non UI thread
    public void draw() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    drawKT();
                    drawET();
                } catch (IOException | XmlPullParserException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}