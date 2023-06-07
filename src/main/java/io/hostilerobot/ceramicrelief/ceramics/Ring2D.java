package io.hostilerobot.ceramicrelief.ceramics;

import org.apache.commons.math.fraction.Fraction;

import java.util.ArrayList;
import java.util.Set;

public class Ring2D {
    // number of sides in the ring
    private int sides;
    // we may lock a ratio so it doesn't get automatically modified
    private ArrayList<Boolean> isRatioLocked;
    private ArrayList<Boolean> isAngleLocked;
    // represents the current state of the side ratios
    private ArrayList<Fraction> sideRatios;
    // represents the current state of the angles
    private ArrayList<Fraction> angles;

    // we can have a set of ratios or angles synced so they are the same in a group.
    // this does not necessarily mean that they are locked. However, if one of the items in the group is locked, then the rest of them are.
    private ArrayList<Set<Integer>> syncedRatios;
    private ArrayList<Set<Integer>> syncedAngles;
}
