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
    }

    private static class DoubleChar implements CharSequence {
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
            if((start|end) < 0 || start > 2 || end > 2) {
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

    public static void main(String[] args) {
        String s = "a";
        System.out.println(s.subSequence(0,0));
        System.out.println(s.subSequence(1,1));
//        System.out.println(s.subSequence(-1,-1));
    }
}
