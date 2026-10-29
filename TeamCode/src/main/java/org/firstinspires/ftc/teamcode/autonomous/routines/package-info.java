/**
 * Complete autonomous strategies selected before Start, such as a future
 * preload-and-park routine or a collection-cycle routine.
 *
 * Each implements AutoRoutine and owns its sequence/state machine, path transitions,
 * timeouts, and recovery decisions. Reuse path classes and mechanism actions instead
 * of putting every strategy in one large base class.
 *
 * Obtain mechanism actions through AutoContext.mechanismAction so the same routine
 * can run in AutoPathTest. Check action completion separately from path completion;
 * test mode skips mechanism actions but must still wait for driving to finish.
 * The OpMode updates the follower and Robot; routines must not update them again.
 */
package org.firstinspires.ftc.teamcode.autonomous.routines;

