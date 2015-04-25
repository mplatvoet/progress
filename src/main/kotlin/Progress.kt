package nl.mplatvoet.kotlin.komponents.progress

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.ConcurrentLinkedQueue
import java.text.DecimalFormat
import nl.mplatvoet.kotlin.komponents.properties.delegates.ThreadSafeDelegates

/**
 * Created by mplatvoet on 12-3-14.
 */


public class OutOfRangeException(msg: String, cause: Throwable? = null) : IllegalArgumentException(msg, cause)

public class Progress() {
    private val children = ConcurrentHashMap<Progress, Double>()
    private val callbacks = ConcurrentLinkedQueue<(Double) -> Unit>()

    public var completed: Boolean
        get() = value >= 1.0
        set(suggestedValue) {
            //checking whether this Progress object is managed by children is not thread safe.
            //it's just a way to catch misuse of the API
            if (!children.empty) throw IllegalStateException("children manage the state of this Progress object")

            when {
                suggestedValue -> value = 1.0
                value > 0.0 -> throw IllegalStateException("Progress has been updated by value")
            }
        }

    public var value: Double
        get() = atomicVal.get()!!
        set(suggestedValue) {
            //checking whether this Progress object is managed by children is not thread safe.
            //it's just a way to catch misuse of the API
            if (!children.empty) throw IllegalStateException("children manage the state of this Progress object")
            if (suggestedValue !in (0.0..1.0)) throw OutOfRangeException("[$value] must be within bounds (0.0 .. 1.0)")


            var notify: Boolean
            do {
                val currentVal = atomicVal.get()
                notify = currentVal != suggestedValue
            } while (!atomicVal.compareAndSet(currentVal, suggestedValue))
            if (notify) notifyUpdate()
        }

    private val atomicVal = AtomicReference(0.0)

    fun child(weight: Int): Progress = child(weight.toDouble())

    fun child(weight: Double = 1.0): Progress {
        if (weight < 0.0) throw IllegalArgumentException("weigth can not be negative")
        val child = Progress()
        children.put(child, weight)
        onUpdate { updateValue() }
        return child
    }

    private fun updateValue(): Unit {
        var notify: Boolean
        do {
            val currentVal = atomicVal.get()
            val newValue = calculateProgressFromChildren()
            notify = newValue != value
        } while (!atomicVal.compareAndSet(currentVal, newValue))

        if (notify) notifyUpdate()
    }

    private fun calculateProgressFromChildren(): Double {
        var totalWeight = 0.0
        var totalWeightValue = 0.0
        for ((p, w) in children) {
            totalWeight += w
            totalWeightValue += value * w
        }
        return if (totalWeight > 0.0 && totalWeightValue > 0.0) totalWeightValue / totalWeight else 0.0
    }


    private fun notifyUpdate() {
        //use local var to ensure every listener at least gets the same
        //value notified
        val notifyVal = value
        callbacks.forEach { it(notifyVal) }
    }

    fun onUpdate(notifyOnAdd: Boolean = true, fn: (Double) -> Unit) {
        //Could miss an event, should record what's been called already
        if (notifyOnAdd) fn(value)
        callbacks add fn
    }
}


private val percentageFormat by ThreadSafeDelegates.threadLocalVal { DecimalFormat("##0.00") }

val Double.percentage: String
    get() = if (this in (0.0..1.0)) percentageFormat.format(this * 100) else throw OutOfRangeException("[$this] must be within bounds (0.0 .. 1.0)")


fun main (args: Array<String>) {
    val progress = Progress()
    progress.onUpdate {
        println("${it.percentage}%")
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