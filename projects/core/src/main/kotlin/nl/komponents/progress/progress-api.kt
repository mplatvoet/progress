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

public fun progressControl(executor: (progress: Progress, body: Progress.() -> Unit) -> Unit = { p, fn -> p.fn() }): ProgressControl = concreteProgressControl(executor)

public interface Progress {
    val executor: (progress: Progress, body: Progress.() -> Unit) -> Unit
    val done: Boolean
    val value: Double

    fun update(executor: (progress: Progress, body: Progress.() -> Unit) -> Unit = this.executor,
               notifyOnAdd: Boolean = true,
               body: Progress.() -> Unit)

    val intValue: Int get() = (value * 100).toInt()

    val children: List<ChildProgress>

    fun contains(progress: Progress): Boolean {
        if (progress == this) return true
        children.forEach {
            if (it.progress.contains(progress)) return true
        }
        return false
    }
}

public data class ChildProgress(val progress: Progress, val weight: Double = 1.0)

public interface ProgressControl {
    var value: Double
    fun createChild(weight: Double = 1.0): ProgressControl
    fun addChild(progress: Progress, weight: Double = 1.0)

    fun markAsDone()
    val progress: Progress


}

