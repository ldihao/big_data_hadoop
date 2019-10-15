import java.io.Serializable;
import java.util.HashMap;

public class TreeNode implements Serializable {
	private int id;
	private String name;
	private String dataServerName;
	private TreeNode parent;
	private HashMap<String, TreeNode> children = new HashMap<>();

	TreeNode(int id, String name, String dataServerName, TreeNode parent) {
		this.id = id;
		this.name = name;
		this.dataServerName = dataServerName;
		this.parent = parent;
	}

	TreeNode(int id, String name, String dataServerName) {
		this(id, name, dataServerName, null);
	}

	TreeNode(int id, String name, TreeNode parent) {
		this(id, name, "", parent);
	}

	TreeNode(int id, String name) {
		this(id, name, "", null);
	}

	int getId() {
		return id;
	}

	String getName() {
		return name;
	}

	String getDataServerName() {
		return dataServerName;
	}

	void setDataServerName(String dataServerName) {
		this.dataServerName = dataServerName;
	}

	TreeNode getParent() {
		return parent;
	}

	void setParent(TreeNode parent) {
		this.parent = parent;
	}

	boolean isRoot() {
		return parent == null;
	}

	HashMap<String, TreeNode> getChildren() {
		return children;
	}

	boolean hasChild(String name) {
		return children.containsKey(name);
	}

	boolean addChild(String name, TreeNode child) {
		if (children.containsKey(name))
			return false;
		children.put(name, child);
		return true;
	}

	void removeChild(String name) {
		children.remove(name);
	}
}