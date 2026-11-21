"""
Graphs every table in ShooterData.java: flywheel RPM, hood position, and flight time vs distance.

It reads the real files, so hand edits show up:
  - the dots are the rows parsed from ShooterData.java (the MIN_/MAX_ end rows are left out)
  - the line is sampled by compiling ShooterData.java and running FTCLib's own InterpLUT, so it is
    exactly what the robot gets between rows
  - the hood graph shows Shooter.java's hoodMin / hoodMax

Needs Python with matplotlib, a JDK (javac/java on PATH), and a Gradle build that has downloaded FTCLib.
Run from the repo root:

    python TeamCode/doc/tools/plot_shooter_data.py

Writes TeamCode/doc/images/shooter-rpm.png, shooter-hood.png and shooter-shot-time.png.
"""
import glob
import os
import re
import subprocess
import sys
import tempfile

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
CODE = os.path.join(REPO, "TeamCode", "src", "main", "java", "org", "firstinspires", "ftc", "teamcode")
SHOOTER_DATA = os.path.join(CODE, "utility", "shooterUtility", "ShooterData.java")
SHOOTER = os.path.join(CODE, "subsystems", "outtake", "Shooter.java")
OUT_DIR = os.path.join(REPO, "TeamCode", "doc", "images")

STEP = 0.25  # inches between curve samples

# Colors (light chart theme)
SURFACE, INK, INK2, MUTED, GRID, AXIS, BLUE = (
    "#fcfcfb", "#0b0b0b", "#52514e", "#898781", "#e1e0d9", "#c3c2b7", "#2a78d6")


def find_ftclib_jar():
    caches = os.path.join(os.path.expanduser("~"), ".gradle", "caches")
    jars = glob.glob(os.path.join(caches, "**", "core-2.1.1-runtime.jar"), recursive=True)
    if not jars:
        sys.exit("Couldn't find FTCLib (core-2.1.1-runtime.jar) in the Gradle cache. Build the project once first.")
    return jars[0]


def parse_rows(source, lut_name):
    """Measured rows of one table: lines like `rpmLUT.add(12.8, 2070);` (skips the MIN_/MAX_ end rows)."""
    pattern = re.compile(r"^\s*" + lut_name + r"\.add\(\s*([-\d.]+)\s*,\s*([-\d.]+)\s*\)", re.MULTILINE)
    return [(float(x), float(y)) for x, y in pattern.findall(source)]


def parse_double(source, name):
    match = re.search(name + r"\s*=\s*([-\d.]+)", source)
    return float(match.group(1)) if match else None


SAMPLER = """
import org.firstinspires.ftc.teamcode.utility.shooterUtility.ShooterData;
public class Sampler {
    public static void main(String[] args) {
        ShooterData data = new ShooterData();
        double step = Double.parseDouble(args[0]), max = Double.parseDouble(args[1]);
        for (double d = step; d <= max + 1e-9; d += step) {
            System.out.println(d + "," + data.getRPMVal(d) + "," + data.getAngleVal(d) + "," + data.getShotTimeVal(d));
        }
    }
}
"""


def sample_curves(max_distance):
    """Compiles the real ShooterData.java and asks it for every value from STEP to max_distance."""
    jar = find_ftclib_jar()
    with tempfile.TemporaryDirectory() as work:
        sampler = os.path.join(work, "Sampler.java")
        with open(sampler, "w") as f:
            f.write(SAMPLER)
        classes = os.path.join(work, "classes")
        os.makedirs(classes)
        subprocess.run(["javac", "-nowarn", "-d", classes, "-cp", jar, SHOOTER_DATA, sampler], check=True)
        out = subprocess.run(["java", "-cp", jar + os.pathsep + classes, "Sampler", str(STEP), str(max_distance)],
                             check=True, capture_output=True, text=True).stdout
    curves = {"rpm": [], "hood": [], "time": []}
    for line in out.split():
        d, rpm, hood, t = (float(v) for v in line.split(","))
        curves["rpm"].append((d, rpm))
        curves["hood"].append((d, hood))
        curves["time"].append((d, t))
    return curves


