/**
 * Locations and Pedro path geometry, with no OpMode registration.
 * FieldPoses stores shared coordinates; path classes connect them with lines,
 * curves, headings, and path settings.
 *
 * Group related paths in one class, not one class per individual path. For example,
 * a future PreloadPaths class could hold startToScore and scoreToPark. Shared paths
 * may be used by several routines. Path classes define where/how to drive; routines
 * define when to execute them and when to operate mechanisms.
 *
 * Currently only FieldPoses exists; actual routes will be added as they are defined.
 */
package org.firstinspires.ftc.teamcode.autonomous.paths;

