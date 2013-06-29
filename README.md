# Android Dynamic Loader

Android Dynamic Loader is a plugin system. The host application is like a browser, but instead of load web pages, it load plugins which runs natively on Android system.

You can download the demo from <https://github.com/mmin18/AndroidDynamicLoader/raw/master/host.apk>

## How to run the sample plugins

The sample plugins is under **workspace** folder, but do not try to run them directly, it won't start.

First you need to install **host.apk** on your phone (or you can build the Host project yourself)

Also you need to make sure the Android SDK and Ant is installed and android-sdk/tools, android-sdk/platform-tools, ant is in your **PATH**.

Run the following commands:

	chmod +x tools/update.sh
	tools/update.sh workspace
	cd workspace
	ant run

If it shows "device not found", make sure your phone is connected or simulator is running. "adb devices" will tell.

Since we don't specific a default entry in **workspace.properties**, it will popup a window and let you choose one. I suggest bitmapfun.

## About URL Mapping

// TODO:

## Folders

**/Host** contains the host application (build as host.apk).

**/tools/update.sh** checks your environment and helps you config your plugins. You should always run it once after git clone or create a new plugin.

**/workspace/sample.helloworld** is a most simple plugin.
**/workspace/sample.helloworld/fragment.properties** defines the url mapping of your fragments.

