package com.cmplxen.instead;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class LockScreenReceiver extends BroadcastReceiver {
    private SuggestionView mSuggestionView;
    private SuggestionService mSuggestionService;
    private SuggestionFeed mFeed;


    public LockScreenReceiver(SuggestionService service, SuggestionFeed feed) {
        mSuggestionService = service;
        mFeed = feed;
    }

    // For orthogonality: GC means I can't count on destructor being call synchronously (i.e.
    // responsively when user disables Instead), so I have to call register/unregister
    // explicitly from Service.
    public void register() {
        // Create a view and params(displays the suggestion)
        mSuggestionView = new SuggestionView(mSuggestionService);

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_PRESENT");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.SCREEN_ON");
        mSuggestionService.registerReceiver(this, filter);
    }

    public void unregister() {
        mSuggestionService.unregisterReceiver(this);
        mSuggestionView = null;
    }

    private Suggestion mDisplayedSuggestion;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("LockScreenReceiver", action);

        if (action.equals(Intent.ACTION_USER_PRESENT)) {
            mSuggestionView.hide();
            mFeed.notifySeen(mDisplayedSuggestion);

        //} else if (action.equals(Intent.ACTION_SCREEN_ON)) { //TODO: handle click-on/click-off
        // ACTION_SCREEN_OFF is more responsive (if it always works)
        } else if (action.equals(Intent.ACTION_SCREEN_OFF)) { // assuming this is the only way screen gets locked
            // ASSERT(mFeed returns something)
            mDisplayedSuggestion = mFeed.pop();
            mSuggestionView.show(mDisplayedSuggestion.mMessage);
            Log.d("LockScreenReceiver::onReceive", mDisplayedSuggestion.mMessage + "p:" + mDisplayedSuggestion.mPriority);
        } // else if (action.equals(ACTION_UPDATE_SUGGESTION ) {

    }
}
