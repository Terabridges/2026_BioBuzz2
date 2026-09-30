package org.firstinspires.ftc.teamcode.subsystems;

public interface Subsystem {
    void update();
    void toInit();

    default String getSubsystemName() {
        return getClass().getSimpleName();
    }
}