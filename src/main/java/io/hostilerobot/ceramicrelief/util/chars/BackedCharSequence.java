package io.hostilerobot.ceramicrelief.util.chars;

public class BackedCharSequence implements CharSequence{
    private CharSequence base;
    private int start;
    private int end;

    private BackedCharSequence(CharSequence base) {
        this(base, 0, base.length());
    }
    private BackedCharSequence(CharSequence base, int start, int end) {
        this.base = base;
        this.start = start;
        this.end = end;
    }
    public BackedCharSequence copy() {
        return new BackedCharSequence(base, start, end);
    }

    public static BackedCharSequence from(String base) {
        return new BackedCharSequence(base);
    }
    public static BackedCharSequence from(CharSequence base) {
        if(base instanceof BackedCharSequence baseSeq) {
            // copy underlying pointers rather than creating a chain
            return baseSeq.copy();
        }
        return new BackedCharSequence(base);
    }

    @Override
    public int length() {
        return end - start;
    }

    @Override
    public char charAt(int index) {
        return base.charAt(index + start);
    }

    @Override
    public boolean isEmpty() {
        return end == start;
    }

    // allows us to do certain traversals in place without allocating any other objects
    // warning: if this function is used with a charsequence of unknown origin it could have unintended conseequences, changing the string elsewhere
    public static CharSequence subSequenceMutable(CharSequence target, int start, int end) {
        if(target instanceof BackedCharSequence backedTarget) {
            int adjStart = backedTarget.start + start;
            int adjEnd = backedTarget.end + end;
            if(start < end || (start|end) < 0 || adjStart > backedTarget.end || adjEnd > backedTarget.end) {
                throw new StringIndexOutOfBoundsException();
            }
            backedTarget.start = adjStart;
            backedTarget.end = adjEnd;
            return backedTarget;
        } else {
            return target.subSequence(start, end);
        }
    }

    /*
     * for mutablity, put the value on the stack rather than on the heap
     * Use it like so:
     *
     * void foo(BaseCharSequence bcs) {
     *   int start = bcs.getOriginalBoundStart(); int end = bcs.getOriginalBoundEnd();
     *   baz(bcs); // may be modified!
     *   bcs.setOriginalBounds(start, end);
     * }
     */
    public static int getOriginalBoundStart(CharSequence cs) {
        return switch(cs) {
            case BackedCharSequence backed -> backed.start;
            default -> 0;
        };
    }
    public static int getOriginalBoundEnd(CharSequence cs) {
        return switch(cs) {
            case BackedCharSequence backed -> backed.end;
            default -> cs.length();
        };
    }
    public static CharSequence setOriginalBounds(CharSequence cs, int start, int end) {
        if(cs instanceof BackedCharSequence bcs) {
            if(start > end || (start|end) < 0 || start > bcs.base.length() || end > bcs.base.length())
                throw new StringIndexOutOfBoundsException();
            bcs.start = start;
            bcs.end = end;
            return bcs;
        }
        if(start == 0 && end == cs.length())
            return cs;
        else
            return cs.subSequence(start, end);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        // start == 0 => this.start
        // start == length => this.end
        int adjStart = start + this.start;
        int adjEnd = end + this.start;
        if(start < end || (start|end) < 0 || adjStart > this.end || adjEnd > this.end) {
            throw new StringIndexOutOfBoundsException();
        }
        return new BackedCharSequence(base, adjStart, adjEnd);
    }
}
