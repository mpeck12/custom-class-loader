Code Execution Demonstration App
================================

This application, based on the sample custom class loading application
described in [1] and the hello-jni sample application found in the Android NDK [2],
demonstrates the ability to download and execute Dalvik bytecode
and native code from arbitrary websites.

We added the ability for the application to deliberately perform several
poor security practices:
* Use of plaintext http rather than https to download code, enabling
  susceptibility to man-in-the-middle attacks
* Toggle use of an insecure X509TrustManager that does not validate
  the server's X.509 certificate when connecting over https, enabling
  susceptibility to man-in-the-middle attacks
* Toggle storing downloaded files as world-readable and
  world-writable, opening the files up to manipulation
  by other applications installed on the device

Our motivation was to test the effectiveness of checks that we were
developing for the Android lint tool, as well as to test the effectiveness
of Android operating system platform security improvements that we were
proposing.

Instructions for use:

1. Compile the application:
gradlew build

2. Compile the external Dalvik code (.jar file):
gradlew assembleExternalJar

3. Place the generated .jar file and .so files on a web server.
.jar file: libraries/lib1/build/outputs/secondary_dex.jar
.so files: appjni/build/intermediates/ndk/release/lib/ (Place all of the subdirectories for all of the architectures on the web server)
where <arch> depends on the architecture of the mobile device that the
application will be running on

4. Start the application, and configure its settings to point
to the URL of the .jar (DEX) file and the URL that contains the .so (JNI) files
(the app will automatically append <architecture>/libhello-jni.so to the JNI URL).

[1] http://android-developers.blogspot.com/2011/07/custom-class-loading-in-dalvik.html  
[2] http://developer.android.com/ndk/samples/sample_hellojni.html

OLD README CONTENTS
===================

Port of the work in the [following blog](http://android-developers.blogspot.jp/2011/07/custom-class-loading-in-dalvik.html) 
to the new Gradle based Android Studio build system, as per [this thread on StackOverflow](http://stackoverflow.com/questions/18174022/custom-class-loading-in-dalvik-with-gradle-android-new-build-system/27241083#27241083)

As the Android Studio Gradle plugin now provides [native multidex support](https://developer.android.com/tools/building/multidex.html),
which effectively solves the Android 65k method limit, the main motivation for using custom class loading at runtime is now 
extensibility. In my particular case, I'm trying to make a [plugin framework for AnkiDroid](http://stackoverflow.com/questions/10239596/plugins-architecture-for-an-android-app).

Therefore the main focus of this version of the project is on building the secondary jar file in a clean and modular manner,
which makes it easy to update the main project without having to update the plugins. The main apk is in the app module, and the library which shows the toast is in the libraries/lib1 module with its own namespace `com.example.toastlib`

You can compile the .jar plugin file for the library using the `assembleExternalJar` task -- e.g. from the command line:

`gradlew assembleExternalJar`

which will generate the file `/libraries/lib1/build/outputs/com.example.toastlib.jar` which you can then copy to the sdcard on your device for the main app to import. 

To compile the main app it's currently necessary to make the following two changes to the build configuration:

 * change the first line of '/app/build.gradle' to `apply plugin: 'com.android.application'`
 * remove `':libraries:lib1'` from settings.gradle
