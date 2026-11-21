package org.firstinspires.ftc.teamcode.actions;

import org.firstinspires.ftc.teamcode.autonomous.framework.Alliance;
import org.firstinspires.ftc.teamcode.subsystems.outtake.Outtake;
import org.firstinspires.ftc.teamcode.utility.shooterUtility.TurretAim;

/** Enables continuous aim and spin-up. The routine decides when to stop shooting. */
public final class EnableShootingAction implements Action {
    private final Outtake outtake;
    private final Alliance alliance;
    private final TurretAim.Cell targetCell;
    private Result result = Result.READY;

    public EnableShootingAction(Outtake outtake, Alliance alliance, TurretAim.Cell targetCell) {
        this.outtake = outtake;
        this.alliance = alliance;
        this.targetCell = targetCell;
    }

    @Override public void start() {
        outtake.enableShooting(alliance, targetCell);
        result = Result.SUCCEEDED;
    }

    @Override public void update() {}
    @Override public Result result() { return result; }
    @Override public void stop() {
        if (result == Result.READY) result = Result.CANCELLED;
    }
}
