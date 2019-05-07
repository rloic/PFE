package com.github.rloic.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Function;

public class IterableMapper<T, U> implements Iterable<U> {

    private final Iterable<T> iter;
    private final Function<T, U> mapper;

    public IterableMapper(Iterable<T> iter, Function<T, U> mapper) {
        this.iter = iter;
        this.mapper = mapper;
    }

    @NotNull
    @Override
    public Iterator<U> iterator() {
        Iterator<T> iterator = iter.iterator();
        return new Iterator<U>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public U next() {
                return mapper.apply(iterator.next());
            }
        };
    }
}
