#!/usr/bin/env make -f
# see https://github.com/microg/NetworkLocation
# see http://my.fit.edu/~vkepuska/ece5570/adt-bundle-windows-x86_64/sdk/sources/android-17/com/android/server/LocationManagerService.java
APP=/home/mdt/Source/emdete/android/img/gapps-jb-20121212-signed/system/app/NetworkLocation.apk

all: build.xml AndroidManifest.xml
	JAVA_HOME=/usr/lib/jvm/java-6-sun-1.6.0.26 ANDROID_HOME=/usr/local/android/sdk \
	ant -emacs debug

dbg: all
	/usr/local/android/sdk/platform-tools/adb install -r bin/WirelessLocation-debug.apk

#build.xml: Makefile
#	/usr/local/android/sdk/tools/android create project --package org.pyneo.location --path . --activity WirelessLocation --target android-18
#	rm AndroidManifest.xml
#
#AndroidManifest.xml: NetworkLocation/AndroidManifest.xml
#	xmllint -format $< > $@
#
#NetworkLocation/AndroidManifest.xml: $(APP)
#	java -jar /usr/local/android/baksmali/lib/apktool.jar decode $(APP)
#
