package auxiliary;

import java.util.ArrayList;
import java.util.List;

public class Test {
	public int a = 1;
	private int[] b = {1};
	private String[] arrays;
	private Object isNull;
	private Context context = null;
	private Context nullContext = null;
	private List<String> list = new ArrayList<>();
	private List<Context> mContexts = new ArrayList<>();
	
	public Test(String content) {
		list.add(content);
		context = new Context(7);
		mContexts.add(context);
		arrays = new String[]{"element1", "element2"};
	}
}
