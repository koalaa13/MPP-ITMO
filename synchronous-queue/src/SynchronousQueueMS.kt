import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SynchronousQueueMS<E> : SynchronousQueue<E> {
    private val dummy = Node<E>(null, null, true)
    private val head: AtomicRef<Node<E>> = atomic(dummy)
    private val tail: AtomicRef<Node<E>> = atomic(dummy)

    private suspend fun enqueueAndSuspend(t: Node<E>, element: E?, isSender: Boolean): E? {
        return suspendCoroutine sc@{ cont ->
            val newTail = Node(continuation = cont, element = element, isSender = isSender)
            if (t.next.compareAndSet(null, newTail)) {
                tail.compareAndSet(t, newTail)
            } else {
                cont.resume(null)
                return@sc
            }
        }
    }

    override suspend fun send(element: E) {
        while (true) {
            val t = tail.value
            val h = head.value
            if (t == h || t.isSender) {
                enqueueAndSuspend(t, element, true) ?: continue
                return
            } else {
                val headNext = h.next.value
                if (t != tail.value || h != head.value || h == tail.value || headNext == null) {
                    continue
                }
                if (!headNext.isSender && head.compareAndSet(h, headNext)) {
                    headNext.continuation!!.resume(element)
                    return
                }
            }
        }
    }

    override suspend fun receive(): E {
        while (true) {
            val t = tail.value
            val h = head.value
            if (t == h || !t.isSender) {
                return enqueueAndSuspend(t, null, false) ?: continue
            } else {
                val headNext = h.next.value
                if (t != tail.value || h != head.value || h == tail.value || headNext == null) {
                    continue
                }
                val res = headNext.element
                if (headNext.isSender && head.compareAndSet(h, headNext)) {
                    headNext.continuation!!.resume(res)
                    return res!!
                }
            }
        }
    }

    private class Node<E>(
        val continuation: Continuation<E?>?,
        val element: E?,
        val isSender: Boolean,
        val next: AtomicRef<Node<E>?> = atomic(null)
    )
}
