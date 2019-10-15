import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class DirectoryTree implements Serializable {
	private TreeNode root = new TreeNode(0, "/", "cluster");
	private int nextId;

	DirectoryTree(int initialNextId) {
		nextId = initialNextId;
	}

	DirectoryTree() {
		this(1);
	}

	public static void main(String[] argv) {
		DirectoryTree dt = new DirectoryTree();
		dt.putNode("/test1");
		dt.putNode("/test1/test2");
		System.out.println(dt.getNode("/").getId() + " " + dt.getNode("/").getName());
		System.out.println(dt.getNode("/test1").getId() + " " + dt.getNode("/test1").getName());
		System.out.println(dt.getNode("/test1/").getId() + " " + dt.getNode("/test1/").getName());
		System.out.println(dt.getNode("/test1/test2").getId() + " " + dt.getNode("/test1/test2").getName());
		System.out.println(!dt.putNode("/test1"));
		System.out.println(!dt.putNode("/test1/test2"));
		System.out.println(!dt.putNode("/test2/test3"));
		dt.removeNode("/test1");
		System.out.println(dt.getNode("/test1") != null);
		dt.removeNode("/test1/test2");
		System.out.println(dt.getNode("/test1/test2") == null);
		dt.removeNode("/test1");
		System.out.println(dt.getNode("/test1") == null);
	}

	private int getNextId() {
		return nextId++;
	}

	TreeNode getNode(String path) {
		if (path.length() == 0)
			return root;
		if (path.charAt(0) != '/')
			return null;
		String[] names = path.split("/");
		TreeNode currentNode = root;
		for (String name : names) {
			if (name.length() > 0) {
				HashMap<String, TreeNode> children = currentNode.getChildren();
				if (children.containsKey(name)) {
					currentNode = children.get(name);
				} else {
					return null;
				}
			}
		}
		return currentNode;
	}

	boolean putNode(String path) {
		String name = path.substring(path.lastIndexOf('/') + 1);
		if (name.length() == 0)
			return false;
		TreeNode parent = getNode(path.substring(0, Math.max(0, path.lastIndexOf('/'))));
		if (parent == null || parent.hasChild(name))
			return false;
		TreeNode child = new TreeNode(getNextId(), name, parent);
		return parent.addChild(name, child);
	}

	void removeNode(String path) {
		TreeNode node = getNode(path);
		if (node == null)
			return;
		if (node.getChildren().size() > 0)
			return;
		TreeNode parent = node.getParent();
		if (parent == null)
			return;
		parent.removeChild(node.getName());
	}

	boolean canRemoveNode(String path) {
		TreeNode node = getNode(path);
		if (node == null)
			return false;
		if (node.getChildren().size() > 0)
			return false;
		return node.getParent() != null;
	}

	String[] depthFirstTraversal(String path, boolean full) {
		TreeNode anchor = getNode(path);
		ArrayList<String> res = new ArrayList<>();
		if (anchor != null) {
			String tmpPath;
			if (full) {
				tmpPath = path;
			} else {
				tmpPath = anchor.getName();
			}
			depthFirstTraversalRec(anchor, new ArrayList<>(), tmpPath, res, full);
		}
		return res.toArray(new String[0]);
	}

	private void depthFirstTraversalRec(TreeNode anchor, ArrayList<Boolean> isLast, String tmpPath,
			ArrayList<String> res, boolean full) {
		if (anchor == null)
			return;
		StringBuilder sb = new StringBuilder();
		for (Iterator<Boolean> itr = isLast.iterator(); itr.hasNext();) {
			boolean last = itr.next();
			if (last) {
				if (itr.hasNext()) {
					sb.append("   ");
				} else {
					sb.append("\u2517\u2501\u2501\u2501 ");
				}
			} else {
				if (itr.hasNext()) {
					sb.append("\u2503  ");
				} else {
					sb.append("\u2523\u2501\u2501\u2501 ");
				}
			}
		}
		sb.append(tmpPath);
		res.add(sb.toString());
		isLast.add(true);
		for (Iterator<TreeNode> itr = anchor.getChildren().values().iterator(); itr.hasNext();) {
			TreeNode child = itr.next();
			String childPath;
			if (full) {
				if (tmpPath.equals("/")) {
					childPath = tmpPath + child.getName();
				} else {
					childPath = tmpPath + "/" + child.getName();
				}
			} else {
				childPath = child.getName();
			}
			isLast.set(isLast.size() - 1, !itr.hasNext());
			depthFirstTraversalRec(child, isLast, childPath, res, full);
		}
		isLast.remove(isLast.size() - 1);
	}

	String[] getServerNames() {
		HashSet<String> serverNames = new HashSet<>();
		getServerNamesRec(root, serverNames);
		return serverNames.toArray(new String[0]);
	}

	private void getServerNamesRec(TreeNode anchor, HashSet<String> serverNames) {
		if (anchor == null)
			return;
		serverNames.add(anchor.getDataServerName());
		for (TreeNode child : anchor.getChildren().values()) {
			getServerNamesRec(child, serverNames);
		}
	}

	HashMap<String, ArrayList<String>> getPathDist() {
		HashMap<String, ArrayList<String>> res = new HashMap<>();
		String[] serverNames = getServerNames();
		for (String serverName : serverNames) {
			res.put(serverName, new ArrayList<>());
		}
		getPathDistRec(root, "/", res);
		return res;
	}

	private void getPathDistRec(TreeNode anchor, String tmpPath, HashMap<String, ArrayList<String>> res) {
		if (anchor == null)
			return;
		res.get(anchor.getDataServerName()).add(tmpPath);
		for (TreeNode child : anchor.getChildren().values()) {
			String childPath;
			if (tmpPath.equals("/")) {
				childPath = tmpPath + child.getName();
			} else {
				childPath = tmpPath + "/" + child.getName();
			}
			getPathDistRec(child, childPath, res);
		}
	}

	HashMap<String, Integer> getPathId() {
		HashMap<String, Integer> res = new HashMap<>();
		getPathIdRec(root, "/", res);
		return res;
	}

	private void getPathIdRec(TreeNode anchor, String tmpPath, HashMap<String, Integer> res) {
		if (anchor == null)
			return;
		res.put(tmpPath, anchor.getId());
		for (TreeNode child : anchor.getChildren().values()) {
			String childPath;
			if (tmpPath.equals("/")) {
				childPath = tmpPath + child.getName();
			} else {
				childPath = tmpPath + "/" + child.getName();
			}
			getPathIdRec(child, childPath, res);
		}
	}
}
