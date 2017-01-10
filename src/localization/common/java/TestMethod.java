package localization.common.java;

import java.util.ArrayList;
import java.util.List;

import localization.common.config.Identifier;

public class TestMethod extends Method{
	
	/**
	 * record the execution path info of this test method, contain duplicated
	 * methods if run more than one time
	 */
	private List<ExecutedMethod> _path = null;
	/**
	 * record the path length of this test, method level, such as the running
	 * path as "MehtodA MethodB MethodA MethodC"
	 */
	private int _methodNumber = 0;
	/**
	 * record the lines ran by this test, since some lines may not be ran due to
	 * a test failure
	 */
	private List<Integer> _runLines;
	/**
	 * record the test statement number, such as there are two lines of test in
	 * the test case, this field may be 1 or 2
	 */
	private int _whichStatement = 1;
	
	public TestMethod(int methodID){
		super(methodID);
		_path = new ArrayList<>();
		_runLines = new ArrayList<>();
	}
	
	public void setTestStatementNumber(int whichStatement) {
		_whichStatement = whichStatement;
	}

	public int getTestStatementNumber() {
		return _whichStatement;
	}

	public void addExecutedLine(int lineNumber) {
		_runLines.add(Integer.valueOf(lineNumber));
	}

	public List<Integer> getExecutedLine() {
		return _runLines;
	}

	public List<ExecutedMethod> getExecutionPath() {
		return _path;
	}
	
	public void addExecutedMethod(ExecutedMethod method, int lineNumber) {
		method.addExecutedLine(lineNumber);
		_path.add(method);
		_methodNumber++;
	}
	
	public void addExecutedMethod(ExecutedMethod method) {
		ExecutedMethod newPath = new ExecutedMethod(method);
		_path.add(method);
		_methodNumber++;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof TestMethod)){
			return false;
		}
		TestMethod other = (TestMethod)obj;
		if(other.getMethodID() != getMethodID()){
			return false;
		}
			
		return true;
	}
	
	public int find(ExecutedMethod method) {
		int index = -1;
		if (_methodNumber > 0) {
			for (int i = 0; i < _path.size(); i++) {
				if (_path.get(i).getMethodID() == method.getMethodID()) {
					index = i;
					break;
				}
			}
		}
		return index;
	}
	
	@Override
	public String toString() {
		String methodString = Identifier.getMessage(getMethodID());
		return methodString + "#" + _whichStatement;
	}
}
