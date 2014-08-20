package com.cmplxen.instead;

import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

// TODO: there has got to be a better way to serialize/deserialize these
// as it stands i deserialize manually and hacked together a suggestion lookup (BCR->SRV)
// as i can't pass these objects via Intents

public class Suggestion {
    public int mSeenCount = 0;
    public int mGoodness = -1;
    public String mMessage = "Temporarily out of suggestions";
    public SuggestionCategory mCategory = null;
    public int mPriority = 0;

    public Suggestion() { }

    public boolean isDefault() {
        if (mGoodness == -1) {
            return true;
        }
        return false;
    }

    public void prioritize(SuggestionCategory currentCategory) {
        // heuristic: not-seen >> location-aware > goodness_rank
        // priority: lower is better
        //    score = seen * 100 - location-aware * 50 - goodness

        // if currentCategory == this.Category BONUS
        // elseif this.Category == 'general' OK
        // else NO WAY DON"T SHOW IT!
        mPriority = this.mSeenCount * 100 - this.mGoodness;
        if (currentCategory.mName.contentEquals(this.mCategory.mName)) { // TODO: put this in a Comparator
            Log.d("MATCH!!", "MATCH!!");
            mPriority -= 50;
        }
    }

    private String getSeenKey() {
        return mCategory.mName + "\t" + mMessage;
    }

    public void saveSeen(SharedPreferences prefs) {
        Log.d("Suggestion::saveSeen", getSeenKey() + ":" + mSeenCount);
        prefs.edit().putInt(getSeenKey(), mSeenCount).apply();
    }

    public void loadSeen(SharedPreferences prefs) {
        mSeenCount = prefs.getInt(getSeenKey(), 0);
        Log.d("Suggestion::loadSeen", getSeenKey() + ":" + mSeenCount);
    }

    public Suggestion(XmlResourceParser xpp, SuggestionCategory category) throws IOException, XmlPullParserException {
        // TODO: make this an assert } else if (eventType == XmlResourceParser.START_TAG && tagName == "suggestion") {
        mCategory = category;

        String tagName = null;
        int eventType = -1;

        // parse suggestion
        while (true) {
            eventType = xpp.next();
            tagName = xpp.getName();
            if (eventType == XmlResourceParser.END_TAG && tagName.contentEquals("suggestion")) {
                // DONE
                break;
            } else if (eventType == XmlResourceParser.START_TAG && tagName.contentEquals("message")) {
                // ASSERT(mMessage == null)
                mMessage = xpp.nextText();
            } else if (eventType == XmlResourceParser.START_TAG && tagName.contentEquals("goodness")) {
                // ASSERT(mGoodness == -1);
                mGoodness = Integer.valueOf(xpp.nextText()); // TODO: make sure this is right!
            }
        }
    }
}

class SuggestionComparator implements Comparator<Suggestion> {
    // TODO: figure out how to encapsulate this properly
    public int compare(Suggestion x, Suggestion y) {
        // pos if x is greater than y
        // higher priority is better
        return x.mPriority - y.mPriority;
    }
}

class SuggestionCategory extends ArrayList<Suggestion> {
    public String mName = null;

    public SuggestionCategory(String name) {
        // used to create a default category before any are read in
        mName = name;
    }

    public SuggestionCategory(XmlResourceParser xpp) throws IOException, XmlPullParserException {

        // PRE: xpp is pointing at a START_TAG named FOO
        // ASSERT(eventType == XmlResourceParser.START_TAG && tagName == "suggestion-list")

        mName = xpp.getAttributeValue(null, "name");

        String tagName = null;
        int eventType = 0;

        // parse suggestion list
        while(true) {
            eventType = xpp.next();
            tagName = xpp.getName();
            if (eventType == XmlResourceParser.END_TAG && tagName.contentEquals("suggestion-list")) {
                // DONE
                break; // TODO: done?
            } else if (eventType == XmlResourceParser.START_TAG && tagName.contentEquals("suggestion")) {
                Log.d("SuggestionCategories::SuggestionCategories", "Found suggestion");
                this.add(new Suggestion(xpp, this));
            } else {
                Log.w("SuggestionCategory", "Unexpected XML event: " + eventType + ":" + tagName);
            }
        }
    }

    public Suggestion FindSuggestion(String message) {
        for(Suggestion s: this) {
            if (s.mMessage.contentEquals(message)){
                return s;
            }
        }
        Log.w("SuggestionCategory::FindCategory", "Suggestion not found:" + message);
        return null;
    }
}

class SuggestionCategories extends ArrayList<SuggestionCategory> {
    public SuggestionCategories(XmlResourceParser xpp) throws IOException, XmlPullParserException {
        // ASSERT(at start of document)
        Log.d("SuggestionCategories::SuggestionCategories", "Entered.");
        String tagName = null;
        int eventType = 0;
        do {
            eventType = xpp.next();
            tagName = xpp.getName();
            Log.d("SuggestionCategories", "XML event: " + eventType + ":" + tagName);
            if (eventType == XmlResourceParser.START_DOCUMENT) {
                Log.d("SuggestionFeed", "Start document");
            } else if (eventType == XmlResourceParser.START_TAG && tagName.contentEquals("suggestion-list")) {
                Log.d("SuggestionCategories::SuggestionCategories", "Found list");
                this.add(new SuggestionCategory(xpp));
            }
        } while (eventType != XmlResourceParser.END_DOCUMENT);
    }

    public SuggestionCategory FindCategory(String categoryName) {
        for(SuggestionCategory c: this) {
            if (c.mName.contentEquals(categoryName)) {
                return c;
            }
        }
        Log.w("SuggestionCategories::FindCategory", "Category not found:" + categoryName);
        return null;
    }
}
