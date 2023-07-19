package io.hostilerobot.ceramicrelief.controller.parser;
/**
 * each parser comes equipped with two methods,
 *    int match(CharSequence cs)
 *    Node<T> parse(CharSequence cs)
 *
 *    such that match(cs) >= 0 if and only if the parser is directly looking at a value it can parse, and its result
 *    is the exact length from 0 to the end of the parsable item.
 *
 *    behavior is only defined in the following manner:
 *      * parse is only called if match(cs) >= 0
 *      * parse is only called on a string of length match(s, cs), such that when we call parse(v)
 *        v === cs.subSequence(0, match(cs))
 *      * implementing classes may choose to only parse on an exact match, or may choose to accept leading/traling whitespace, etc,
 *        as long as the match contracts are guaranteed.
 */