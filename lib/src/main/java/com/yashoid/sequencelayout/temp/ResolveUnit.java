package com.yashoid.sequencelayout.temp;

public class ResolveUnit implements Comparable<ResolveUnit> {

    private static ExpandingPool<ResolveUnit> mPool = new ExpandingPool<>(100, new ExpandingPool.InstanceCreator<ResolveUnit>() {

        @Override
        public ResolveUnit newInstance() {
            return new ResolveUnit();
        }

    }, true, true);

    public static ResolveUnit obtain(int elementId, boolean horizontal, SizeInfo sizeInfo) {
        ResolveUnit unit = mPool.acquire();

        unit.reset();

        unit.elementId = elementId;
        unit.horizontal = horizontal;
        unit.sizeInfo = sizeInfo;

        return unit;
    }

    private int elementId;
    private boolean horizontal;
    private SizeInfo sizeInfo;

    private boolean isWrappingKnown = false;
    private boolean isWrapping;

    private int size = -1;

    private int start = -1;
    private int end = -1;

    private ResolveUnit() {

    }

    private void reset() {
        elementId = 0;
        sizeInfo = null;

        isWrappingKnown = false;

        size = -1;

        start = -1;
        end = -1;
    }

    public int getElementId() {
        return elementId;
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    public SizeInfo getSizeInfo() {
        return sizeInfo;
    }

    public void setWrapping(boolean wrapping) {
        isWrappingKnown = true;
        isWrapping = wrapping;
    }

    public boolean isWrappingKnown() {
        return isWrappingKnown;
    }

    public boolean isWrapping() {
        return isWrapping;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isSizeSet() {
        return size != -1;
    }

    public int getSize() {
        return size;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public boolean isPositionSet() {
        return start != -1 && end != -1;
    }

    void resetResolvedData() {
        isWrappingKnown = false;
        size = -1;
        start = -1;
        end = -1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof ResolveUnit)) {
            return false;
        }

        ResolveUnit ru = (ResolveUnit) obj;

        return ru.horizontal == horizontal && ru.elementId == elementId;
    }

    @Override
    public int compareTo(ResolveUnit o) {
        if (horizontal != o.isHorizontal()) {
            return horizontal ? -1 : 1;
        }

        return elementId - o.getElementId();
    }

    protected void release() {
        reset();

        mPool.release(this);
    }

    @Override
    public String toString() {
        return (horizontal ? "h" : "v") + " " + elementId + " " + (size >= 0 ? "" + size : "?") + " sizeInfo:" + sizeInfo;
    }

}
