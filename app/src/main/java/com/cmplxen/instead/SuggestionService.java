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
// AS IT STANDS: i need to keep this alive to maintain a reference to the screen lock BroadcastReceiver
//    if I allow it to be destroyed i can probably workaround the issue of communicating (it can read the suggestion
//    via a SharedPreferences maybe, and just send an Intent back to notify that a thing has been read *maybe*
//    though synchronization could be an issue), but i can't seem to create a view from within the BCR anyway, so it needs to persist
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

    @Override
    public void onStart(Intent intent, int startId) {
        String action = intent.getAction();
        if (action == null){
            action = "(null)";
        }
        Log.d("SuggestionService::onStart", action);

        // if intent == updatequeue
        // else if intent == start
        // else if intent == locationchanged

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

        // Create a BroadcastReceiver to handle displaying messages to the lock screen
        // and fill up its message queue
        mScreenLockReceiver = new ScreenLockReceiver(this);
        for (String s : this.getResources().getStringArray(R.array.default_suggestions)) {
            mScreenLockReceiver.suggestionQueue.add(s);
        }
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

class PrioritizedSuggestion {
    public int priority = 0;
    public String text = null;
}

class ScreenLockReceiver extends BroadcastReceiver {
    private SuggestionView mSuggestionView;
    private SuggestionService mSuggestionService;
    public ConcurrentLinkedQueue<String> suggestionQueue = null;

    public static final String SUGGESTION_SEEN_ACTION = "com.cmplxen.instead.intent.action.SUGGESTION_SEEN";
    public static final String UPDATE_MESSAGE_ACTION = "com.cmplxen.instead.intent.action.UPDATE_MESSAGE";

    public ScreenLockReceiver(SuggestionService service) {
        mSuggestionService = service;

        // create: init from defaults, remember previous positions, cached data
        // TODO: remember positions, cache via SharedPreference maybe
        // For now I just load the default list into the queue
        suggestionQueue = new ConcurrentLinkedQueue<String>();

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
        suggestionQueue = null;
    }

    private String mMessage = mDefaultMessage;
    private static final String mDefaultMessage = "Temporarily out of ideas -- meditate!";

    private void updateMessage() {
        mMessage = suggestionQueue.poll();
        if (mMessage == null) {
            mMessage = mDefaultMessage;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "ScreenLockReceiver invoked", Toast.LENGTH_LONG).show();
        String action = intent.getAction();
        Log.d("ScreenLockReceiver", action);
        Toast.makeText(context, action, Toast.LENGTH_LONG).show();
        if (action.equals(Intent.ACTION_USER_PRESENT)) {
            // hide the window,  grab the next message (so we are ready for another lock)
            // then notify the suggestion service that the message was seen so it can
            // change the queue if it needs to
            // TODO: NOTE that if i stick with this, the queue-updater should just re-stuff
            // the seen message (with an updated priority) and NOT tell this receiver
            // to update as the queue ordering won't have changed
            mSuggestionView.hide();
            String seenMessage = mMessage;
            updateMessage();

            // TODO send 'suggestion seen intent' back to service so it can update the queue
        //} else if (action.equals(Intent.ACTION_SCREEN_ON)) { //TODO: maybe i don't need this
        // ACTION_SCREEN_OFF is more responsive (if it always works)
        } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {

            mSuggestionView.show(mMessage);
        } else if (action.equals(UPDATE_MESSAGE_ACTION)) { // service will notify BCR to updater when the queue ordering has changed
            updateMessage();
        }

    }
}




class SuggestionView extends View {

    private Paint mPaint;

    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowManager;

    private Context context;

    private boolean isHidden;

    private String messageText = "No suggestion";

    public void show(String message) {
        if (!isHidden) {
            //Toast.makeText(context, "SV: show() called but not hidden!", Toast.LENGTH_LONG).show();
            return;
        }
        this.messageText = message;
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
        canvas.drawText(this.messageText, 0, 100, mPaint);
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
