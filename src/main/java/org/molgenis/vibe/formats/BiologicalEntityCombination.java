package org.molgenis.vibe.formats;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

/**
 * A combination of 2 {@link BiologicalEntity} subclass objects.
 * @param <T1> the first {@link BiologicalEntity} subclass type
 * @param <T2> the second {@link BiologicalEntity} subclass type
 */
public abstract class BiologicalEntityCombination<T1 extends BiologicalEntity, T2 extends BiologicalEntity> {
    /**
     * The first {@link BiologicalEntity} subclass object.
     */
    private T1 t1;

    /**
     * The second {@link BiologicalEntity} subclass object.
     */
    private T2 t2;

    public T1 getT1() {
        return t1;
    }

    public T2 getT2() {
        return t2;
    }

    public BiologicalEntityCombination(T1 t1, T2 t2) {
        this.t1 = requireNonNull(t1);
        this.t2 = requireNonNull(t2);
    }

    /**
     * While uniqueness is based on the 2 available {@link BiologicalEntity#id}s (as each combination should only occur
     * once and data belonging to it should be consistent), {@link #toString()} can be used for testing whether data
     * retrieval from external sources yielded the expected results.
     * @return
     */
    @Override
    public String toString() {
        return "BiologicalEntityCombination{" +
                "t1=" + t1 +
                ", t2=" + t2 +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BiologicalEntityCombination<?, ?> that = (BiologicalEntityCombination<?, ?>) o;
        return Objects.equals(t1, that.t1) &&
                Objects.equals(t2, that.t2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(t1, t2);
    }
}
