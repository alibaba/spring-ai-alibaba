The main issues to be addressed here are:
1) There are resource conflicts (such as browser, file access)
2) Some resources need to be used across agents, for example, the previous agent needs it, and the next agent executes navigate commands in the browser.
3) Other times, different agents need some temporary intermediate result states.
4) When the Plan ends, all resources must be guaranteed to be closed.

So, the general strategy is:
1) Assembly is placed in ManusConfiguration.
2) Classes that may have resource conflicts are separated out using the Service approach
3) Tools must implement and provide PlanBasedLifecycleService to ensure resources can be closed at the end
4) Tools should not use static.
