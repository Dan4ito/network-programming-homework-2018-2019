package com.fmi.mpr.hw.chat;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.util.Scanner;

public class Client{

	final static String MULTICAST_IP_ADDRESS = "224.0.0.3";
	final static int MULTICAST_PORT = 3000;

	final static String SERVER_IP_ADDRESS = "localhost";		// loopback address
	final static int SERVER_PORT = 8080;

	final static int BUFFER_SIZE = 4096;
	static Boolean KEEP_READING = true;
	
	final static String REGISTER_PREFIX = ">";
	final static String LOGOUT_PREFIX = "<";
	
	static String userName = null;
	
	public void doWork() throws InterruptedException, IOException {
		DatagramSocket sendSocket = new DatagramSocket();									// socket endpoint for sending data
		InetAddress address = InetAddress.getByName(SERVER_IP_ADDRESS);						// address of the server 208.43.....

		Scanner scanner = new Scanner(System.in);
		registerClient(scanner, sendSocket, address);										// register
		
	

		String message;
		message = scanner.nextLine();

		while(true) {
			System.out.println();
			// add here
			break;
			
		}
		logoutClient(sendSocket, address);
	
		scanner.close();
		sendSocket.close();
	}

	private void registerClient(Scanner scanner, DatagramSocket sendSocket, InetAddress address) {
		
	}

	private void logoutClient(DatagramSocket sendSocket, InetAddress address) {
																			
	}


	public static void main(String[] args) throws InterruptedException, IOException{
		Client client = new Client();
		client.doWork();
	}
}

