package localization.instrument.visitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
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

import localization.common.config.Constant;
import localization.common.config.DynamicRuntimeInfo;
import localization.common.config.Identifier;
import localization.common.java.JavaFile;
import localization.common.java.Method;
import localization.common.util.Debugger;
import localization.instrument.gen.GenStatement;
import localization.instrument.gen.InsertVariableGenerator;

public class MutantFlagInstrumentVisitor extends TraversalVisitor {

	private final static String __name__ = "@MutantFlagInstrumentVisitor ";

	private String _clazzName = "";
	private String _clazzFileName = "";
	private CompilationUnit _cu;
	private String _methodFlag = Constant.INSTRUMENT_SOURCE;
	private DynamicRuntimeInfo _dynamicRuntimeInfo = null; 
	private Set<Method> _allMethods = null;
	private int _mutantLineNumber;

	public MutantFlagInstrumentVisitor(int mutantLineNumber, DynamicRuntimeInfo dynamicRuntimeInfo) {
		_mutantLineNumber = mutantLineNumber;
		_dynamicRuntimeInfo = dynamicRuntimeInfo;
	}
	
	public void setAllMethods(Set<Method> allMethods) {
		this._allMethods = allMethods;
	}

	@Override
	public void setFlag(String methodFlag) {
	}
	
	@Override
	public void reset() {
		
	}

	@Override
	public void setMethod(Method method) {
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
		
		boolean needInstrument = false;
		if(_allMethods != null){
			for(Method method : _allMethods){
				if (method.match(node, _clazzName)) {
					needInstrument = true;
					break;
				}
			}
		}

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

		String keyValue = String.valueOf(Identifier.getIdentifier(message));
		
		//optimize instrument
		message = Constant.INSTRUMENT_FLAG + _methodFlag + "#" + keyValue;
		
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

		if(needInstrument){
			Statement startGuard = GenStatement.genASTNode(Constant.INSTRUMENT_FLAG + _methodFlag + ">>START" + "#" + keyValue, 0);
			blockStatement.add(startGuard);
		
			InsertVariableGenerator genVariablePrinter = new InsertVariableGenerator(_dynamicRuntimeInfo, node, _cu, message);
			blockStatement.addAll(genVariablePrinter.generate());
			
			Statement endGuard = GenStatement.genASTNode(Constant.INSTRUMENT_FLAG + _methodFlag + ">>END" + "#" + keyValue, 0);
			blockStatement.add(endGuard);
		}

		int startLine = _cu.getLineNumber(node.getStartPosition());
		int endLine = _cu.getLineNumber(node.getStartPosition() + node.getLength());
		if(startLine <= _mutantLineNumber && _mutantLineNumber <= endLine){
			
			AST ast = AST.newAST(AST.JLS8);
			for (; i < methodBody.statements().size(); i++) {
				ASTNode astNode = (Statement) methodBody.statements().get(i);
				int beforeLine = _cu.getLineNumber(astNode.getStartPosition());
				int afterline = _cu.getLineNumber(astNode.getStartPosition() + astNode.getLength());
				if(_mutantLineNumber >= beforeLine && _mutantLineNumber <= afterline){
					blockStatement.addAll(process(astNode, keyValue));
				}else{
					blockStatement.add(ASTNode.copySubtree(ast, astNode));
				}
			}
			
		} else if(needInstrument) {
			AST ast = AST.newAST(AST.JLS8);
			for (; i < methodBody.statements().size(); i++) {
				ASTNode astNode = (ASTNode) methodBody.statements().get(i);
				blockStatement.add(ASTNode.copySubtree(ast, astNode));
			}
		} else{
			return true;
		}
		methodBody.statements().clear();
		for (ASTNode statement : blockStatement) {
			methodBody.statements().add(ASTNode.copySubtree(methodBody.getAST(), statement));
		}

		return true;
	}
	
