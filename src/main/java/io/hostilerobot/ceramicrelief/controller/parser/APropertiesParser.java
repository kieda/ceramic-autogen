package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.*;

import java.util.List;

public class APropertiesParser<X> implements AParser<List<ASection<X>>> {
    private final ASectionNameParser sectionNameParser;
    private final List<AParser<? extends X>> parsers;

    public APropertiesParser(ASectionNameParser sectionNameParser, List<AParser<? extends X>> parsers) {
        this.sectionNameParser = sectionNameParser;
        this.parsers = parsers;
    }

    @Override
    public AProperties<X> parse(CharSequence cs) {
        AProperties<X> result = new AProperties<>();
        int sectionPos = sectionNameParser.match(cs);

        final int totalLen = cs.length();
        int finalPos = sectionPos;
        ASectionName currentName = sectionNameParser.parse(cs.subSequence(0, sectionPos));
        cs = cs.subSequence(sectionPos, cs.length());
        ANodeList<X> currentNodes = new ANodeList<>();

        foundMatch:
        while(finalPos < totalLen) {
            // section name takes the highest precedence
            // this is since the name might also be valid, but then we're left with a dangling colon
            sectionPos = sectionNameParser.match(cs);
            if (sectionPos >= 0) {
                // add currentName and currentNodes as a section
                result.add(new ASection<>(currentName, currentNodes));
                currentNodes = new ANodeList<>(); // reset node list

                // get the new name
                currentName = sectionNameParser.parse(cs.subSequence(0, sectionPos));
                cs = cs.subSequence(sectionPos, cs.length());
                finalPos += sectionPos;
                continue foundMatch;
            }

            for (AParser<? extends X> pp : parsers) {
                int matchPos = pp.match(cs);
                if (matchPos >= 0) {
                    ANode<? extends X> parsedNode = pp.parse(cs.subSequence(0, matchPos));
                    if(!parsedNode.ignore()) {
                        // don't add nodes for ignored nodes, but still skip forward
                        currentNodes.add((ANode<X>) parsedNode);
                    }
                    cs = cs.subSequence(matchPos, cs.length());
                    finalPos += matchPos;
                    continue foundMatch;
                    // found a value match. Continue to find more value matches
                }
            }

            // no matches found.
            break foundMatch;
        }
        // add the last section to the result
        result.add(new ASection<>(currentName, currentNodes));
        return result;
    }

    @Override
    public int match(CharSequence cs) {
        // we must start out with a section
        int sectionPos = sectionNameParser.match(cs);
        if(sectionPos < 0) {
            return -1;
        }
        final int totalLen = cs.length();
        int finalPos = sectionPos;
        cs = cs.subSequence(sectionPos, cs.length());

        // move forward
        foundMatch:
        while(finalPos < totalLen) {
            // move forward a sectionName if we found one
            // sectionName takes highest precedence
            sectionPos = sectionNameParser.match(cs);
            if (sectionPos >= 0) {
                cs = cs.subSequence(sectionPos, cs.length());
                finalPos += sectionPos;
                continue foundMatch;
            }

            for (AParser<? extends X> pp : parsers) {
                int matchPos = pp.match(cs);
                if (matchPos >= 0) {
                    cs = cs.subSequence(matchPos, cs.length());
                    finalPos += matchPos;
                    continue foundMatch;
                    // found a value match. Continue to find more value matches
                }
            }

            // no matches found. Return pos
            return finalPos;
        }

        return finalPos;
    }
}
