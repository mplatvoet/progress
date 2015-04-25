package nl.mplatvoet.kotlin.komponents.progress

import java.text.DecimalFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.properties.ReadOnlyProperty


public class OutOfRangeException(msg: String, cause: Throwable? = null) : IllegalArgumentException(msg, cause)

public class Progress() {
    private val children = ConcurrentHashMap<Progress, Double>()
    private val callbacks = ConcurrentLinkedQueue<(Double) -> Unit>()

    public var completed: Boolean
        get() = value >= 1.0
        set(suggestedValue) {
            //checking whether this Progress object is managed by children is not thread safe.
            //it's just a way to catch misuse of the API
            if (!children.isEmpty()) throw IllegalStateException("children manage the state of this Progress object")

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
            if (!children.isEmpty()) throw IllegalStateException("children manage the state of this Progress object")
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
        if (weight < 0.0) throw IllegalArgumentException("weight can not be negative")
        val child = Progress()
        children.put(child, weight)
        child.onUpdate { updateValue() }
        return child
    }

    private fun updateValue() {
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


private val percentageFormat by ThreadLocalVal { DecimalFormat("##0.00") }

val Double.percentage: String
    get() = if (this in (0.0..1.0)) percentageFormat.format(this * 100) else throw OutOfRangeException("[$this] must be within bounds (0.0 .. 1.0)")



private class ThreadLocalVal<T>(private val initializer: () -> T) : ReadOnlyProperty<Any?, T> {
    private val threadLocal = object : ThreadLocal<T>() {
        override fun initialValue(): T = initializer()
    }

    public override fun get(thisRef: Any?, desc: PropertyMetadata): T = threadLocal.get() : T
}