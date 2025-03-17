import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

fun main() {
    println("Please enter the size of an array:")
    val arraySize = readlnOrNull()!!.toInt()
    val list = List(arraySize) { Random.nextInt() }
    val execStrategies = listOf(
        SingleThreadCalculator(),
        BlockingCalculator(),
        AtomicCalculator()
    )
    execStrategies.forEach { calculator ->
        println("Running ${calculator.title()}...")
        val (result, duration) = measureExecTime {
            calculator.calculateThreaded(list)
        }
        println("${calculator.title()} result: $result")
        println("Execution time: ${duration.toMillis()} ms")
        println()
    }
}

fun <T> measureExecTime(block: () -> T): Pair<T, Duration> {
    val start = Instant.now()
    val result = block()
    val duration = Duration.between(start, Instant.now())
    return Pair(result, duration)
}

interface ThreadedCalculator {
    fun calculateThreaded(input: Collection<Int>): Int
    fun title(): String
}

class SingleThreadCalculator : ThreadedCalculator {

    private fun calculate(input: Collection<Int>): Int {
        return input.filter { it % 2 == 0 }
            .reduce { acc, value -> acc xor value }
    }

    override fun calculateThreaded(input: Collection<Int>): Int {
        return calculate(input)
    }

    override fun title(): String {
        return "Single-threaded calculator"
    }
}

class BlockingCalculator : ThreadedCalculator {

    private companion object {
        val THREAD_COUNT = Runtime.getRuntime().availableProcessors()
        var accumulator = 0
    }

    override fun calculateThreaded(input: Collection<Int>): Int {
        val threads = input.chunked(THREAD_COUNT)
            .map { chunk ->
                Thread {
                    chunk.filter { it % 2 == 0 }
                        .forEach { xor(it) }
                }.also { it.start() }
            }
        threads.forEach { it.join() }
        return accumulator
    }

    override fun title(): String {
        return "Thread-blocking calculator"
    }

    @Synchronized
    private fun xor(value: Int) {
        accumulator = accumulator xor value
    }
}

class AtomicCalculator : ThreadedCalculator {

    private companion object {
        val THREAD_COUNT = Runtime.getRuntime().availableProcessors()
        val accumulator = AtomicInteger(0)
    }

    override fun calculateThreaded(input: Collection<Int>): Int {
        val threads = input.chunked(THREAD_COUNT)
            .map { chunk ->
                Thread {
                    chunk.filter { it % 2 == 0 }
                        .forEach { xor(it) }
                }.also { it.start() }
            }
        threads.forEach { it.join() }
        return accumulator.get()
    }

    override fun title(): String {
        return "Atomic calculator"
    }

    private fun xor(value: Int) {
        var current: Int
        var newValue: Int
        do {
            current = accumulator.get()
            newValue = current xor value
        } while (!accumulator.compareAndSet(current, newValue))
    }
}




