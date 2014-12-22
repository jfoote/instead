# Instead

A half-baked, experimental Android app designed to keep you present, happy, and off your damn smartphone. 

_Warning This code not complete -- the project builds and runs (Android 4), but lacks functionality, comments, unit tests, and so on._

## Story

I gave up my smartphone for 2.5 years and it was great. However, I was taking fewer pictures of my kids and had to occassionally borrow my wife's smartphone so I could tether my laptop for business trips (thanks [Ting](https://www.ting.com) for being awesome).

This app was [partially-]developed for personal use while I was waiting for the Nexus 5 price to drop. Once I got ahold of a Nexus 5 I figured out that not installing any output-oriented apps, disabling/signing-out of bothersome Google apps (including gmail/inbox), and neutering the browser was sufficient for my use case. 

I generally really enjoy focused time on my PC and mobile devices, but I put a high premium on the time that I spend using them. So, while I think this app could help some folks, competing priorities with my day job and side research have prevented me from finishing it. 

So, I've published what I've gotten so far in case someone might find the concept or code snippets useful. If you have any questions or are interested in this philosophy feel free to [drop me a line](mailto:jmfoote@loyola.edu).

## State of the app

The goal of the app was to download lists of things to do in the real world based on the user's location in a fashion that allows the end user to own their data (as opposed to how Google, Facebook, etc. do their its-not-evil-if-u-like-it evil thing).

The app displays suggestions from a dummy list of things to to the user's lock screen when it is enabled. The "list" is a priority queue that factors in how many times a user has seen a particular idea to try to keep things fresh. There may be some stub code in there to try to determine if the user "ignored" an idea and factor that in as well. 

When enabled, the app grabs the user's location and uses some Google APIs to figure out what class of location they are at. It never got connected back to the list mechanism.

## Functional Design Notes

The primary entry point to the system is the SettingsActivity (SettingsActivity.java). When the user enables suggestions, an instance of SuggestionService (SuggestionService.java) is created. SuggestionService orchestrates communication and resource allocation for the application, including creating and adjusting the SuggestionFeed (SuggestionFeed.java), which is the actual feed of suggestions that is shown to the user when they wake their device.

When the service is instantiated, it in turn creates an instance of LockScreenReceiver (LockScreenReceiver.java), which handles system events to show/hide suggestions on the lock screen and send Intents back to the SuggestionService to notify it when the user has seen a suggestion it has pulled from the SuggestionFeed.

The SuggestionFeed is a ConcurrentPriorityQueue of Suggestions (Suggestion.java). The SuggestionService puts Suggestions into the SuggestionFeed and sets the current "category" of Suggestions (which is slated to be based on the user's location, etc.). Suggestions are grouped by SuggestionCategory and prioritized in the SuggestionFeed via a heuristic based on the current category (location), how many times the user has seen the Suggestion, and a "how good is this suggestion" integer value that is input at compile time. The LockScreenReceiver pulls Suggestions from the SuggestionFeed and notifies the SuggestionService when one has been seen so that it can update the "how many times the user has seen this suggestion" value and re-insert it into the Feed.

The SuggestionService also creates a LocationMonitor (LocationMonitor.java) that tracks the users location and notifies the SuggestionService when the "location type" changes. This is a work in progress, and generally a complete mess.
