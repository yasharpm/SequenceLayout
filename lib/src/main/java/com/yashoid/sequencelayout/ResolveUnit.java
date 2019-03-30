package com.yashoid.sequencelayout;

class ResolveUnit implements Comparable<ResolveUnit> {

    private static ExpandingPool<ResolveUnit> mPool = new ExpandingPool<>(100, new ExpandingPool.InstanceCreator<ResolveUnit>() {

        @Override
        public ResolveUnit newInstance() {
            return new ResolveUnit();
        }

    }, true, true);

    static ResolveUnit obtain(int elementId, boolean horizontal, Span span) {
        ResolveUnit unit = mPool.acquire();

        unit.reset();

        unit.elementId = elementId;
        unit.horizontal = horizontal;
        unit.span = span;

        return unit;
    }

    private int elementId;
    private boolean horizontal;
    private Span span;

    private boolean isWrappingKnown = false;
    private boolean isWrapping;

    private int size = -1;

    private int start = -1;
    private int end = -1;

    private ResolveUnit() {

    }

    private void reset() {
        elementId = 0;
        span = null;

        isWrappingKnown = false;

        size = -1;

        start = -1;
        end = -1;
    }

    int getElementId() {
        return elementId;
    }

    boolean isHorizontal() {
        return horizontal;
    }

    Span getSpan() {
        return span;
    }

    void setWrapping(boolean wrapping) {
        isWrappingKnown = true;
        isWrapping = wrapping;
    }

    boolean isWrappingKnown() {
        return isWrappingKnown;
    }

    boolean isWrapping() {
        return isWrapping;
    }

    void setSize(int size) {
        this.size = size;
    }

    boolean isSizeSet() {
        return size != -1;
    }

    int getSize() {
        return size;
    }

    void setStart(int start) {
        this.start = start;
    }

    void setEnd(int end) {
        this.end = end;
    }

    int getStart() {
        return start;
    }

    int getEnd() {
        return end;
    }

    boolean isPositionSet() {
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

    void release() {
        reset();

        mPool.release(this);
    }

    @Override
    public String toString() {
        return (horizontal ? "h" : "v") + " " + elementId + " " + (size >= 0 ? "" + size : "?") + " span:" + span;
    }

}
