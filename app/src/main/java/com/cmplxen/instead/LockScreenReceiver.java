/*
This file contains the Broadcast Receiver that manages the lock screen and supporting functionality.
*/

package com.cmplxen.instead;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

public class LockScreenReceiver extends BroadcastReceiver {
    /*
    A Broadcast Receiver that handles lockscreen-related system intents.
    This Broadcast receiver reads suggestions from the suggestion service via a threadsafe
    priority queue and sends data regarding seen messages back to the suggestion service via
    intents.
     */
    private SuggestionView mSuggestionView; // view that displays the suggestion
    private SuggestionService mSuggestionService; // suggestion service
    private SuggestionFeed mFeed; // feed of suggestions

    public LockScreenReceiver(SuggestionService service, SuggestionFeed feed) {
        // store feed for reading suggestions to show to lock screen, and feed itself
        mSuggestionService = service;
        mFeed = feed;
    }

    public void register() {
        /*
        Registers this receiver to receive various types of intents: whether the lock screen has
        been enabled, whether the user has disabled the lock screen, and so on.
         */

        // Create a view to display the suggestion on the lock screen
        mSuggestionView = new SuggestionView(mSuggestionService);

        // Register for lock-screen-related intents
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_PRESENT");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.SCREEN_ON");
        mSuggestionService.registerReceiver(this, filter);
    }

    public void unregister() {
        /*
        Deregisters this receiver. Generally called by the service when it is exiting.
         */
        mSuggestionService.unregisterReceiver(this);
        mSuggestionView = null;
    }

    private Suggestion mDisplayedSuggestion = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        /*
        Displays or hides the suggestion based on the intent action. Called by the system
        thread.
         */

        String action = intent.getAction();
        Log.d("LockScreenReceiver", action);
        if (action.equals(Intent.ACTION_USER_PRESENT) && mDisplayedSuggestion != null) {

            // Hide the suggestion view and update stats for the suggestion (by sending an intent
            // to the suggestion service thread, which calls a related suggestion feed method)
            mSuggestionView.hide();
            mFeed.notifySeen(mDisplayedSuggestion);

        } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {

            // Pop a suggestion from the suggestion feed and display it
            mDisplayedSuggestion = mFeed.pop();
            mSuggestionView.show(mDisplayedSuggestion.mMessage);
            Log.d("LockScreenReceiver::onReceive", mDisplayedSuggestion.mMessage + "p:" + mDisplayedSuggestion.mPriority);
        }
        // TODO: handle click-on/click-off
        // TODO: handle updating the suggestion when the screen is off (for background location updates, etc.)
        // TODO: handle any other methods for locking the screen (if any exist)

    }
}

class SuggestionView extends View {
    /*
    This is the view that displays the suggestion on the lock screen. It is used by the
    lock screen broadcast receiver.
     */

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
        mParams.setTitle("Instead");
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
