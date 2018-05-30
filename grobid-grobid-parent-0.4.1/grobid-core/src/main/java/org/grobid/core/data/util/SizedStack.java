package org.grobid.core.data.util;

/**
 * Created by aman on 27/3/17.
 */
import java.util.Stack;

public class SizedStack<T> extends Stack<T> {
    private int maxSize;

    public SizedStack(int size) {
        super();
        this.maxSize = size;
    }

    @Override
    public T push(T object) {
        //remove elements when the stack is full
        while (this.size() >= maxSize) {
            this.remove(0);
        }
        return super.push(object);
    }
}