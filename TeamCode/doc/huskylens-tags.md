# HuskyLens AprilTag mapping

Training recorded on October 13, 2026. This mapping reflects the tags taught
on the team's original HuskyLens, not an assumed training order.

Use the **HuskyLens learned ID** in `hasHuskyLensTag(id)` and
`getHuskyLensTag(id)`. The `id` in returned detections and telemetry is also
the learned ID. The official AprilTag ID is the number on the field tag.

| Official AprilTag ID | HuskyLens learned ID | Cell |
| --- | --- | --- |
| 42 | 1 | Blue, opposite audience |
| 43 | 2 | Blue, opposite audience |
| 44 | 3 | Blue, opposite audience |
| 45 | 4 | Blue, opposite audience |
| 30 | 5 | Red, opposite audience |
| 31 | 6 | Red, opposite audience |
| 32 | 7 | Red, opposite audience |
| 33 | 8 | Red, opposite audience |
| 34 | 9 | Red, audience side |
| 35 | 10 | Red, audience side |
| 36 | 11 | Red, audience side |
| 37 | 12 | Red, audience side |
| 38 | 13 | Blue, audience side |
| 39 | 14 | Blue, audience side |
| 40 | 15 | Blue, audience side |
| 41 | 16 | Blue, audience side |

For example, `robot.vision.hasHuskyLensTag(1)` checks for official tag 42.
`robot.vision.getHuskyLensTag(1)` returns its latest detection or null if absent.

This file is a reference only; the code does not load it. If the sensor is
retrained or replaced, verify the learned IDs and update this mapping.
