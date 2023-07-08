package io.hostilerobot.ceramicrelief.controller;

/**
 * what we want:
 *    watch a directory DIR for file changes
 *
 *    on an update: parse using the default parsing format
 *    default parsing format
 *        multiple sections followed by key/value pairs
 *        a key/value are essentially numbers, names, or a list of names/numbers
 *        the section defines how we handle encountering a new key/value pair
 *        can have different section plugins
 */