package localization.core.path;

import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import localization.common.java.Method;

public class RomoveMethodBodyVisitor extends ASTVisitor {
	
	private Set<Method> _removeMethod = null;
	
	public RomoveMethodBodyVisitor(Set<Method> removeMethod) {
		_removeMethod = removeMethod;
	}
	
	@Override
	public boolean visit(MethodDeclaration node) {
		if(_removeMethod != null){
			for(Method method : _removeMethod){
				if(method.match(node)){
					node.getBody().statements().clear();
					break;
				}
			}
		}
		return true;
	}
	
}
