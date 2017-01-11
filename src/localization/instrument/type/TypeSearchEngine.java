package localization.instrument.type;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.print.attribute.standard.PageRanges;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

//import com.sun.org.apache.bcel.internal.classfile.Visitor;
//import com.sun.org.apache.bcel.internal.generic.Type;

import localization.common.config.Constant;
import localization.common.java.JavaFile;
import localization.common.util.Debugger;

public class TypeSearchEngine {

	private final static String __name__ = "@TypeSearchEngine ";

	public static boolean searchType(String path, String name) {
		path = path.replaceAll("\\.", Constant.PATH_SEPARATOR);
		String javaFile = path + Constant.PATH_SEPARATOR + name + ".java";
		File file = new File(javaFile);
		if (!file.exists()) {
			return false;
		}
		return true;
	}
	
	public static List<String> searchSimpleMethod(String javaFilePath){
		File file = new File(javaFilePath);
		if(!file.exists()){
			if (Debugger.debugOn()) {
				Debugger.debug(__name__ + "searchSimpleMethod Not a file for the given file path : " + javaFilePath);
				return null;
			}
		}
		
		CompilationUnit unit = JavaFile.genASTFromSource(JavaFile.readFileToString(file), ASTParser.K_COMPILATION_UNIT);
		CollectMethodVisitor collectMethodVisitor = new CollectMethodVisitor();
		unit.accept(collectMethodVisitor);
		return collectMethodVisitor.getMethods();
	}

//	public static void main(String[] args) {
//		String path = "/Users/Jiajun/Code/Java/fault-localization/test_1_buggy/src/code";
//		System.out.println(searchType(path, "AnotherClazz"));
//	}
}

class CollectMethodVisitor extends ASTVisitor {
	
	private List<String> _methods = new ArrayList<>(); 
	
	public List<String> getMethods(){
		return _methods;
	}
	
	@Override
	public boolean visit(MethodDeclaration node) {
		
		ASTNode parent = node.getParent();
		while (!(parent instanceof CompilationUnit)) {
			if (parent instanceof TypeDeclaration) {
				TypeDeclaration typeDeclaration = (TypeDeclaration) parent;
				if(!Modifier.isPublic(typeDeclaration.getModifiers())){
					return true;
				}
				break;
			} else if(parent instanceof AnonymousClassDeclaration){
				return true;
			}
			parent = parent.getParent();
		}
		
		if(!Modifier.isStatic(node.getModifiers()) && Modifier.isPublic(node.getModifiers())){
			Type retType = node.getReturnType2();
			if(retType != null && !retType.toString().equals("void") && (retType.isPrimitiveType() || retType.toString().equals("String"))){
				if(node.parameters().size() == 0){
					//add more constraint for the method to ensure the method is side-effect free
					//simple strategy, method that has no assignment and method invocation is side-effect free
					SideEffectMethod sideEffectMethod = new SideEffectMethod();
					node.accept(sideEffectMethod);
					if(sideEffectMethod.isFree()){
						_methods.add(node.getName().getFullyQualifiedName());
					}
				}
			}
		}
		
		return true;
	}
	
	class SideEffectMethod extends ASTVisitor {
		private boolean _sideEffectFree = true;
		@Override
		public boolean visit(MethodInvocation node) {
			_sideEffectFree = false;
			return false;
		}
		
		public boolean visit(Assignment node){
			_sideEffectFree = false;
			return false;
		}
		
		public boolean isFree(){
			return _sideEffectFree;
		}
	}
}