	private List<ASTNode> process(ASTNode statement, String keyValue) {

		List<ASTNode> result = new ArrayList<>();
		
		int startLine = _cu.getLineNumber(statement.getStartPosition());
		int endLine = _cu.getLineNumber(statement.getStartPosition() + statement.getLength());
		if(startLine > _mutantLineNumber || endLine < _mutantLineNumber){
			result.add(ASTNode.copySubtree(AST.newAST(AST.JLS8), statement));
			return result;
		}

		if (statement instanceof IfStatement) {
			
			IfStatement ifStatement = (IfStatement) statement;
			startLine = _cu.getLineNumber(ifStatement.getExpression().getStartPosition());
			if(startLine == _mutantLineNumber){
				String mutantFlagMessage = Constant.INSTRUMENT_FLAG + Constant.INSTRUMENT_MUTANT + "#" + keyValue;
				ASTNode inserted = GenStatement.genASTNode(mutantFlagMessage, 0);
				result.add(inserted);
				result.add(ASTNode.copySubtree(AST.newAST(AST.JLS8), statement));
				return result;
			}
			

			Statement thenBody = ifStatement.getThenStatement();
			
			if(thenBody != null){
				startLine = _cu.getLineNumber(thenBody.getStartPosition());
				endLine = _cu.getLineNumber(thenBody.getStartPosition() + thenBody.getLength());
				if(startLine <= _mutantLineNumber && _mutantLineNumber <= endLine){
					Block thenBlock = null;
					if (thenBody instanceof Block) {
						thenBlock = (Block) thenBody;
					} else {
						AST ast = AST.newAST(AST.JLS8);
						thenBlock = ast.newBlock();
						thenBlock.statements().add(ASTNode.copySubtree(thenBlock.getAST(), thenBody));
					}

					Block newThenBlock = processBlock(thenBlock, keyValue);
					ifStatement.setThenStatement((Statement) ASTNode.copySubtree(ifStatement.getAST(), newThenBlock));
				} 
			}
			
			Statement elseBody = ifStatement.getElseStatement();
			if (elseBody != null) {
				startLine = _cu.getLineNumber(elseBody.getStartPosition());
				endLine = _cu.getLineNumber(elseBody.getStartPosition() + elseBody.getLength());
				if(startLine <= _mutantLineNumber && _mutantLineNumber <= endLine){
					Block elseBlock = null;
					if (elseBody instanceof Block) {
						elseBlock = (Block) elseBody;
					} else {
						AST ast = AST.newAST(AST.JLS8);
						elseBlock = ast.newBlock();
						elseBlock.statements().add(ASTNode.copySubtree(elseBlock.getAST(), elseBody));
					}
					Block newElseBlock = processBlock(elseBlock, keyValue);
					ifStatement.setElseStatement((Statement) ASTNode.copySubtree(ifStatement.getAST(), newElseBlock));
				}
			}
			result.add(ifStatement);
		} else if (statement instanceof WhileStatement) {

			WhileStatement whileStatement = (WhileStatement) statement;
			
			int lineNumber = _cu.getLineNumber(whileStatement.getExpression().getStartPosition());
			if(lineNumber == _mutantLineNumber){
				String mutantFlagMessage = Constant.INSTRUMENT_FLAG + Constant.INSTRUMENT_MUTANT + "#" + keyValue;
				ASTNode inserted = GenStatement.genASTNode(mutantFlagMessage, 0);
				result.add(inserted);
				result.add(ASTNode.copySubtree(AST.newAST(AST.JLS8), statement));
				return result;
			}
			
			Statement whilebody = whileStatement.getBody();
			
			if (whilebody != null) {
				startLine = _cu.getLineNumber(whilebody.getStartPosition());
				endLine = _cu.getLineNumber(whilebody.getStartPosition() + whilebody.getLength());
				if(startLine <= _mutantLineNumber && _mutantLineNumber <= endLine){
					Block whileBlock = null;
					if (whilebody instanceof Block) {
						whileBlock = (Block) whilebody;
					} else {
						AST ast = AST.newAST(AST.JLS8);
						whileBlock = ast.newBlock();
						whileBlock.statements().add(ASTNode.copySubtree(whileBlock.getAST(), whilebody));
					}
	
					Block newWhileBlock = processBlock(whileBlock, keyValue);
					whileStatement.setBody((Statement) ASTNode.copySubtree(whileStatement.getAST(), newWhileBlock));
				}
			}
			result.add(whileStatement);
		} else if (statement instanceof ForStatement) {

			ForStatement forStatement = (ForStatement) statement;
			
			int lineNumber = _cu.getLineNumber(forStatement.getExpression().getStartPosition());
			if(lineNumber == _mutantLineNumber){
				String mutantFlagMessage = Constant.INSTRUMENT_FLAG + Constant.INSTRUMENT_MUTANT + "#" + keyValue;
				ASTNode inserted = GenStatement.genASTNode(mutantFlagMessage, 0);
				result.add(inserted);
				result.add(ASTNode.copySubtree(AST.newAST(AST.JLS8), statement));
				return result;
			}
			
			Statement forBody = forStatement.getBody();
			
			
			if (forBody != null) {
				startLine = _cu.getLineNumber(forBody.getStartPosition());
				endLine = _cu.getLineNumber(forBody.getStartPosition() + forBody.getLength());
				if(startLine <= _mutantLineNumber && _mutantLineNumber <= endLine){
					Block forBlock = null;
					if (forBody instanceof Block) {
						forBlock = (Block) forBody;
					} else {
						AST ast = AST.newAST(AST.JLS8);
						forBlock = ast.newBlock();
						forBlock.statements().add(ASTNode.copySubtree(forBlock.getAST(), forBody));
					}
	
					Block newForBlock = processBlock(forBlock, keyValue);
					forStatement.setBody((Statement) ASTNode.copySubtree(forStatement.getAST(), newForBlock));
				}
			}

			result.add(forStatement);
		} else if (statement instanceof DoStatement) {

			DoStatement doStatement = (DoStatement) statement;
			
			int lineNumber = _cu.getLineNumber(doStatement.getExpression().getStartPosition());
			if(lineNumber == _mutantLineNumber){
				result.add(ASTNode.copySubtree(AST.newAST(AST.JLS8), statement));
				String mutantFlagMessage = Constant.INSTRUMENT_FLAG + Constant.INSTRUMENT_MUTANT + "#" + keyValue;
				ASTNode inserted = GenStatement.genASTNode(mutantFlagMessage, 0);
				result.add(inserted);
				return result;
			}
			
			Statement doBody = doStatement.getBody();
			if (doBody != null) {
				startLine = _cu.getLineNumber(doBody.getStartPosition());
				endLine = _cu.getLineNumber(doBody.getStartPosition() + doBody.getLength());
				if(startLine <= _mutantLineNumber && _mutantLineNumber <= endLine){
					Block doBlock = null;
					if (doBody instanceof Block) {
						doBlock = (Block) doBody;
					} else {
						AST ast = AST.newAST(AST.JLS8);
						doBlock = ast.newBlock();
						doBlock.statements().add(ASTNode.copySubtree(doBlock.getAST(), doBody));
					}
	
					Block newDoBlock = processBlock(doBlock, keyValue);
					doStatement.setBody((Statement) ASTNode.copySubtree(doStatement.getAST(), newDoBlock));
				}
			}

			result.add(doStatement);
		} else if (statement instanceof Block) {
			Block block = (Block) statement;
			Block newBlock = processBlock(block, keyValue);
			result.add(newBlock);
		} else if (statement instanceof EnhancedForStatement) {

			EnhancedForStatement enhancedForStatement = (EnhancedForStatement) statement;
			
			int lineNumber = _cu.getLineNumber(enhancedForStatement.getExpression().getStartPosition());
			if(lineNumber == _mutantLineNumber){
				String mutantFlagMessage = Constant.INSTRUMENT_FLAG + Constant.INSTRUMENT_MUTANT + "#" + keyValue;
				ASTNode inserted = GenStatement.genASTNode(mutantFlagMessage, 0);
				result.add(inserted);
				result.add(ASTNode.copySubtree(AST.newAST(AST.JLS8), statement));
				return result;
			}
			
			Statement enhancedBody = enhancedForStatement.getBody();
			if (enhancedBody != null) {
				
				startLine = _cu.getLineNumber(enhancedBody.getStartPosition());
				endLine = _cu.getLineNumber(enhancedBody.getStartPosition() + enhancedBody.getLength());
				if(startLine <= _mutantLineNumber && _mutantLineNumber <= endLine){
				
					Block enhancedBlock = null;
					if (enhancedBody instanceof Block) {
						enhancedBlock = (Block) enhancedBody;
					} else {
						AST ast = AST.newAST(AST.JLS8);
						enhancedBlock = ast.newBlock();
						enhancedBlock.statements().add(ASTNode.copySubtree(enhancedBlock.getAST(), enhancedBody));
					}
					Block newEnhancedBlock = processBlock(enhancedBlock, keyValue);
					enhancedForStatement
							.setBody((Statement) ASTNode.copySubtree(enhancedForStatement.getAST(), newEnhancedBlock));
				}
			}

			result.add(enhancedForStatement);
		} else if (statement instanceof SwitchStatement) {

			SwitchStatement switchStatement = (SwitchStatement) statement;
			
			int lineNumber = _cu.getLineNumber(switchStatement.getExpression().getStartPosition());
			if(lineNumber == _mutantLineNumber){
				String mutantFlagMessage = Constant.INSTRUMENT_FLAG + Constant.INSTRUMENT_MUTANT + "#" + keyValue;
				ASTNode inserted = GenStatement.genASTNode(mutantFlagMessage, 0);
				result.add(inserted);
				result.add(ASTNode.copySubtree(AST.newAST(AST.JLS8), statement));
				return result;
			}
			
			List<ASTNode> statements = new ArrayList<>();
			AST ast = AST.newAST(AST.JLS8);
			for (Object object : switchStatement.statements()) {
				ASTNode astNode = (ASTNode) object;
				statements.add(ASTNode.copySubtree(ast, astNode));
			}

			switchStatement.statements().clear();

			for (ASTNode astNode : statements) {
				for(ASTNode node : process(astNode, keyValue)){
					switchStatement.statements().add(ASTNode.copySubtree(switchStatement.getAST(), node));
				}
			}

			result.add(switchStatement);
		} else if (statement instanceof TryStatement) {

			TryStatement tryStatement = (TryStatement) statement;

			Block tryBlock = tryStatement.getBody();
			
			if (tryBlock != null) {
				startLine = _cu.getLineNumber(tryBlock.getStartPosition());
				endLine = _cu.getLineNumber(tryBlock.getStartPosition() + tryBlock.getLength());
				if(startLine <= _mutantLineNumber && _mutantLineNumber <= endLine){
					Block newTryBlock = processBlock(tryBlock, keyValue);
					tryStatement.setBody((Block) ASTNode.copySubtree(tryStatement.getAST(), newTryBlock));
				}
			}

			List catchList = tryStatement.catchClauses();
			if(catchList != null){
				for (Object object : catchList) {
					if (object instanceof CatchClause) {
						CatchClause catchClause = (CatchClause) object;
						Block catchBlock = catchClause.getBody();
						Block newCatchBlock = processBlock(catchBlock, keyValue);
						catchClause.setBody((Block) ASTNode.copySubtree(catchClause.getAST(), newCatchBlock));
					}
				}
			}

			Block finallyBlock = tryStatement.getFinally();
			if (finallyBlock != null) {
				startLine = _cu.getLineNumber(finallyBlock.getStartPosition());
				endLine = _cu.getLineNumber(finallyBlock.getStartPosition() + finallyBlock.getLength());
				if(startLine <= _mutantLineNumber && _mutantLineNumber <= endLine){
					Block newFinallyBlock = processBlock(finallyBlock, keyValue);
					tryStatement.setFinally((Block) ASTNode.copySubtree(tryStatement.getAST(), newFinallyBlock));
				}
			}

			result.add(tryStatement);
		} else {
			int lineNumber = _cu.getLineNumber(statement.getStartPosition());
			String mutantFlagMessage = Constant.INSTRUMENT_FLAG + Constant.INSTRUMENT_MUTANT + "#" + keyValue;
			
			Statement copy = (Statement) ASTNode.copySubtree(AST.newAST(AST.JLS8), statement);
			Statement insert = GenStatement.genASTNode(mutantFlagMessage, lineNumber);

			if (statement instanceof ConstructorInvocation) {
				result.add(copy);
				result.add(insert);
			} else if (statement instanceof ContinueStatement || statement instanceof BreakStatement
					|| statement instanceof ReturnStatement || statement instanceof ThrowStatement
					|| statement instanceof AssertStatement || statement instanceof ExpressionStatement
					|| statement instanceof ConstructorInvocation
					|| statement instanceof VariableDeclarationStatement) {
				result.add(insert);
				result.add(copy);

			} else if (statement instanceof LabeledStatement) {
				result.add(insert);
				result.add(copy);
			} else if (statement instanceof SynchronizedStatement) {
				result.add(insert);
				result.add(copy);
			} else {
				result.add(insert);
				result.add(copy);
			}
		}

		return result;
	}

