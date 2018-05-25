package com.github.jhg023.simplenet.utility;

public final class IntPair<T> {

    private final int key;

    private final T value;

    public IntPair(int key, T value) {
        this.key = key;
        this.value = value;
    }

    public int getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "[" + key + ", " + value + "]";
    }

}
