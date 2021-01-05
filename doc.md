**Snapyr for Android** makes it simple to send your data to any tool without having to learn, test or implement a new API every time.

All of Snapyr's client sources are open-source.

Snapyr supports any Android device running API 14 (Android 4.0) and higher. This includes Amazon Fire devices.

#Analytics-Android and Unique Identifiers

One of the most important parts of any analytics platform is the ability to consistently and accurately identify users. To do this, the platform must assign and persist some form of identification on the device, so you can analyze user actions effectively. This is especially important for funnel conversion analysis and retention analysis.

Naturally the Snapyr SDK needs a unique ID for each user. The very first time an Android app that uses Snapyr launches, the Snapyr SDK generates a UUID and saves it on the device’s disk. This is used as the anonymousId and stays constant for the user on the device. To create a new user on the same device, call reset on the Snapyr client.

The Snapyr SDK also collects the Advertising ID provided by Play Services. Make sure the Play Services Ads library is included as a dependency for your application. This is the ID that should be used for advertising purposes. This value is set to context.device.advertisingId.

Snapyr also collects the Android ID as context.device.id. Some destinations rely on this field being the Android ID, so double check the destinations’ vendor documentation if you choose to override the default value.

#API call queuing in Analytics-Android

The Analytics-Android library queues API calls and uploads them in batches. This limits the number of network calls made, and helps save battery on the user’s device.

When you send an event, the library saves it to disk. When the queue size reaches the maximum size you specify (20 by default), the library flushes the queue and uploads the events in a single batch. Since the data is saved immediately, it isn’t lost even if the app is killed or the operating system crashes.

The queue behavior might differ for Device-mode destinations. For example, Mixpanel’s SDK queues events and then flushes them only when the app goes to the background.

This is why even if you see events in the debugger, the Device-mode destination may not show them on their dashboards yet because they might still be in their mobile SDK’s queue. The opposite may also happen: the Device-mode destination SDK might send events to its servers before Snapyr sends its queue, so events could show up in the destination’s dashboard before they appear in the Snapyr debugger.

##Queue persistance in Analytics-Android

Analytics-Android uses a persistent disk queue, so the events persist even when the app is killed. On app restart, the library reads them from disk and uploads the events. The queue works on top of Tape, which is designed to even survive process and system crashes.

Analytics-Android saves up to 1000 calls on disk, and these never expire.

[block:api-header]
{
  "title": "Step 1: Install the Library"
}
[/block]
The easiest way to install the Snapyr Analytics-Android library is using a build system like Gradle. This makes it simple to upgrade versions and add destinations. The library is distributed using Maven Central. Just add the analytics module to your build.gradle file as in the example lines below:
[block:code]
{
  "codes": [
    {
      "code": "dependencies {\n  implementation 'com.snapyr.analytics.android:analytics:4.+'\n  }",
      "language": "text"
    }
  ]
}
[/block]

[block:api-header]
{
  "title": "Step 2. Initialize the Client"
}
[/block]

We recommend initializing the client in your Application subclass. You’ll need your Snapyr Write Key.

[block:code]
{
  "codes": [
    {
      "code": "// Create an analytics client with the given context and Snapyr write key.\nAnalytics analytics = new Analytics.Builder(context, YOUR_WRITE_KEY)\n  .trackApplicationLifecycleEvents() // Enable this to record certain application events automatically!\n  .recordScreenViews() // Enable this to record screen views automatically!\n  .build();\n\n// Set the initialized instance as a globally accessible instance.\nAnalytics.setSingletonInstance(analytics);\n",
      "language": "java"
    },
    {
      "code": "// Create an analytics client with the given context and Snapyr write key.\nval analytics = Analytics.Builder(context, YOUR_WRITE_KEY)\n  .trackApplicationLifecycleEvents() // Enable this to record certain application events automatically!\n  .recordScreenViews() // Enable this to record screen views automatically!\n  .build()\n\n// Set the initialized instance as a globally accessible instance.\nAnalytics.setSingletonInstance(analytics);",
      "language": "kotlin"
    }
  ]
}
[/block]
You can automatically track lifecycle events such as Application Opened, Application Installed, Application Updated to start quickly with core events. These are optional, but highly recommended.

[block:api-header]
{
  "title": "Customize the Client (Optional)"
}
[/block]
The entry point of the library is through the Analytics class. As you might have seen in the quickstart, here’s how you initialize the Analytics client with its defaults.


