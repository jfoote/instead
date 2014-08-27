# Instead

## Functional Design Notes

The primary entry point to the system is the SettingsActivity (SettingsActivity.java). When the user enables suggestions, an instance of SuggestionService (SuggestionService.java) is created. SuggestionService orchestrates communication and resource allocation for the application, including creating and adjusting the SuggestionFeed (SuggestionFeed.java). 

When the service is instantiated, it in turn creates an instance of LockScreenReceiver (LockScreenReceiver.java), which handles system events to show/hide suggestions on the lock screen and send Intents back to the SuggestionService to notify it when the user has seen a suggestion it has pulled from the SuggestionFeed.

The SuggestionFeed is a ConcurrentPriorityQueue of Suggestions (Suggestion.java). The SuggestionService puts Suggestions into the SuggestionFeed and sets the current "category" of Suggestions (which is slated to be based on the user's location, etc.). Suggestions are grouped by SuggestionCategory and prioritized in the SuggestionFeed via a heuristic based on the current category (location), how many times the user has seen the Suggestion, and a "how good is this suggestion" integer value that is input at compile time. The LockScreenReceiver pulls Suggestions from the SuggestionFeed and notifies the SuggestionService when one has been seen so that it can update the "how many times the user has seen this suggestion" value and re-insert it into the Feed.

The SuggestionService also creates a LocationMonitor (LocationMonitor.java) that tracks the users location and notifies the SuggestionService when the "location type" changes. This is a work in progress, and generally a complete mess.
