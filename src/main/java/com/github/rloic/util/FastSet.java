package com.github.rloic.util;

import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class FastSet implements IntSet {

    private final int[] positionOf;
    private final int[] elements;

    private FastSet(int[] positionOf, int[] elements) {
        this.positionOf = positionOf;
        this.elements = elements;
    }

    public FastSet(int capacity) {
        this.positionOf = new int[capacity];
        this.elements = new int[capacity + 1];
    }

    public FastSet(FastSet other) {
        this.positionOf = Arrays.copyOf(other.positionOf, other.positionOf.length);
        this.elements = Arrays.copyOf(other.elements, other.elements.length);
    }

    public static FastSet full(int capacity) {
        int[] elements = new int[capacity + 1];
        int[] positionOf = new int[capacity];
        elements[0] = capacity;
        for (int i = 0; i < capacity; i++) {
            elements[i + 1] = i;
            positionOf[i] = i + 1;
        }
        return new FastSet(positionOf, elements);
    }

    @NotNull
    @Override
    public IntIterator iterator() {
        return new IntIterator() {
            private int k = 1;

            @Override
            public int nextInt() {
                return elements[k++];
            }

            @Override
            public boolean hasNext() {
                return k <= size();
            }
        };
    }

    @Override
    public boolean remove(int k) {
        if (!contains(k)) return false;
        int w = elements[elements[0]];
        elements[positionOf[k]] = w;
        positionOf[w] = positionOf[k];
        positionOf[k] = 0;
        elements[0] -= 1;
        return true;
    }

    @Override
    public boolean add(int key) {
        if (contains(key)) return false;
        elements[0] += 1;
        elements[elements[0]] = key;
        positionOf[key] = elements[0];
        return true;
    }

    @Override
    public boolean contains(int key) {
        return positionOf[key] > 0 && positionOf[key] <= size() && elements[positionOf[key]] == key;
    }

    @Override
    public int[] toIntArray() {
        return Arrays.copyOfRange(elements, 1, size() + 1);
    }

    @Override
    public int[] toIntArray(int[] a) {
        System.arraycopy(elements, 1, elements, 0, size() + 1);
        return a;
    }

    @Override
    public int[] toArray(int[] a) {
        System.arraycopy(elements, 1, elements, 0, size() + 1);
        return a;
    }

    @Override
    public boolean addAll(IntCollection c) {
        boolean hasChanged = false;
        for (int element : c) {
            hasChanged |= add(element);
        }
        return hasChanged;
    }

    @Override
    public boolean containsAll(IntCollection c) {
        for (int element : c) {
            if (!contains(element)) return false;
        }
        return true;
    }

    @Override
    public boolean removeAll(IntCollection c) {
        boolean hasChanged = false;
        for (int element : c) {
            hasChanged |= remove(element);
        }
        return hasChanged;
    }

    @Override
    public boolean retainAll(IntCollection c) {
        boolean hasChanged = false;
        for (int element : this) {
            if (!c.contains(element)) {
                hasChanged |= remove(element);
            }
        }
        return hasChanged;
    }

    @Override
    public int size() {
        return elements[0];
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @NotNull
    @Override
    public Object[] toArray() {
        Integer[] result = new Integer[elements.length - 1];
        for (int i = 1; i <= size(); i++) {
            result[i - 1] = elements[i];
        }
        return result;
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] ts) {
        return ts;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> collection) {
        for (Object element : collection) {
            if (element instanceof Integer && !contains((int) element)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Integer> collection) {
        boolean someWasAdded = false;
        for (Integer element : collection) {
            if (element != null && add((int) element)) {
                someWasAdded = true;
            }
        }
        return someWasAdded;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> collection) {
        boolean hasChanged = false;
        for (Object element : collection) {
            if (element instanceof Integer) {
                hasChanged |= remove((int) element);
            }
        }
        return hasChanged;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> collection) {
        boolean hasChanged = false;
        for (int element : this) {
            if (!collection.contains(element)) {
                hasChanged |= remove(element);
            }
        }
        return hasChanged;
    }

    @Override
    public void clear() {
        elements[0] = 0;
    }

    @Override
    public String toString() {
        return "{" +
                Arrays.stream(elements, 1, size() + 1).mapToObj(Integer::toString).collect(Collectors.joining(", ")) +
                "}";
    }
}
