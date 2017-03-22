package localization.common.java;

import java.util.List;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;

import localization.common.config.Identifier;
import localization.common.util.Debugger;

public class Method {
	
	private final String __name__ = "@Method ";
	
	private int _methodID = -1;
	
	public Method(int methodID){
		_methodID = methodID;
	}
	
	public int getMethodID(){
		return _methodID;
	}
	
	public boolean match(MethodDeclaration methodDeclaration) {
		String methodString = Identifier.getMessage(_methodID);
		String[] concreteInfo = methodString.split("#");
		if(concreteInfo.length < 4){
			System.err.println(__name__ + "#match Method Info Error : " + methodString);
			return false;
		}
		String fullClazzPath = concreteInfo[0];
		String _retType = concreteInfo[1];
		String methodName = concreteInfo[2];
		//?,int,float
		String[] arguments = concreteInfo[3].split(",");
	
		Type type = methodDeclaration.getReturnType2();
		String typeStr = "?";
		if (type != null) {
			typeStr = type.toString();
		}
		if (!typeStr.equals(_retType)) {
			return false;
		}
		String name = methodDeclaration.getName().getFullyQualifiedName();
		List<Object> methodParams = methodDeclaration.parameters();
		if (!name.equals(methodName) || methodParams.size() != (arguments.length - 1)) {
			return false;
		}
		for (int i = 0; i < methodParams.size(); i++) {
			if (methodParams.get(i) instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) methodParams.get(i);
				if (!singleVariableDeclaration.getType().toString().equals(arguments[i+1])) {
					return false;
				}
			} else {
				if (Debugger.debugOn()) {
					Debugger.debug(__name__ + "# match method parameter is not a SingleVariableDeclaration : "
							+ methodParams.get(i).toString());
				}
				return false;
			}
		}
		return true;
	}
	
	public boolean match(MethodDeclaration methodDeclaration, String packageAndClazzName) {
		if (match(methodDeclaration)) {
			String methodString = Identifier.getMessage(_methodID);
			if (methodString != null && packageAndClazzName != null) {
//				int index = packageAndClazzName.indexOf("$");
//				if(index > 0){
//					packageAndClazzName = packageAndClazzName.substring(0, index);
//				}
				if (methodString.contains(packageAndClazzName)) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof Method)){
			return false;
		}
		Method other = (Method)obj;
		if(other.getMethodID() != _methodID){
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		String methodString = Identifier.getMessage(_methodID);
		return methodString;
	}
}
