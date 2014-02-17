WirelessLocation
================

This is a network location provider for Android. It aims to be a replacement
for the same service provided by Google.

Installation
============

As the google App for this service it a system-app and furtheron only
system-apps are allowed to install a location-provider and only a single
network-location-provider may be installed you need an Android with no such app
installed. A plain cyanogenmod without gapps will do. After you have installed
this app you have to make it a system app. You can do so by one of the nice
apps for that (for example "/system/ app mover" which is avaiable free & open
source) or make it "by hand". You just get a shell on the device (`adb shell`),
get root (`su`), remount the system directory rewritable (`mount -o remount,rw
/system`) and copy the app there (`cp
/data/app/com.google.android.location-1.apk
/system/app/com.google.android.location-1.apk`). After that you have to reboot
and clean the dalvik cache.

Building
========

This app is plain old stupid, no mvn, no gradl, only ant is needed.

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

