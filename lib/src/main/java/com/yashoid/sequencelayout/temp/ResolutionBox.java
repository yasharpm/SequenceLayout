package com.yashoid.sequencelayout.temp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResolutionBox {

    private ArrayList<ResolveUnit> mUnits;

    ResolutionBox() {
        mUnits = new ArrayList<>();
    }

    List<ResolveUnit> getUnits() {
        return mUnits;
    }

    void passUnits(ResolutionBox dest) {
        dest.mUnits.addAll(mUnits);
        mUnits.clear();
    }

    void resetUnits() {
        for (ResolveUnit unit: mUnits) {
            unit.resetResolvedData();
        }
    }

    public void add(ResolveUnit unit) {
        int index = Collections.binarySearch(mUnits, unit);

        if (index < 0) {
            index = -index - 1;

            mUnits.add(index, unit);
        }
    }

    public ResolveUnit take(ResolveUnit unit) {
        int index = Collections.binarySearch(mUnits, unit);

        if (index < 0) {
            return null;
        }

        return mUnits.remove(index);
    }

    public ResolveUnit take(int elementId, boolean horizontal) {
        ResolveUnit lookupUnit = ResolveUnit.obtain(elementId, horizontal, null);

        ResolveUnit foundUnit = take(lookupUnit);

        lookupUnit.release();

        return foundUnit;
    }

    public ResolveUnit find(ResolveUnit unit) {
        int index = Collections.binarySearch(mUnits, unit);

        if (index >= 0) {
            return mUnits.get(index);
        }

        return null;
    }

    public ResolveUnit find(int elementId, boolean horizontal) {
        ResolveUnit lookupUnit = ResolveUnit.obtain(elementId, horizontal, null);

        ResolveUnit foundUnit = find(lookupUnit);

        lookupUnit.release();

        return foundUnit;
    }

}
