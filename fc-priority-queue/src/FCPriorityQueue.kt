import FCPriorityQueue.OperationType.*
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls
import java.util.*

class FCPriorityQueue<E : Comparable<E>> {
    companion object {
        private const val WAIT_ITERATIONS = 10
        private const val FIND_FREE_IND_ITERATIONS = 10
        private const val SEED = 1337228L
        private val rnd = Random(SEED)
    }

    private val fcArraySize = 4 * Thread.activeCount()
    private val q = PriorityQueue<E>()
    private val lock = atomic(false)
    private val array = atomicArrayOfNulls<Operation<E>?>(fcArraySize)

    private fun help() {
        for (i in 0 until fcArraySize) {
            val curOperation = array[i].value ?: continue
            when (curOperation.type) {
                ADD -> {
                    if (array[i].compareAndSet(curOperation, Operation(MADE, null))) {
                        q.add(curOperation.value)
                    }
                }
                PEEK -> array[i].compareAndSet(curOperation, Operation(MADE, q.peek()))
                POLL -> {
                    val res = q.poll()
                    if (!array[i].compareAndSet(curOperation, Operation(MADE, res)) && res != null) {
                        q.add(res)
                    }
                }
                MADE -> {
                }
            }
        }
    }

    private fun askForHelp(operation: Operation<E>): Pair<Boolean, E?> {
        var ind = rnd.nextInt(fcArraySize)
        for (i in 0 until FIND_FREE_IND_ITERATIONS) {
            if (array[ind].compareAndSet(null, operation)) {
                for (j in 0 until WAIT_ITERATIONS) {
                    val op = array[ind].value as Operation<E>
                    if (op.type == MADE && array[ind].compareAndSet(op, null)) {
                        return Pair(true, op.value)
                    }
                }
                return if (array[ind].compareAndSet(operation, null)) {
                    Pair(false, null)
                } else {
                    val res = array[ind].value?.value
                    array[ind].value = null
                    Pair(true, res)
                }
            }
            ind++
            if (ind == fcArraySize) {
                ind = 0
            }
        }
        return Pair(false, null)
    }


    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        while (true) {
            if (lock.compareAndSet(expect = false, update = true)) {
                val res = q.poll()
                help()
                lock.compareAndSet(expect = true, update = false)
                return res
            } else {
                val helpRes = askForHelp(Operation(POLL, null))
                if (helpRes.first) {
                    return helpRes.second
                }
            }
        }
    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        while (true) {
            if (lock.compareAndSet(expect = false, update = true)) {
                val res = q.peek()
                help()
                lock.compareAndSet(expect = true, update = false)
                return res
            } else {
                val helpRes = askForHelp(Operation(PEEK, null))
                if (helpRes.first) {
                    return helpRes.second
                }
            }
        }

    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        while (true) {
            if (lock.compareAndSet(expect = false, update = true)) {
                q.add(element)
                help()
                lock.compareAndSet(expect = true, update = false)
                return
            } else {
                val helpRes = askForHelp(Operation(ADD, element))
                if (helpRes.first) {
                    return
                }
            }
        }
    }

    private enum class OperationType {
        POLL, PEEK, ADD, MADE
    }

    private class Operation<E>(val type: OperationType, val value: E?)
}