	private Block processBlock(Block block, String keyValue) {
		Block newBlock = AST.newAST(AST.JLS8).newBlock();
		if (block == null) {
			return newBlock;
		}
		for (Object object : block.statements()) {
			ASTNode astNode = (ASTNode) object;
			List<ASTNode> newStatements = process(astNode, keyValue);
			for (ASTNode newStatement : newStatements) {
				newBlock.statements().add(ASTNode.copySubtree(newBlock.getAST(), newStatement));
			}
		}
		return newBlock;
	}
	
	public static void main(String[] args) {
		String file = "/Users/Jiajun/Code/Java/defects4j/lang_1_buggy/src/main/java/org/apache/commons/lang3/math/NumberUtils.java";
		CompilationUnit unit = JavaFile.genASTFromSource(JavaFile.readFileToString(file), ASTParser.K_COMPILATION_UNIT);
		String methodString = "org.apache.commons.lang3.math.NumberUtils#float#toFloat#?,String,float";
		Set<Method> methods = new HashSet<>();
		Method method1 = new Method(Identifier.getIdentifier(methodString));
		methodString = "org.apache.commons.lang3.math.NumberUtils#double#toDouble#?,String";
		Method method2 = new Method(Identifier.getIdentifier(methodString));
		methods.add(method1);
		methods.add(method2);
		DynamicRuntimeInfo dynamicRuntimeInfo = new DynamicRuntimeInfo("lang", 1);
		MutantFlagInstrumentVisitor mutantFlagInstrumentVisitor = new MutantFlagInstrumentVisitor(392, dynamicRuntimeInfo);
		mutantFlagInstrumentVisitor.setAllMethods(methods);
		unit.accept(mutantFlagInstrumentVisitor);
		JavaFile.writeStringToFile("/Users/Jiajun/Code/Java/defects4j/lang_1_buggy/src/main/java/org/apache/commons/lang3/math/NumberUtils2.java", unit.toString());
	}


}

