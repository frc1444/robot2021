package com.first1444.frc.robot2020;

import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;

public class LiveSimulation {
	public static void main(String[] args){
		// I can't get this to work so don't try it
		RobotBase.startRobot(Main::createRobot);
		System.out.println("Simulation starting");
		
		DriverStationSim.setDsAttached(false);
        DriverStationSim.setEnabled(false);
		
		System.out.println("Autonomous starting");
        DriverStationSim.setAutonomous(true);
        DriverStationSim.setEnabled(true);
		
		Timer.delay(15);
		System.out.println("Teleop starting");
        DriverStationSim.setEnabled(false);
		
		Timer.delay(.5);
        DriverStationSim.setAutonomous(false);
        DriverStationSim.setEnabled(true);
		
		Timer.delay(135);
        DriverStationSim.setEnabled(false);
		System.out.println("Match ended");
		
	}
}
