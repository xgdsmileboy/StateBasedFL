package localization.instrument.visitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import localization.common.config.Constant;
import localization.common.config.DynamicRuntimeInfo;
import localization.common.config.Identifier;
import localization.common.java.Method;
import localization.common.util.Debugger;
import localization.instrument.Instrument;
import localization.instrument.gen.GenStatement;
import localization.instrument.gen.InsertVariableGenerator;

public class StateCollectInstrumentVisitor extends TraversalVisitor {

	private final static String __name__ = "@StateCollectInstrumentVisitor ";

	private String _clazzName = "";
	private String _clazzFileName = "";
	private CompilationUnit _cu;
	private Method _method = null;
	private String _methodFlag = Constant.INSTRUMENT_SOURCE;
	private DynamicRuntimeInfo _dynamicRuntimeInfo = null; 
	private Set<Method> _allMethods = null;

	public StateCollectInstrumentVisitor(Method method, DynamicRuntimeInfo dynamicRuntimeInfo) {
		_method = method;
		_dynamicRuntimeInfo = dynamicRuntimeInfo;
	}
	
	public StateCollectInstrumentVisitor(String methodFlag, DynamicRuntimeInfo dynamicRuntimeInfo) {
		_methodFlag = methodFlag;
		_dynamicRuntimeInfo = dynamicRuntimeInfo;
	}
	
	public void setAllMethods(Set<Method> allMethods) {
		this._allMethods = allMethods;
	}

	@Override
	public void setFlag(String methodFlag) {
		_methodFlag = methodFlag;
	}
	
	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMethod(Method method) {
		_method = method;
	}

	@Override
	public boolean visit(CompilationUnit node) {
		_cu = node;
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
		
		if(_allMethods != null){
			boolean needInstrument = false;
			for(Method method : _allMethods){
				if (method.match(node, _clazzName)) {
					needInstrument = true;
					break;
				}
			}
			if(!needInstrument){
				return true;
			}
		}

		int keyValue = 0;
		if(_method == null){
			StringBuffer buffer = new StringBuffer(_clazzName + "#");
	
			String retType = "?";
			if (node.getReturnType2() != null) {
				retType = node.getReturnType2().toString();
			}
			StringBuffer param = new StringBuffer("?");
			for (Object object : node.parameters()) {
				if (!(object instanceof SingleVariableDeclaration)) {
					if (Debugger.debugOn()) {
						Debugger.debug(
								__name__ + "#visit Parameter is not a SingleVariableDeclaration : " + object.toString());
					}
					param.append(",?");
				} else {
					SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration) object;
					param.append("," + singleVariableDeclaration.getType().toString());
				}
			}
			// add method return type
			buffer.append(retType + "#");
			// add method name
			buffer.append(node.getName().getFullyQualifiedName() + "#");
			// add method params, NOTE: the first parameter starts at index 1.
			buffer.append(param);
	
			String message = buffer.toString();
	
			keyValue =Identifier.getIdentifier(message);
			
		} else {
			keyValue = _method.getMethodID();
		}
		
		//optimize instrument
		String message = Constant.INSTRUMENT_FLAG + _methodFlag + "#" + String.valueOf(keyValue);
		
		Block methodBody = node.getBody();

		if (methodBody == null) {
			return true;
		}

		List<ASTNode> blockStatement = new ArrayList<>();
		
		int i = 0;
		if (methodBody.statements().size() > 0) {
			ASTNode astNode = (ASTNode) methodBody.statements().get(0);
			if(astNode instanceof ConstructorInvocation || astNode instanceof SuperConstructorInvocation){
				i = 1;
				blockStatement.add(astNode);
			}
		}

		Statement startGuard = GenStatement.genASTNode(Constant.INSTRUMENT_FLAG + _methodFlag + ">>START" + "#" + String.valueOf(keyValue), 0);
		blockStatement.add(startGuard);
		
		InsertVariableGenerator genVariablePrinter = new InsertVariableGenerator(_dynamicRuntimeInfo, node, _cu, message);
		blockStatement.addAll(genVariablePrinter.generate());
		
		Statement endGuard = GenStatement.genASTNode(Constant.INSTRUMENT_FLAG + _methodFlag + ">>END" + "#" + String.valueOf(keyValue), 0);
		blockStatement.add(endGuard);

		for (; i < methodBody.statements().size(); i++) {
			ASTNode astNode = (ASTNode) methodBody.statements().get(i);
			blockStatement.add((ASTNode) ASTNode.copySubtree(AST.newAST(AST.JLS8), astNode));
		}
		methodBody.statements().clear();
		for (ASTNode statement : blockStatement) {
			methodBody.statements().add(ASTNode.copySubtree(methodBody.getAST(), statement));
		}

		return true;
	}

	
	public static void main(String[] args) {
		DynamicRuntimeInfo dynamicRuntimeInfo = new DynamicRuntimeInfo("chart", 1);
		String path = "/Users/Jiajun/Code/Java/defects4j/chart_1_buggy/source/org/jfree/chart/event/ChartChangeEvent.java";
		String methodString = "org.jfree.chartevent.ChangeChartEvent#?#ChartChangeEvent#?,Object,JFreeChart";
		Set<Method> methods = new HashSet<>();
		Method method = new Method(Identifier.getIdentifier(methodString));
		methods.add(method);
		StateCollectInstrumentVisitor stateCollectInstrumentVisitor = new StateCollectInstrumentVisitor(Constant.INSTRUMENT_SOURCE, dynamicRuntimeInfo);
		stateCollectInstrumentVisitor.setAllMethods(methods);
		Instrument.execute(path, stateCollectInstrumentVisitor);
	}

}
