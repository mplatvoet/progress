package example

import nl.mplatvoet.komponents.progress.OutOfRangeException
import nl.mplatvoet.komponents.progress.progressControl
import java.text.DecimalFormat
import kotlin.properties.ReadOnlyProperty

fun main(args: Array<String>) {
    val control = progressControl()
    control.progress.update {
        println("${value.percentage}%")
    }

    val sub1 = control.createChild(0.1)
    val sub2 = control.createChild(5.0)
    val sub2sub1 = sub2.createChild()
    val sub2sub2 = sub2.createChild()
    val sub3 = control.createChild()
    val sub4 = control.createChild(2.0)

    sub1.value = 0.25
    sub1.value = 0.50
    sub1.value = 0.75
    sub1.value = 1.0

    sub2sub1.markAsDone()
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

private val percentageFormat by ThreadLocalVal { DecimalFormat("##0.00") }

val Double.percentage: String
    get() = if (this in (0.0..1.0)) percentageFormat.format(this * 100) else throw OutOfRangeException("[$this] must be within bounds (0.0 .. 1.0)")


private class ThreadLocalVal<T>(private val initializer: () -> T) : ReadOnlyProperty<Any?, T> {
    private val threadLocal = object : ThreadLocal<T>() {
        override fun initialValue(): T = initializer()
    }

    public override fun get(thisRef: Any?, desc: PropertyMetadata): T = threadLocal.get() : T
}

