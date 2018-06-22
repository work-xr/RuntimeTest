package com.sprd.runtime.camera;

/**
 * Created by hefeng on 18-5-30.
 */

public class Tuple <A, B>{
    public final A first;
    public final B second;

    public Tuple(A first, B second) {
        this.first = first;
        this.second = second;
    }
}
