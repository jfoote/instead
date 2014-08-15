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


// TODO: consider serializing all state to SharedPreferences and switching this to an IntentService
// AS IT STANDS: i need to keep this alive to maintain a reference to the screen lock BroadcastReceiver
//    if I allow it to be destroyed i can probably workaround the issue of communicating (it can read the suggestion
//    via a SharedPreferences maybe, and just send an Intent back to notify that a thing has been read *maybe*
//    though synchronization could be an issue), but i can't seem to create a view from within the BCR anyway, so it needs to persist
public class SuggestionService extends Service {
    private String[] genericSuggestions;
    private int genericSuggestionsPosition = 0;
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
        String action = intent.getAction();

        if (action == null) {
            Log.d("SuggestionService::onStart", "(null)");
        }
        Log.d("SuggestionService::onStart", action);

        if (action == INITIALIZE_ACTION){

            Log.d("SuggestionService", "started");

            // Create feed of suggestions
            try {
                mFeed = new SuggestionFeed(this);
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Create a BroadcastReceiver to handle displaying messages to the lock screen
            // and fill up its message queue
            mLockScreenReceiver = new LockScreenReceiver(this, mFeed);
            mLockScreenReceiver.register();

        } else if (action == SuggestionService.SUGGESTION_SEEN_ACTION) {
            // TODO:
            //    - add support for serializing/de-serializing 'seen' count
            //    - move on: add more lists and attach to location update events
            //        - Location change:Do a places search and cache the result; factor into next suggestion
            //

            // TODO: ASSERT(mFeed and ScreenLockRecevier exist)
            mFeed.handleSeen(intent);
        }

    }

    @Override
    public void onDestroy() {
        Log.d("suggestion_service", "destroyed");
        mLockScreenReceiver.unregister();
        mLockScreenReceiver = null;
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

}

