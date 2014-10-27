# Android Dynamic Loader

Android Dynamic Loader is a plugin system. The host application is like a browser, but instead of load web pages, it load plugins which runs natively on Android system.

You can download the demo from <https://github.com/mmin18/AndroidDynamicLoader/raw/master/host.apk> (35k).

![HelloWorldDemo](https://raw.github.com/mmin18/AndroidDynamicLoader/master/HelloWorldDemo.png)

![BitmapFunDemo](https://raw.github.com/mmin18/AndroidDynamicLoader/master/BitmapFunDemo.png)

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

## About UI Container

In a normal Android application, we use Activity as the root UI container. But since Activity is registered in AndroidManifest.xml, and we can't modify manifest in runtime, we must find an alternative UI container - Fragment.

The Fragment itself, introduced in Android 3.0 Honeycomb, is a perfect UI container, and it has lifecycle and state management.

Once the plugin and its dependency is downloaded, an Activity (MainActivity.java) will be started, create an instance of the specific fragment, and add the fragment into the root view.

See the [HelloFragment.java](https://github.com/mmin18/AndroidDynamicLoader/blob/master/workspace/sample.helloworld/src/sample/helloworld/HelloFragment.java) sample.

## About URL Mapping

Since we use Fragment as UI container, each page is implemented in Fragment instead of Activity. So how do we start a new page?

We use URL, just like a browser does. For instance, in a browser, we open `http://mydomain.com/helloworld.html`. In plugins, we open `app://helloworld`.

	Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("app://helloworld"));
	startActivity(i);

Each host is mapped to a single fragment, you define the url mapping table in **project/fragment.properties**.

See the [helloworld fragment.properties](https://github.com/mmin18/AndroidDynamicLoader/blob/master/workspace/sample.helloworld/fragment.properties) sample.

## About Resources

In the plugins, we pack resources and codes in the same package. We use R.java as the index of resources.

But instead of using **context.getResources()**, we use **MyResources.getResource(Me.class)** to get the resources which in the same package as **Me.class**.

Here is a sample in HelloFragment.java

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		// MyResources manages the resources in specific package.
		// Using a Class object to obtain an instance of MyResources.

		// In this case, hello.xml is in the same package as HelloFragment class

		MyResources res = MyResources.getResource(HelloFragment.class);

		// Using MyResources.inflate() if you want to inflate some layout in
		// this package.
		return res.inflate(getActivity(), R.layout.hello, container, false);

	}

You can use MyResources to get drawable, string, or inflate layout.

## Folders

`/Host` contains the host application (build as host.apk).

`/tools/update.sh` checks your environment and helps you config your plugins. You should always run it once after git clone or create a new plugin.

`/workspace/sample.helloworld` is a most simple plugin.
`/workspace/sample.helloworld/fragment.properties` defines the url mapping of your fragments.

`/site/***/site.txt` is the definition file for all the plugins files, dependency and fragments url mapping table.

