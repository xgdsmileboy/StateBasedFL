package localization.instrument.visitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import localization.common.config.Constant;
import localization.common.config.Identifier;
import localization.common.java.JavaFile;
import localization.common.java.Method;
import localization.common.util.Debugger;
import localization.instrument.gen.GenStatement;

/**
 * This class is used for instrument for each method, only one print statement
 * for each method
 * 
 * @author Jiajun
 *
 */
public class MethodInstrumentVisitor extends TraversalVisitor {

	private final static String __name__ = "@MethodInstrumentVisitor ";

	private String _methodFlag = Constant.INSTRUMENT_SOURCE;
	private String _clazzName = "";
	private String _clazzFileName = "";
	private CompilationUnit _cu;
	private Method _method = null;

	public MethodInstrumentVisitor() {
	}

	public MethodInstrumentVisitor(String methodFlag) {
		_methodFlag = methodFlag;
	}

	public MethodInstrumentVisitor(Method method) {
		_method = method;
	}

	@Override
	public void reset() {
		_methodFlag = Constant.INSTRUMENT_SOURCE;
		_method = null;
		_clazzFileName = "";
		_clazzName = "";
		_cu = null;
	}

	@Override
	public void setFlag(String methodFlag) {
		_methodFlag = methodFlag;
	}

	@Override
	public void setMethod(Method method) {
		_method = method;
	}

	@Override
	public boolean visit(CompilationUnit node) {
		if (node.getPackage().getName() != null
				&& node.getPackage().getName().getFullyQualifiedName().equals("auxiliary")) {
			return false;
		}
		_cu = node;
		if (_method != null) {
			// filter unrelative files
			String methodString = Identifier.getMessage(_method.getMethodID());
			if (!methodString.contains(_cu.getPackage().getName().getFullyQualifiedName())) {
				if (Debugger.debugOn()) {
					Debugger.debug(__name__ + "@visit Not the right java file.");
				}
				return false;
			}
		}
		_clazzName = node.getPackage().getName().getFullyQualifiedName();
		for (Object object : node.types()) {
			if (object instanceof TypeDeclaration) {
				TypeDeclaration type = (TypeDeclaration) object;
				if (Modifier.isPublic(type.getModifiers())) {
					_clazzName += Constant.INSTRUMENT_DOT_SEPARATOR + type.getName().getFullyQualifiedName();
					_clazzFileName = _clazzName;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		if (!Modifier.isPublic(node.getModifiers())) {
			_clazzName = _clazzFileName + "$" + node.getName().getFullyQualifiedName();
		}
		return true;
	}

	@Override
	public boolean visit(MethodDeclaration node) {

		if (_method != null && !_method.match(node, _clazzName)) {
			return true;
		}

		String name = node.getName().getFullyQualifiedName();

		if (Modifier.isStatic(node.getModifiers())) {
			return true;
		}

		if (_methodFlag.equals(Constant.INSTRUMENT_TEST) && (name.equals("setUp") || name.equals("countTestCases")
				|| name.equals("createResult") || name.equals("run") || name.equals("runBare") || name.equals("runTest")
				|| name.equals("tearDown") || name.equals("toString") || name.equals("getName")
				|| name.equals("setName"))) {
			return true;
		}

		// filter those functional methods in test class path, test method name
		// starting with "test" in Junit 3 while with annotation as "@Test" in
		// Junit 4, 
		//TODO should be optimized since the "contain" method is time consuming
		if (_methodFlag.equals(Constant.INSTRUMENT_TEST)
				&& !name.startsWith("test") && !node.toString().contains("@Test")) {
			return true;
		}

		// filter those methods that defined in anonymous classes
		ASTNode parent = node.getParent();
		while (parent != null && !(parent instanceof TypeDeclaration)) {
			if (parent instanceof ClassInstanceCreation) {
				return true;
			}
			parent = parent.getParent();
		}

		if (node.getBody() != null) {
			Block body = node.getBody();
			List<ASTNode> backupStatement = new ArrayList<>();
			AST ast = AST.newAST(AST.JLS8);

			ASTNode thisOrSuperStatement = null;
			if (body.statements().size() > 0) {
				ASTNode astNode = (ASTNode) body.statements().get(0);
				int startIndex = 0;
				if (astNode instanceof SuperConstructorInvocation
						|| (astNode instanceof ConstructorInvocation && astNode.toString().startsWith("this"))) {
					thisOrSuperStatement = ASTNode.copySubtree(ast, astNode);
					startIndex = 1;
				}
				for (; startIndex < body.statements().size(); startIndex++) {
					ASTNode statement = (ASTNode) body.statements().get(startIndex);
					backupStatement.add(ASTNode.copySubtree(ast, statement));
				}
			}

			// StringBuffer buffer = new
			// StringBuffer(Constant.INSTRUMENT_KEY_TYPE + _clazzName);
			StringBuffer buffer = new StringBuffer(_clazzName + "#");

			String retType = "?";
			if (node.getReturnType2() != null) {
				retType = node.getReturnType2().toString();
			}
			StringBuffer param = new StringBuffer("?");
			for (Object object : node.parameters()) {
				if (!(object instanceof SingleVariableDeclaration)) {
					if (Debugger.debugOn()) {
						Debugger.debug(__name__ + "#visit Parameter is not a SingleVariableDeclaration : "
								+ object.toString());
					}
					param.append(",?");
				} else {
					SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) object;
					param.append("," + singleVariableDeclaration.getType().toString());
				}
			}
			// add method return type
			// buffer.append(Constant.INSTRUMENT_KEY_METHOD + retType +
			// "#");
			buffer.append(retType + "#");
			// add method name
			buffer.append(node.getName().getFullyQualifiedName() + "#");
			// add method params, NOTE: the first parameter starts at index
			// 1.
			buffer.append(param);
			String message = buffer.toString();
			int keyValue = Identifier.getIdentifier(message);
			// optimize instrument
			message = Constant.INSTRUMENT_FLAG + _methodFlag + "#" + String.valueOf(keyValue);

			int lineNumber = _cu.getLineNumber(node.getBody().getStartPosition());

			Statement insert = GenStatement.genASTNode(message, lineNumber);

			body.statements().clear();
			if (thisOrSuperStatement != null) {
				body.statements().add(ASTNode.copySubtree(body.getAST(), thisOrSuperStatement));
			}
			body.statements().add(ASTNode.copySubtree(body.getAST(), insert));
			for (ASTNode statement : backupStatement) {
				body.statements().add(ASTNode.copySubtree(body.getAST(), statement));
			}
		}

		return true;
	}

	public static void main(String[] args) {
		String filePath = "/Users/Jiajun/Code/Java/defects4j/math_1_buggy/src/test/java/org/apache/commons/math3/analysis/integration/IterativeLegendreGaussIntegratorTest.java";
		CompilationUnit compilationUnit = JavaFile.genASTFromSource(JavaFile.readFileToString(filePath),
				ASTParser.K_COMPILATION_UNIT);
		compilationUnit.accept(new MethodInstrumentVisitor());
		// System.out.println(compilationUnit.toString());
		// JavaFile.writeStringToFile(filePath, compilationUnit.toString());
	}

}
