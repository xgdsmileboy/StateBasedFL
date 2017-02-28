package auxiliary;

public class TestMain {
	
	public static void main(String[] args) {
		Test test = new Test("test");
		String content = Dumper.dump(test, 4, 5);
//		int test = 1;
//		String content = Dumper.dump((Object)1, 4, 5);
		System.out.println(content);
	}
	
}
