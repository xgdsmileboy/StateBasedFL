package localization.instrument.visitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.StatementEvent;
import javax.xml.transform.Templates;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.internal.compiler.ast.FalseLiteral;

import com.sun.org.apache.xalan.internal.xsltc.compiler.Template;

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
		if(_clazzName.equals("auxiliary")){
			return false;
		}
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
		List<String> paramList = new ArrayList<>();
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
					paramList.add(singleVariableDeclaration.getName().getFullyQualifiedName());
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
		AST ast = AST.newAST(AST.JLS8);
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
		
//		//if a constructor, do not instrument 
//		if(node.isConstructor()){
//			blockStatement.add((ASTNode) ASTNode.copySubtree(ast, endGuard));
//			for (; i < methodBody.statements().size(); i++) {
//				ASTNode astNode = (ASTNode) methodBody.statements().get(i);
//				blockStatement.add((ASTNode) ASTNode.copySubtree(AST.newAST(AST.JLS8), astNode));
//			}
//		} else {
			List<Statement> tmpNodeList = new ArrayList<>();
			if(!Modifier.isStatic(node.getModifiers())){
				tmpNodeList.add(GenStatement.genThisFieldDumpMethodInvocation(message));
			}
			
			for(String param : paramList){
				tmpNodeList.add(GenStatement.genVariableDumpMethodInvation(message, param));
			}
			tmpNodeList.add(endGuard);
			
			for (; i < methodBody.statements().size(); i++) {
				ASTNode astNode = (ASTNode) methodBody.statements().get(i);
				if(astNode instanceof Statement){
					blockStatement.addAll(processMethodBody((Statement) astNode, message, tmpNodeList));
				} else {
					blockStatement.add(ASTNode.copySubtree(ast, astNode));
				}
			}
			ASTNode lastStatement = blockStatement.get(blockStatement.size() - 1);
			if(node.getReturnType2().toString().equals("void") && !(lastStatement instanceof ReturnStatement || lastStatement instanceof ThrowStatement)){
				for(Statement insert : tmpNodeList){
					blockStatement.add(ASTNode.copySubtree(ast, insert));
				}
			}
