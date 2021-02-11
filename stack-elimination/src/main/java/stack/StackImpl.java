package stack;

import kotlinx.atomicfu.AtomicRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StackImpl implements Stack {
    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    private final Random rnd;
    private final AtomicRef<Node> head;
    private final List<AtomicRef<Long>> eliminationArray;
    private final int ELIMINATION_ARRAY_SIZE = 10;
    private final int WAIT_ITERATIONS = 10;
    private final int FIND_FREE_INDEX_ITERATIONS = 10;
    private final Long stolen = (long) Integer.MAX_VALUE + 1;

    public StackImpl() {
        rnd = new Random(0);
        head = new AtomicRef<>(null);
        eliminationArray = new ArrayList<>(ELIMINATION_ARRAY_SIZE);
        for (int i = 0; i < ELIMINATION_ARRAY_SIZE; ++i) {
            eliminationArray.add(new AtomicRef<Long>(null));
        }
    }

    @Override
    public void push(int x) {
        Long toAdd = (long) x;
        for (int i = 0, ind = rnd.nextInt(ELIMINATION_ARRAY_SIZE); i < FIND_FREE_INDEX_ITERATIONS; ++i) {
            if (eliminationArray.get(ind).compareAndSet(null, toAdd)) {
                for (int j = 0; j < WAIT_ITERATIONS; ++j) {
                    if (eliminationArray.get(ind).compareAndSet(stolen, null)) {
                        return;
                    }
                }
                if (eliminationArray.get(ind).compareAndSet(toAdd, null)) {
                    break;
                } else {
                    eliminationArray.get(ind).compareAndSet(stolen, null);
                    return;
                }
            }
            ind++;
            if (ind == ELIMINATION_ARRAY_SIZE) {
                ind = 0;
            }
        }
        while (true) {
            Node was = head.getValue();
            Node newHead = new Node(x, was);
            if (head.compareAndSet(was, newHead)) {
                return;
            }
        }
    }

    @Override
    public int pop() {
        for (int i = 0, ind = rnd.nextInt(ELIMINATION_ARRAY_SIZE); i < FIND_FREE_INDEX_ITERATIONS; ++i) {
            Long value = eliminationArray.get(ind).getValue();
            if (value != null && !value.equals(stolen) && eliminationArray.get(ind).compareAndSet(value, stolen)) {
                return value.intValue();
            }
            ind++;
            if (ind == ELIMINATION_ARRAY_SIZE) {
                ind = 0;
            }
        }
        while (true) {
            Node curHead = head.getValue();
            if (curHead == null) {
                return Integer.MIN_VALUE;
            }
            if (head.compareAndSet(curHead, curHead.next.getValue())) {
                return curHead.x;
            }
        }
    }
}
