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

package example.threaded

import nl.komponents.progress.OutOfRangeException
import nl.komponents.progress.Progress
import java.text.DecimalFormat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.properties.ReadOnlyProperty

fun main(args: Array<String>) {

    val executorService = Executors.newFixedThreadPool(1)
    Progress.defaultExecutor = { executorService.execute(it) }
    val latch = CountDownLatch(1)

    val masterControl = Progress.containerControl()
    masterControl.progress.update {
        println("${value.percentage}%")
        if (done) latch.countDown()
    }

    val firstChild = masterControl.child(0.1)
    val secondChild = masterControl.containerChild(5.0)
    val secondChildFirstChild = secondChild.child()
    val secondChildSecondChild = secondChild.child()
    val thirdChild = masterControl.child()
    val fourthChild = masterControl.child(2.0)

    firstChild.value = 0.25
    Thread.sleep(10)
    firstChild.value = 0.50
    Thread.sleep(10)
    firstChild.value = 0.75
    Thread.sleep(10)
    firstChild.value = 1.0

    secondChildFirstChild.markAsDone()
    secondChildSecondChild.value = 0.5
    Thread.sleep(10)
    secondChildSecondChild.value = 1.0

    thirdChild.value = 0.25
    thirdChild.value = 0.50
    Thread.sleep(10)
    thirdChild.value = 0.75
    thirdChild.value = 1.0

    fourthChild.value = 0.25
    fourthChild.value = 0.50
    Thread.sleep(10)
    fourthChild.value = 0.75
    fourthChild.value = 1.0

    latch.await()
    executorService.shutdown()
}

private val percentageFormat by ThreadLocalVal { DecimalFormat("##0.00") }

val Double.percentage: String
    get() = if (this in (0.0..1.0)) percentageFormat.format(this * 100) else throw OutOfRangeException("[$this] must be within bounds (0.0 .. 1.0)")


private class ThreadLocalVal<T>(private val initializer: () -> T) : ReadOnlyProperty<Any?, T> {
    private val threadLocal = object : ThreadLocal<T>() {
        override fun initialValue(): T = initializer()
    }

    public override fun get(thisRef: Any?, desc: PropertyMetadata): T = threadLocal.get()
}

