package com.cmplxen.instead;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
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
    public static final String INITIALIZE_ACTION = "com.cmplxen.instead.intent.action.INITIALIZE_ACTION";

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

        if (action == null) {
            Log.d("SuggestionService::onStart", "(null)");
        }
        Log.d("SuggestionService::onStart", action);

        if (action == INITIALIZE_ACTION){

            Log.d("SuggestionService", "started");

            // Create a BroadcastReceiver to handle displaying messages to the lock screen
            // and fill up its message queue
            mScreenLockReceiver = new ScreenLockReceiver(this);
            for (String s : this.getResources().getStringArray(R.array.default_suggestions)) {
                mScreenLockReceiver.suggestionQueue.add(s);
            }
            mScreenLockReceiver.register();

        } else if (action == ScreenLockReceiver.SUGGESTION_SEEN_ACTION) {
            // TODO: update the scoring/queue/serialize stuff
            // things to cache, maybe their location (depending on how responsive the API is)
            //    - count of how many times user has seen a suggestion
            // STOPPED HERE:
            // heuristic: seen >> location-aware > goodness_rank
            //    score = seen * 100 + location-aware * 50 + goodness
            //   try to distribute goodness evenly across each list
            //   we should store only "seen"
            //  TODOs:
            //    - switch to a priority queue
            //    - somehow define the default list (and therefore other lists) with 'goodness_rank'
            //    - make the above work, then add support for serializing/de-serializing 'seen' count
            //    - move on: add more lists and make it location-aware
        }




        // TODO:
        // Get the next suggestion and write it to the lock screen
        // Attach to screen on/off/lock events [DONE]
        //   Screen on:
        //     - (should display the current suggestion)
        //     - mark the current suggestion as viewed
        //   Screen off/screen lock:
        //     - get the next suggestion and display it
        // Create the SuggestionFeed
        // Attach to location update events
        //   Location change:
        //     Do a places search and cache the result; factor into next suggestion





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

    public static final String SUGGESTION_SEEN_ACTION = "com.cmplxen.instead.intent.action.SUGGESTION_SEEN_ACTION";
    public static final String SUGGESTION_SEEN_MESSAGE = "com.cmplxen.instead.intent.action.SUGGESTION_SEEN_MESSAGE";

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

    private String mMessage = sDefaultMessage;
    private static final String sDefaultMessage = "Temporarily out of ideas -- meditate!";

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "ScreenLockReceiver invoked", Toast.LENGTH_LONG).show();
        String action = intent.getAction();
        Log.d("ScreenLockReceiver", action);
        Toast.makeText(context, action, Toast.LENGTH_LONG).show();

        if (action.equals(Intent.ACTION_USER_PRESENT)) {
            mSuggestionView.hide();

            // send 'suggestion seen intent' back to service so it can update the queue
            Intent seenIntent = new Intent(context, SuggestionService.class);
            seenIntent.setAction(SUGGESTION_SEEN_ACTION);
            seenIntent.putExtra(SUGGESTION_SEEN_MESSAGE, mMessage);
            context.startService(seenIntent);

        //} else if (action.equals(Intent.ACTION_SCREEN_ON)) { //TODO: handle click-on/click-off
        // ACTION_SCREEN_OFF is more responsive (if it always works)
        } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
            mMessage = suggestionQueue.poll();
            if (mMessage == null) {
                Log.d("ScreenLockReceiver", "suggestionQueue is out of messages");
                mMessage = sDefaultMessage;
            }
            mSuggestionView.show(mMessage);
        } // else if (action.equals(ACTION_UPDATE_SUGGESTION ) {

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
        Log.d("SuggestionView", "calling addView");
        mWindowManager.addView(this, mParams);
        isHidden = false;
    }

    public void hide(){
        if (isHidden) {
            //Toast.makeText(context, "SV: hide() called but hidden!", Toast.LENGTH_LONG).show();
            return;
        }
        Log.d("SuggestionView", "calling removeView");
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

    /*
    // This code prints a demo message to the lockscreen as an "alarm"; TODO: either make this
    // optional or delete this stub code
    String message = "This is a test";
    Settings.System.putString(getApplicationContext().getContentResolver(),
    Settings.System.NEXT_ALARM_FORMATTED, message);
     */
}
