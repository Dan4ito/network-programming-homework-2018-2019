package com.fmi.mpr.hw.chat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

public class Client implements Runnable{

	final static String MULTICAST_IP_ADDRESS = "224.0.0.3";
	final static int MULTICAST_PORT = 3000;

	final static String SERVER_IP_ADDRESS = "localhost";		// loopback address
	final static int SERVER_PORT = 8080;

	final static int BUFFER_SIZE = 4096*3;
	static boolean iS_CLIENT_ALIVE = true;
	
	final static String REGISTER_PREFIX = ">";
	final static String LOGOUT_PREFIX = "<";
	final static String EXIT_COMMAND = "EXIT";
	final static String SEND_DATA_COMMAND = "SEND";
	
	static String userName;
	
	public void doWork() throws InterruptedException, IOException {
		DatagramSocket sendSocket = new DatagramSocket();									// socket endpoint for sending data
		InetAddress address = InetAddress.getByName(SERVER_IP_ADDRESS);						// address of the server 208.43.....

		Scanner scanner = new Scanner(System.in);
		registerClient(scanner, sendSocket, address);										// register
		
		Thread readMessages = new Thread(new Client());										// spawn a thread to read message
		readMessages.start();

		String input;
		input = scanner.nextLine();

		while (!input.equals(EXIT_COMMAND)) {
			if (input.startsWith(SEND_DATA_COMMAND)) {		
				String url = input.split(" ")[1];				// Dimitar: SEND image.png -> image.png
				String[] split = url.split(File.separator + File.separator);		// escape
				String fileName = split[split.length - 1];
				byte[] fileToByteArray = null;
				try {
					fileToByteArray = fileToByteArray(url);
				} catch (IOException e) {
					System.out.println("Invalid file, try again");
					input = scanner.nextLine();
					continue;
				}

				input = userName + ": Sends a file | " + fileName;
				DatagramPacket packetToSend = new DatagramPacket(input.getBytes(), input.getBytes().length, address,
						SERVER_PORT);
				sendSocket.send(packetToSend);

				input = userName + ":" + SEND_DATA_COMMAND + " " + fileName + ":";
				byte[] finalBytes = Arrays.copyOf(input.getBytes(),
						input.getBytes().length + fileToByteArray.length);													// expand byte array
				System.arraycopy(fileToByteArray, 0, finalBytes, input.getBytes().length, fileToByteArray.length);			// fill the expansion
				packetToSend = new DatagramPacket(finalBytes, finalBytes.length, address, SERVER_PORT);
				sendSocket.send(packetToSend);
			} else {
				input = userName + ": " + input;
				DatagramPacket packetToSend = new DatagramPacket(input.getBytes(), input.getBytes().length, address,
						SERVER_PORT);
				sendSocket.send(packetToSend);
			}

			input = scanner.nextLine();
		}

		iS_CLIENT_ALIVE = false;
		logoutClient(sendSocket, address);
		readMessages.join();
		scanner.close();
		sendSocket.close();
	}

	private void registerClient(Scanner scanner, DatagramSocket sendSocket, InetAddress address)
			throws IOException {

		System.out.println("Enter chat name: ");
		userName = scanner.nextLine();
		userName = REGISTER_PREFIX + userName;

		DatagramPacket registerPacket = new DatagramPacket(userName.getBytes(), userName.getBytes().length, address,
				SERVER_PORT);
		sendSocket.send(registerPacket);			// send the username with the registering prefix to the server

		byte[] buffer = new byte[BUFFER_SIZE];
		DatagramPacket registerResponse = new DatagramPacket(buffer, buffer.length);
		sendSocket.receive(registerResponse);		// get the response from server
		String registerAnswerToString = new String(registerResponse.getData(), registerResponse.getOffset(), registerResponse.getLength());		// convert the response to string

		while (!registerAnswerToString.equals("Success")) {
			System.out.println("User with that chat name already exists: ");
			userName = scanner.nextLine();
			userName = REGISTER_PREFIX + userName;

			registerPacket = new DatagramPacket(userName.getBytes(), userName.getBytes().length, address, SERVER_PORT);		// fill again registerPacket and send it
			sendSocket.send(registerPacket);
			registerResponse = new DatagramPacket(buffer, buffer.length);													// get registerResponse again
			sendSocket.receive(registerResponse);
			registerAnswerToString = new String(registerResponse.getData(), registerResponse.getOffset(), registerResponse.getLength());
		}

		userName = userName.substring(REGISTER_PREFIX.length());	// set username with getting rid of the register prefix
		System.out.println("You are in the chat room!");
	}

	private void logoutClient(DatagramSocket sendSocket, InetAddress address) throws IOException {
		String msg = LOGOUT_PREFIX + userName;
		DatagramPacket logoutPacket = new DatagramPacket(msg.getBytes(), msg.getBytes().length, address, SERVER_PORT);		// send logout packet to the server
		sendSocket.send(logoutPacket);																						// no need to handle response here
	}

	@Override
	public void run() {
		InetAddress address = null;
		try {
			address = InetAddress.getByName(MULTICAST_IP_ADDRESS);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e.getMessage());
		}

		try (MulticastSocket clientSocket = new MulticastSocket(MULTICAST_PORT)) {
			clientSocket.joinGroup(address);

			while (iS_CLIENT_ALIVE) {

				byte[] finalBytes = new byte[BUFFER_SIZE];
				DatagramPacket receivedPacket = new DatagramPacket(finalBytes, finalBytes.length);
				clientSocket.receive(receivedPacket);							// Username:SEND file:XXX   // XXX - bytes of the file

				String receivedMessage = new String(receivedPacket.getData());
				String[] partsOfReceivedMessage = receivedMessage.split(":");
				if (!partsOfReceivedMessage[0].equals(userName)) {				// don't resend to sender
					if (partsOfReceivedMessage.length > 2 && partsOfReceivedMessage[1].startsWith(SEND_DATA_COMMAND)) {		// handle if files sent
						int offsetLength = partsOfReceivedMessage[0].length() + partsOfReceivedMessage[1].length() + 2;
						byte[] fileBytes = Arrays.copyOfRange(receivedPacket.getData(), offsetLength,
								receivedPacket.getLength() - 1);
						String fileName = partsOfReceivedMessage[1].split(" ")[1];
						byteArrayToFile("Users" + File.separator + userName, fileBytes, fileName);
					} else {																								// message handle
						System.out.println(receivedMessage);
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	
	public static byte[] fileToByteArray(String url) throws IOException { 

		File file = new File(url);

		FileInputStream fis = new FileInputStream(file);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[BUFFER_SIZE];
		int bytesRead;
		while((bytesRead = fis.read(buf)) != -1) {
			bos.write(buf, 0, bytesRead);
		}

		byte[] bytes = bos.toByteArray();
		fis.close();

		return bytes;
	}
  
	public static void byteArrayToFile(String directory, byte[] bytes, String name) throws IOException {

		File dir = new File(directory);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		File resultFile = new File(directory + File.separator + name);
		FileOutputStream fos = new FileOutputStream(resultFile);
		fos.write(bytes);
		fos.flush();
		fos.close();
	}


	public static void main(String[] args) throws InterruptedException, IOException{
		Client client = new Client();
		System.out.println("Send file command is: " + client.SEND_DATA_COMMAND + "\n");
		System.out.println("Exit command is: " + client.EXIT_COMMAND + "\n");
		System.out.println("Happy chatting!\n");
		client.doWork();
	}
}

