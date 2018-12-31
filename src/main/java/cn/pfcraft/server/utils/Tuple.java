package cn.pfcraft.server.utils;


import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

public class Tuple<K, V>  {
    public static <K, V> Tuple<K, V> of(K first, V second) {
        return new Tuple<>(first, second);
    }
    private final K first;
    private final V second;
    public Tuple(K first, V second) {
        this.first = checkNotNull(first);
        this.second = checkNotNull(second);
    }

    public K getFirst() {
        return first;
    }

    public V getSecond() {
        return second;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.first, this.second);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple<?, ?> tuple = (Tuple<?, ?>) o;
        return Objects.equals(first, tuple.first) &&
                Objects.equals(second, tuple.second);
    }
}
