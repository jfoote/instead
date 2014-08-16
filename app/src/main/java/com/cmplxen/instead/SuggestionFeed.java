package com.cmplxen.instead;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * This file contains the feed of suggestions.
 */
public class SuggestionFeed {
    /*
    The feed of suggestions that are displayed to the user. The feed is created by the suggestion
    service. Both the service and the the Lock Screen broadcast receiver hold references to it.
    The Lock Screen broadcast receiver reads suggestions from the feed. The suggestion service
    updates the feed based on defaults, system events such as the user seeing an suggestion, and
    so on.
    The core of the feed is a priority queue that ranks messages based on a score. See the
    Suggestion object for how the score is calculated.
     */
    
    private PriorityBlockingQueue<Suggestion> mQueue = null;
    private String mMessage = sDefaultMessage;
    private static final String sDefaultMessage = "Temporarily out of ideas -- meditate!";
    private SuggestionCategories mSuggestionCategories;
    private Service mService;
    private SuggestionCategory mCurrentCategory;
    private static final String sSuggestionPreferencesName = "com.cmplxen.instead.user.suggestions.state";

    public SuggestionFeed(Service service) throws XmlPullParserException, IOException {
        mService = service;
        // init queue

        XmlResourceParser xpp = mService.getResources().getXml(R.xml.suggestions);
        mSuggestionCategories =  new SuggestionCategories(xpp);
        xpp.close();

        SharedPreferences prefs = service.getSharedPreferences(sSuggestionPreferencesName,
                Context.MODE_PRIVATE);

        mQueue = new PriorityBlockingQueue<Suggestion>(8, new SuggestionComparator());
        for (SuggestionCategory c: mSuggestionCategories) {
            for (Suggestion s: c) {
                s.loadSeen(prefs);
                Log.d("SuggestionFeed::SuggestionFeed", "adding " + c.mName + ":" + s.mMessage +
                ":" + s.mSeenCount);
                s.prioritize(c);
                mQueue.add(s);
            }
        }
    }

    public Suggestion pop() {
        // called from broadcastreceiver
        String message;
        Suggestion s = mQueue.poll();
        if (s == null) {
            Log.d("SuggestionFeed", "queue is out of messages");
            s = new Suggestion();
        }
        return s;
    }


    // TODO: where should this logic live?
    public void handleSeen(Intent intent) {
        String category = intent.getStringExtra(SuggestionService.SUGGESTION_SEEN_CATEGORY);
        String message = intent.getStringExtra(SuggestionService.SUGGESTION_SEEN_MESSAGE);

        Log.d("SuggestionFeed::handleSeen", category + ":" + message);
        SuggestionCategory c = this.mSuggestionCategories.FindCategory(category);
        if (c == null) {
            Log.e("SuggestionFeed::handleSeen", "category not found: " + c);
            return;
        }
        Suggestion s = c.FindSuggestion(message);
        if (s == null) {
            Log.e("SuggestionFeed::handleSeen", "suggestion not found: " + s);
            return;
        }

        if (!s.isDefault()) {
            s.mSeenCount++;
            s.prioritize(this.mCurrentCategory);
            Log.d("handleSeen", message + " p:" + s.mPriority);
            s.saveSeen(mService.getSharedPreferences(sSuggestionPreferencesName,
                    Context.MODE_PRIVATE));
        } else {
            Log.w("SuggestionFeed::handleSeen", "Default suggestion was seen");
        }

        this.mQueue.add(s);
    }

    public void notifySeen(Suggestion suggestion) {
        // called from broadcastrecevier, so we have to pass this off
        // send 'suggestion seen intent' back to service so it can update the state2
        Context context = mService.getApplicationContext();
        Intent seenIntent = new Intent(context, SuggestionService.class);
        seenIntent.setAction(SuggestionService.SUGGESTION_SEEN_ACTION);
        seenIntent.putExtra(SuggestionService.SUGGESTION_SEEN_MESSAGE, suggestion.mMessage);
        seenIntent.putExtra(SuggestionService.SUGGESTION_SEEN_CATEGORY, suggestion.mCategory.mName);
        context.startService(seenIntent);
    }
}