//		}
		
		methodBody.statements().clear();
		for (ASTNode statement : blockStatement) {
			methodBody.statements().add(ASTNode.copySubtree(methodBody.getAST(), statement));
		}

		return true;
	}
	
	public List<Statement> processMethodBody(Statement statement, String message, List<Statement> insertedNodes){
		List<Statement> result = new ArrayList<>();
		if (statement instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement) statement;

			Statement thenBody = ifStatement.getThenStatement();
			if (thenBody != null) {
				Block thenBlock = null;
				if (thenBody instanceof Block) {
					thenBlock = (Block) thenBody;
				} else {
					AST ast = AST.newAST(AST.JLS8);
					thenBlock = ast.newBlock();
					thenBlock.statements().add(ASTNode.copySubtree(thenBlock.getAST(), thenBody));
				}

				Block newThenBlock = processBlock(thenBlock, message, insertedNodes);
				ifStatement.setThenStatement((Statement) ASTNode.copySubtree(ifStatement.getAST(), newThenBlock));
			}

			Statement elseBody = ifStatement.getElseStatement();
			if (elseBody != null) {
				Block elseBlock = null;
				if (elseBody instanceof Block) {
					elseBlock = (Block) elseBody;
				} else {
					AST ast = AST.newAST(AST.JLS8);
					elseBlock = ast.newBlock();
					elseBlock.statements().add(ASTNode.copySubtree(elseBlock.getAST(), elseBody));
				}
				Block newElseBlock = processBlock(elseBlock, message, insertedNodes);
				ifStatement.setElseStatement((Statement) ASTNode.copySubtree(ifStatement.getAST(), newElseBlock));
			}
			result.add(ifStatement);
		} else if (statement instanceof WhileStatement) {

			WhileStatement whileStatement = (WhileStatement) statement;
			Statement whilebody = whileStatement.getBody();
			if (whilebody != null) {
				Block whileBlock = null;
				if (whilebody instanceof Block) {
					whileBlock = (Block) whilebody;
				} else {
					AST ast = AST.newAST(AST.JLS8);
					whileBlock = ast.newBlock();
					whileBlock.statements().add(ASTNode.copySubtree(whileBlock.getAST(), whilebody));
				}

				Block newWhileBlock = processBlock(whileBlock, message, insertedNodes);
				whileStatement.setBody((Statement) ASTNode.copySubtree(whileStatement.getAST(), newWhileBlock));
			}

			result.add(whileStatement);
		} else if (statement instanceof ForStatement) {

			ForStatement forStatement = (ForStatement) statement;
			Statement forBody = forStatement.getBody();
			if (forBody != null) {
				Block forBlock = null;
				if (forBody instanceof Block) {
					forBlock = (Block) forBody;
				} else {
					AST ast = AST.newAST(AST.JLS8);
					forBlock = ast.newBlock();
					forBlock.statements().add(ASTNode.copySubtree(forBlock.getAST(), forBody));
				}
				Block newForBlock = processBlock(forBlock, message, insertedNodes);
				forStatement.setBody((Statement) ASTNode.copySubtree(forStatement.getAST(), newForBlock));
			}

			result.add(forStatement);
		} else if (statement instanceof DoStatement) {

			DoStatement doStatement = (DoStatement) statement;
			Statement doBody = doStatement.getBody();
			if (doBody != null) {
				Block doBlock = null;
				if (doBody instanceof Block) {
					doBlock = (Block) doBody;
				} else {
					AST ast = AST.newAST(AST.JLS8);
					doBlock = ast.newBlock();
					doBlock.statements().add(ASTNode.copySubtree(doBlock.getAST(), doBody));
				}

				Block newDoBlock = processBlock(doBlock, message, insertedNodes);
				doStatement.setBody((Statement) ASTNode.copySubtree(doStatement.getAST(), newDoBlock));
			}

			result.add(doStatement);
		} else if (statement instanceof Block) {
			Block block = (Block) statement;
			Block newBlock = processBlock(block, message, insertedNodes);
			result.add(newBlock);
		} else if (statement instanceof EnhancedForStatement) {

			EnhancedForStatement enhancedForStatement = (EnhancedForStatement) statement;
			Statement enhancedBody = enhancedForStatement.getBody();
			if (enhancedBody != null) {
				Block enhancedBlock = null;
				if (enhancedBody instanceof Block) {
					enhancedBlock = (Block) enhancedBody;
				} else {
					AST ast = AST.newAST(AST.JLS8);
					enhancedBlock = ast.newBlock();
					enhancedBlock.statements().add(ASTNode.copySubtree(enhancedBlock.getAST(), enhancedBody));
				}
				Block newEnhancedBlock = processBlock(enhancedBlock, message, insertedNodes);
				enhancedForStatement
						.setBody((Statement) ASTNode.copySubtree(enhancedForStatement.getAST(), newEnhancedBlock));
			}

			result.add(enhancedForStatement);
		} else if (statement instanceof SwitchStatement) {

			SwitchStatement switchStatement = (SwitchStatement) statement;
			List<ASTNode> statements = new ArrayList<>();
			AST ast = AST.newAST(AST.JLS8);
			for (Object object : switchStatement.statements()) {
				ASTNode astNode = (ASTNode) object;
				statements.add(ASTNode.copySubtree(ast, astNode));
			}

			switchStatement.statements().clear();

			for (ASTNode astNode : statements) {
				if (astNode instanceof Statement) {
					Statement s = (Statement) astNode;
					for (Statement statement2 : processMethodBody(s, message, insertedNodes)) {
						switchStatement.statements().add(ASTNode.copySubtree(switchStatement.getAST(), statement2));
					}
				} else {
					switchStatement.statements().add(ASTNode.copySubtree(switchStatement.getAST(), astNode));
				}
			}
			result.add(switchStatement);
		} else if (statement instanceof TryStatement) {

			TryStatement tryStatement = (TryStatement) statement;

			Block tryBlock = tryStatement.getBody();
			if (tryBlock != null) {
				Block newTryBlock = processBlock(tryBlock, message, insertedNodes);
				tryStatement.setBody((Block) ASTNode.copySubtree(tryStatement.getAST(), newTryBlock));
			}

			List catchList = tryStatement.catchClauses();
			if(catchList != null){
				for (Object object : catchList) {
					if (object instanceof CatchClause) {
						CatchClause catchClause = (CatchClause) object;
						Block catchBlock = catchClause.getBody();
						Block newCatchBlock = processBlock(catchBlock, message, insertedNodes);
						catchClause.setBody((Block) ASTNode.copySubtree(catchClause.getAST(), newCatchBlock));
					}
				}
			}

			Block finallyBlock = tryStatement.getFinally();
			if (finallyBlock != null) {
				Block newFinallyBlock = processBlock(finallyBlock, message, insertedNodes);
				tryStatement.setFinally((Block) ASTNode.copySubtree(tryStatement.getAST(), newFinallyBlock));
			}

			result.add(tryStatement);
		} else {
			AST ast = AST.newAST(AST.JLS8);
			if(statement instanceof ReturnStatement || statement instanceof ThrowStatement){
				for(Statement insert : insertedNodes){
					result.add((Statement) ASTNode.copySubtree(ast, insert));
				}
			}
			result.add((Statement) ASTNode.copySubtree(ast, statement));
		}

		return result;
	}
	
	private Block processBlock(Block block, String message, List<Statement> insertedNodes) {
		Block newBlock = AST.newAST(AST.JLS8).newBlock();
		if (block == null) {
			return newBlock;
		}
		
		for (Object object : block.statements()) {
			if (object instanceof Statement) {
				Statement statement = (Statement) object;
				List<Statement> newStatements = processMethodBody(statement, message, insertedNodes);
				for (Statement newStatement : newStatements) {
					newBlock.statements().add(ASTNode.copySubtree(newBlock.getAST(), newStatement));
				}
			} else {
				if (Debugger.debugOn()) {
					Debugger.debug(__name__ + "#processBlock UNKNOWN astNode : " + object.toString());
				}
			}
		}
		return newBlock;
	}

	
	public static void main(String[] args) {
		DynamicRuntimeInfo dynamicRuntimeInfo = new DynamicRuntimeInfo("lang", 28);
		String path = "/Users/Jiajun/Code/Java/manualD4J/lang_28_buggy/src/main/java/org/apache/commons/lang3/text/translate/CharSequenceTranslator.java";
		String methodString = "org.apache.commons.lang3.text.translate.CharSequenceTranslator#String#translate#?,CharSequence";
//		DynamicRuntimeInfo dynamicRuntimeInfo = new DynamicRuntimeInfo("chart", 1);
//		String path = "/Users/Jiajun/Code/Java/manualD4J/chart_1_buggy/source/org/jfree/chart/LegendItem.java";
//		String methodString = "org.jfree.chart.LegendItem#void#setDataset#?,Dataset";
		Set<Method> methods = new HashSet<>();
		Method method = new Method(Identifier.getIdentifier(methodString));
		methods.add(method);
		StateCollectInstrumentVisitor stateCollectInstrumentVisitor = new StateCollectInstrumentVisitor(Constant.INSTRUMENT_SOURCE, dynamicRuntimeInfo);
		stateCollectInstrumentVisitor.setAllMethods(methods);
		Instrument.execute(path, stateCollectInstrumentVisitor);
	}

}
