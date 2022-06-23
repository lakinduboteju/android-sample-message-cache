# Skylink Message Cache SDK - Demo application

The application which uses the latest Skylink Message Cache SDK for Android demonstrates the use of persistent message caching feature.

The architecture of the app is the way we organize the code to have a clear structure. We try to separate between the application layer and the SDK usage layer.

With the separated parts, the user can easily change each part without changing the others and extend the functionality of the application.
For example, the user can using different view components to display GUI of the application while keeping the same logics of using the SDK.

The **MVP (Model - View - Presenter)** architecture used in the app mainly divided into three main parts : View (UI) - Presenter - Service

* `ChatUI` : Responsible for displaying GUI and getting user events.
* `ChatPresenter` : Responsible for communicating with the CharService to get the job done. Acts as the interface between the view and CharService.
* `ChatService` : Responsible for communicating with Skylink SDKs.

Implementation of the app is divided in to 2 Gradle modules.

* `app` : Implements the UI and app logic
* `skylink_sample` : Contains Java class implementations taken from [skylink-android-sample](https://github.com/Temasys/skylink-android-sample) project.

ChatService implementation is completely taken from the [skylink-android-sample](https://github.com/Temasys/skylink-android-sample) project and modified slightly to enable message caching feature.

## How to run the sample project

### STEP 1
Clone this repository.

### STEP 2
Import the project into Android Studio with File -> Open and select the project.

### STEP 3
Follow the instructions [here](https://temasys.io/creating-an-account-generating-a-key/) to create an App and a key on the Temasys Console.

### STEP 4
Create a copy of `config_example.xml` under `skylink_sample/src/main/res/values` and name it `config.xml` (OR any other name).
You may also choose to not create a new file and edit `config_example.xml` itself as needed.

### STEP 5
Add your preferred values for `app_key` and `app_secret`. An appropriate App key and corresponding secret are required to connect to Skylink successfully.

Instructions for populating the config.xml file can be found in [skylink-android-sample/README.md](https://bitbucket.org/TemasysCommunications/skylink-android-sample/src/master/README.md).

### STEP 6
Build the project.

### STEP 7
Run the app module.

## SDK documentation

* [Skylink Message Cache SDK for Android Reference](https://cdn.temasys.com.sg/skylink/skylinkmessagecachesdk/android/latest/doc/reference/sg/com/temasys/skylink/sdk/messagecache/SkylinkMessageCache.html)


## Implementation details

### 1. Configuring message caching

[skylink_sample/src/main/java/sg/com/temasys/skylink/sdk/sampleapp/service/ChatService.java#Line:126](skylink_sample/src/main/java/sg/com/temasys/skylink/sdk/sampleapp/service/ChatService.java#lines-126)

```java
SkylinkConfig skylinkConfig = new SkylinkConfig();

// Enable message caching (Message caching is disabled by default in SkylinkConfig)
skylinkConfig.setMessageCachingEnable(true);

// Set maximum number of messages that will be cached per Skylink Room (default value is 50)
skylinkConfig.setMessageCachingLimit(100);
```

#### Documentation

* [`void SkylinkConfig.setMessageCachingEnable(boolean)`](https://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/doc/reference/sg/com/temasys/skylink/sdk/rtc/SkylinkConfig.html#setMessageCachingEnable(boolean))

* [`boolean SkylinkConfig.isMessageCachingEnable()`](https://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/doc/reference/sg/com/temasys/skylink/sdk/rtc/SkylinkConfig.html#isMessageCachingEnable())

* [`void SkylinkConfig.setMessageCachingLimit(int)`](https://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/doc/reference/sg/com/temasys/skylink/sdk/rtc/SkylinkConfig.html#setMessageCachingLimit(int))

* [`int SkylinkConfig.getMessageCachingLimit()`](https://cdn.temasys.com.sg/skylink/skylinksdk/android/latest/doc/reference/sg/com/temasys/skylink/sdk/rtc/SkylinkConfig.html#getMessageCachingLimit())

### 2. Get cached messages

[app/src/main/java/sg/com/temasys/skylink/sdk/messagecache/demo/ChatPresenter.java#Line:121](app/src/main/java/sg/com/temasys/skylink/sdk/messagecache/demo/ChatPresenter.java#lines-121)

```java
if ( SkylinkMessageCache.getInstance().isEnabled() ) {
    JSONArray cachedMsgs = SkylinkMessageCache.getInstance().getReadableSession("<your-room-name>").getCachedMessages();
    // Assuming all cached messages are String messages (not JSONObject or JSONArray messages).
    for (int i = 0; i < cachedMsgs.length(); i++) {
        JSONObject cachedMsg = (JSONObject) cachedMsgs.get(i);
        String senderId   = cachedMsg.getString("senderId");
        String msgContent = cachedMsg.getString("data");
        long timestamp    = cachedMsg.getLong("timeStamp");
    }
}
```
