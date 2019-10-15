import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class Client {
	private String clusterAddress = "localhost";
	private int clusterPort = 2019;
	private Socket socket;
	private ObjectInputStream sIn;
	private ObjectOutputStream sOut;
	private String currentPath = "/";
	private Scanner stdin = new Scanner(System.in);
	private HashSet<String> validOperationSet = new HashSet<String>() {
		{
			add("chkdist");
			add("mkdir");
			add("touch");
			add("rmdir");
			add("rm");
			add("chmod");
			add("tree");
			add("fulltree");
			add("ls");
			add("stat");
			add("cd");
			add("exit");
		}
	};
	private HashSet<String> noPathOperationSet = new HashSet<String>() {
		{
			add("chkdist");
			add("tree");
			add("fulltree");
			add("ls");
			add("exit");
		}
	};

	Client() {
		try {
			socket = new Socket(clusterAddress, clusterPort);
			sOut = new ObjectOutputStream(socket.getOutputStream());
			sIn = new ObjectInputStream(socket.getInputStream());
			System.out.println("Connection established");
			if (sIn.readUTF().equals("Ready")) {
				while (true) {
					try {
						Request request = parseRequest();
						if (request != null) {
							sOut.writeObject(request);
							sOut.flush();
							if (!request.content.equals("exit")) {
								Response response = (Response) sIn.readObject();
								if (response.content.length() == 0) {
									handle(request, response);
								} else {
									System.out.println(response.content);
								}
							} else {
								break;
							}
						}
					} catch (ClassNotFoundException | NumberFormatException e) {
						e.printStackTrace();
					}
				}
			} else {
				System.out.println("Preparation failed");
				socket.close();
			}
			System.out.println("exit");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] argv) {
		new Client();
	}

	private Request parseRequest() {
		System.out.print(currentPath + "$ ");
		String cmd = stdin.nextLine();
		ArrayList<String> argvList = new ArrayList<>();
		for (String v : cmd.split(" ")) {
			if (v.trim().length() > 0) {
				argvList.add(v.trim());
			}
		}
		String[] argv = argvList.toArray(new String[0]);
		String operation = argv[0];
		if (validOperationSet.contains(operation)) {
			Request request = new Request(operation);
			String path = null;
			int[] permission = null;
			if (noPathOperationSet.contains(operation)) {
				path = currentPath;
			}
			if (argv.length >= 2) {
				String relativePath = argv[1];
				if (relativePath.startsWith("/")) {
					path = relativePath;
				} else if (relativePath.equals("..") || relativePath.startsWith("../")) {
					path = relativePath.replaceFirst("..", currentPath.substring(0, currentPath.lastIndexOf("/")));
					if (path.length() == 0) {
						path = "/";
					}
				} else if (currentPath.equals("/")) {
					path = currentPath + relativePath;
				} else {
					path = currentPath + "/" + relativePath;
				}
			}
			if (argv.length >= 3) {
				if (argv[2].length() >= 3) {
					permission = new int[3];
					for (int i = 0; i < 3; ++i) {
						permission[i] = Integer.parseInt(argv[2].substring(i, i + 1));
					}
				}
			}
			if (path != null) {
				request.data.put("path", path);
				if (permission != null) {
					request.data.put("permission", permission);
				}
				return request;
			} else {
				System.out.println("[path] is not given");
			}
		} else {
			System.out.println("Invalid operation: " + operation);
		}
		return null;
	}

	private void handle(Request request, Response response) {
		switch (request.content) {
		case "chkdist":
			String[] serverNames = (String[]) response.data.get("serverNames");
			HashMap<String, ArrayList<String>> pathDist = (HashMap<String, ArrayList<String>>) response.data
					.get("pathDist");
			HashMap<String, Integer> pathId = (HashMap<String, Integer>) response.data.get("pathId");
			for (String serverName : serverNames) {
				System.out.println("metadata server: " + serverName);
				ArrayList<String> pathArray = pathDist.get(serverName);
				for (String path : pathArray) {
					System.out.println("id: " + pathId.get(path) + "\t" + "path: " + path);
				}
				System.out.println();
			}
			break;
		case "tree":
			// Same as "fulltree"
		case "fulltree":
			for (String entry : (String[]) response.data.get("entries")) {
				System.out.println(entry);
			}
			break;
		case "ls":
			for (String name : response.data.keySet()) {
				MetaData metaData = (MetaData) response.data.get(name);
				HashMap<String, String> meta = metaData.getMeta();
				System.out.println(meta.get("permission") + "\t" + meta.get("atime") + "\t" + meta.get("name"));
			}
			break;
		case "stat":
			MetaData metaData = (MetaData) response.data.get("metaData");
			HashMap<String, String> meta = metaData.getMeta();
			System.out.print("id: " + response.data.get("id") + "    ");
			System.out.print("name: " + meta.get("name") + "    ");
			System.out.println("type: " + meta.get("type"));
			System.out.println("permission: " + meta.get("permission"));
			System.out.println("atime: " + meta.get("atime"));
			System.out.println("mtime: " + meta.get("mtime"));
			System.out.println("ctime: " + meta.get("ctime"));
			System.out.println("metadata server: " + response.data.get("metaDataServerName"));
			break;
		case "cd":
			currentPath = (String) response.data.get("newPath");
			break;
		}
	}
}
