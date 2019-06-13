package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import lejos.hardware.Audio;
import lejos.hardware.BrickFinder;
import lejos.hardware.Keys;
import lejos.hardware.ev3.EV3;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.internal.ev3.EV3Battery;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.robotics.navigation.MovePilot;
import lejos.robotics.navigation.Navigator;
import lejos.robotics.navigation.Waypoint;
import lejos.robotics.pathfinding.Path;
import lejos.utility.Delay;

public class Main {

	static EV3LargeRegulatedMotor LeftDrivingMotor, RightDrivingMotor;
	static EV3MediumRegulatedMotor LeftCollectMotor, RightCollectMotor;
	static MovePilot pilot;
	static EV3 ev3brick;
	static Keys buttons;
	static Wheel wheel1, wheel2;
	static Chassis chassis;
	static ServerSocket serv;
	static ObjectInputStream reader;
	static Socket socket;
	static PrintWriter writer;

	public static void main(String[] args) {
		boolean stop = false;

		setupRobot();
		
		System.out.println("Start");
		Delay.msDelay(2500);

		try {
			openServer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Map<String, Double> map = null;
		
		collect();
		
		while(!stop) {
			try {
				while((map = (Map<String, Double>)reader.readObject()) != null) {
					if(map.isEmpty()) {
						stop = true;
						reader.close();
						break;
					}
				
					System.out.println(map);

					//Do something with the map
					followMap(map);
					//System.out.println(map);
					Delay.msDelay(2500);
					
					hold();
					
					dispense();
					
					//Request the next map
					writer.write("next" + '\n');
					writer.flush();
				}
			} catch (ClassNotFoundException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
		
		}
		

		try {
			closeServer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		System.out.println("Done");
		Delay.msDelay(2500);
		
	}

	private static void followMap(Map<String, Double> map) {
		List<String> functions = new ArrayList<String>(map.keySet());
		List<Double> values = new ArrayList<Double>();
		
		Collections.sort(functions, new Sort());
		
		for(int i = 0; i < functions.size(); i++) {
			values.add(map.get(functions.get(i)));
		}
		
		for(int i = 0; i < map.size(); i++) {
			String function = functions.get(i).substring(0, 6);
			
				
			switch (function) {
			case "rotate":
				System.out.println("Rotating " + values.get(i) + " °");
				rotate(values.get(i));
				break;
			
			case "travel":
				System.out.println("Traveling " + values.get(i) + " cm");
				moveForward(values.get(i));
				break;
				
			case "xxxxxx":
				hold();
				dispense();
				break;
				
			case "finish":
				System.out.println("Victory");
				victory();
				break;

			default:
				System.out.println("Unknown function");
				break;
			}
			
		}
		
	}
	
	
	private static void victory() {
		ev3brick = (EV3) BrickFinder.getLocal();
		Audio audio = ev3brick.getAudio();
		hold();
		audio.setVolume(10);
		audio.playNote(Audio.XYLOPHONE, 420, 500);
		audio.playNote(Audio.XYLOPHONE, 510, 500);
		audio.playNote(Audio.XYLOPHONE, 420, 500);
		audio.playNote(Audio.XYLOPHONE, 640, 500);
		audio.playNote(Audio.XYLOPHONE, 220, 500);
		
	}
	
	
	private static void collect() {
		LeftCollectMotor.setSpeed(720);
		RightCollectMotor.setSpeed(720);
		LeftCollectMotor.backward();
		RightCollectMotor.forward();
	}

	private static void dispense() {
		LeftCollectMotor.setSpeed(720);
		RightCollectMotor.setSpeed(720);
		LeftCollectMotor.forward();
		RightCollectMotor.backward();
	}

	private static void hold() {
		LeftCollectMotor.stop();
		RightCollectMotor.stop();
	}

	private static void setupRobot() {
		LeftDrivingMotor = new EV3LargeRegulatedMotor(MotorPort.A);
		RightDrivingMotor = new EV3LargeRegulatedMotor(MotorPort.C);

		LeftCollectMotor = new EV3MediumRegulatedMotor(MotorPort.D);
		RightCollectMotor = new EV3MediumRegulatedMotor(MotorPort.B);

		ev3brick = (EV3) BrickFinder.getLocal();
		buttons = ev3brick.getKeys();
		
		// Hvis rotationen er upræcis kan det kalibreres ved at ændre offset, dog vil
		// det være bedst at ændre faktoren i selve rotate() funktionen 5.525 offset er perfekt på ren overflade. 
		wheel1 = WheeledChassis.modelWheel(LeftDrivingMotor, 4.237).offset(-5.625);
		wheel2 = WheeledChassis.modelWheel(RightDrivingMotor, 4.237).offset(5.625);

		chassis = new WheeledChassis(new Wheel[] { wheel1, wheel2 }, WheeledChassis.TYPE_DIFFERENTIAL);

		pilot = new MovePilot(chassis);
		pilot.setLinearSpeed(7);
		pilot.setLinearAcceleration(pilot.getLinearAcceleration() / 4);
		pilot.setAngularSpeed(20);
	}

	private static void moveForward(double distance) {
		pilot.travel(distance);
	}

	private static void moveBackward(double distance) {
		pilot.travel(-distance);
	}

	private static void rotate(double angle) {
		// Faktor for 360 er 0.165
		pilot.rotate(angle * 1.0);
	}

	private static void openServer() throws IOException {
		serv = new ServerSocket(3005);
		socket = serv.accept();
		reader = new ObjectInputStream(socket.getInputStream());
		writer = new PrintWriter(socket.getOutputStream());
	}

	private static void closeServer() throws IOException {
		serv.close();
		socket.close();
		reader.close();
		writer.close();
	}

	
	static class Sort implements Comparator<String> {
		
		@Override
		public int compare(String rotate, String travel) {
			String rotateSub = rotate.substring(6, rotate.length());
			String travelSub = travel.substring(6, travel.length());;
			int rotateNum = Integer.parseInt(rotateSub); 
			int travelNum = Integer.parseInt(travelSub);
			
			if(rotateNum < travelNum) {
				return -1;
			}
			if(rotateNum > travelNum) {
				return 1;
			}
			
			return 0;
		}}
	
	
}