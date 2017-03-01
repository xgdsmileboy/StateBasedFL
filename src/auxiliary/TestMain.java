package auxiliary;

import java.util.HashMap;
import java.util.Map;

public class TestMain {
	
	public static void main(String[] args) {
//		Test test = new Test("test");
//		String content = Dumper.dump(test, 4, 5);
////		int test = 1;
////		String content = Dumper.dump((Object)1, 4, 5);
//		System.out.println(content);
		
		StringBuffer stringBuffer = new StringBuffer();
		Integer key = Integer.valueOf(1);
		Map<Integer, StringBuffer> map = new HashMap<>();
		map.put(key, stringBuffer);
		for(int i = 0; i < 100; i++){
		}
	}
	
}
