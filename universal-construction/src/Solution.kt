/**
 * @author Maksimov Nikita
 */
class Solution : AtomicCounter {
    private val root = Node(0, Consensus())

    private val last = ThreadLocal.withInitial {
        root
    }

    override fun getAndAdd(x: Int): Int {
        while (true) {
            val old = last.get()
            val res = Node(old.value + x, Consensus())
            last.set(last.get().next.decide(res))
            if (last.get() == res) {
                return old.value
            }
        }
    }

    private class Node(val value: Int, val next: Consensus<Node>)
}