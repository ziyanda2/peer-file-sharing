/**
 * 
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * 
 */
public class Client {
	private DatagramSocket datagramSocket;
	private ListView<String> fileListView;
	private FileChooser fileChooser;
	private boolean isSeeder = false;
	private List<File> fileList; 
	
	


	public void selectionMode(Stage primaryStage) {
		Button seederMode = new Button("Seeder Mode");
		Button leecherMode = new Button("Leecher Mode");
		
		VBox mode = new VBox(10, seederMode, leecherMode); 
		Scene scene = new Scene(mode, 300, 200);
		
		seederMode.setOnAction(e -> openSeederMode(primaryStage));
		leecherMode.setOnAction(e -> openLeecherMode(primaryStage));
		
		primaryStage.setScene(scene);
		primaryStage.setTitle("File sharing");
		primaryStage.show();
		
		
	}
	
	private void openSeederMode(Stage stage) {
		isSeeder = true;
		fileListView = new ListView<>();
		fileChooser = new FileChooser();
		Button addButton = new Button("Add File");
		addButton.setOnAction(e -> addFile());
		
		
		VBox seederLayout = new VBox(10, fileListView, addButton);
		Scene seederScene = new Scene(seederLayout, 400, 300);
		stage.setScene(seederScene);
		
		startSeeder();
		
	}
	
	private void openLeecherMode(Stage stage) {
		isSeeder = false;
		TextField hostField = new TextField();
		hostField.setPromptText("Enter host address");
		TextField portField = new TextField();
		portField.setPromptText("Enter port number");
		Button connectButton = new Button("Connect");
		
		fileListView = new ListView<>();
		Button retrieveButton = new Button("Retrieve File");
		retrieveButton.setDisable(true);
		
		connectButton.setOnAction(e -> {
			try {
				datagramSocket = new DatagramSocket();
				InetAddress address = InetAddress.getByName(hostField.getText());
				int port = Integer.parseInt(portField.getText());
				requestFileList(address, port, fileListView);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
		retrieveButton.setOnAction(e -> {
			try {
				int index = fileListView.getSelectionModel().getSelectedIndex();
				InetAddress address = InetAddress.getByName(hostField.getText());
				int port = Integer.parseInt(portField.getText());
				requestFile(index, address, port);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
		
		VBox leecherLayout = new VBox(10, hostField, portField, connectButton, fileListView, retrieveButton);
		Scene leecherScene = new Scene(leecherLayout, 400, 300);
		stage.setScene(leecherScene);
		
		
	}
	
	//Functionality to add file to Seeder mode
	private void addFile() {
		File file = fileChooser.showOpenDialog(null);
		if(file != null) {
			fileListView.getItems().add(file.getName());
		}
	}
	
	private void startSeeder() {
		try {
			datagramSocket = new DatagramSocket(9876);
			System.out.println("Seeder server started. Waiting for Leecher...");
			
			byte[] receiveData = new byte[1024];
			DatagramPacket datagramPacket = new DatagramPacket(receiveData, receiveData.length);
			datagramSocket.receive(datagramPacket);
			String command = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
			
			if(command.equals("LIST")) {
				sendFileList(datagramPacket.getAddress(), datagramPacket.getPort());
			}else if (command.startsWith("FILE")) {
				int fileIndex = Integer.parseInt(command.split(" ")[1]);
				sendFile(fileIndex, datagramPacket.getAddress(), datagramPacket.getPort());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void sendFileList(InetAddress address, int port) {
		try {
			StringBuilder fileListString = new StringBuilder();
			for(int i = 0; i < fileList.size(); i++) {
				fileListString.append(i).append(": ").append(fileList.get(i).getName()).append("\n");
			}
			
			byte[] sendData = fileListString.toString().getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData,sendData.length,address, port);
			datagramSocket.send(sendPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void sendFile(int index, InetAddress address, int port) {
		try {
			File file = fileList.get(index);
			byte[] fileData = java.nio.file.Files.readAllBytes(file.toPath());
			
			DatagramPacket sendPacket = new DatagramPacket(fileData,fileData.length, address, port);
			datagramSocket.send(sendPacket);
			System.out.println("File sent: " + file.getName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void requestFileList(InetAddress address, int port, ListView<String> fileListView) {
		try {
			byte[] sendData = "LIST".getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
			datagramSocket.send(sendPacket);
			
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			datagramSocket.receive(receivePacket);
			
			String fileList = new String(receivePacket.getData(), 0, receivePacket.getLength());
			String[] files = fileList.split("\n");
			for (String file : files) {
				fileListView.getItems().add(file);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void requestFile(int index, InetAddress address, int port) {
		try {
			String command = "FILE " + index;
			byte[] sendData = command.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
			datagramSocket.send(sendPacket);
			
			byte[] fileData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(fileData, fileData.length);
			datagramSocket.receive(receivePacket);
			
			saveFileLocally("received_file_" + index + ".bin", receivePacket.getData(), receivePacket.getLength());
			System.out.println("File received: Saving file...");
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
	
	private void saveFileLocally(String fileName, byte[] fileData, int length) {
		try(FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
			fileOutputStream.write(fileData, 0, length);
			System.out.print("File saved: " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
