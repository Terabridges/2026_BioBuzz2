/**
 * Locations and Pedro path geometry, with no OpMode registration.
 * FieldLandmarks stores the field dimensions and reference points.
 * FieldPoses stores robot starting poses and waypoints.
 * Define named routes in AutoPaths using Pedro's Paths API.
 *
 * Group related paths in one class, not one class per individual path. For example,
 * a future PreloadPaths class could hold startToScore and scoreToPark. Shared paths
 * may be used by several routines. Path classes define where/how to drive; routines
 * define when to execute them and when to operate mechanisms.
 */
package org.firstinspires.ftc.teamcode.autonomous.paths;

