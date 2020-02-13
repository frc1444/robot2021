package com.first1444.frc.robot2020.autonomous.actions;

import com.first1444.frc.robot2020.subsystems.BallShooter;
import com.first1444.frc.robot2020.subsystems.Intake;
import com.first1444.frc.robot2020.subsystems.balltrack.BallTracker;
import me.retrodaredevil.action.SimpleAction;

import static java.lang.Math.abs;

/**
 * Shoots all of the balls at a certain RPM
 */
public class ShootAllRpmAction extends SimpleAction {
    private static final double RPM_TOLERANCE = 100.0;
    private final Intake intake;
    private final BallShooter ballShooter;
    private final BallTracker ballTracker;
    private final double rpm;
    public ShootAllRpmAction(Intake intake, BallShooter ballShooter, BallTracker ballTracker, double rpm) {
        super(true);
        this.intake = intake;
        this.ballShooter = ballShooter;
        this.ballTracker = ballTracker;
        this.rpm = rpm;
    }

    @Override
    protected void onUpdate() {
        super.onUpdate();
        if(ballTracker.getBallCount() == 0){
            setDone(true);
            return;
        }
        ballShooter.setDesiredRpm(rpm);
        double currentRpm = ballShooter.getCurrentRpm();
        if(abs(rpm - currentRpm) < RPM_TOLERANCE){
            intake.setFeederSpeed(1.0);
            intake.setIndexerSpeed(.75);
        }
    }
}
