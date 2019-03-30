package com.yashoid.sequencelayout;

import java.util.ArrayList;

public class ExpandingPool<T> {

    public interface InstanceCreator<T> {

        T newInstance();

    }

    private int mSize;
    private InstanceCreator<T> mInstanceCreator;
    private boolean mCanOverGrow;

    private ArrayList<T> mInstances;

    public ExpandingPool(int size, InstanceCreator<T> instanceCreator, boolean expandFirst, boolean canOverGrow) {
        mSize = size;
        mInstanceCreator = instanceCreator;
        mCanOverGrow = canOverGrow;

        mInstances = new ArrayList<>(size);

        if (expandFirst) {
            for (int i = 0; i < size; i++) {
                mInstances.add(instanceCreator.newInstance());
            }
        }
    }

    public T acquire() {
        if (mInstances.size() == 0) {
            return mInstanceCreator.newInstance();
        }

        return mInstances.remove(mInstances.size() - 1);
    }

    public boolean release(T instance) {
        if (!mCanOverGrow && mInstances.size() >= mSize) {
            return false;
        }

        mInstances.add(instance);

        return true;
    }

}
