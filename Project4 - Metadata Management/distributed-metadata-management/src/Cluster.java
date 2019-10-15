import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Cluster {
	private final HashMap<String, ObjectInputStream> sIn = new HashMap<>();
	private final HashMap<String, ObjectOutputStream> sOut = new HashMap<>();
	private final HashMap<String, Integer> size = new HashMap<>();
	private final HashMap<String, Boolean> alive = new HashMap<>();
	private DirectoryTree dt;
	private ServerSocket dataServerSocket;
	private ServerSocket clientSocket;
	private int dataServerPort = 2018;
	private int clientPort = 2019;
	private ExecutorService executor = Executors.newCachedThreadPool();

	Cluster() {
		constructDirectoryTree();
		try {
			dataServerSocket = new ServerSocket(dataServerPort);
			clientSocket = new ServerSocket(clientPort);
			executor.execute(() -> {
				while (true) {
					try {
						Socket socket = dataServerSocket.accept();
						ObjectInputStream sIn = new ObjectInputStream(socket.getInputStream());
						ObjectOutputStream sOut = new ObjectOutputStream(socket.getOutputStream());
						String name = sIn.readUTF();
						int size = sIn.readInt();
						sOut.writeUTF("Ready");
						sOut.flush();
						synchronized (alive) {
							this.sIn.put(name, sIn);
							this.sOut.put(name, sOut);
							this.size.put(name, size);
							alive.put(name, true);
						}
						System.out.println(name + " is alive");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			while (true) {
				Socket socket = clientSocket.accept();
				executor.execute(() -> {
					try {
						ObjectInputStream sIn = new ObjectInputStream(socket.getInputStream());
						ObjectOutputStream sOut = new ObjectOutputStream(socket.getOutputStream());
						sOut.writeUTF("Ready");
						sOut.flush();
						while (true) {
							Request request = (Request) sIn.readObject();
							if (!request.content.equals("exit")) {
								handle(request, sOut);
							} else {
								socket.close();
								break;
							}
						}
					} catch (IOException | ClassNotFoundException e) {
						e.printStackTrace();
					}
				});
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] argv) {
		new Cluster();
	}

	private void constructDirectoryTree() {
		dt = new DirectoryTree();
		File directoryTreeBackup = new File("directoryTree");
		if (directoryTreeBackup.isFile()) {
			try {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(directoryTreeBackup));
				dt = (DirectoryTree) ois.readObject();
				ois.close();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private void backupDirectoryTree() {
		try {
			File directoryTreeBackup = new File("directoryTree");
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(directoryTreeBackup));
			oos.writeObject(dt);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handle(Request request, ObjectOutputStream sOut) throws IOException {
		Response response = new Response("Invalid request");
		switch (request.content) {
		case "chkdist":
			response.data.put("serverNames", dt.getServerNames());
			response.data.put("pathDist", dt.getPathDist());
			response.data.put("pathId", dt.getPathId());
			response.content = "";
			break;
		case "mkdir":
			if (request.data.containsKey("path")) {
				response.content = putNode((String) request.data.get("path"), 2);
			}
			break;
		case "touch":
			if (request.data.containsKey("path")) {
				String path = (String) request.data.get("path");
				TreeNode node = dt.getNode(path);
				if (node == null) {
					if (parentIsDirectory(path)) {
						response.content = putNode(path, 1);
					} else {
						response.content = "Cannot create " + path;
					}
				} else {
					MetaData metaData = getMeta(node.getDataServerName(), node.getId());
					if (metaData != null) {
						metaData.setAtime(new Date());
						metaData.setMtime(new Date());
						metaData.setCtime(new Date());
						response.content = putMeta(node.getDataServerName(), node.getId(), metaData);
					} else {
						response.content = "No metadata retrieved";
					}
				}
			}
			break;
		case "rmdir":
			if (request.data.containsKey("path")) {
				String path = (String) request.data.get("path");
				if (pathIsDirectory(path)) {
					response.content = removeNode(path);
				} else {
					response.content = path + " is not a directory";
				}
			}
			break;
		case "rm":
			if (request.data.containsKey("path")) {
				response.content = removeNode((String) request.data.get("path"));
			}
			break;
		case "chmod":
			if (request.data.containsKey("path") && request.data.containsKey("permission")) {
				String path = (String) request.data.get("path");
				int[] permission = (int[]) request.data.get("permission");
				TreeNode node = dt.getNode(path);
				if (node != null) {
					MetaData metaData = getMeta(node.getDataServerName(), node.getId());
					if (metaData != null) {
						metaData.setPermission(permission);
						metaData.setCtime(new Date());
						response.content = putMeta(node.getDataServerName(), node.getId(), metaData);
					} else {
						response.content = "No metadata retrieved";
					}
				} else {
					response.content = path + " not found";
				}
			}
			break;
		case "tree":
			if (request.data.containsKey("path")) {
				String path = (String) request.data.get("path");
				response.data.put("entries", dt.depthFirstTraversal(path, false));
				response.content = "";
			}
			break;
		case "fulltree":
			if (request.data.containsKey("path")) {
				String path = (String) request.data.get("path");
				response.data.put("entries", dt.depthFirstTraversal(path, true));
				response.content = "";
			}
			break;
		case "ls":
			if (request.data.containsKey("path")) {
				String path = (String) request.data.get("path");
				TreeNode node = dt.getNode(path);
				if (node != null) {
					response.content = "";
					if (pathIsDirectory(path)) {
						for (String name : node.getChildren().keySet()) {
							TreeNode child = node.getChildren().get(name);
							MetaData metaData = getMeta(child.getDataServerName(), child.getId());
							if (metaData != null) {
								response.data.put(name, metaData);
							} else {
								response.content = "No metadata retrieved";
								break;
							}
						}
					} else {
						MetaData metaData = getMeta(node.getDataServerName(), node.getId());
						if (metaData != null) {
							response.data.put(node.getName(), metaData);
						} else {
							response.content = "No metadata retrieved";
						}
					}
				} else {
					response.content = path + " not found or is not a directory";
				}
			}
			break;
		case "stat":
			if (request.data.containsKey("path")) {
				String path = (String) request.data.get("path");
				TreeNode node = dt.getNode(path);
				if (node != null) {
					MetaData metaData = getMeta(node.getDataServerName(), node.getId());
					if (metaData != null) {
						response.data.put("id", node.getId());
						response.data.put("metaData", metaData);
						response.data.put("metaDataServerName", node.getDataServerName());
						response.content = "";
					} else {
						response.content = "No metadata retrieved";
					}
				} else {
					response.content = path + " not found";
				}
			}
			break;
		case "cd":
			if (request.data.containsKey("path")) {
				String path = (String) request.data.get("path");
				if (pathIsDirectory(path)) {
					response.data.put("newPath", path);
					response.content = "";
				} else {
					response.content = path + " is not a directory";
				}
			}
			break;
		}
		sOut.writeObject(response);
		sOut.flush();
		System.out.println(request.content + " OK " + response.content);
	}

	private boolean pathIsDirectory(String path) {
		TreeNode node = dt.getNode(path);
		if (node == null)
			return false;
		if (node.isRoot())
			return true;
		MetaData metaData = getMeta(node.getDataServerName(), node.getId());
		if (metaData == null)
			return false;
		return metaData.getMeta().get("type").equals("directory");
	}

	private boolean parentIsDirectory(String path) {
		return pathIsDirectory(path.substring(0, Math.max(0, path.lastIndexOf('/'))));
	}

	private String putNode(String path, int type) {
		String name = path.substring(path.lastIndexOf('/') + 1);
		if (dt.putNode(path)) {
			TreeNode node = dt.getNode(path);
			String dataServerName = getLightestDataServer();
			if (dataServerName != null) {
				MetaData metaData = new MetaData(name, type, new Date(), new Date(), new Date());
				String res = putMeta(dataServerName, node.getId(), metaData);
				if (res.length() == 0) {
					node.setDataServerName(dataServerName);
					backupDirectoryTree();
				} else {
					dt.removeNode(path);
				}
				return res;
			} else {
				return "No available data server";
			}
		} else {
			return "Cannot create " + path;
		}
	}

	private String removeNode(String path) {
		TreeNode node = dt.getNode(path);
		if (node != null && dt.canRemoveNode(path)) {
			String res = removeMeta(node.getDataServerName(), node.getId());
			if (res.length() == 0) {
				dt.removeNode(path);
				backupDirectoryTree();
			}
			backupDirectoryTree();
			return res;
		} else {
			return "Cannot remove " + path;
		}
	}

	private MetaData getMeta(String dataServerName, int id) {
		if (id == 0)
			return null;
		try {
			Request request = new Request("Get");
			request.data.put("id", id);
			ObjectOutputStream sOut = null;
			ObjectInputStream sIn = null;
			synchronized (alive) {
				if (alive.get(dataServerName)) {
					sOut = this.sOut.get(dataServerName);
					sIn = this.sIn.get(dataServerName);
				}
			}
			if (sOut != null && sIn != null) {
				sOut.writeObject(request);
				sOut.flush();
				Response response = (Response) sIn.readObject();
				if (response.content.length() == 0) {
					return (MetaData) response.data.get("metaData");
				} else {
					System.out.println(response.content);
					return null;
				}
			} else {
				System.out.println(dataServerName + " is unavailable");
				return null;
			}
		} catch (IOException | ClassNotFoundException e) {
			synchronized (alive) {
				alive.put(dataServerName, false);
			}
			System.out.println(dataServerName + " is dead");
			return null;
		}
	}

	private String putMeta(String dataServerName, int id, MetaData metaData) {
		if (id == 0)
			return "Cannot modify /";
		try {
			Request request = new Request("Put");
			request.data.put("id", id);
			request.data.put("metaData", metaData);
			ObjectOutputStream sOut = null;
			ObjectInputStream sIn = null;
			synchronized (alive) {
				if (alive.get(dataServerName)) {
					sOut = this.sOut.get(dataServerName);
					sIn = this.sIn.get(dataServerName);
				}
			}
			if (sOut != null && sIn != null) {
				sOut.writeObject(request);
				sOut.flush();
				Response response = (Response) sIn.readObject();
				if (response.content.length() == 0) {
					synchronized (alive) {
						size.put(dataServerName, (int) response.data.get("size"));
					}
				}
				return response.content;
			} else {
				return dataServerName + " is unavailable";
			}
		} catch (IOException | ClassNotFoundException e) {
			synchronized (alive) {
				alive.put(dataServerName, false);
			}
			System.out.println(dataServerName + " is dead");
			return dataServerName + " is dead";
		}
	}

	private String removeMeta(String dataServerName, int id) {
		if (id == 0)
			return "Cannot remove /";
		try {
			Request request = new Request("Remove");
			request.data.put("id", id);
			ObjectOutputStream sOut = null;
			ObjectInputStream sIn = null;
			synchronized (alive) {
				if (alive.get(dataServerName)) {
					sOut = this.sOut.get(dataServerName);
					sIn = this.sIn.get(dataServerName);
				}
			}
			if (sOut != null && sIn != null) {
				sOut.writeObject(request);
				sOut.flush();
				Response response = (Response) sIn.readObject();
				if (response.content.length() == 0) {
					synchronized (alive) {
						size.put(dataServerName, (int) response.data.get("size"));
					}
				}
				return response.content;
			} else {
				return dataServerName + " is unavailable";
			}
		} catch (IOException | ClassNotFoundException e) {
			synchronized (alive) {
				alive.put(dataServerName, false);
			}
			System.out.println(dataServerName + " is dead");
			return dataServerName + " is dead";
		}
	}

	private String getLightestDataServer() {
		int minSize = -1;
		String candidate = null;
		synchronized (alive) {
			for (String name : size.keySet()) {
				if (alive.get(name) && (minSize < 0 || size.get(name) < minSize)) {
					candidate = name;
					minSize = size.get(name);
				}
			}
		}
		return candidate;
	}
}
