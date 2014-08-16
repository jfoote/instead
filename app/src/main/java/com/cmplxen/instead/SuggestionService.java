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

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/*
This file contains the core component of the application -- the suggestion service .
 */

// TODO:
//    - move on: add more lists and attach to location update events
//        - Location change:Do a places search and cache the result; factor into next suggestion
//

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

        } else if (action == SuggestionService.SUGGESTION_SEEN_ACTION) {
            // This is passed from the screen lock broadcast receiver when the user
            // sees a message; update the queue

            mFeed.handleSeen(intent);
        }

    }

    @Override
    public void onDestroy() {
        Log.d("suggestion_service", "destroyed");
        mLockScreenReceiver.unregister();
        mLockScreenReceiver = null;
        mFeed = null;
    }
}



