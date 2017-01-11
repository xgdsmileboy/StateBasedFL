package localization.instrument.gen;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import localization.common.config.Constant;
import localization.common.config.DynamicRuntimeInfo;
import localization.common.config.InfoBuilder;
import localization.common.util.Debugger;
import localization.instrument.type.TypeSearchEngine;

public class InsertVariableGenerator {

	private final static String __name__ = "@InserVariablePrinter ";

	private CompilationUnit _cu = null;
	private MethodDeclaration _methodDeclaration = null;
	private String _locMessage = "";
	private DynamicRuntimeInfo _dynamicRuntimeInfo = null;

	public InsertVariableGenerator(DynamicRuntimeInfo dynamicRuntimeInfo, MethodDeclaration methodDeclaration, CompilationUnit cu, String locMessage) {
		_methodDeclaration = methodDeclaration;
		_cu = cu;
		_locMessage = locMessage;
		_dynamicRuntimeInfo = dynamicRuntimeInfo;
	}

	public List<ASTNode> generate() {
		List<ASTNode> statements = new ArrayList<>();

		boolean insertField = true;
		ASTNode astNode = _methodDeclaration.getParent();
		while (!(astNode instanceof CompilationUnit)) {
			if (astNode instanceof TypeDeclaration) {
				break;
			} else if(astNode instanceof AnonymousClassDeclaration){
				insertField = false;
			}
			astNode = astNode.getParent();
		}
		if (!(astNode instanceof TypeDeclaration)) {
			if (Debugger.debugOn()) {
				Debugger.debug(__name__ + "#generate Parent node is not a TypeDeclaration of method : "
						+ _methodDeclaration.toString());
			}
			return statements;
		}

		TypeDeclaration typeDeclaration = (TypeDeclaration) astNode;

		if(insertField){
			// print field information
			statements.addAll(insertFieldsPrinter(typeDeclaration));
		}

		// print parameter information
		List<ASTNode> params = _methodDeclaration.parameters();
		if (params.size() > 0) {
			statements.addAll(insertParamsPrinter(params));
		}

		return statements;
	}

	private List<ASTNode> insertFieldsPrinter(TypeDeclaration typeDeclaration) {
		List<ASTNode> statements = new ArrayList<>();
		FieldDeclaration[] fieldDeclarations = typeDeclaration.getFields();
		for (FieldDeclaration field : fieldDeclarations) {
			if (!Modifier.isFinal(field.getModifiers())) {
				String prefix = "this";
				//static function can not read non-static field
				if (!Modifier.isStatic(field.getModifiers()) && Modifier.isStatic(_methodDeclaration.getModifiers())) {
					continue;
				}
				//non-static function should add class name to reach the static field
				if (Modifier.isStatic(field.getModifiers()) && !Modifier.isStatic(_methodDeclaration.getModifiers())) {
					prefix = typeDeclaration.getName().getFullyQualifiedName();
				}
				Type type = field.getType();
				List<ASTNode> variables = field.fragments();
				for (ASTNode astNode : variables) {
					if (astNode instanceof VariableDeclarationFragment) {
						VariableDeclarationFragment variableDeclarationFragment = (VariableDeclarationFragment) astNode;
						SimpleName name = variableDeclarationFragment.getName();
						List<ASTNode> nodes = genStatement(prefix, type, name, _locMessage);
						if(nodes != null){
							statements.addAll(nodes);
						}
					} else {
						if (Debugger.debugOn()) {
							Debugger.debug(__name__ + "#insertFieldsPrinter Wrong field VariableDeclarationFragment : "
									+ astNode.toString());
						}
					}
				}
			}
		}
		return statements;
	}

