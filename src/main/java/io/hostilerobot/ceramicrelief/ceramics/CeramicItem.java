package io.hostilerobot.ceramicrelief.ceramics;

import org.apache.commons.math.fraction.Fraction;

/**
 * representation of a physical ceramic item built from slabs
 */
public class CeramicItem {
    // units for the physical object
    private Unit unit;
    // scale for the object
    private double scale;
    // representation of the item via planes
    private PlanarItem representation;
    // thickness of the walls
    private Fraction thickness;

    // todo - center of mass?
}
