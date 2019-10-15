import java.io.Serializable;
import java.util.HashMap;

public class Request implements Serializable {
	String content;
	HashMap<String, Object> data = new HashMap<>();

	Request(String content) {
		this.content = content;
	}
}
