package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {
    private static abstract class Node {
        AtomicRef<Node> next;
        int x;

        Node(int x, Node next) {
            this.x = x;
            this.next = new AtomicRef<>(next);
        }
    }

    private static class RemovedNode extends Node {
        RemovedNode(int x, Node next) {
            super(x, next);
        }
    }

    private static class ExistingNode extends Node {
        ExistingNode(int x, Node next) {
            super(x, next);
        }
    }

    private static class Window {
        Node cur, next;

        Window(Node cur, Node next) {
            this.cur = cur;
            this.next = next;
        }
    }

    private final Node head = new ExistingNode(Integer.MIN_VALUE, new ExistingNode(Integer.MAX_VALUE, null));

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        while (true) {
            Node cur = head, next = cur.next.getValue();
            boolean flag = false;
            while (next.x < x) {
                Node node = next.next.getValue();
                if (node instanceof RemovedNode) {
                    node = node.next.getValue();
                    if (cur.next.compareAndSet(next, node)) {
                        next = node;
                    } else {
                        flag = true;
                        break;
                    }
                } else {
                    cur = next;
                    next = node;
                }
            }
            if (flag) {
                continue;
            }
            while (true) {
                Node node = next.next.getValue();
                if (node instanceof RemovedNode) {
                    node = node.next.getValue();
                    if (!cur.next.compareAndSet(next, node)) {
                        break;
                    }
                } else {
                    return new Window(cur, next);
                }
            }
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.cur instanceof RemovedNode || w.next instanceof RemovedNode || w.next.next.getValue() instanceof RemovedNode) {
                continue;
            }
            if (w.next.x == x) {
                return false;
            }
            Node node = new ExistingNode(x, w.next);
            if (w.next instanceof ExistingNode && w.cur.next.compareAndSet(w.next, node)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            Node node = w.next.next.getValue();
            if (w.cur instanceof RemovedNode || w.next instanceof RemovedNode || node instanceof RemovedNode) {
                continue;
            }
            if (w.next.x != x) {
                return false;
            }
            Node newNode = new RemovedNode(w.next.x, w.next.next.getValue());
            if (w.next.next.compareAndSet(node, newNode)) {
                w.cur.next.compareAndSet(w.next, node);
                return true;
            }
        }
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        return w.next.x == x;
    }
}