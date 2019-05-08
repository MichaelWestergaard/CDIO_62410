package main;

import lejos.hardware.BrickFinder;
import lejos.hardware.Keys;
import lejos.hardware.ev3.EV3;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.navigation.MovePilot;
import lejos.robotics.navigation.Navigator;
import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;
import lejos.utility.Delay;

public class Main {

	public static void main(String[] args) {
		
		EV3LargeRegulatedMotor LeftDrivingMotor = new EV3LargeRegulatedMotor(MotorPort.A);
		EV3LargeRegulatedMotor RightDrivingMotor = new EV3LargeRegulatedMotor(MotorPort.C);

		EV3LargeRegulatedMotor LeftCollectMotor = new EV3LargeRegulatedMotor(MotorPort.D);
		EV3LargeRegulatedMotor RightCollectMotor = new EV3LargeRegulatedMotor(MotorPort.B);
		
		EV3 ev3brick = (EV3) BrickFinder.getLocal();
		Keys buttons = ev3brick.getKeys();
		
		Waypoint[] coordinates = {
				new Waypoint(0, 20),
				new Waypoint(20, 20),
				new Waypoint(20, 0),
				new Waypoint(0, 0)
		};

		Wheel wheel1 = WheeledChassis.modelWheel(LeftDrivingMotor, 5.5).offset(-6);
		Wheel wheel2 = WheeledChassis.modelWheel(RightDrivingMotor, 5.5).offset(6);

		Chassis chassis = new WheeledChassis( new Wheel[] { wheel1, wheel2 },
				WheeledChassis.TYPE_DIFFERENTIAL);

		MovePilot ev3robot = new MovePilot(chassis);
		Navigator navbot = new Navigator(ev3robot);
		
		Path directions = new Path();
		for(Waypoint wp : coordinates) {
			directions.add(wp);
		}
		
		navbot.setPath(directions);
		navbot.singleStep(true);

		
		while(navbot.getWaypoint() != null) {
			LCD.drawString("Moving to", 0, 4);
			LCD.drawString("x: " + navbot.getWaypoint().x + " y: " + navbot.getWaypoint().y, 0, 5);
			navbot.followPath();
		}
		
	}

}