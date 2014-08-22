package com.cmplxen.instead;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class NetworkService extends IntentService {
    public static final String GET_PLACES_ACTION = "com.cmplxen.instead.intent.action.GET_PLACES_ACTION";
    public static final String GET_PLACES_LATITUDE = "com.cmplxen.instead.intent.action.GET_PLACES_LATITUDE";
    public static final String GET_PLACES_LONGITUDE = "com.cmplxen.instead.intent.action.GET_PLACES_LONGITUDE";

    public NetworkService() {
        super("NetworkService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Toast.makeText(getApplicationContext(), "NetSvc called", Toast.LENGTH_SHORT).show();
        if (intent != null) {
            final String action = intent.getAction();
            if (action.contentEquals(GET_PLACES_ACTION)) {
                Double locLat = intent.getDoubleExtra(GET_PLACES_LATITUDE, 0);
                Double locLong = intent.getDoubleExtra(GET_PLACES_LONGITUDE, 0);
                Toast.makeText(getApplicationContext(), "NetSvc:lat=" + locLat + ",long=" + locLong, Toast.LENGTH_SHORT).show();
                String place = getPlace(locLat, locLong);

                Context context = getApplicationContext();
                Intent seenIntent = new Intent(context, SuggestionService.class);
                seenIntent.setAction(SuggestionService.LOCATION_CHANGE_ACTION);
                seenIntent.putExtra(SuggestionService.LOCATION_CHANGE_PLACE, place);
                context.startService(seenIntent);
                // TODO: send place back to main thread


            }
        }
    }

    private String getPlace(double locLat, double locLong) {
        HttpResponse response = null;
        String result = "";
        try {
            /*
            HttpClient client = new DefaultHttpClient();
            HttpPost request = new HttpPost(new URI("https://maps.googleapis.com/maps/api/place/nearbysearch/json"));
            // See https://developers.google.com/places/documentation/search
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("key", "AIzaSyBuWXfg4Np1TAT5D_MyFdNA6v_JRLwhe1k"));
            nameValuePairs.add(new BasicNameValuePair("location", String.valueOf(locLat) + "," + String.valueOf(locLong)));
            nameValuePairs.add(new BasicNameValuePair("radius", "50")); // 50 meters?
            request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            //request.setURI(new URI("https://maps.googleapis.com/maps/api/place/nearbysearch/json?parameters"));
            response = client.execute(request);
            result = EntityUtils.toString(response.getEntity());
            */

            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https").authority("maps.googleapis.com").appendEncodedPath("maps/api/place/nearbysearch/json");
            builder.appendQueryParameter("key", "AIzaSyBuWXfg4Np1TAT5D_MyFdNA6v_JRLwhe1k");
            builder.appendQueryParameter("location", String.valueOf(locLat) + "," + String.valueOf(locLong));
            builder.appendQueryParameter("radius", "50");

            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet(builder.toString());
            Toast.makeText(getApplicationContext(), "NetSvc:uri=" + builder.toString(), Toast.LENGTH_LONG).show();
            // See https://developers.google.com/places/documentation/search

            //request.setURI(new URI("https://maps.googleapis.com/maps/api/place/nearbysearch/json?parameters"));
            response = client.execute(request);
            result = EntityUtils.toString(response.getEntity());
            //result = builder.toString();

            Log.d("SuggestionService::getPlace", response.toString());
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }
}
