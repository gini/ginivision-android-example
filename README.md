GiniVision SDK for Android Example App
======================================

This is a example app for the GiniVision SDK for Android. See
http://developer.gini.net/ginivision-android/html/index.html for
further documentation.

Before You Can Run The App
--------------------------

You have to edit the resource file *src/main/res/values/strings.xml*
and replace the values of the *gini_api_client_id* and
*gini_api_client_secret* with valid credentials. Please send a mail
to technical-support@gini.net if you didn't have received credentials
yet.

Build & Install the App
-----------------------

Go into the project directory and enter:

```
   $ ./gradlew assembleDebug
   $ ./gradlew installDebug
```

