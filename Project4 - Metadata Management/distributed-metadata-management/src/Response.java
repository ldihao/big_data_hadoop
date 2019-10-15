import java.io.Serializable;
import java.util.HashMap;

public class Response implements Serializable {
	String content;
	HashMap<String, Object> data = new HashMap<>();

	Response(String content) {
		this.content = content;
	}
}
