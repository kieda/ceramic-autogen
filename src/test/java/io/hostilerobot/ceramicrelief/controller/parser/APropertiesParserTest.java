package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class APropertiesParserTest {
    private static final ASectionNameParser SECTION_NAME_PARSER = new ASectionNameParser();
    private static final ANameParser NAME_PARSER = new ANameParser();
    private static final APropertiesParser<CharSequence> NAME_PROPERTIES_PARSER = new APropertiesParser<>(SECTION_NAME_PARSER, List.of(NAME_PARSER, AWhitespaceParser.getInstance()));


    /* properties definition for testing */
    private sealed interface PropertyTree{}
    private record VProperties(VSection... sections) implements PropertyTree{}
    private record VSection(String sectionName, PropertyTree... items) implements PropertyTree{}
    private record VName(String name) implements PropertyTree{}
    private record VList(PropertyTree... items) implements PropertyTree{}
    private record VPair(PropertyTree key, PropertyTree val) implements PropertyTree{}

    private static VProperties pr(VSection... sections) {
        return new VProperties(sections);
    }
    private static VSection s(String sectionName, PropertyTree... items) {
        return new VSection(sectionName, items);
    }
    private static VList l(PropertyTree... items) {
        return new VList(items);
    }
    private static VName n(String name) {
        return new VName(name);
    }
    private static VPair p(PropertyTree key, PropertyTree val) {
        return new VPair(key, val);
    }


    private static void assertPropertiesEqual(PropertyTree expected, ANode<? extends Object> actual) {
        switch (expected) {
            case VName(String expectedName) -> {
                assertEquals(AName.class, actual.getClass());
                AName actualName = (AName) actual;
                assertEquals(expectedName, String.valueOf(actualName.getValue()));
            }
            case VSection(String expectedSectionName, PropertyTree[] expectedItems) -> {
                assertEquals(ASection.class, actual.getClass());
                ASection<Object> actualSection = (ASection<Object>) actual;
                assertNotNull(actualSection.getValue());
                ASectionName actualSectionName = actualSection.getValue().getKey();
                ANodeList<Object> actualNodes = actualSection.getValue().getVal();
                assertNotNull(actualSectionName);
                assertNotNull(actualNodes);
                assertEquals(expectedSectionName, String.valueOf(actualSectionName.getValue()));
                assertEquals(expectedItems.length, actualNodes.size());
                for(int i = 0; i < expectedItems.length; i++) {
                    assertPropertiesEqual(expectedItems[i], actualNodes.get(i));
                }
            }
            case VProperties(VSection[] expectedSections) -> {
                assertEquals(AProperties.class, actual.getClass());
                AProperties<Object> actualProperties = (AProperties<Object>) actual;
                assertNotNull(actualProperties.getValue());
                assertEquals(expectedSections.length, actualProperties.size());
                for(int i = 0; i < expectedSections.length; i++) {
                    assertPropertiesEqual(expectedSections[i], actualProperties.get(i));
                }
            }
            case VList(PropertyTree[] expectedItems) -> {
                assertEquals(AList.class, actual.getClass());
                var actualList = (AList<Object>) actual;
                assertNotNull(actualList.getValue());
                assertEquals(expectedItems.length, actualList.size());
                for (int i = 0; i < expectedItems.length; i++) {
                    assertPropertiesEqual(expectedItems[i], actualList.get(i));
                }
            }
            case VPair(PropertyTree key, PropertyTree val) -> {
                assertEquals(APair.class, actual.getClass());
                var actualPair = (APair<Object, Object>) actual;
                assertNotNull(actualPair.getValue());
                assertPropertiesEqual(key, actualPair.getValue().getKey());
                assertPropertiesEqual(val, actualPair.getValue().getVal());
            }
        }
    }

    /* test properties where all values are names */

    static class NamePropertiesArguments extends ParserTestArguments<PropertyTree> {
        {
            addStrippedTest("hello: abc def",
                    pr(s("hello", n("abc"), n("def"))));
            addStrippedTest("hello: abc def world:",
                    pr(
                            s("hello", n("abc"), n("def")),
                            s("world")
                    ));
            addStrippedTest("hello: abc def world: a123 bc3",
                    pr(
                            s("hello", n("abc"), n("def")),
                            s("world", n("a123"), n("bc3"))
                    ));

            // currently we allow "" as a valid section name
            // however we run into funky cases where "def" is picked up as the second section name
            // todo - either permit "" and add quotations for names, or disallow empty string (I'm leaning towards the latter)
            addStrippedTest(": abc def : a123 bc3",
                    pr(
                            s("", n("abc")),
                            s("def", n("a123"), n("bc3"))
                    ));
            addStrippedTest(":::abc",
                    pr(
                            s(""),
                            s(""),
                            s("", n("abc"))
                    ));
            addTest("abc: abc 123",
                    pr(s("abc", n("abc"))), "abc: abc".length()
            );
        }}

    @ParameterizedTest
    @ArgumentsSource(NamePropertiesArguments.class)
    void testNameProperties(String input, PropertyTree expectedParse, int expectedMatch) {
        assertEquals(expectedMatch, NAME_PROPERTIES_PARSER.match(input));
        assertPropertiesEqual(expectedParse, NAME_PROPERTIES_PARSER.parse(input));
    }

    /* test properties where values can be lists and names */

    private static final APropertiesParser<? extends Object> LIST_AND_NAMES_PROPERTY;
    static {
        List<AParser<? extends Object>> propertyParsers = new ArrayList<>();
        propertyParsers.add(null); // will be list parsing
        propertyParsers.add(NAME_PARSER);

        List<AParser<? extends Object>> listParsers = new ArrayList<>();
        listParsers.add(null); // will be properties parsing
        listParsers.addAll(propertyParsers);

        // we add whitespace parsing to the properties, but not to the list parsing.
        propertyParsers.add(AWhitespaceParser.getInstance());

        var listParser = new AListParser<>(listParsers); // I should be using var more often but old habits die hard
        propertyParsers.set(0, listParser);
        listParsers.set(1, listParser);

        // note that we don't have properties as a direct value of properties, but they may be permitted in lists
        // if we did have properties in the baseParsers, then there would be effectively no difference due to the format of properties
        LIST_AND_NAMES_PROPERTY = new APropertiesParser<>(SECTION_NAME_PARSER, propertyParsers);
        listParsers.set(0, LIST_AND_NAMES_PROPERTY);
    }

    static class ListPropertiesArguments extends ParserTestArguments<PropertyTree> {
        {
            addStrippedTest("hello: (world, k: val val2, wow) (haha, oops:, :)",
                    pr(
                        s("hello",
                                l(n("world"), pr(s("k", n("val"), n("val2"))), n("wow")),
                                l(n("haha"), pr(s("oops")), pr(s("")))
                        )
                    ));
            addStrippedTest("hello: (haha, oops: (abc, efg) val2 mySection:val3 (val4), end)",
                    pr(
                        s("hello",
                            l(
                                n("haha"),
                                pr(
                                    s("oops", l(n("abc"), n("efg")), n("val2")),
                                    s("mySection", n("val3"), l(n("val4")))
                                ),
                                n("end")
                            )
                        )
                    ));
            addStrippedTest("hello : (haha) : (why) :",
                    pr(
                        s("hello", l(n("haha"))),
                        s("", l(n("why"))),
                        s("")
                    ));
            addTest("hello : (haha # comment\n) : (why) : # haha this is a comment\n\t ",
                    pr(
                            s("hello", l(n("haha"))),
                            s("", l(n("why"))),
                            s("")
                    ), "hello : (haha # comment\n) : (why) :".length());
        }
    }

    @ParameterizedTest
    @ArgumentsSource(ListPropertiesArguments.class)
    void testListProperties(String input, PropertyTree expectedParse, int expectedMatch) {
        assertEquals(expectedMatch, LIST_AND_NAMES_PROPERTY.match(input));
        assertPropertiesEqual(expectedParse, LIST_AND_NAMES_PROPERTY.parse(input));
    }

    /* test properties where values can be pairs and names */
    private static final APropertiesParser<? extends Object> PAIRS_AND_NAMES_PROPERTY;
    static {
        List<AParser<? extends Object>> propertyParsers = new ArrayList<>();
        propertyParsers.add(null); // will be pair parsing
        propertyParsers.add(NAME_PARSER);

        List<AParser<? extends Object>> rawKeyParsers = new ArrayList<>(); // used to parse keys when it's in the RAW format. Disallow parsing properties here.
        rawKeyParsers.addAll(propertyParsers);

        List<AParser<? extends Object>> pairParsers = new ArrayList<>(); // used to parse pairs
        pairParsers.add(null); // will be property parsing
        pairParsers.addAll(propertyParsers);

        // we add whitespace parsing to the properties, but not to the pair parsing.
        propertyParsers.add(AWhitespaceParser.getInstance());

        var pairParser = new APairParser<>( // I should be using var more often but old habits die hard
            pairParsers, pairParsers,
            rawKeyParsers, pairParsers
        );
        propertyParsers.set(0, pairParser);
        rawKeyParsers.set(0, pairParser);
        pairParsers.set(1, pairParser);

        // note that we don't have properties as a direct value of properties, but they may be permitted in lists
        // if we did have properties in the baseParsers, then there would be effectively no difference due to the format of properties
        PAIRS_AND_NAMES_PROPERTY = new APropertiesParser<>(SECTION_NAME_PARSER, propertyParsers);
        pairParsers.set(0, PAIRS_AND_NAMES_PROPERTY);
    }

    static class PairPropertiesArguments extends ParserTestArguments<PropertyTree> {
        {
            addStrippedTest("""
                    test123:
                        {hello = world}
                        james = hi # hi james!
                        cats = cool
                    imports:
                        useMyImportPackage
                        add = lol
                    """, pr(
                    s("test123",
                        p(n("hello"), n("world")),
                        p(n("james"), n("hi")),
                        p(n("cats"), n("cool"))
                    ),
                    s("imports",
                        n("useMyImportPackage"),
                        p(n("add"), n("lol"))
                    )
            ));
            String nestedTest = """
                    testGroupVal:
                        {hello = world: abc def = wow}
                    testGroupKey:
                        {world: asdf = nope}
                    testRawVal:
                        hello =
                            world:
                                abc
                                def = wow
                            cats:
                                meow!# todo - we need to add in an optional ";" character to escape properties
                                     # because otherwise we can't get back to the previous level without using "{" "}" brackets
                    """;
            addTest(nestedTest, pr(
                    s("testGroupVal",
                        p(n("hello"), pr(s("world", n("abc"), p(n("def"), n("wow")))))
                    ),
                    s("testGroupKey",
                        p(
                            pr(s("world", n("asdf"))),
                            n("nope")
                        )
                    ),
                    s("testRawVal",
                        p(n("hello"), pr(
                            s("world", n("abc"), p(n("def"), n("wow"))),
                            s("cats", n("meow!"))
                        )))
            ), nestedTest.indexOf('#'));
            addStrippedTest("""
                    testGroupVal:
                        {hello = world: abc scooby_doo: scooby_snack = yum}
                    testGroupKey:
                        {scooby_doo: {scooby_snack = always} shaggy: {scooby_snack = needs_a_shower} = MysteryGang}
                    """, pr(
                        s("testGroupVal",
                            p(n("hello"),
                                pr(
                                    s("world", n("abc")),
                                    s("scooby_doo", p(n("scooby_snack"), n("yum")))
                                )
                            )
                        ),
                        s("testGroupKey",
                            p(pr(
                                s("scooby_doo", p(n("scooby_snack"), n("always"))),
                                s("shaggy", p(n("scooby_snack"), n("needs_a_shower")))
                            ),
                            n("MysteryGang"))
                        )
                    )
            );

        }
    }

    @ParameterizedTest
    @ArgumentsSource(PairPropertiesArguments.class)
    void testPairProperties(String input, PropertyTree expectedParse, int expectedMatch) {
//        assertEquals(expectedMatch, PAIRS_AND_NAMES_PROPERTY.match(input));
        var parsed = PAIRS_AND_NAMES_PROPERTY.parse(input);
        assertPropertiesEqual(expectedParse, parsed);
    }
}