[block:code]
{
  "codes": [
    {
      "code": "Analytics analytics = new Analytics.Builder(context, writeKey).build();\n",
      "language": "java"
    },
    {
      "code": "val analytics = Analytics.Builder(context, writeKey).build()\n",
      "language": "kotlin"
    }
  ]
}
[/block]
The Analytics.Builder class lets you customize settings for the Analytics client, including things like the flush interval and packaging Device-mode destinations. Refer to the Javadocs for details on customizable parameters.

We also maintain a global default instance which is initialized with defaults suitable to most implementations.


[block:code]
{
  "codes": [
    {
      "code": "// You can also register your custom instance as a global singleton.\nAnalytics.setSingletonInstance(analytics);\nAnalytics.with(context).track(...);",
      "language": "java"
    },
    {
      "code": "// You can also register your custom instance as a global singleton.\nAnalytics.setSingletonInstance(analytics)\nAnalytics.with(context).track(...)",
      "language": "kotlin"
    }
  ]
}
[/block]
In general, Snapyr recommends that you use the Builder method because it provides the most flexibility. Remember you can call Analytics.setSingletonInstance only ONCE, so it’s best to put the initialization code inside your custom Application class.


[block:code]
{
  "codes": [
    {
      "code": "public class MyApp extends Application {\n  @Override public void onCreate() {\n    Analytics analytics = new Analytics.Builder(context, writeKey).build();\n    Analytics.setSingletonInstance(analytics);\n\n    // Safely call Analytics.with(context) from anywhere within your app!\n    Analytics.with(context).track(\"Application Started\");\n  }\n}",
      "language": "java"
    },
    {
      "code": "class MyApp : Application() {\n  override fun onCreate() {\n    val analytics = Analytics.Builder(context, writeKey).build()\n    Analytics.setSingletonInstance(analytics)\n\n    // Safely call Analytics.with(context) from anywhere within your app!\n    Analytics.with(context).track(\"Application Started\")\n  }\n}",
      "language": "kotlin"
    }
  ]
}
[/block]
Once you initialize an Analytics client, you can safely call any of its tracking methods from any thread. These events are dispatched asynchronously to the Snapyr servers and to any Device-mode destinations.

Note: You should only ever initialize ONE instance of the Analytics client. These are expensive to create and throw away, and in most cases, you should stick to Snapyr's singleton implementation to make using the SDK easier.
[block:api-header]
{
  "title": "Step 3. Add Permissions"
}
[/block]
Ensure that the necessary permissions are declared in your application’s AndroidManifest.xml.

[block:code]
{
  "codes": [
    {
      "code": " <!-- Required for internet. -->\n<uses-permission android:name=\"android.permission.INTERNET\"/>\n",
      "language": "text",
      "name": null
    }
  ]
}
[/block]
#Main Snapyr SDK Calls
[block:api-header]
{
  "title": "Identify"
}
[/block]

[block:callout]
{
  "type": "info",
  "body": "For any of the different methods described in this doc, you can replace the properties and traits in the code samples with variables that represent the data collected.",
  "title": "Good to know"
}
[/block]

Identify calls let you tie a user to their actions, and record traits about them. It includes a unique User ID and any optional traits you know about them.

Example identify call:
[block:code]
{
  "codes": [
    {
      "code": "Analytics.with(context).identify(\"a user's id\", new Traits().putName(\"John Doe\"), null);\n",
      "language": "java"
    },
    {
      "code": "Analytics.with(context).identify(\"a user's id\", Traits().putName(\"John Doe\"), null)\n",
      "language": "kotlin"
    }
  ]
}
[/block]
Snapyr recommends that you make an Identify call once when the user’s first creates an account, and only using the Identify call later when their traits change. Snapyr remembers the previous userIDs and merges the new traits with the old ones.
[block:code]
{
  "codes": [
    {
      "code": "// Initially when you only know the user's name\nAnalytics.with(context).identify(new Traits().putName(\"Michael Bolton\"));\n\n// Sometime later in your app when the user gives you their email\nAnalytics.with(context).identify(new Traits().putEmail(\"mbolton@example.com\"));\n",
      "language": "java"
    },
    {
      "code": "// Initially when you only know the user's name\nAnalytics.with(context).identify(Traits().putName(\"Michael Bolton\"))\n\n// Sometime later in your app when the user gives you their email\nAnalytics.with(context).identify(Traits().putEmail(\"mbolton@example.com\"))",
      "language": "kotlin"
    }
  ]
}
[/block]
Remember, you can replace the properties and traits in the code samples with variables that represent the data you actually collected.

