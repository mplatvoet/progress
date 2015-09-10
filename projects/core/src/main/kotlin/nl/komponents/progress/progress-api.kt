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


/**
 * Represents the consumer part of progress tracking
 */
public interface Progress {
    companion object {
        /**
         * defaultExecutor is the executor to be used when none is configured
         *
         * By default this just executes immediately
         */
        @Volatile var defaultExecutor : (() -> Unit) -> Unit = { fn -> fn() }

        @Volatile var callbackType : CallbackType = CallbackType.BUFFERED

        public fun control(executor: (() -> Unit) -> Unit = defaultExecutor): SingleProgressControl {
            return concreteSingleProgressControl(executor)
        }

        public fun containerControl(executor: (() -> Unit) -> Unit = defaultExecutor): ContainerProgressControl {
            return concreteContainerProgressControl(executor)
        }
    }

    val executor: (() -> Unit) -> Unit
    val done: Boolean get() = value >= 1.0
    val value: Double

    fun update(executor: (() -> Unit) -> Unit = this.executor,
               notifyOnAdd: Boolean = true,
               callbackType : CallbackType = Progress.callbackType,
               body: Progress.() -> Unit)


    fun contains(progress: Progress): Boolean
}

public enum class CallbackType {
    BUFFERED,
    ALWAYS
}

public data class ChildProgress(val progress: Progress, val weight: Double = 1.0)

public interface ProgressControl {
    val progress: Progress
}

public interface SingleProgressControl : ProgressControl {
    var value: Double
    fun markAsDone()
}

public interface ContainerProgressControl : ProgressControl {

    fun child(weight: Double = 1.0): SingleProgressControl {
        val child = Progress.control(progress.executor)
        addChild(child.progress, weight)
        return child
    }

    fun containerChild(weight: Double = 1.0): ContainerProgressControl {
        val child = Progress.containerControl(progress.executor)
        addChild(child.progress, weight)
        return child
    }

    fun addChild(progress: Progress, weight: Double = 1.0)
}

public open class ProgressException(msg: String, cause: Throwable? = null) : Exception(msg, cause)
public open class ArgumentException(msg: String, cause: Throwable? = null) : ProgressException(msg, cause)
public open class OutOfRangeException(msg: String, cause: Throwable? = null) : ProgressException(msg, cause)