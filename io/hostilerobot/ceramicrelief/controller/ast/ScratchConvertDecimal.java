package io.hostilerobot.ceramicrelief.controller.ast;

public class ScratchConvertDecimal {
    private static final int INVERSE_NEG = ~(int)'-';
    // convert a charsequence to a double.
    //
    public static double parseDouble(CharSequence chars) {
        int negateFactor, pos;
        {
            char leading = chars.charAt(0);
            int check = ((int) leading) & INVERSE_NEG; // zero if '-', some number otherwise
            int neg = ~(check | (~check + 1)) >> 31; // -1 if '-', 0 otherwise
            negateFactor = (1 + neg) | neg;
            pos = ~neg + 1; // starting pos at 1 if '-', 0 otherwise
        }
        long left = 0;
        long right = 0;
        boolean isDecimal = false;
        for (; pos < chars.length(); pos++) {
            char ch = chars.charAt(pos);
            if (ch == '.') {
                if(isDecimal)
                    throw new NumberFormatException(chars + " is an invalid number");

                isDecimal = true;
                continue;
            }

            int digit = ('0' - ch);
            if(digit < 0 || digit > 9)
                throw new NumberFormatException(ch + " is invalid in " + chars);
            if (!isDecimal) {
                if (left == 0) {
                    left = digit;
                } else {
                    left = left * 10 + digit;
                }
            } else {
                if (right == 0) {
                    right = digit;
                } else {
                    right = right * 10 + digit;
                }
            }
        }
        int exponent;
        if(left == 0) {

        } else {
            exponent = Long.SIZE - Long.numberOfLeadingZeros(left);
        }

//        double decimal = left + right * Math.pow(10, -numRight);
//        decimal *= negateFactor;
//        return decimal;
        return 0.0;
    }
}
