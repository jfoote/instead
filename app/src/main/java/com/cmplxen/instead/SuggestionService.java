package com.cmplxen.instead;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import android.util.Log;
import android.widget.Toast; // TODO: used for printf-style debugging, remove later
import android.provider.Settings;

// TODO: delete below if widget test doesn't work out
import android.view.View;
import android.view.WindowManager;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.content.Context;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/*
Suggestion selection goal: Deliver usable selections
- Don't repeat suggestions
- Give usable suggestions
getNext() {
   if location-is-reliable:
       get_location_idea
    else:
       get_default_suggestion
}
 */

// TODO: consider serializing all state to SharedPreferences and switching this to an IntentService
public class SuggestionService extends Service {
    private String[] genericSuggestions;
    private int genericSuggestionsPosition = 0;
    private ScreenLockReceiver mScreenLockReceiver;

    public SuggestionService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private String getNextSuggestion() {
        // Advance position and return suggestion
        genericSuggestionsPosition++;
        if (genericSuggestionsPosition >= genericSuggestions.length) {
            genericSuggestionsPosition = 0;
        }
        return genericSuggestions[genericSuggestionsPosition];
    }

    @Override
    public void onStart(Intent intent, int startId) {
        // For time consuming an long tasks you can launch a new thread here...

        Log.d("suggestion_service", "started");


        // TODO:
        // Get the next suggestion and write it to the lock screen
        // Attach to screen on/off/lock events
        //   Screen on:
        //     - (should display the current suggestion)
        //     - mark the current suggestion as viewed
        //   Screen off/screen lock:
        //     - get the next suggestion and display it
        // Create the SuggestionFeed
        // Attach to location update events
        //   Location change:
        //     Do a places search and cache the result; factor into next suggestion

        // This code prints a demo message to the lockscreen as an "alarm"
        String message = "This is a test";
        Settings.System.putString(getApplicationContext().getContentResolver(),
                Settings.System.NEXT_ALARM_FORMATTED, message);








        // Create a BroadcastReceiver to display the suggestion when the lock
        // screen is enabled (and not display it when the lock screen is disabled)


        mScreenLockReceiver = new ScreenLockReceiver(this);
        mScreenLockReceiver.register();



    }

    @Override
    public void onDestroy() {
        Log.d("suggestion_service", "destroyed");
        //unregisterReceiver(screenLockReceiver);
        mScreenLockReceiver.unregister();
        mScreenLockReceiver = null;
    }
}

class SuggestionView extends View {

    private Paint mPaint;

    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowManager;

    private Context context;

    private boolean isHidden;

    public void show() {
        if (!isHidden) {
            //Toast.makeText(context, "SV: show() called but not hidden!", Toast.LENGTH_LONG).show();
            return;
        }
        mWindowManager.addView(this, mParams);
        isHidden = false;
    }

    public void hide(){
        if (isHidden) {
            //Toast.makeText(context, "SV: hide() called but hidden!", Toast.LENGTH_LONG).show();
            return;
        }
        mWindowManager.removeView(this);
        isHidden = true;
    }


    public SuggestionView(Context context) {
        super(context);
        mPaint = new Paint();
        mPaint.setTextSize(50);
        mPaint.setARGB(200, 200, 200, 200);

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, 150, 10, 10,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT);

        mParams.gravity = Gravity.CENTER;
        mParams.setTitle("Window test");
        isHidden = true;

        mWindowManager = (WindowManager)context.getSystemService(context.WINDOW_SERVICE);
        // TODO: maybe assign things to null onDestroy
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawText("test test test", 0, 100, mPaint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}

class SuggestionFeed {
    private String[] genericSuggestions;
    private int genericSuggestionsPosition = 0;
    private ConcurrentLinkedQueue<String> mReceiverQueue;

    public SuggestionFeed(SuggestionService service) {
        // create: init from defaults, remember previous positions, cached data
        // TODO: remember positions, cache via SharedPreference maybe
        // For now I just load the default list into the queue
        mReceiverQueue = new ConcurrentLinkedQueue<String>();
        for (String s : service.getResources().getStringArray(R.array.default_suggestions)) {
            mReceiverQueue.add(s);
        }
    }

    public String next() {
        String result = mReceiverQueue.poll();
        if (result != null) {
            // TODO: send an Intent back here (via the Suggestion Service) STOPPED HERE
        }
        return result;
    }

    public void update(String suggestionSeen) {
        // TODO: call this when an intent from next() is received and update some sort of table of suggestions
        // TODO: could measure time deltas here to be "smart" :)
    }
}

class ScreenLockReceiver extends BroadcastReceiver {
    SuggestionView mSuggestionView;
    SuggestionService mSuggestionService;
    public ScreenLockReceiver(SuggestionService service) {
        mSuggestionService = service;
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

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "ScreenLockReceiver invoked", Toast.LENGTH_LONG).show();
        String action = intent.getAction();
        Log.d("ScreenLockReceiver", action);
        Toast.makeText(context, action, Toast.LENGTH_LONG).show();
        if (action.equals(Intent.ACTION_USER_PRESENT)) {
            mSuggestionView.hide();
        } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
            //mSuggestionView.show(); // TODO: maybe i don't need this; ACTION_SCREEN_OFF is more
            // responsive (if it always works)
        } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
            mSuggestionView.show();
        }
    }
}
