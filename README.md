WirelessLocation
================

This is a network location provider for Android. It aims to be a replacement
for the same service provided by Google.

Installation
============

As the google solution for this service it a system-app and furtheron only
system-apps are allowed to install a location-provider and only a single
network-location-provider may be installed you need an Android with no such app
installed. A plain cyanogenmod without gapps will do.

The adb Way
-----------

To overwrite the original system-app or to install the app on a gapps-free
device do the following steps: Switch adb to run as root with `adb root`, make
the /system directory writeable with `adb remount`, copy the app to the right
place with `adb push bin/NetworkLocation-debug.apk
/system/app/NetworkLocation.apk`, clean the dalvik cache with `adb shell rm
/cache/dalvik-cache/system@app@NetworkLocation.apk@classes.dex` and reboot your
device with `adb shell reboot` or by the gui.

The GUI Way
-----------

If you have a gapps free device you can jump over this step. To
deinstall the original app move it out of /system to be not a
system-app anymore (for example with "/system/ app mover" which is avaiable
free & open source), deinstall the app, reboot and then follow the next steps.

You can use adb to install the app (`adb install NetworkLocation-debug.apk`) or
install it from an url. You have to allow to install apps from unknown places
in that case. After you have installed this app you have to make it a system
app. You can do so by one of the nice apps for that (for example "/system/ app
mover" which is avaiable free & open source).

The original app may have different names. Common ones are NetworkLocation.apk
or com.google.android.location-1.apk. The app-name in the GUI is
NetworkLocation or (german) Netzwerkstandort.

Debugging
=========

The app does alot of debug logging but that level must be enabled. you can do
so by issuing `setprop log.tag.nlp DEBUG` in a root-shell on the device.

Building
========

This app is plain old stupid, no mvn, no gradle, only ant is needed.

License
=======

The app itself is published under GPLv3. Feel free to use the code in you app
as long as you follow the rules. I would be more than happy to recieve notice
if you do so while i do not require so. For further information see the file
COPYING.

WARRANTIES
==========

There are no WARRANTIES, neither for the software, nor the build process, or
the information or anything around the project. If you start using this
software you agree that you are competent to do so and are responsible for any
kind of results doing so.

Acknowledgment
==============

The base for a location-provider is badly documented. So i used a different,
wonderful project named μg which comes with a network location provider. Thanks
alot for the support and the work to mar-v-in!

