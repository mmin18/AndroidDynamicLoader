## Android Dynamic Loader

#### Legacy projects that shows how dynamic loader works.

Sample projects to load dex file (dalvik executable code) and res file (android resources, probably packaged in the same apk file) into the current android runtime environment.

## ActivityLoader
You can develop a new version of SampleActivity in project ActivityDeveloper, generate a apk to ActivityLoader/assets/apks/ and launch that SampleActivity in ActivityLoader.

The SampleActivity cannot have it's own resources.

## FragmentLoader
You can develop com.dianping.example.SampleFragment in project FragmentDeveloper, generate a apk to FragmentLoader/assets/apks/ and launch that apk in FragmentLoader.

The SampleFragment can also load resources.