/*
 * Copyright (c) 2015 Mark Platvoet<mplatvoet@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package nl.komponents.progress

import java.util.ArrayList
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference


public fun concreteProgressControl(executor: (progress: Progress, body: Progress.() -> Unit) -> Unit): ProgressControl {
    return JvmProgress(executor)
}


private class JvmProgress(override val executor: (progress: Progress, body: Progress.() -> Unit) -> Unit) : ProgressControl, Progress {
    private val childProgresses = ConcurrentLinkedQueue<ChildProgress>()
    private val callbacks = ConcurrentLinkedQueue<Callback>()
    private val atomicVal = AtomicReference(0.0)

    override val progress: Progress
        get() = this

    override fun markAsDone() {
        value = 1.0
    }

    public override val done: Boolean
        get() = value >= 1.0


    public override var value: Double
        get() = atomicVal.get()
        set(suggestedValue) {
            //checking whether this Progress object is managed by children is not thread safe.
            //it's just a way to catch misuse of the API
            if (!childProgresses.isEmpty()) throw IllegalStateException("children manage the state of this Progress object")
            if (suggestedValue !in (0.0..1.0)) throw OutOfRangeException("[$value] must be within bounds (0.0 .. 1.0)")


            var notify: Boolean
            do {
                val currentVal = atomicVal.get()
                notify = currentVal != suggestedValue
            } while (!atomicVal.compareAndSet(currentVal, suggestedValue))
            if (notify) notifyUpdate()
        }


    override fun createChild(weight: Double): ProgressControl {
        val child = JvmProgress(executor)
        addChild(child.progress, weight)
        return child
    }

    override val children: List<ChildProgress>
        get() = ArrayList(childProgresses)


    //Prevents a copy of the children list
    override fun contains(progress: Progress): Boolean {
        if (this == progress) return true

        childProgresses forEach {
            if (it.progress.contains(progress)) return false
        }

        return false
    }

    override fun addChild(progress: Progress, weight: Double) {
        if (weight < 0.0) throw IllegalArgumentException("weight can not be negative")
        if (contains(progress)) throw IllegalArgumentException("circular reference")

        childProgresses add ChildProgress(progress, weight)
        progress.update { updateValue() }
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
        for ((p, w) in childProgresses) {
            totalWeight += w
            totalWeightValue += p.value * w
        }
        return if (totalWeight > 0.0 && totalWeightValue > 0.0) totalWeightValue / totalWeight else 0.0
    }


    private fun notifyUpdate() {
        callbacks.forEach { cb -> cb.execute(this) }
    }

    override fun update(executor: (progress: Progress, body: Progress.() -> Unit) -> Unit, notifyOnAdd: Boolean, body: Progress.() -> Unit) {
        //Could miss an event, should record what's been called already
        val callback = Callback(executor, body)
        if (notifyOnAdd) {
            callback.execute(this)
        }
        callbacks add callback
    }

}

private class Callback(private val executor: (progress: Progress, body: Progress.() -> Unit) -> Unit,
                       private val cb: Progress.() -> Unit) {
    fun execute(progress: Progress) = executor(progress, cb)
}

public class OutOfRangeException(msg: String, cause: Throwable? = null) : IllegalArgumentException(msg, cause)