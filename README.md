##PermissionDispatcher
rewrite the <a href="https://github.com/hotchemi/PermissionsDispatcher">PermissionsDispatcher<a/> for Java
and add code for "Xiaomi" phone;



## Download

To add it to your project, include the following in your **project** `build.gradle` file:

```groovy
buildscript {
  dependencies {
    classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'
  }
}
```

And on your **app module** `build.gradle`:

`${latest.version}` is [![Download](https://api.bintray.com/packages/a10188755550/maven/permissiondispatcher/images/download.svg)](https://bintray.com/a10188755550/maven/permissiondispatcher/_latestVersion)

```groovy
apply plugin: 'android-apt'

dependencies {
  compile 'org.jokar:permissiondispatcher:${latest.version}'
  apt 'org.jokar:permissiondispatcher-processor:${latest.version}'
}

repositories {
    maven {
        url 'https://dl.bintray.com/a10188755550/maven' 
    }
}
```

## Licence

```
Copyright 2016 Shintaro Katafuchi, Marcel Schnelle, Yoshinori Isogai,JokAr

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```