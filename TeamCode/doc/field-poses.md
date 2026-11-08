# BIOBUZZ field landmarks

`autonomous/paths/FieldPoses.java` stores nominal field references for building paths.
It does not define autonomous routes or measured robot starting poses.

## Coordinates

Look down on the field from the primary audience side, with red on the left.
The near-left inside corner is `(0, 0)`; X increases right and Y increases toward
the rear/scoring side. Distances are inches. Pedro headings are radians:
right = 0, rear = pi/2, left = pi, audience = 3pi/2.

The code uses a **nominal 144 x 144 planning frame**, matching the existing
`PoseLogging` conversion and [Pedro's coordinate documentation](https://pedropathing.com/docs/pathing/reference/coordinates).
For FTC-centered coordinates, `pedroX = ftcY + 72`, `pedroY = 72 - ftcX`,
and `pedroHeading = ftcHeading - pi/2`.

The source dimensions and tile locations are official. The Pedro coordinates
in the table are our calculations from that nominal layout, not exact coordinate
values published by FIRST. Raised CELL targets below are derived separately from
the official CAD. The code stores coordinates
as literal numbers; the 24-inch tile size is not a configurable variable.
They are not a survey of the practice field. Edge tiles are trimmed, and perimeter types vary;
measure actual landmarks in this same frame before using them in competition.
Do not rescale inches to make a different perimeter fit 144.

## Landmarks

| Constant | X | Y | Reference / derivation |
| --- | ---: | ---: | --- |
| `HIVE_FRAME_CENTER` | 72 | 72 | Fixed structure at field center |
| `RED_GARDEN_CENTER` | 11.5 | 1 | A1; midpoint of 23 x 2 strip |
| `BLUE_GARDEN_CENTER` | 132.5 | 143 | F6; opposite Garden |
| `RED_LOADING_ZONE_CENTER` | 5.5 | 108 | A5; half of 11-inch depth, row 5 midpoint |
| `BLUE_LOADING_ZONE_CENTER` | 138.5 | 36 | F2; opposite Loading Zone |
| `FLOWER_AUDIENCE_LEFT_WALL_ANCHOR` | 48 | 0 | B/C seam at audience wall |
| `FLOWER_AUDIENCE_RIGHT_WALL_ANCHOR` | 96 | 0 | D/E seam at audience wall |
| `FLOWER_REAR_LEFT_WALL_ANCHOR` | 48 | 144 | B/C seam at rear wall |
| `FLOWER_REAR_RIGHT_WALL_ANCHOR` | 96 | 144 | D/E seam at rear wall |

Left/right always mean audience-view left/right. Alliance partners are related
by a **180-degree rotation**: `(x, y) -> (144 - x, 144 - y)`.
For a robot pose, also add pi to the heading and wrap it to one revolution.

## Using these references

Every landmark has a placeholder heading of zero. Choose a robot heading and
offset the reference for chassis clearance and mechanism reach in each path.
The Garden strips and Loading Zones are too shallow to treat their centers as
generic robot-center targets. Flower anchors mark the wall attachment, so measure
the ring center and intake/scoring offset before choosing a Flower drive pose.

The frame occupies the center of the field; its nominal width/depth are
49.46 x 38.95 inches. `HIVE_FRAME_CENTER` is not a waypoint through the structure.
CELLS tip during play, so their openings and AprilTags are not fixed field poses.
Use the raised CELL targets below only when the selected CELL is fully up.

## Raised CELL aiming targets

Each coordinate assumes that specific CELL is fully raised, with the opposite
damper compressed. These are shooting references, not robot drive destinations.
Audience/rear identify the two ends of each HIVE; rear is the official scoring side.

| Constant | X | Y |
| --- | ---: | ---: |
| `RED_HIVE_AUDIENCE_CELL_UP_AIM` | 59.25 | 56.2 |
| `RED_HIVE_REAR_CELL_UP_AIM` | 59.25 | 87.8 |
| `BLUE_HIVE_AUDIENCE_CELL_UP_AIM` | 84.75 | 56.2 |
| `BLUE_HIVE_REAR_CELL_UP_AIM` | 84.75 | 87.8 |

`HIVE_CELL_UP_AIM_HEIGHT_INCHES` is **59.5 inches above the tiles**.
Pedro `Pose` holds the horizontal X/Y projection only; its heading is not height.

Derivation: figure 9-10 gives a 25.5-inch HIVE centerline separation, so the
centered X coordinates are 59.25 and 84.75. The official STEP CAD v26-27.2
contains a raised red audience CELL. The midpoint of the inner opening boundary's
bounds on its front rib is about 15.82 inches toward the audience from the frame
center, at about 59.47 inches high. Rounded to tenths, this gives Y = 56.2 and
height = 59.5. The other raised targets follow the structure's symmetry about
the centered frame. These are calculated nominal targets, not published Pedro
coordinates or calibrated shot solutions. Each HIVE has only one raised CELL
at a time; choose the active end before aiming.

`TEST_START` remains a local test origin used by Do Nothing. Replace it with a
measured robot-center start when registering an actual route in `AutoCatalog`.

## Official sources

Verified November 8, 2026:

- [BIOBUZZ ARENA, TU06](https://ftc-resources.firstinspires.org/ftc/archive/2027/game/manual-09):
  section 9.2 (nominal field/tiles), figures 9-2 through 9-5 (zones/grid),
  section 9.6 and figure 9-8 (frame and moving Hives).
- [Event Field Setup Guide, V1.0](https://ftc-resources.firstinspires.org/ftc/archive/2027/field/eventfieldguide):
  sections 7 (trimmed tiles), 8.3-8.4 (Loading Zones/Gardens),
  9 (centered frame), and 10 (Flower mounts).
- [Field CAD and acceptance resources](https://ftc-resources.firstinspires.org/ftc/archive/2027/field)
  for checking dimensions against the actual field.
- [Official STEP CAD, v26-27.2](https://ftc-resources.firstinspires.org/ftc/archive/2027/field/field-cad-step):
  raised red audience CELL opening, front `am-5866-red: Red Goal Rib` inner boundary.
  The CAD's vertical Y and audience-facing Z axes are converted to height and Pedro Y.
