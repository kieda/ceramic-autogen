package io.hostilerobot.ceramicrelief.util.chars;

public class SmallCharSequence {
    private static CharSequence EMPTY = new Empty();
    private static class Empty implements CharSequence{
        private Empty() {}

        @Override
        public int length() {
            return 0;
        }

        @Override
        public char charAt(int index) {
            throw new StringIndexOutOfBoundsException(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            if((start|end) == 0) {
                return make();
            }
            throw new StringIndexOutOfBoundsException();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public String toString() {
            return "";
        }

        @Override
        public int hashCode() {
            return "".hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj instanceof CharSequence cs) {
                return cs.isEmpty();
            }
            return false;
        }
    }
    private static class SingleChar implements CharSequence{
        private final char value;
        private SingleChar(char value) {
            this.value = value;
        }
        @Override
        public int length() {
            return 1;
        }

        @Override
        public char charAt(int index) {
            if(index != 0) {
                throw new StringIndexOutOfBoundsException(index);
            }
            return value;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            int together = (start|end);
            if(together != 0 || together != 1) {
                // start and end can either be 1 or 0
                if(start == end)
                    return make();
                else
                    return this;
            }
            throw new StringIndexOutOfBoundsException();
        }

        private String toString = null;
        @Override
        public String toString() {
            if(toString == null)
                toString = String.valueOf(value);
            return toString;
        }
        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if(obj == this)
                return true;
            if(obj instanceof SingleChar one) {
                return one.value == this.value;
            } else if(obj instanceof CharSequence other && other.length() == this.length()) {
                return other.charAt(0) == this.charAt(0);
            }
            return false;
        }
    }

    private final static class DoubleChar implements CharSequence {
        private final char first;
        private final char second;
        private DoubleChar(char first, char second) {
            this.first = first;
            this.second = second;
        }
        @Override
        public int length() {
            return 2;
        }

        @Override
        public char charAt(int index) {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            int sub = end - start;
            if((start|end|sub) < 0 || start > 2 || end > 2) {
                throw new StringIndexOutOfBoundsException();
            }

            switch(end - start) {
                case 0:
                    return make();
                case 1:
                    if(start == 0) // 0..1
                        return make(first);
                    else // 1..2
                        return make(second);
                default: // 0..2
                    return this;
            }
        }

        private String toString = null;
        @Override
        public String toString() {
            if(toString == null)
                toString = String.valueOf(new char[]{first, second});
            return toString;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if(obj == this)
                return true;
            if(obj instanceof DoubleChar two) {
                return two.first == this.first && two.second == this.second;
            } else if(obj instanceof CharSequence other && other.length() == this.length()) {
                return other.charAt(0) == this.charAt(0) && other.charAt(1) == this.charAt(1);
            }
            return false;
        }
    }

    public static CharSequence make() {
        return EMPTY;
    }
    public static CharSequence make(char c) {
        return new SingleChar(c);
    }
    public static CharSequence make(char a, char b) {
        return new DoubleChar(a, b);
    }
}