The Identify call has the following fields:


[block:parameters]
{
  "data": {
    "0-0": "userId *String,optional* ",
    "0-1": "The database ID for this user.",
    "1-0": "traits *Traits,optional* ",
    "1-1": "A map of traits about the user, such as their name, email, address, etc.",
    "2-0": "options *Options, optional*",
    "2-1": "Extra options for the call."
  },
  "cols": 2,
  "rows": 3
}
[/block]

The Android library currently automatically sends the userId and anonymousId as traits. Additionally, traits are sent in the context.traits field with every message.
[block:api-header]
{
  "title": "Track"
}
[/block]
The Track call lets you record the actions your users perform. Every action triggers what we call an “event”, which can also have associated properties.

To get started, the Analytics-Android SDK can automatically tracks a few key common events using the Snapyr Native Mobile Spec, such as the Application Installed, Application Updated and Application Opened. You can enable this option during initialization.

You might also want to track events that indicate success for your mobile app, like Signed Up, Item Purchased or Article Bookmarked. Snapyr recommends tracking just a few important events. You can always add more later!

Example track call:


[block:code]
{
  "codes": [
    {
      "code": "Analytics analytics = new Analytics.Builder(context, writeKey)\n  .trackApplicationLifecycleEvents()\n  .build();\n\nAnalytics.with(context).track(\"Product Viewed\", new Properties().putValue(\"name\", \"Opened Loot Crate\"));\n",
      "language": "java"
    },
    {
      "code": "val analytics = Analytics.Builder(context, writeKey)\n  .trackApplicationLifecycleEvents()\n  .build()\n\nAnalytics.with(context).track(\"Product Viewed\", Properties().putValue(\"name\", \"Opened Loot Crate\"))",
      "language": "kotlin"
    }
  ]
}
[/block]
This example Track call tells us that your user just triggered the Product Viewed event with a name of “Opened Loot Crate.”

The Track call properties can be anything you want to record, for example:


[block:code]
{
  "codes": [
    {
      "code": "Analytics.with(context).track(\"Purchased Item\", new Properties().putValue(\"sku\", \"13d31\").putRevenue(199.99));",
      "language": "java"
    },
    {
      "code": "Analytics.with(context).track(\"Purchased Item\", Properties().putValue(\"sku\", \"13d31\").putRevenue(199.99))\n",
      "language": "kotlin"
    }
  ]
}
[/block]

The Track call includes the following fields:


[block:parameters]
{
  "data": {
    "0-0": "name String,required",
    "0-1": "A name for the tracked action.",
    "1-1": "A map of properties for this action, e.g. revenue if the action was a purchase.",
    "2-1": "Extra options for the call.",
    "2-0": "options Options,optional",
    "1-0": "properties Properties,optional"
  },
  "cols": 2,
  "rows": 3
}
[/block]

[block:api-header]
{
  "title": "Action Handling"
}
[/block]
The Snapyr Action handler provides ability to responsd to action events sent by Snapyr (waiting for response from the Snapyr server before opening an offer or welcome popup, for example).
You can add action callback when configuring Analytics:
[block:code]
{
  "codes": [
    {
      "code": "Analytics analytics = new Analytics.Builder(context, writeKey)\n  .defaultOptions(defaultOptions)\n  .actionHandler(new SnapyrActionHandler() {\n    @Override\n    public void handleAction(SnapyrAction action) {\n      Toast.makeText(this, \"Action received: \" + action.action}, Toast.LENGTH_SHORT).show()\n    }\n  })",
      "language": "java"
    },
    {
      "code": "",
      "language": "kotlin"
    }
  ]
}
[/block]

[block:api-header]
{
  "title": "Screen"
}
[/block]
The Screen method lets you you record whenever a user sees a screen of your mobile app, along with optional extra information about the page being viewed.

You’ll want to record a screen event an event whenever the user opens a screen in your app. This could be a view, fragment, dialog or activity depending on your app.

Not all services support screen, so when it’s not supported explicitly, the screen method tracks as an event with the same parameters.

