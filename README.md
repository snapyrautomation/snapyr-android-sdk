snapyr-android-sdk
=================

snapyr-android-sdk is an Android client for [Snapyr](https://snapyr.com)

# Using the SDK

You can find usage documentation at [https://github.com/snapyrautomation/snapyr-developer-documentation](https://github.com/snapyrautomation/snapyr-developer-documentation)

## Building SDK

To build the sdk you should have JDK 1.8, Android SDK (android-28) installed.

```bash
./gradlew clean
./gradlew check build assembleAndroidTest
```

This will create two files, `snapyr-debug.aar` and `snapyr-release.aar`, in the directory `snapyr/build/output/aar`.

