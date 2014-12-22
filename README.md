# Instead

A half-baked, experimental app designed to keep you present, happy, and off your damn smartphone. 

*Warning* This code not complete -- the project builds and runs, but lacks functionality, comments, unit tests, and so on.

## Story

I gave up my smartphone for 2.5 years and it was great. However, I was taking fewer pictures of my kids and had to occassionally borrow my wife's smartphone so I could tether my laptop for business trips (which was easy thanks to [Ting](https://www.ting.com)). 

This app was developed for personal use while I was waiting for the Nexus 5 price to drop. Once I got ahold of a Nexus 5 I figured out that not installing any output-oriented apps, disabling/signing-out of bothersome Google apps (including gmail/inbox), and neutering the browser was sufficient for my use case. 

I generally really enjoy focused time on my PC and mobile devices, but I put a high premium on the time that I spend using them. So, while I think this app could help some folks, competing priorities with my day job and side research have prevented me from finishing it. 

So, I've published what I've gotten so far in case someone might find the concept or code snippets useful. 


## Functional Design Notes

The primary entry point to the system is the SettingsActivity (SettingsActivity.java). When the user enables suggestions, an instance of SuggestionService (SuggestionService.java) is created. SuggestionService orchestrates communication and resource allocation for the application, including creating and adjusting the SuggestionFeed (SuggestionFeed.java), which is the actual feed of suggestions that is shown to the user when they wake their device.

When the service is instantiated, it in turn creates an instance of LockScreenReceiver (LockScreenReceiver.java), which handles system events to show/hide suggestions on the lock screen and send Intents back to the SuggestionService to notify it when the user has seen a suggestion it has pulled from the SuggestionFeed.

The SuggestionFeed is a ConcurrentPriorityQueue of Suggestions (Suggestion.java). The SuggestionService puts Suggestions into the SuggestionFeed and sets the current "category" of Suggestions (which is slated to be based on the user's location, etc.). Suggestions are grouped by SuggestionCategory and prioritized in the SuggestionFeed via a heuristic based on the current category (location), how many times the user has seen the Suggestion, and a "how good is this suggestion" integer value that is input at compile time. The LockScreenReceiver pulls Suggestions from the SuggestionFeed and notifies the SuggestionService when one has been seen so that it can update the "how many times the user has seen this suggestion" value and re-insert it into the Feed.

The SuggestionService also creates a LocationMonitor (LocationMonitor.java) that tracks the users location and notifies the SuggestionService when the "location type" changes. This is a work in progress, and generally a complete mess.
