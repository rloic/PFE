package com.github.rloic.util;

import it.unimi.dsi.fastutil.ints.IntConsumer;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.IntPredicate;

public class FastSet implements IntIterable {

    private final int[] elements;
    private final int[] indexOf;

    public FastSet(int capacity) {
        elements = new int[capacity + 1];
        indexOf = new int[capacity];
    }

    public FastSet(FastSet set) {
        this.elements = Arrays.copyOf(set.elements, set.elements.length);
        this.indexOf = Arrays.copyOf(set.indexOf, set.indexOf.length);
    }

    public int size() {
        return elements[0];
    }

    private void setSize(int size) {
        elements[0] = size;
    }

    public boolean contains(int element) {
        int index = indexOf[element];
        return index > 0 && index <= size() && elements[index] == element;
    }

    public void add(int element) {
        if (contains(element)) {
            return;
        }
        setSize(size() + 1);
        elements[size()] = element;
        indexOf[element] = size();
    }

    public void remove(int element) {
        if (!contains(element)) return;
        int lastElement = elements[size()];
        int indexOfElement = indexOf[element];
        elements[indexOfElement] = lastElement;
        indexOf[lastElement] = indexOfElement;
        indexOf[element] = 0;
        setSize(size() - 1);
    }

    public void xor(FastSet other) {
        other.forEach((IntConsumer) value -> {
            if (contains(value)) {
                remove(value);
            } else {
                add(value);
            }
        });
    }

    public void clear() {
        setSize(0);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean isNotEmpty() {
        return size() != 0;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("{");

        if (isNotEmpty()) {
            for (int i = 1; i < size(); i++) {
                builder.append(elements[i]);
                builder.append(",");
            }
            builder.append(elements[size()]);
        }

        builder.append("}");
        return builder.toString();
    }

    public void forEach(IntConsumer consumer) {
        for(int i = 1; i <= size(); i++) {
            consumer.accept(elements[i]);
        }
    }

    public int first(IntPredicate predicate) {
        int i = 1;
        while (i <= size() && !predicate.test(elements[i])) {
            i += 1;
        }
        if (i <= size()) {
            return elements[i];
        } else {
            return -1;
        }
    }

    public void forEach(IntWithIndexConsumer consumer) {
        for (int i = 1; i <= size(); i++) {
            consumer.accept(i - 1, elements[i]);
        }
    }

    public boolean any(IntPredicate predicate) {
        int i = 1;
        while (i <= size() && !predicate.test(elements[i])) {
            i += 1;
        }
        return i != size() + 1;
    }

    public boolean all(IntPredicate predicate) {
        int i = 1;
        while (i <= size() && predicate.test(elements[i])) {
            i += 1;
        }
        return i == size() + 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FastSet fastSet = (FastSet) o;
        return fastSet.size() == size() && fastSet.all(this::contains);
    }

    @Override
    public int hashCode() {
        int result = size();
        for (int i = 1; i <= size(); i++) {
            result ^= elements[i];
        }
        return result;
    }

    @NotNull
    @Override
    public IntIterator iterator() {
        return new IntIterator() {
            int i = 1;

            @Override
            public int nextInt() {
                return elements[i++];
            }

            @Override
            public boolean hasNext() {
                return i <= size();
            }
        };
    }
}
