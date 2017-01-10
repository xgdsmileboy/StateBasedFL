package localization.mutation.visitor;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import localization.common.util.Pair;

public class CollectMethodVisitor extends ASTVisitor {
	
	private Set<Pair<String, String>> _allMethods = new HashSet<>();
	private String _packageName = "";
	private String _clazzName = "";
	
	@Override
	public boolean visit(CompilationUnit node) {
		_packageName = node.getPackage().getName().getFullyQualifiedName();
		return true;
	}
	
	@Override
	public boolean visit(TypeDeclaration node) {
		_clazzName = node.getName().getFullyQualifiedName();
		
		String clazzPath = _packageName + "." + _clazzName;
		
		for(MethodDeclaration methodDeclaration : node.getMethods()){
			if(methodDeclaration.getBody() == null){
				continue;
			}
			if(methodDeclaration.getBody().statements().size() > 0){
				Pair<String, String> pair = new Pair<>(clazzPath, methodDeclaration.getName().getFullyQualifiedName());
				_allMethods.add(pair);
			}
		}
		return true;
	}
	
	public Set<Pair<String, String>> getAllMethods(){
		return _allMethods;
	}
}
