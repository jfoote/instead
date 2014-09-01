package com.cmplxen.instead;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import android.util.Log;

// TODO: delete below if widget test doesn't work out
import android.view.View;
import android.view.WindowManager;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.content.Context;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/*
This file contains the core component of the application -- the suggestion service .
 */

// very roughly implemented place lookup -- need to parse json and display it somehow
// TODO:
//    - re-work design so that NetworkService is not an IntentService
//        - probably best to re-work location to use less power/poll as a starting point
//    - match Places types to suggestion categories
//    - add more suggestions
//    - make display pretty
//        - made font bigger; need to make a widget as well
//    - test; re-design for test (and knowledge of threats)


public class SuggestionService extends Service {
    /*
    The service that manages suggestions for the application. This service is created when
    suggestions are enabled by the user.

    ****
    Design note: why this is a Service instead of an IntentService

    To support the goal of having a suggestion appear instantaneously on the lock screen in the face
    of many system events, I am using a threadsafe queue to pass suggestions from a Service (which
    manages the feed of suggestions) to a BroadcastReceiver (which detects when the screen is locked,
    etc.). For this reason the BroadcastReceiver must maintain a reference to the queue, and thus
    it must stay in memory. This is achieved by having the Service stay in memory while holding a
    reference to the BroadcastReceiver.

    One alternative may be to serialize suggestions to a SharedPreferences object and use that
    for communication from the Service to the BroadcastReceiver. Potential downsides to this include
    slowdown (I believe a SharedPreferences object wraps a file) and race conditions.
    */

    private LockScreenReceiver mLockScreenReceiver;
    private SuggestionFeed mFeed;
    public static final String INITIALIZE_ACTION = "com.cmplxen.instead.intent.action.INITIALIZE_ACTION";

    public static final String SUGGESTION_SEEN_ACTION = "com.cmplxen.instead.intent.action.SUGGESTION_SEEN_ACTION";
    public static final String SUGGESTION_SEEN_MESSAGE = "com.cmplxen.instead.intent.action.SUGGESTION_SEEN_MESSAGE";
    public static final String SUGGESTION_SEEN_CATEGORY = "com.cmplxen.instead.intent.action.SUGGESTION_SEEN_CATEGORY";

    public static final String LOCATION_CHANGE_ACTION = "com.cmplxen.instead.intent.action.LOCATION_CHANGE_ACTION";
    public static final String LOCATION_CHANGE_PLACE = "com.cmplxen.instead.intent.action.LOCATION_CHANGE_PLACE";

    public SuggestionService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onStart(Intent intent, int startId) {
        /*
        This method handles intents for the service, including intents to start the service (enable
        suggestions) and update the suggestion feed based on which suggestions have been
        seen.
         */
        String action = intent.getAction();

        // If the service is created without any params, log it (this is probably a mistake).
        if (action == null) {
            Log.w("SuggestionService::onStart", "(null)");
        }
        Log.d("SuggestionService::onStart", action);
        Toast.makeText(getApplicationContext(), "SugSvc::action=" + action, Toast.LENGTH_SHORT).show();

        // Branch on the intent action
        if (action == INITIALIZE_ACTION){
            // This is passed when the service is created: enable suggestions

            Log.d("SuggestionService", "started");

            // Create the feed of suggestions. It will take care of loading any saved user
            // data into itself.
            try {
                mFeed = new SuggestionFeed(this);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Create a BroadcastReceiver to handle displaying messages to the lock screen;
            // pass it the suggestion feed to pull messages from
            mLockScreenReceiver = new LockScreenReceiver(this, mFeed);
            mLockScreenReceiver.register();

            // TODO: break this out into a separate option
            LocationMonitor.start(getApplicationContext()); // TODO: handle return value or switch to try/catch

        } else if (action == SuggestionService.SUGGESTION_SEEN_ACTION) {
            // This is passed from the screen lock broadcast receiver when the user
            // sees a message; update the queue

            mFeed.handleSeen(intent);
        } else if (action == SuggestionService.LOCATION_CHANGE_ACTION) {
            String place = intent.getStringExtra(SuggestionService.LOCATION_CHANGE_PLACE);
            Toast.makeText(getApplicationContext(), "SugSvc:place=" + place, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroy() {
        Log.d("suggestion_service", "destroyed");
        mLockScreenReceiver.unregister();
        LocationMonitor.stop(getApplicationContext());
        mLockScreenReceiver = null;
        mFeed = null;
    }
}



