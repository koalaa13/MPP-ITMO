package dijkstra

import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator
import kotlin.concurrent.thread
import kotlin.random.Random

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> o1!!.distance.compareTo(o2!!.distance) }

private class ConcurrentMultiQueue(queuesCount: Int, comparator: Comparator<Node>) {
    private var queues = arrayOf<PriorityQueue<Node>>()

    init {
        for (i in 0..queuesCount) {
            queues += PriorityQueue<Node>(comparator)
        }
    }

    fun extractMin(): Node? {
        var ind1 = Random.nextInt(queues.size)
        var ind2 = Random.nextInt(queues.size)
        if (ind1 > ind2) {
            ind1 = ind2.also { ind2 = ind1 }
        }
        synchronized(queues[ind1]) {
            synchronized(queues[ind2]) {
                val node1 = queues[ind1].peek()
                val node2 = queues[ind2].peek()
                if (node1 == null && node2 == null) {
                    return null
                }
                if (node1 != null && node2 == null) {
                    return queues[ind1].poll()
                }
                if (node1 == null && node2 != null) {
                    return queues[ind2].poll()
                }
                if (node1 != null && node2 != null) {
                    return if (node1.distance < node2.distance) {
                        queues[ind1].poll()
                    } else {
                        queues[ind2].poll()
                    }
                }
            }
        }
        return null
    }

    fun add(v: Node) {
        val ind = Random.nextInt(queues.size)
        synchronized(queues[ind]) {
            queues[ind].add(v)
        }
    }
}

fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    start.distance = 0
    val q = ConcurrentMultiQueue(2 * workers, NODE_DISTANCE_COMPARATOR)
    q.add(start)
    val activeNodes = AtomicInteger(1)

    val onFinish = Phaser(workers + 1)
    repeat(workers) {
        thread {
            while (activeNodes.get() > 0) {
                val v = q.extractMin()
                if (v != null) {
                    for (e in v.outgoingEdges) {
                        while (true) {
                            val toDist = e.to.distance
                            val fromDist = v.distance
                            if (toDist > fromDist + e.weight) {
                                if (e.to.casDistance(toDist, fromDist + e.weight)) {
                                    q.add(e.to)
                                    activeNodes.incrementAndGet()
                                    break;
                                }
                            } else {
                                break
                            }
                        }
                    }
                    activeNodes.decrementAndGet()
                }
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}