Deprecated
==========

This example app is deprecated. It shows usage of the deprecated Gini Vision Library 1.0.

Please refer to either the example apps in the Gini Vision Library 2.0+ [repository](https://github.com/gini/gini-vision-lib-android) or our new standalone [example app](https://github.com/gini/gini-vision-lib-android-example).

GiniVision SDK for Android Example App
======================================

This is an example app for the GiniVision SDK for Android. See
http://developer.gini.net/ginivision-android/html/index.html for
further documentation.

Before You Can Run The App
--------------------------

You have to edit the resource file *src/main/res/values/strings.xml*
and replace the values of the *gini_api_client_id* and
*gini_api_client_secret* with valid credentials. Please send an email
to technical-support@gini.net if you haven't received credentials
yet.

Resolve dependencies
--------------------

The GiniVision SDK is protected and can be resolved by gradle only if you provide a username and password:

```
   $ ./gradlew -PginiRepoUser="your_username" -PginiRepoPassword="your_password"
```

Build & Install the App
-----------------------

Go into the project directory and enter:

```
   $ ./gradlew assembleDebug
   $ ./gradlew installDebug
```

