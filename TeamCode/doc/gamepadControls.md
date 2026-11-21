# GAMEPAD 1

# Booleans
* A: Toggle ramp
* X: Toggle Shooting (will auto shoot if in zone)
* Y: Manual shot, hold (turns the flywheel on, shoots while held once it's at speed, stops when let go. Ignores zone, turret and ball count)
* B: Toggle flywheel
* Down: Switch target Cell (close / far, starts on close). Auto shoot only fires from the target Cell's zone
* Left:
* Up:
* Right:
* LBump: While held, slow mode on
* RBump:
* LStickButton:
* RStickButton:
* Back:
* Start:

# 0 - 1 Values
* LTrigger: Intake out
* RTrigger: Intake in (intake + transfer 100% until senses first ball, then transfer drops to 30%. At 4 balls the intake drops to 30% too)

# -1 - 1 Values
* LStickX: Movement
* LStickY: Movement
* RStickX: Movement
* RStickY: Movement

# GAMEPAD 2

# Booleans
* A: Reset pose, far corner (our side wall, back against the rear wall, facing the audience), and clear offsets
* X: Reset pose, close corner (our side wall, back against the audience wall, facing away from the audience), and clear offsets
* Y: Recalibrate the Pinpoint gyro (robot held still 0.5 s; press with X or A in the corner to fix heading too)
* B: clear offsets
* Down: offset hood down
* Left: offset turret left
* Up: offset hood up
* Right: offset turret right
* LBump: offset rpm down
* RBump: offset rpm up
* LStickButton:
* RStickButton:
* Back:
* Start: Switch alliance (red / blue). Works in INIT and during the match; remembered until the app restarts

# 0 - 1 Values
* LTrigger:
* RTrigger:

# -1 - 1 Values
* LStickX:
* LStickY:
* RStickX:
* RStickY: