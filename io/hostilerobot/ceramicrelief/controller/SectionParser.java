package io.hostilerobot.ceramicrelief.controller;

public interface SectionParser<K, V> {
    public K parseKey(String k);
    public V parseVal(String v);
}
