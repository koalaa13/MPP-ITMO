package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private final AtomicRef<Node> head;
    private final AtomicRef<Node> tail;

    public MSQueue() {
        Node dummyNode = new Node(0, null);
        this.head = new AtomicRef<>(dummyNode);
        this.tail = new AtomicRef<>(dummyNode);
    }

    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x, null);
        while (true) {
            Node curTail = tail.getValue();
            if (curTail.next.compareAndSet(null, newTail)) {
                tail.compareAndSet(curTail, newTail);
                return;
            } else {
                tail.compareAndSet(curTail, curTail.next.getValue());
            }
        }
    }

    @Override
    public int dequeue() {
        while (true) {
            Node curHead = head.getValue();
            Node headNext = curHead.next.getValue();
            Node curTail = tail.getValue();
            if (head.getValue() == curHead) {
                if (headNext == null) {
                    return Integer.MIN_VALUE;
                }
                if (curHead == curTail) {
                    tail.compareAndSet(curTail, curTail.next.getValue());
                } else {
                    if (head.compareAndSet(curHead, headNext)) {
                        return headNext.x;
                    }
                }
            }
        }
    }

    @Override
    public int peek() {
        Node curHead = head.getValue();
        if (curHead.next.getValue() == null) {
            return Integer.MIN_VALUE;
        }
        return curHead.next.getValue().x;
    }

    private static class Node {
        final int x;
        final AtomicRef<Node> next;

        Node(int x, Node next) {
            this.x = x;
            this.next = new AtomicRef<>(next);
        }
    }
}