Example screen call:
[block:code]
{
  "codes": [
    {
      "code": "// category \"Feed\" and a property \"Feed Length\"\nAnalytics.with(context).screen(\"Feed\", new Properties().putValue(\"Feed Length\", \"26\"));\n\n// no category, name \"Photo Feed\" and a property \"Feed Length\"\nAnalytics.with(context).screen(null, \"Photo Feed\", new Properties().putValue(\"Feed Length\", \"26\"));\n\n// category \"Smartwatches\", name \"Purchase Screen\", and a property \"sku\"\nAnalytics.with(context).screen(\"Smartwatches\", \"Purchase Screen\", new Properties().putValue(\"sku\", \"13d31\"));\n\n// no category, name \"Photo Feed\" and a property \"Feed Length\"\nAnalytics.with(context).screen(null, \"Photo Feed\", new Properties().putValue(\"Feed Length\", \"26\"));\n\n// category \"Smartwatches\", name \"Purchase Screen\", and a property \"sku\"\nAnalytics.with(context).screen(\"Smartwatches\", \"Purchase Screen\", new Properties().putValue(\"sku\", \"13d31\"));\n",
      "language": "java"
    },
    {
      "code": "",
      "language": "kotlin"
    }
  ]
}
[/block]

The screen call has the following fields:


[block:parameters]
{
  "data": {
    "0-0": "category String,optional*",
    "0-1": "A category for the screen. Optional if name is provided.",
    "1-1": "A name for the screen. Optional if category is provided.",
    "2-1": "A map of properties for this screen.",
    "3-1": "Extra options for the call.",
    "3-0": "options Options,optional",
    "2-0": "properties Properties,optional",
    "1-0": "name String,optional*"
  },
  "cols": 2,
  "rows": 4
}
[/block]
Find details on the Screen payload in the Snapyr Screen call spec.

###Automatic Screen Tracking

The Snapyr SDK can automatically instrument screen calls, using the label of the activity you declared in the manifest as the screen’s name. Fragments and views do not trigger screen calls automatically, however you can manually call the Screen method for these.




[block:code]
{
  "codes": [
    {
      "code": "Analytics analytics = new Analytics.Builder(context, writeKey)\n  .recordScreenViews()\n  .build();",
      "language": "java"
    },
    {
      "code": "",
      "language": "kotlin"
    }
  ]
}
[/block]

[block:api-header]
{
  "title": "Group"
}
[/block]
Group calls let you associate an identified user user with a group. A group could be a company, organization, account, project or team! It also lets you record custom traits about the group, like industry or number of employees.

This is useful for tools like Intercom, Preact and Totango, as it ties the user to a group of other users.

Example group call:
[block:code]
{
  "codes": [
    {
      "code": "Analytics.with(context).group(\"a user's id\", \"a group id\", new Traits().putEmployees(20));\n",
      "language": "java"
    }
  ]
}
[/block]
The group call has the following fields:


[block:parameters]
{
  "data": {
    "0-0": "userId String,required",
    "0-1": "The database ID for this user.",
    "1-1": "The database ID for this group.",
    "2-1": "A map of traits about the group, such as the number of employees, industry, etc.",
    "3-1": "Extra options for the call.",
    "3-0": "options Options,optional",
    "2-0": "traits Traits,optional",
    "1-0": "groupdId String,required"
  },
  "cols": 2,
  "rows": 4
}
[/block]
Find more details about the Group method, including the Group call payload, in the Snapyr Group call spec.


[block:api-header]
{
  "title": "Alias"
}
[/block]
Alias is how you associate one identity with another. This is an advanced method, but it is required to manage user identities successfully in some Snapyr destinations, such as Mixpanel or Kissmetrics.

Mixpanel used the Alais call to associate an anonymous user with an identified user once they sign up. For Kissmetrics, if your user switches IDs, you can use ‘alias’ to rename the ‘userId’.

Example alias call:


[block:code]
{
  "codes": [
    {
      "code": "Analytics.with(context).alias(newId);\nAnalytics.with(context).identify(newId);",
      "language": "java"
    }
  ]
}
[/block]
The alias call has the following fields:

newId String,required	The new ID to track this user with.
options Options,optional	Extra options for the call.
For more details about alias, including the alias call payload, check out the Snapyr Alais call spec.

Note that the previousId is the value passed in as the userId, which Snapyr cached after you made an identify call. Snapyr passes that value as the previousId when you call alias and pass in a newId. If you have not called identify, the previousId is set to the anonymousId.

#Context
Context is a dictionary of extra information you can provide about a specific API call. You can add any custom data to the context dictionary that you want to have access to in the raw logs. Some keys in the context dictionary have semantic meaning and are collected for you automatically, such as information about the user’s device.


