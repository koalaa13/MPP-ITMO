import java.util.concurrent.atomic.AtomicReference

class Solution(private val env: Environment) : Lock<Solution.Node> {
    private val tail: AtomicReference<Node?> = AtomicReference(null)

    class Node(
        val thread: Thread = Thread.currentThread(),
        val locked: AtomicReference<Boolean> = AtomicReference(true),
        val next: AtomicReference<Node?> = AtomicReference(null)
    )

    override fun lock(): Node {
        val my = Node()
        val pred = tail.getAndSet(my)
        if (pred != null) {
            pred.next.set(my)
            while (my.locked.value) {
                env.park()
            }
        }
        return my
    }

    override fun unlock(node: Node) {
        if (node.next.get() == null) {
            if (tail.compareAndSet(node, null)) {
                return
            } else {
                while (node.next.get() == null) {
                }
            }
        }
        val next = node.next
        next.value!!.locked.set(false)
        env.unpark(next.get()!!.thread)
    }
}