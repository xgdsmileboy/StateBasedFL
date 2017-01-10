package localization.common.java;

import java.util.ArrayList;
import java.util.List;

import localization.common.config.Identifier;

public class ExecutedMethod extends Method{
	
	/**
	 * records the line executed in this method
	 */
	private List<Integer> _executedLines = null;
	/**
	 * record the cycle in which we should sample the corresponding method
	 * state. For example, if this {@code ExecutedMethod} is in the path of of
	 * {@code TestMethod} A, the {@code _sampleCycle} is 1 means that if we
	 * mutate the code in this {@code ExecutedMethod}, we can collect wrong
	 * program state at {@code TestMethod} A.
	 */
	private int _sampleCycle = 1;
	
	private boolean _canBeAsWathPoint = false;
	
	public ExecutedMethod(ExecutedMethod method){
		super(method.getMethodID());
		_executedLines = new ArrayList<>();
		_executedLines.addAll(method.getExecutedLines());
	}

	public ExecutedMethod(int methodID) {
		super(methodID);
		_executedLines = new ArrayList<>();
	}
	
	public void setCanBeWatchPoint(boolean canBeAsWatchPoint){
		_canBeAsWathPoint = canBeAsWatchPoint;
	}
	
	public boolean getCanBeWatchPoint(){
		return _canBeAsWathPoint;
	}
	
	public void setSampleCycle(int cycle) {
		_sampleCycle = cycle;
	}

	public int getSampleCycle() {
		return _sampleCycle;
	}
	
	public boolean containLine(int line){
		return _executedLines.contains(line);
	}

	public void addExecutedLine(int lineNumber) {
		_executedLines.add(Integer.valueOf(lineNumber));
	}

	public List<Integer> getExecutedLines() {
		return _executedLines;
	}
	
    @Override
    public boolean equals(Object obj) {
    	if(obj == null || !(obj instanceof ExecutedMethod)){
    		return false;
    	}
    	ExecutedMethod other = (ExecutedMethod) obj;
    	if(other.getMethodID() != getMethodID() || other.getSampleCycle() != _sampleCycle){
//    	if(other.getMethodID() != getMethodID()){
    		return false;
    	}
    	return true;
    }
    
    @Override
    public String toString() {
    	String methodString = Identifier.getMessage(getMethodID());
    	StringBuffer stringBuffer = new StringBuffer(methodString + "#");
    	if (_executedLines.size() > 0) {
			stringBuffer.append(_executedLines.get(0));
		} else {
			stringBuffer.append("0");
		}
		for (int i = 1; i < _executedLines.size(); i++) {
			stringBuffer.append("," + _executedLines.get(i));
		}
		stringBuffer.append("#" + _sampleCycle);
    	return stringBuffer.toString();
    }
}