[block:code]
{
  "codes": [
    {
      "code": "AnalyticsContext analyticsContext = Analytics.with(context).getAnalyticsContext();\nanalyticsContext.putValue(...).putReferrer(...).putCampaign(...);\n",
      "language": "java"
    }
  ]
}
[/block]
You can read more about these special fields in the Snapyr Common spec documentation.

To alter data specific to the device object you can use the following:

[block:code]
{
  "codes": [
    {
      "code": "AnalyticsContext analyticsContext = Analytics.with(context).getAnalyticsContext();\nanalyticsContext.device().putValue(\"advertisingId\", \"1\");\n",
      "language": "java"
    }
  ]
}
[/block]
To opt out of automatic data collection, clear the context after initializing the client. Do this BEFORE you send any events.


[block:code]
{
  "codes": [
    {
      "code": "Analytics analytics = new Analytics.Builder(context, writeKey).defaultOptions(defaultOptions).build();\nAnalyticsContext context = getAnalyticsContext();\ncontext.clear();\n",
      "language": "text"
    }
  ]
}
[/block]

#Utility methods
##Retrieve AnonymousId
You can retrieve the anonymousId set by the library by using:


[block:code]
{
  "codes": [
    {
      "code": "Analytics.with(context).getAnalyticsContext().traits().anonymousId();\n",
      "language": "java"
    }
  ]
}
[/block]
##Reset
The reset method clears the SDK’s internal stores for the current user and group. This is useful for apps where users log in and out with different identities on the same device over time.

The example code below clears all information about the user.


[block:code]
{
  "codes": [
    {
      "code": "Analytics.with(context).reset();\n",
      "language": "java"
    }
  ]
}
[/block]
Reset does not clear events in the queue, and any remaining events in the queue are sent the next time the app starts. You might want to call Flush before you call Reset.

Note: When you call reset, the next time the app opens Snapyr generates a new AnonymousId. This can impact the number of Monthly Tracked Users (MTUs) you process.

##Collecting Stats
Local device stats help you quickly see how many events you sent to Snapyr, the average time bundled destinations took to run, and similar metrics.
[block:code]
{
  "codes": [
    {
      "code": "StatsSnapshot snapshot = Analytics.with(context).getSnapshot();\nlog(snapshot.integrationOperationAverageDuration);\nlog(snapshot.flushCount);\n",
      "language": "text"
    }
  ]
}
[/block]
##Adding debug logging
If you run into issues while using the Android library, you can enable logging to help trace the issue. Logging also helps you see how long destinations take to complete their calls so you can find performance bottlenecks.

The logging is enabled by default in the default singleton instance if your application is running in debug mode. If you use a custom instance, attach a LogLevel to the Builder and set the logging level there, as in the example below.
[block:code]
{
  "codes": [
    {
      "code": "Analytics analytics = new Analytics.Builder(context, writeKey).logLevel(LogLevel.VERBOSE)...build();\n",
      "language": "text"
    }
  ]
}
[/block]
You can choose to disable logging completely (LogLevel.NONE), enable basic logging for the SDK (LogLevel.BASIC), enable basic logging for Device-mode destination (LogLevel.INFO), or simply log everything (LogLevel.VERBOSE).

Snapyr recommends that you turn logging off in production modes of your app.

##Privacy methods
###Opt-out
Depending on the audience for your app (for example, children) or the countries where you sell your app (for example, the EU), you may need to offer the ability for users to opt-out of analytics data collection inside your app. You can turn off ALL destinations including Snapyr itself:


[block:code]
{
  "codes": [
    {
      "code": "Analytics.with(this).optOut(true);\n",
      "language": "java"
    }
  ]
}
[/block]
Set the opt-out status for the current device and analytics client combination. This flag persists across device reboots, so you can call it once in your application, such as in a screen where a user can opt out of analytics tracking.


###Anonymizing IP
The Snapyr iOS, Android, and JavsScript Analytics.js libraries automatically derive and set the IP address for events recorded on the user’s device. The IP is not collected on the device itself, but instead is filled in by Snapyr's servers when they receive a message.

To prevent Snapyr from recording the users’ IP in destinations and S3, you can set the event’s context.ip field to 0.0.0.0. The Snapyr servers won’t overwrite this data if it comes from the client, and so do not record the IP address of the client.