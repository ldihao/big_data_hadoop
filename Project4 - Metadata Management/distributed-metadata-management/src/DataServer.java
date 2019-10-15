import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

public class DataServer {
	private String name;
	private HashMap<Integer, MetaData> storage = new HashMap<>();
	private String clusterAddress = "localhost";
	private int clusterPort = 2018;
	private Socket socket;
	private ObjectInputStream sIn;
	private ObjectOutputStream sOut;

	DataServer(String name) {
		this.name = name;
		prepareBackup();
		try {
			socket = new Socket(clusterAddress, clusterPort);
			sOut = new ObjectOutputStream(socket.getOutputStream());
			sIn = new ObjectInputStream(socket.getInputStream());
			System.out.println("Connection established");
			sOut.writeUTF(name);
			sOut.writeInt(storage.size());
			sOut.flush();
			if (sIn.readUTF().equals("Ready")) {
				System.out.println("Preparation completed");
				while (true) {
					try {
						handle((Request) sIn.readObject());
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
			System.out.println("Preparation failed");
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] argv) {
		System.out.print("Data Server Name: ");
		Scanner stdin = new Scanner(System.in);
		new DataServer(stdin.nextLine());
	}

	private void handle(Request request) throws IOException {
		System.out.print(request.content);
		Response response = new Response("Invalid request");
		switch (request.content) {
		case "Get":
			if (request.data.containsKey("id")) {
				int id = (int) request.data.get("id");
				if (storage.containsKey(id)) {
					response.content = "";
					response.data.put("metaData", storage.get(id));
				} else {
					response.content = "Not found";
				}
			}
			break;
		case "Put":
			if (request.data.containsKey("id") && request.data.containsKey("metaData")) {
				int id = (int) request.data.get("id");
				MetaData metaData = (MetaData) request.data.get("metaData");
				createBackup(id, metaData);
				storage.put(id, metaData);
				response.content = "";
				response.data.put("size", storage.size());
			}
			break;
		case "Remove":
			if (request.data.containsKey("id")) {
				int id = (int) request.data.get("id");
				removeBackup(id);
				storage.remove(id);
				response.content = "";
				response.data.put("size", storage.size());
			}
			break;
		}
		sOut.writeObject(response);
		sOut.flush();
		System.out.println(" OK " + response.content);
	}

	private void prepareBackup() {
		File backupDirectory = new File(name);
		if (backupDirectory.isDirectory()) {
			for (File file : backupDirectory.listFiles()) {
				if (file.isFile()) {
					try {
						int id = Integer.parseInt(file.getName());
						ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
						MetaData metaData = (MetaData) ois.readObject();
						storage.put(id, metaData);
						ois.close();
					} catch (IOException | ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			backupDirectory.mkdir();
		}
	}

	private void createBackup(int id, MetaData metaData) throws IOException {
		File backupFile = new File(name + "/" + id);
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(backupFile));
		oos.writeObject(metaData);
		oos.close();
	}

	private void removeBackup(int id) {
		File backupFile = new File(name + "/" + id);
		backupFile.delete();
	}
}
