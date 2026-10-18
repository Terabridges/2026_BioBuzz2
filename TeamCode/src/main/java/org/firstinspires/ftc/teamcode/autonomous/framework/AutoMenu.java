package org.firstinspires.ftc.teamcode.autonomous.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Hardware-independent menu: alliance -> starting side -> routine/variation.
 * Browsing never changes the confirmed selection. Enter on a routine confirms it;
 * Back preserves the selection while returning to its parent folder.
 * Both OpModes use this model, with input/telemetry handled by SelectableAutoOpMode.
 */
public final class AutoMenu {
    private enum Level { ALLIANCE, SIDE, ROUTINE }

    private final List<AutoCatalog.Entry> entries;
    private Level level;
    private int cursor;
    private int allianceCursor;
    private int sideCursor;
    private Alliance alliance;
    private StartSide side;
    private AutoCatalog.Entry selected;

    public AutoMenu(List<AutoCatalog.Entry> entries) {
        if (entries.isEmpty()) throw new IllegalArgumentException("Auto menu needs at least one routine");
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        for (AutoCatalog.Entry entry : this.entries) Objects.requireNonNull(entry);
        reset();
    }

    public void reset() {
        level = Level.ALLIANCE;
        cursor = 0;
        allianceCursor = 0;
        sideCursor = 0;
        alliance = null;
        side = null;
        selected = entries.get(0);
    }

    public AutoCatalog.Entry selected() { return selected; }
    public int cursor() { return cursor; }

    public String location() {
        if (level == Level.ALLIANCE) return "Choose alliance";
        if (level == Level.SIDE) return alliance.label;
        return alliance.label + " > " + side.label;
    }

    public List<String> options() {
        if (level == Level.ALLIANCE) {
            return alliances().stream().map(value -> value.label).collect(Collectors.toList());
        }
        if (level == Level.SIDE) {
            return sides().stream().map(value -> value.label).collect(Collectors.toList());
        }
        return routines().stream().map(entry -> entry.config.name).collect(Collectors.toList());
    }

    public void move(int direction) {
        cursor = Math.floorMod(cursor + direction, options().size());
    }

    public void enter() {
        if (level == Level.ALLIANCE) {
            allianceCursor = cursor;
            alliance = alliances().get(cursor);
            level = Level.SIDE;
            cursor = 0;
        } else if (level == Level.SIDE) {
            sideCursor = cursor;
            side = sides().get(cursor);
            level = Level.ROUTINE;
            cursor = 0;
        } else {
            selected = routines().get(cursor);
        }
    }

    public void back() {
        if (level == Level.ROUTINE) {
            level = Level.SIDE;
            cursor = sideCursor;
        } else if (level == Level.SIDE) {
            level = Level.ALLIANCE;
            cursor = allianceCursor;
        }
    }

    private List<Alliance> alliances() {
        return entries.stream().map(entry -> entry.config.alliance).distinct()
                .sorted().collect(Collectors.toList());
    }

    private List<StartSide> sides() {
        return entries.stream().filter(entry -> entry.config.alliance == alliance)
                .map(entry -> entry.config.startSide).distinct().sorted().collect(Collectors.toList());
    }

    private List<AutoCatalog.Entry> routines() {
        return entries.stream().filter(entry -> entry.config.alliance == alliance
                && entry.config.startSide == side).collect(Collectors.toList());
    }
}
