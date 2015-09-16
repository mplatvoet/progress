#Progress
Track progress for [Kotlin](http://kotlinlang.org)

[![CircleCI branch](https://img.shields.io/circleci/project/mplatvoet/progress/master.svg)](https://circleci.com/gh/mplatvoet/progress/tree/master) [![Maven Central](https://img.shields.io/maven-central/v/nl.komponents.progress/progress.svg)](http://search.maven.org/#browse%7C-300825966) [![DUB](https://img.shields.io/dub/l/vibe-d.svg)](https://github.com/mplatvoet/progress/blob/master/LICENSE)

```kt
//private part
val control = Progress.control()

//public part
val progress = control.progress

//get notified on updates
progress.update {
	println("${value}")
}

//set value
control.value = 0.25
control.value = 0.50
control.value = 0.75
control.value = 1.0
```

## Getting started
This version is build against `kotlin-stdlib:0.13.xxxx`.

###Gradle
```groovy
dependencies {
    compile 'nl.komponents.progress:progress:0.3.+'
}
```

###Maven
```xml
<dependency>
	<groupId>nl.komponents.progress</groupId>
	<artifactId>progress</artifactId>
	<version>[0.3.0, 0.4.0)</version>
</dependency>
```