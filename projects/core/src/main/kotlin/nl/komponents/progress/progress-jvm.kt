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

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference


public fun concreteSingleProgressControl(executor: (() -> Unit) -> Unit): SingleProgressControl = JvmSingleProgress(executor)
public fun concreteContainerProgressControl(executor: (() -> Unit) -> Unit): ContainerProgressControl = JvmContainerProgress(executor)


private class JvmSingleProgress(executor: (() -> Unit) -> Unit) : SingleProgressControl, CallbackSupport(executor), Progress {
    private val atomicVal = AtomicReference(0.0)

    override val progress: Progress = object : Progress by this {}

    override fun markAsDone() {
        value = 1.0
    }

    public override val done: Boolean
        get() = value >= 1.0


    public override var value: Double
        get() = atomicVal.get()
        set(suggestedValue) {
            if (suggestedValue !in (0.0..1.0)) throw OutOfRangeException("[$value] must be within bounds (0.0 .. 1.0)")

            var notify: Boolean
            do {
                val currentVal = atomicVal.get()
                notify = currentVal != suggestedValue
            } while (!atomicVal.compareAndSet(currentVal, suggestedValue))
            if (notify) notifyUpdate()
        }


    override fun contains(progress: Progress): Boolean = this == progress
}

private class JvmContainerProgress(executor: (() -> Unit) -> Unit) : ContainerProgressControl, CallbackSupport(executor), Progress {
    private val childProgresses = ConcurrentLinkedQueue<ChildProgress>()
    private val atomicVal = AtomicReference(0.0)

    private val self = this
    override val progress: Progress = object : Progress by this {
        override fun equals(other: Any?): Boolean = self.equals(other)
        override fun hashCode(): Int = self.hashCode()
    }

    public override val value: Double
        get() = atomicVal.get()

    override fun contains(progress: Progress): Boolean {
        if (this == progress) return true
        return childProgresses any { child -> child.progress.contains(progress) }
    }

    override fun addChild(progress: Progress, weight: Double) {
        if (weight < 0.0) throw ArgumentException("weight can not be negative")
        if (progress.contains(this)) throw ArgumentException("circular reference")

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
}

private abstract class CallbackSupport(override val executor: (() -> Unit) -> Unit) : Progress {
    private val callbacks = ConcurrentLinkedQueue<Callback>()

    protected fun notifyUpdate() {
        callbacks.forEach { cb -> cb.execute(this) }
    }

    override fun update(executor: (() -> Unit) -> Unit,
                        notifyOnAdd: Boolean,
                        callbackType: CallbackType,
                        body: Progress.() -> Unit) {
        val callback = when (callbackType) {
            CallbackType.BUFFERED -> JoinedCallback(executor, body)
            CallbackType.ALWAYS -> AlwaysCallback(executor, body)
        }

        if (notifyOnAdd) {
            callback.execute(this)
        }
        callbacks add callback
    }
}

private interface Callback {
    fun execute(progress: Progress)
}

private class AlwaysCallback(private val executor: (() -> Unit) -> Unit,
                             private val cb: Progress.() -> Unit) : Callback {
    override fun execute(progress: Progress) {
        executor {
            progress.cb()
        }
    }
}

private class JoinedCallback(private val executor: (() -> Unit) -> Unit,
                             private val cb: Progress.() -> Unit) : Callback {
    private val scheduled = AtomicInteger(0)

    override fun execute(progress: Progress) {
        val count = scheduled.incrementAndGet()
        if (count == 1) {
            executor {
                scheduled.decrementAndGet()
                progress.cb()
            }
        } else {
            scheduled.decrementAndGet()
        }
    }
}

