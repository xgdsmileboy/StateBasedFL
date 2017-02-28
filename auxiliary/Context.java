package auxiliary;

import java.util.ArrayList;
import java.util.List;

public class Context {
	private String name = "context";
	public List<Integer> data = new ArrayList<>();
	public Context(int index) {
		this.data.add(index);
	}
}
