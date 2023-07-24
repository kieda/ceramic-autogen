package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.ANode;
import io.hostilerobot.ceramicrelief.controller.ast.AProperties;
import io.hostilerobot.ceramicrelief.controller.ast.ASection;

import java.util.ArrayList;
import java.util.List;

/**
 * the top level of yap files will be in the Properties format.
 * we could have this just be a list of values if we wanted, but it's ok to enforce at least a little bit of structure, no?
 */
public class YappingParser implements AParser<List<ASection<? extends Object>>> {
    /*
     * name :
     * asdf = asdf1
     * (key, val) = (1, 2)
     * ==
     *
     * asdf = name : 123 key = val name2: key = val
     * {asdf = name : [123, key = val] name2: key = val }
     * name: {asdf 123 = 123}
     * name: asdf {123 = 123}
     *
     * {name: asdf 123 = 123} -- if we're in GROUP Properties may be a key
     *                        -- if we're in RAW Properties may not be a key.
     * (name: asdf, name2: asdf = 123)
     * (name:[asdf], name2:[asdf = 123])
     * (name:[asdf], name2:[{asdf = name3:[asdf hijk]}])
     *
     * (yes. we can resolve this too)
     * name2:
     *    {asdf = name3: asdf}
     *    hijk
     * properties may not be a value in properties.
     *
     * Base = Pairs, Lists, Quotients, Decimals, Names, Comments, Whitespace
     * List = (Base)
     * Pairs = (keyGroup: Base, valGroup: Base + Properties, keyRaw: Base, valRaw: Base + Properties)
     * Properties = Base
     */
    AQuotientParser QUOTIENTS = new AQuotientParser();
    ADecimalParser DECIMALS = new ADecimalParser();
    ANameParser NAMES = new ANameParser();
    List<AParser<? extends Object>> BASE_PARSE_TYPES = new ArrayList<>();
    {
        // (123 , 456) = (123, 456) - parse pairs first
        BASE_PARSE_TYPES.add(null); // will be Pairs
        BASE_PARSE_TYPES.add(null); // will be lists
        BASE_PARSE_TYPES.add(QUOTIENTS); // parse quotients first 123 12/3
        BASE_PARSE_TYPES.add(DECIMALS);  // decimals next
        BASE_PARSE_TYPES.add(NAMES);     // then names
        BASE_PARSE_TYPES.add(ACommentParser.getInstance());  // then comments
        BASE_PARSE_TYPES.add(AWhitespaceParser.getInstance()); // finally whitespace
        // todo - remove whitespace parsing except on properties. Other items have their own rules for whitespace.
    }
    AListParser<? extends Object> LISTS = new AListParser<>(BASE_PARSE_TYPES);
    {
        BASE_PARSE_TYPES.set(1, LISTS);
    }
    List<AParser<? extends Object>> BASE_AND_PROPERTIES = new ArrayList<>();
    {
        BASE_AND_PROPERTIES.add(null); // very first is properties
        BASE_AND_PROPERTIES.addAll(BASE_PARSE_TYPES); // then parse the rest
    }
    // {asdf: asdf = asdf = asdf = asdf}
    // how would this resolve?
    // {(asdf: asdf) = (asdf = asdf = asdf)}
    // {(asdf: asdf) = (asdf = (asdf = asdf))}
    APairParser<? extends Object, ? extends Object> PAIRS = new APairParser<>(
            // group key,        group val
            BASE_AND_PROPERTIES, BASE_AND_PROPERTIES,
            // raw key,          raw val
            BASE_PARSE_TYPES,    BASE_AND_PROPERTIES);
    {
        BASE_PARSE_TYPES.set(0, PAIRS);
        BASE_AND_PROPERTIES.set(1, PAIRS);
    }

    ASectionNameParser SECTION_NAMES = new ASectionNameParser();
    APropertiesParser<? extends Object> PROPERTIES = new APropertiesParser<>(SECTION_NAMES, BASE_PARSE_TYPES);
    {
        BASE_AND_PROPERTIES.set(0, PROPERTIES);
    }

    @Override
    public AProperties parse(CharSequence cs) {
        return PROPERTIES.parse(cs);
    }

    @Override
    public int match(CharSequence cs) {
        return PROPERTIES.match(cs);
    }
}