	private List<ASTNode> insertParamsPrinter(List<ASTNode> params) {
		List<ASTNode> statements = new ArrayList<>();
		for (ASTNode param : params) {
			if (param instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) param;
				Type type = singleVariableDeclaration.getType();
				SimpleName name = singleVariableDeclaration.getName();
				List<ASTNode> nodes = genStatement(null, type, name, _locMessage);
				if(nodes != null){
					statements.addAll(nodes);
				}

			} else {
				if (Debugger.debugOn()) {
					Debugger.debug(__name__ + "#insertParamsPrinter Parameter is not SingleVariableDeclaration : "
							+ param.toString());
				}
			}
		}
		return statements;
	}

	private List<ASTNode> genStatement(String prefix, Type type, SimpleName name, String message) {
		List<ASTNode> statements = new ArrayList<>();
		if (type.isPrimitiveType() || type.toString().equals("String")) {
			statements.add(GenStatement.genPrimitiveStatement(prefix, message, name.toString()));
			return statements;
		} else if (type.isSimpleType()) {
			String sourcePath = InfoBuilder.buildSourceSRCPath(_dynamicRuntimeInfo, true);
			boolean imported = false;
			for(Object object : _cu.imports()){
				if (object instanceof ImportDeclaration) {
					ImportDeclaration importDeclaration = (ImportDeclaration) object;
					String importName = importDeclaration.getName().getFullyQualifiedName();
					int index = importName.lastIndexOf(".");
					if(index < 0){
						if (Debugger.debugOn()) {
							Debugger.debug(__name__ + "#genStatement Parse the format of Import failed : " + importDeclaration);
						}
						continue;
					}
					String importClazzName = importName.substring(index + 1);
					if(importClazzName.equals(type.toString())){
						imported = true;
						String packageName = importName.substring(0, index);
						String absoluteJavaFilePath = sourcePath + Constant.PATH_SEPARATOR + packageName;
						if(TypeSearchEngine.searchType(absoluteJavaFilePath, importClazzName)){
							//find clazz
							String file = absoluteJavaFilePath.replaceAll("\\.", Constant.PATH_SEPARATOR) + Constant.PATH_SEPARATOR + importClazzName + ".java";
							List<ASTNode> nodes = genStatement(prefix, file, name, message);
							if(nodes != null){
								statements.addAll(nodes);
							}
							
						} else{
							if(Debugger.debugOn()){
								Debugger.debug(__name__ + "getStatement Not a type imported : " + importClazzName);
							}
						}
						break;
					}
				} else {
					if(Debugger.debugOn()){
						Debugger.debug(__name__ + "#genStatement Import is not an ImportDeclaration : " + object);
					}
				}
			}
			if(!imported){
				String currPackage = _cu.getPackage().getName().getFullyQualifiedName();
				String absoluteJavaFilePath = sourcePath + Constant.PATH_SEPARATOR + currPackage;
				if(TypeSearchEngine.searchType(absoluteJavaFilePath, type.toString())){
					//find the type
					String file = absoluteJavaFilePath.replaceAll("\\.", Constant.PATH_SEPARATOR) + Constant.PATH_SEPARATOR + type.toString() + ".java";
					List<ASTNode> nodes = genStatement(prefix, file, name, message);
					if(nodes != null){
						statements.addAll(nodes);
					}
				}
			}
			
		} else if (type.isQualifiedType() || type.isArrayType() || type.isParameterizedType() || type.isUnionType()) {
			Statement newStatement = GenStatement.genNullCheckerStatement(prefix, name.getFullyQualifiedName(), message);
			if(newStatement != null){
				statements.add(newStatement);
			}
			
		} else {
			if (Debugger.debugOn()) {
				Debugger.debug(__name__ + "#genStatement  UNKNOWN (Not print) type : " + type);
			}
		}

		return statements;
	}
	
	private List<ASTNode> genStatement(String prefix, String javaFilePath, SimpleName name, String message){
		List<ASTNode> statements = new ArrayList<>();
		List<String> methods = TypeSearchEngine.searchSimpleMethod(javaFilePath);
		if(methods != null){
			for(String method : methods){
				Statement newStatement = GenStatement.genMethodInvocationStatement(prefix, name.getFullyQualifiedName(), method, message);
				statements.add(newStatement);
			}
		}
		return statements;
	}

}
