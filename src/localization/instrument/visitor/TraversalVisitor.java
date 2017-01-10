package localization.instrument.visitor;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

import localization.common.java.Method;

/**
 * Traverse a {@code CompilationUnit}
 * 
 * @author Jiajun
 *
 */
public abstract class TraversalVisitor extends ASTVisitor {
	
	public boolean traverse(CompilationUnit compilationUnit){
		compilationUnit.accept(this);
		return true;
	}
	
	public abstract void reset();
	
	public abstract void setFlag(String methodFlag);
	
	public abstract void setMethod(Method method); 
	
}
