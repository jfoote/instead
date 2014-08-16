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

    The Feed (running on the Service thread) sends data to the Lock Screen Broadcast Receiver
    (running on a system thread) via a threadsafe priority queue. The Lock Screen Broadcast
    Receiver sends data to the feed via an Intent that is handled by the Service thread.
     */

    private PriorityBlockingQueue<Suggestion> mQueue = null; // core queue that feeds suggestions
    private SuggestionCategories mSuggestionCategories; // all suggestions by category
    private Service mService; // reference to the suggestion service
    private SuggestionCategory mCurrentCategory; // the current category of suggestions
    private static final String sSuggestionPreferencesName = "com.cmplxen.instead.user.suggestions.state";

    public SuggestionFeed(Service service) throws XmlPullParserException, IOException {
        /*
        Initializes the queue; needs a reference to the suggestion service to load resources, save
        feed state, etc.
         */
        mService = service;

        // Load the suggestions into memory from app resources
        XmlResourceParser xpp = mService.getResources().getXml(R.xml.suggestions);
        mSuggestionCategories =  new SuggestionCategories(xpp);
        xpp.close();

        // Load the suggestions into the priority queue, factoring in any available
        // serialized user data (how many times a suggestion has been seen) along the way
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
        /*
        Pops the next suggestion from the priority queue.
        WARNING: This is generally called from a Broadcast Receiver (on a separate thread) so
        this implementation must be thread safe (though not necessarily re-entrant).
         */
        Suggestion s = mQueue.poll();
        if (s == null) {
            Log.d("SuggestionFeed", "queue is out of messages");
            s = new Suggestion();
        }
        return s;
    }


    // TODO: where should this logic live?
    public void handleSeen(Intent intent) {
        /*
        Handles a "suggestion seen" intent sent from the lock screen Broadcast Receiver, which
        the Receiver sends by calling SuggestionFeed::notifySeen.
        Updates the feed by increasing the "seen" counter for the suggestion, re-calculates
        the suggestion priority, then puts the suggestion back in the queue.
        NOTE: This method is (currently) called from the suggestion service.
        NOTE: If I get around to fixing Suggestion serialization this might become simpler.
         */

        // Get the suggestion data from the intent
        String category = intent.getStringExtra(SuggestionService.SUGGESTION_SEEN_CATEGORY);
        String message = intent.getStringExtra(SuggestionService.SUGGESTION_SEEN_MESSAGE);

        // Look up the suggestion from the intent data
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

        // Update the suggestion data and put it back in the queue
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
        /*
        Marks a suggestion as "seen". This method is called by the Lock Screen Broadcast Receiver
        directly. The Broadcast Receiver can't (or shouldn't, rather) update the suggestion metadata
        directly as it is running on a separate thread. So, this method inserts an intent that
        tells the feed to update the suggestion info, etc. (see handleSeen above) on the Service
        thread.
         */
        Context context = mService.getApplicationContext();
        Intent seenIntent = new Intent(context, SuggestionService.class);
        seenIntent.setAction(SuggestionService.SUGGESTION_SEEN_ACTION);
        seenIntent.putExtra(SuggestionService.SUGGESTION_SEEN_MESSAGE, suggestion.mMessage);
        seenIntent.putExtra(SuggestionService.SUGGESTION_SEEN_CATEGORY, suggestion.mCategory.mName);
        context.startService(seenIntent);
    }
}

