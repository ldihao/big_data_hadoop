import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class MetaData implements Serializable {
	private final HashMap<Integer, String> typeMapping = new HashMap<Integer, String>() {
		{
			put(1, "regular file");
			put(2, "directory");
			put(3, "symbolic link");
		}
	};
	private String name;
	private int type;
	private Date atime;
	private Date mtime;
	private Date ctime;
	private int[] permission;

	MetaData(String name, int type, Date atime, Date mtime, Date ctime) {
		if (name == null || name.length() == 0)
			throw new AssertionError();
		if (type != 1 && type != 2 && type != 3)
			throw new AssertionError();
		this.name = name;
		this.type = type;
		this.atime = atime;
		this.mtime = mtime;
		this.ctime = ctime;
		this.permission = new int[] { 7, 5, 5 };
	}

	public static void main(String[] argv) {
		MetaData metaData1 = new MetaData("test1", 1, new Date(), new Date(), new Date());
		MetaData metaData2 = new MetaData("test2", 2, new Date(), new Date(), new Date());
		MetaData metaData3 = new MetaData("test3", 3, new Date(), new Date(), new Date());
		HashMap<String, String> meta1 = metaData1.getMeta();
		HashMap<String, String> meta2 = metaData2.getMeta();
		HashMap<String, String> meta3 = metaData3.getMeta();
		for (String key : meta1.keySet()) {
			System.out.println(key + ": " + meta1.get(key));
		}
		System.out.println();
		for (String key : meta2.keySet()) {
			System.out.println(key + ": " + meta2.get(key));
		}
		System.out.println();
		for (String key : meta3.keySet()) {
			System.out.println(key + ": " + meta3.get(key));
		}
		System.out.println();
	}

	HashMap<String, String> getMeta() {
		HashMap<String, String> res = new HashMap<>();
		res.put("name", name);
		res.put("type", typeMapping.get(type));
		res.put("atime", atime.toString());
		res.put("mtime", mtime.toString());
		res.put("ctime", ctime.toString());
		res.put("permission", getPermission());
		return res;
	}

	String getPermission() {
		StringBuilder sb = new StringBuilder();
		if (type == 2) {
			sb.append('d');
		} else {
			sb.append('-');
		}
		for (int i = 0; i < 3; ++i) {
			if ((permission[i] & 0b100) != 0) {
				sb.append('r');
			} else {
				sb.append('-');
			}
			if ((permission[i] & 0b010) != 0) {
				sb.append('w');
			} else {
				sb.append('-');
			}
			if ((permission[i] & 0b001) != 0) {
				sb.append('x');
			} else {
				sb.append('-');
			}
		}
		return sb.toString();
	}

	boolean setName(String name) {
		if (name == null || name.length() == 0)
			return false;
		this.name = name;
		return true;
	}

	void setAtime(Date atime) {
		this.atime = atime;
	}

	void setMtime(Date mtime) {
		this.mtime = mtime;
	}

	void setCtime(Date ctime) {
		this.ctime = ctime;
	}

	boolean setPermission(int[] permission) {
		if (permission == null || permission.length < 3)
			return false;
		this.permission = Arrays.copyOfRange(permission, 0, 3);
		return true;
	}
}
