package io.hostilerobot.ceramicrelief.controller;

/**
 * what we want:
 *    watch a directory DIR for file changes
 *
 *    on an update: parse using the default parsing format
 *    default parsing format: JSON
 *
 *    using: apache commons IO FileAlterationMonitor
 *          + jackson JSON/databind
 *
 *  how it works:
 *      * watch a directory for .json files
 *      * each .json file will represent a test case
 *      * on a change, we run whatever code is associated with the test
 *
 *  format:
 *      {
 *          "type": JavaClassTestType
 *          "vertices": ( [ vertex ... ] | { name : vertex } )
 *          "faces": [ triangle ... ]
 *      }
 *
 *
 */