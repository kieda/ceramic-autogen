//package io.hostilerobot.ceramicrelief.controller.ast;
//
//import java.util.AbstractMap;
//import java.util.Map;
//
//public class ASection<PK, PV> implements ANode<Map.Entry<String, AList<APair<PK, PV>>>> {
//    private final Map.Entry<String, AList<APair<PK, PV>>> section;
//    public ASection(String sectionName,
//                    AList<APair<PK, PV>> sectionItems) {
//        section = new AbstractMap.SimpleImmutableEntry<>(sectionName, sectionItems);
//    }
//
//    @Override
//    public Map.Entry<String, AList<APair<PK, PV>>> getValue() {
//        return section;
//    }
//
//    @Override
//    public int size() {
//        return section.getValue().size();
//    }
//}
