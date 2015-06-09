#Progress usage documentation

Progress is a small utility for tracking progress in Kotlin. It is written with concurrency scenarios in mind and 
is thus entirely thread safe. Progress tracking consists of two separate parts:

- `Progress` for receiving updates
- `ProgressControl` for changing the state.

##ProgressControl
A `ProgressControl` allows you to mutate the state, ergo setting the progress value, for the associated `Progress`.
There are two types of `ProgressControl`s:

- a single value control `SingleProgressControl`
- a container control `ContainerProgressControl`

A `SingleProgressControl` allows you to directly set a value
```kt
val control = Progress.control()

control.value = 1.0 // must be between 0.0 and 1.0 (inclusive)
```

A `ContainerProgressControl` allows you to depend on more than one `Progress` instances

```kt
val control = Progress.containerControl()

// create a SingleProcessControl as a child
// of this control
val one = control.child()

// create a ContainerProcessControl as a child
// of this control
val second = control.containerChild()

// another child from the second
// container with a custom weight 
// for determining the impact on 
// overall progress
val third = second.child(0.5)

// so this weighs heavy on the progress
val fourth = second.child(2.0)
```

##Progress
Progress is the client part of progress tracking. You can register a callback for receiving updates on progress or
just query the current state. This can be shared across threads. You obtain a `Progress` instance from a `ProgressControl`
instance.

```kt
val control = //...
val progress = control.progress

progress.update {
  //called upon updates
}

// current value between 0.0 and 1.0 (inclusive)
progress.value 

// boolean value that is true when progress.value == 1.0
progress.done

```



##Example

```kt
val masterControl = Progress.containerControl()
masterControl.progress.update {
    println("${value}")
}

val firstChild = masterControl.child(0.1)
val secondChild = masterControl.containerChild(5.0)
val secondChildFirstChild = secondChild.child()
val secondChildSecondChild = secondChild.child()
val thirdChild = masterControl.child()
val fourthChild = masterControl.child(2.0)

firstChild.value = 0.25
firstChild.value = 0.50
firstChild.value = 0.75
firstChild.value = 1.0

secondChildFirstChild.markAsDone()
secondChildSecondChild.value = 0.5
secondChildSecondChild.value = 1.0

thirdChild.value = 0.25
thirdChild.value = 0.50
thirdChild.value = 0.75
thirdChild.value = 1.0

fourthChild.value = 0.25
fourthChild.value = 0.50
fourthChild.value = 0.75
fourthChild.value = 1.0

```