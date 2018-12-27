package com.fmi.mpr.hw.chat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class Server {

	final static String MULTICAST_IP_ADDRESS = "224.0.0.3";
	final static int MULTICAST_PORT = 3000;
	
	final static int SERVER_PORT = 8080;
	
	final static int BUFFER_SIZE = 4096;

	final static String REGISTER_PREFIX = ">";
	final static String LOGOUT_PREFIX = "<";
	
	private Set<String> connectedUsers;

	public Server() throws IOException {
		connectedUsers = new HashSet<String>();
		doWork();
	}

	private void doWork() throws IOException {
		try (DatagramSocket receiveSocket = new DatagramSocket(SERVER_PORT)) {
			System.out.println("Chat room server started!");
			while (true) {
				byte[] byteData = new byte[BUFFER_SIZE];
				DatagramPacket receivedPacketFromClient = new DatagramPacket(byteData, byteData.length);
				receiveSocket.receive(receivedPacketFromClient);

				boolean isMessageOnly = false;
				boolean sendToMulticast = true;
				String messageReceivedFromClient = new String(receivedPacketFromClient.getData(), receivedPacketFromClient.getOffset(),
						receivedPacketFromClient.getLength());
				String nameOfClient = null;

				// register
				if (messageReceivedFromClient.startsWith(REGISTER_PREFIX)) {
					nameOfClient = messageReceivedFromClient.substring(1);
					sendToMulticast = !connectedUsers.contains(nameOfClient);								// whether we should send to multicast
					registerClient(nameOfClient, receiveSocket, receivedPacketFromClient.getAddress(), receivedPacketFromClient.getPort());
					messageReceivedFromClient = nameOfClient + " entered the chat room...";
					isMessageOnly = true;
				}
				// logout
				else if (messageReceivedFromClient.startsWith(LOGOUT_PREFIX)) {
					nameOfClient = messageReceivedFromClient.substring(1);
					logoutClient(nameOfClient);
					messageReceivedFromClient = nameOfClient+ " left the chat room...";
					isMessageOnly = true;
				} else {
					nameOfClient = messageReceivedFromClient.split(":")[0];
				}

				if (sendToMulticast && (isMessageOnly || connectedUsers.contains(nameOfClient))) {

					byte[] bytesForMulticast = isMessageOnly ? messageReceivedFromClient.getBytes() : receivedPacketFromClient.getData();	// bytes for register/logout or dataPacket

					InetAddress address = InetAddress.getByName(MULTICAST_IP_ADDRESS);
					DatagramPacket multicastPacket = new DatagramPacket(bytesForMulticast, bytesForMulticast.length, address,
							MULTICAST_PORT);
					receiveSocket.send(multicastPacket);
				}
			}
		}

	}

	private void registerClient(String nameOfClient, DatagramSocket socket, InetAddress address, int port) throws IOException {
		String registerResponse;
		if (connectedUsers.contains(nameOfClient)) {
			registerResponse = "Name is already taken!";
		} else {
			registerResponse = "Success";
			connectedUsers.add(nameOfClient);
			System.out.println(nameOfClient + " entered the chat room!");
		}

		DatagramPacket registerResponsePacket = new DatagramPacket(registerResponse.getBytes(), registerResponse.getBytes().length, address, port);
		socket.send(registerResponsePacket);
	}

	private void logoutClient(String nameOfClient) {
		if (connectedUsers.contains(nameOfClient)) {
			connectedUsers.remove(nameOfClient);
			System.out.println(nameOfClient + " left the chat room...");
		}
	}

	public static void main(String[] args) throws IOException {
	//	File file = new File("asd.txt");
	//	file.createNewFile();
		Server chatServer = new Server();
		
	}

}
