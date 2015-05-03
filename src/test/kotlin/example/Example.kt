package example

import nl.mplatvoet.kotlin.komponents.progress.Progress
import nl.mplatvoet.kotlin.komponents.progress.percentage

fun main(args: Array<String>) {
    val progress = Progress()
    progress.onUpdate { p->
        println("${p.percentage}%")
    }

    val sub1 = progress.child(0.1)
    val sub2 = progress.child(5)
    val sub2sub1 = sub2.child()
    val sub2sub2 = sub2.child()
    val sub3 = progress.child()
    val sub4 = progress.child(2)

    sub1.value = 0.25
    sub1.value = 0.50
    sub1.value = 0.75
    sub1.value = 1.0

    sub2sub1.completed = true
    sub2sub2.value = 0.5
    sub2sub2.value = 1.0

    sub3.value = 0.25
    sub3.value = 0.50
    sub3.value = 0.75
    sub3.value = 1.0

    sub4.value = 0.25
    sub4.value = 0.50
    sub4.value = 0.75
    sub4.value = 1.0
}

