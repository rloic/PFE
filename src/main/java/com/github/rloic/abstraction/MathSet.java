package com.github.rloic.abstraction;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Extension of Java Set collection. Add the ability to perform union and intersection between sets
 * @param <E> The Element type of the Set
 */
public class MathSet<E> extends HashSet<E> implements Set<E> {

    public MathSet() {
        super();
    }

    public MathSet(@NotNull Collection<? extends E> c) {
        super(c);
    }

    public MathSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public MathSet(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Create a new Set S such as S = this ∪ other
     * @param other An other Set of E
     * @return The new Set S
     */
    public MathSet<E> union(Set<E> other) {
        MathSet<E> newSet = new MathSet<>(this);
        if (other != null) newSet.addAll(other);
        return newSet;
    }

    /**
     * Make a union between the current set and the other set
     * If inPlace is true make the union inPlace (modify the current object) else
     * create and return the union set
     * @param other An other Set of E
     * @param inPlace Indicates if the method must create a new set or perform the union on the current object (not pure)
     * @return The new Set (currentObject if inPlace == true) else a new object
     */
    public MathSet<E> union(Set<E> other, boolean inPlace) {
        if (inPlace) {
            if (other != null) addAll(other);
            return this;
        } else {
            return union(other);
        }
    }

}
