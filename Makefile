#!/usr/bin/env make -f
all:
	ant -emacs debug

run: all

dbg: all
	adb shell 'su - 0 -c "mount -o remount,rw /system"'
	adb push bin/NetworkLocation-debug.apk /sdcard/.
	adb shell 'su - 0 -c "cp /sdcard/NetworkLocation-debug.apk /system/app/NetworkLocation.apk"'
	adb shell 'su - 0 -c "rm /sdcard/NetworkLocation-debug.apk"'

clean:
	rm -rf bin/ gen/
