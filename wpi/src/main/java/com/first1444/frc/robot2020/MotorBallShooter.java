package com.first1444.frc.robot2020;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.InvertType;
import com.ctre.phoenix.motorcontrol.TalonFXFeedbackDevice;
import com.ctre.phoenix.motorcontrol.can.TalonFX;
import com.first1444.dashboard.shuffleboard.PropertyComponent;
import com.first1444.dashboard.shuffleboard.SendableComponent;
import com.first1444.dashboard.value.BasicValue;
import com.first1444.dashboard.value.ValueProperty;
import com.first1444.frc.robot2020.subsystems.BallShooter;
import com.first1444.frc.robot2020.subsystems.balltrack.BallTracker;
import com.first1444.frc.util.pid.PidKey;
import com.first1444.frc.util.valuemap.sendable.MutableValueMapSendable;

import static com.first1444.frc.robot2020.CtreUtil.nativeToRpm;
import static com.first1444.frc.robot2020.CtreUtil.rpmToNative;

public class MotorBallShooter implements BallShooter {
    private static final boolean VELOCITY_CONTROL = true;
    private static final double BALL_DETECT_CURRENT_THRESHOLD = 40.0; // TODO change
    private static final double BALL_DETECT_CURRENT_RECOVERY = 20.0; // TODO change
    private final BallTracker ballTracker;
    private final DashboardMap dashboardMap;
    private final TalonFX talon;

    private double rpm;
    private double currentRpm;
    private boolean ballDetectThresholdMet = false;

    public MotorBallShooter(BallTracker ballTracker, DashboardMap dashboardMap) {
        this.ballTracker = ballTracker;
        this.dashboardMap = dashboardMap;
        talon = new TalonFX(RobotConstants.CAN.SHOOTER);
        talon.configFactoryDefault(RobotConstants.INIT_TIMEOUT);
        talon.setInverted(InvertType.InvertMotorOutput);
        talon.setSensorPhase(true);
        talon.configSelectedFeedbackSensor(TalonFXFeedbackDevice.IntegratedSensor, RobotConstants.PID_INDEX, RobotConstants.INIT_TIMEOUT);

        var sendable = new MutableValueMapSendable<>(PidKey.class);
        var pidConfig = sendable.getMutableValueMap();
        pidConfig.setDouble(PidKey.CLOSED_RAMP_RATE, .25);
        pidConfig.setDouble(PidKey.P, .1);
        pidConfig.setDouble(PidKey.I, .00014);

        CtreUtil.applyPid(talon, pidConfig, RobotConstants.INIT_TIMEOUT);
        pidConfig.addListener(key -> CtreUtil.applyPid(talon, pidConfig, RobotConstants.LOOP_TIMEOUT));

        dashboardMap.getDebugTab().add("Shooter PID", new SendableComponent<>(sendable));

        dashboardMap.getDebugTab().add("Shooter Actual (Native)", new PropertyComponent(ValueProperty.createGetOnly(() -> BasicValue.makeDouble(talon.getSelectedSensorVelocity()))));
    }

    @Override
    public void setDesiredRpm(double rpm) {
        this.rpm = rpm;
    }

    @Override
    public void run() {
        final double rpm = this.rpm;
        this.rpm = 0;
        if(rpm != 0 && VELOCITY_CONTROL){
            double velocity = rpmToNative(rpm, RobotConstants.FALCON_ENCODER_COUNTS_PER_REVOLUTION);
            talon.set(ControlMode.Velocity, velocity);
            dashboardMap.getDebugTab().getRawDashboard().get("Shooter Desired RPM").getStrictSetter().setDouble(rpm);
            dashboardMap.getDebugTab().getRawDashboard().get("Shooter Desired Velocity").getStrictSetter().setDouble(velocity);
        } else {
            talon.set(ControlMode.PercentOutput, rpm / BallShooter.MAX_RPM);
            dashboardMap.getDebugTab().getRawDashboard().get("Shooter Desired RPM").getStrictSetter().setDouble(rpm);
            dashboardMap.getDebugTab().getRawDashboard().get("Shooter Desired Velocity").getStrictSetter().setDouble(0.0);
        }
        double currentRpm = nativeToRpm(talon.getSelectedSensorVelocity(), RobotConstants.FALCON_ENCODER_COUNTS_PER_REVOLUTION);
        this.currentRpm = currentRpm;
        dashboardMap.getDebugTab().getRawDashboard().get("Shooter Actual (RPM)").getStrictSetter().setDouble(currentRpm);

        double current = talon.getSupplyCurrent();
        if(current > BALL_DETECT_CURRENT_THRESHOLD){
            ballDetectThresholdMet = true;
        } else if(current < BALL_DETECT_CURRENT_RECOVERY){
            if(ballDetectThresholdMet){ // ball just left
                ballTracker.removeBallTop();
            }
            ballDetectThresholdMet = false;
        }
    }

    @Override
    public double getCurrentRpm() {
        return currentRpm;
    }
}