def plot_table(title, ylabel, rows, curve, fmt, out_name, x_max, limits=()):
    plt.rcParams.update({"font.family": ["Segoe UI", "DejaVu Sans"], "font.size": 11})
    fig, ax = plt.subplots(figsize=(11.5, 6.2), dpi=100)
    fig.patch.set_facecolor(SURFACE)
    ax.set_facecolor(SURFACE)

    ax.plot([c[0] for c in curve], [c[1] for c in curve], color=INK2, lw=2.5, zorder=2, solid_capstyle="round")
    ax.scatter([r[0] for r in rows], [r[1] for r in rows], s=80, color=BLUE, edgecolor=SURFACE,
               linewidth=2, zorder=3)

    # Value on every row, above the dot. When two rows are close together, the first one's label goes to its
    # left and the second one's to its lower right, so they don't collide (the tables rise, so the curve
    # leaves that dot upward or flat and the space below-right is clear)
    y_min = min(r[1] for r in rows)
    y_max = max(r[1] for r in rows)
    span = (y_max - y_min) or 1
    for i, (x, y) in enumerate(rows):
        close_before = i > 0 and x - rows[i - 1][0] < 6
        close_after = i + 1 < len(rows) and rows[i + 1][0] - x < 6
        if close_after and not close_before:
            offset, ha, va = (-9, 0), "right", "center"
        elif close_before:
            offset, ha, va = (7, -7), "left", "top"
        else:
            offset, ha, va = (0, 11), "center", "bottom"
        ax.annotate(fmt(y), (x, y), textcoords="offset points", xytext=offset, ha=ha, va=va,
                    color=INK2, fontsize=10)

    # Point out any place the table goes down as distance goes up
    for (x0, y0), (x1, y1) in zip(rows, rows[1:]):
        if y1 < y0:
            ax.annotate(f"Drops {fmt(y0)} → {fmt(y1)}\nfrom {x0:g} to {x1:g} in.", ((x0 + x1) / 2, (y0 + y1) / 2),
                        textcoords="offset points", xytext=(30, -45), color=INK2, fontsize=10,
                        arrowprops=dict(arrowstyle="-", color=MUTED, lw=1))

    # Servo limits as dashed lines, labeled at the left edge (the right end is where the data sits).
    # The top limit's label goes above its line and the bottom one's below, away from the curve
    middle = (y_min + y_max) / 2
    for value, label in limits:
        ax.axhline(value, color=MUTED, lw=1.2, ls=(0, (5, 4)), zorder=1)
        above = value >= middle
        ax.annotate(label, (0, value), textcoords="offset points", xytext=(6, 4 if above else -4), ha="left",
                    va="bottom" if above else "top", color=INK2, fontsize=10)

    pad = span * 0.12
    low = min([y_min] + [v for v, _ in limits]) - pad
    high = max([y_max] + [v for v, _ in limits]) + pad
    ax.set_xlim(0, x_max)
    ax.set_ylim(low, high)
    ax.set_xlabel("Distance, turret pivot to Cell (in.)", color=INK2)
    ax.set_ylabel(ylabel, color=INK2)
    ax.grid(True, color=GRID, lw=1)
    ax.set_axisbelow(True)
    for side in ("top", "right"):
        ax.spines[side].set_visible(False)
    for side in ("left", "bottom"):
        ax.spines[side].set_color(AXIS)
    ax.tick_params(colors=INK2, length=0)

    first, last = rows[0][0], rows[-1][0]
    fig.text(0.03, 0.955, title, fontsize=17, fontweight="bold", color=INK)
    fig.text(0.03, 0.905, f"Dots are the {len(rows)} rows in ShooterData.java; the line is what InterpLUT gives the "
             f"robot between them. Outside {first:g}–{last:g} in. it holds the end value.", fontsize=11.5, color=INK2)
    fig.subplots_adjust(left=0.08, right=0.98, top=0.85, bottom=0.10)
    path = os.path.join(OUT_DIR, out_name)
    fig.savefig(path, facecolor=SURFACE)
    plt.close(fig)
    print("wrote", os.path.relpath(path, REPO))


def main():
    with open(SHOOTER_DATA) as f:
        source = f.read()
    with open(SHOOTER) as f:
        shooter_source = f.read()

    rpm_rows = parse_rows(source, "rpmLUT")
    hood_rows = parse_rows(source, "angleLUT")
    time_rows = parse_rows(source, "shotTimeLUT")
    x_max = max(r[0] for r in rpm_rows + hood_rows + time_rows) + 8
    curves = sample_curves(x_max)

    hood_limits = []
    hood_min = parse_double(shooter_source, "hoodMin")
    hood_max = parse_double(shooter_source, "hoodMax")
    if hood_max is not None:
        hood_limits.append((hood_max, f"hoodMax {hood_max:g}"))
    if hood_min is not None:
        hood_limits.append((hood_min, f"hoodMin {hood_min:g}"))

    plot_table("Flywheel RPM table (ShooterData)", "Flywheel RPM", rpm_rows, curves["rpm"],
               lambda v: f"{v:.0f}", "shooter-rpm.png", x_max)
    plot_table("Hood table (ShooterData)", "Hood servo position", hood_rows, curves["hood"],
               lambda v: f"{v:.3f}", "shooter-hood.png", x_max, hood_limits)
    plot_table("Flight time table (ShooterData)", "Flight time (s)", time_rows, curves["time"],
               lambda v: f"{v:.2f} s", "shooter-shot-time.png", x_max)


if __name__ == "__main__":
    main()
