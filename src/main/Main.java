package main;

import lejos.hardware.lcd.LCD;
import lejos.utility.Delay;

public class Main {

	public static void main(String[] args) {
		
		LCD.drawString("Hello", 0, 0);
		Delay.msDelay(2500);

	}

}
