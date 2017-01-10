package localization.core.path;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import localization.common.config.Constant;
import localization.common.config.DynamicRuntimeInfo;
import localization.common.config.Identifier;
import localization.common.config.InfoBuilder;
import localization.common.java.ExecutedMethod;
import localization.common.java.JavaFile;
import localization.common.java.Method;
import localization.common.java.Mutant;
import localization.common.java.TestMethod;
import localization.common.util.ExecuteCommand;
import localization.common.util.LevelLogger;
import localization.common.util.Pair;
import localization.instrument.Instrument;
import localization.instrument.visitor.DeInstrumentVisitor;
import localization.instrument.visitor.MethodInstrumentVisitor;
import localization.instrument.visitor.MutantFlagInstrumentVisitor;
import localization.instrument.visitor.StateCollectInstrumentVisitor;
import localization.instrument.visitor.StatementInstrumentVisitor;
import localization.mutation.MutantGenerator;

public class Collector {
	private DynamicRuntimeInfo _dynamicRuntimeInfo;
	private String _testSRCPath;
	private String _sourceSRCPath;
	
	public Collector(DynamicRuntimeInfo dynamicRuntimeInfo) {
		_dynamicRuntimeInfo = dynamicRuntimeInfo;
		_testSRCPath = InfoBuilder.buildTestSRCPath(_dynamicRuntimeInfo, true);
		_sourceSRCPath = InfoBuilder.buildSourceSRCPath(_dynamicRuntimeInfo, true);
	}
	
	private void writeTestMethodIntoFile(List<TestMethod> testMethods, String filePath){
		StringBuffer stringBuffer = new StringBuffer("");
		String newLine = System.getProperty("line.separator");
		for (TestMethod testMethod : testMethods) {
			stringBuffer.append("@" + testMethod.toString() + newLine);
			for (ExecutedMethod executedMethod : testMethod.getExecutionPath()) {
				stringBuffer.append(executedMethod.toString());
				stringBuffer.append(newLine);
			}
			stringBuffer.append(newLine);
		}
		JavaFile.writeStringToFile(Constant.STR_FAILED_TEST_FILE, stringBuffer.toString(), false);
	}
	
	private void collectStateIntoFile(String sourceFilePath, String targetFileContainer, int testStatement, int sampleCycle) {
		File file = new File(sourceFilePath);
		Map<Integer, StringBuffer> allStates = new HashMap<>();
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("[junit] ")) {
					continue;
				}
				line = line.substring("[junit] ".length());
				if (line.startsWith("[INST]T")) {
					testStatement--;
				}
				if (testStatement <= 0) {
					break;
				}
			}
			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("[junit] ")) {
					continue;
				}
				line = line.substring("[junit] ".length());
				if (line.startsWith("[INST]M>>START")) {
					sampleCycle--;
				}
				if (sampleCycle <= 0) {
					break;
				}
			}
			boolean isNewLine = true;
			String newLine = System.getProperty("line.separator");
			StringBuffer lastStringBuffer = new StringBuffer();
			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("[junit] ")) {
					continue;
				}
				line = line.substring("[junit] ".length());
				if (line.startsWith("[INST]M>>START")) {
					if(isNewLine){
						lastStringBuffer.append(newLine);
						isNewLine = false;
					}
					continue;
				}
				if (line.startsWith("[INST]T")) {
					continue;
				}
				if (!line.startsWith("[INST]M")) {
					break;
				}
				String[] info = line.split("#");
				String value = "";
				if (info.length < 3) {
					LevelLogger.error("collectStateIntoFile pass info error : " + line);
					continue;
				}
				if(info.length == 4){
					value = info[3];
				}
				isNewLine = true;
				Integer integer = Integer.valueOf(info[1]);
				if(allStates.containsKey(integer)){
					StringBuffer sBuffer = allStates.get(integer);
					sBuffer.append(info[2] + ":" + value + " ");
					lastStringBuffer = sBuffer;
				} else {
					StringBuffer sBuffer = new StringBuffer();
					sBuffer.append(info[2] + ":" + value + " ");
					allStates.put(integer, sBuffer);
					lastStringBuffer = sBuffer;
				}
			}
			bufferedReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		for(Entry<Integer, StringBuffer> entry : allStates.entrySet()){
			StringBuffer sBuffer = entry.getValue();
			if(sBuffer == null || sBuffer.toString().trim().length() < 1){
				continue;
			}
			String methodString = Identifier.getMessage(entry.getKey());
			String filePath = targetFileContainer + Constant.PATH_SEPARATOR + methodString;
			String message = sBuffer.toString().trim() + System.getProperty("line.separator");
			JavaFile.writeStringToFile(filePath, message, true);
		}
	}
	
	private Set<Integer> collectPositiveStateIntoFile(String sourceFilePath, String targetFileContainer) {
		File file = new File(sourceFilePath);
		Map<Integer, StringBuffer> allStates = new HashMap<>();
		Set<Integer> collectStateMethod = new HashSet<>();
		Integer currentTest = 0;
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(file));
			String line = null;
			boolean isNewLine = true;
			String newLine = System.getProperty("line.separator");
			StringBuffer lastStringBuffer = new StringBuffer();
			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("[junit] ")) {
					continue;
				}
				line = line.substring("[junit] ".length());
				if (line.startsWith("[INST]M>>START")) {
					if(isNewLine){
						lastStringBuffer.append(newLine);
						isNewLine = false;
					}
					continue;
				}
				if (line.startsWith("[INST]T")) {
					String[] info = line.split("#");
					if(info.length >= 2){
						currentTest = Integer.valueOf(info[1]);
					}
					continue;
				}
				if (!line.startsWith("[INST]M")) {
					continue;
				}
				String[] info = line.split("#");
				String value = "";
				if (info.length < 3) {
					LevelLogger.error("collectStateIntoFile pass info error : " + line);
					continue;
				}
				if(info.length == 4){
					value = info[3];
				}
				isNewLine = true;
				collectStateMethod.add(currentTest);
				Integer integer = Integer.valueOf(info[1]);
				if(allStates.containsKey(integer)){
					StringBuffer sBuffer = allStates.get(integer);
					sBuffer.append(info[2] + ":" + value+ " ");
					lastStringBuffer = sBuffer;
				} else {
					StringBuffer sBuffer = new StringBuffer();
					sBuffer.append(info[2] + ":" + value + " ");
					allStates.put(integer, sBuffer);
					lastStringBuffer = sBuffer;
				}
			}
			bufferedReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		for(Entry<Integer, StringBuffer> entry : allStates.entrySet()){
			StringBuffer sBuffer = entry.getValue();
			if(sBuffer == null || sBuffer.toString().trim().length() < 1){
				continue;
			}
			String methodString = Identifier.getMessage(entry.getKey());
			String filePath = targetFileContainer + Constant.PATH_SEPARATOR + methodString;
			String message = sBuffer.toString().trim() + System.getProperty("line.separator");
			JavaFile.writeStringToFile(filePath, message, true);
		}
		return collectStateMethod;
	}
	
	private void collectAllFailedTestState(List<TestMethod> failedTestMethods){
		Instrument.execute(_testSRCPath, new StatementInstrumentVisitor(Constant.INSTRUMENT_TEST));
		
		Set<Method> allExecutedMethods = new HashSet<>();
		Set<Integer> allMethodID = new HashSet<>();
		for(TestMethod testMethod : failedTestMethods){
			for(ExecutedMethod executedMethod : testMethod.getExecutionPath()){
				allMethodID.add(executedMethod.getMethodID());
			}
		}
		for(Integer integer : allMethodID){
			allExecutedMethods.add(new Method(integer));
		}
		StateCollectInstrumentVisitor stateCollectInstrumentVisitor = new StateCollectInstrumentVisitor(Constant.INSTRUMENT_SOURCE, _dynamicRuntimeInfo);
		stateCollectInstrumentVisitor.setAllMethods(allExecutedMethods);
		Instrument.execute(_sourceSRCPath, stateCollectInstrumentVisitor);
		
		for(TestMethod testMethod : failedTestMethods){
			String methodString = Identifier.getMessage(testMethod.getMethodID());
			String[] methodInfo = methodString.split("#");
			if(methodInfo.length < 4){
				LevelLogger.error("collectAllFailedTestState parse test method string error : " + methodString);
				continue;
			}
			ExecuteCommand.executeDefects4JTest(InfoBuilder.buildDefects4JTestCommand(_dynamicRuntimeInfo, methodInfo[0], methodInfo[2]), Constant.STR_TMP_OUTPUT_FILE);
			String testDataFile = Constant.STR_FAILED_DATA_COLLECT_PATH + Constant.PATH_SEPARATOR + methodInfo[0] + "_" + methodInfo[2];
			collectStateIntoFile(Constant.STR_TMP_OUTPUT_FILE, testDataFile, testMethod.getTestStatementNumber(), 0);
		}
		Instrument.execute(_testSRCPath, new DeInstrumentVisitor());
		Instrument.execute(_sourceSRCPath, new DeInstrumentVisitor());
	}
	
	
	private void backupFailedTest(List<TestMethod> allFailedTestMethods){
		Set<Method> allTestMethod = new HashSet<>();
		allTestMethod.addAll(allFailedTestMethods);
		RomoveMethodBodyVisitor removeMethodBodyVisitor = new RomoveMethodBodyVisitor(allTestMethod);
		for(TestMethod testMethod : allFailedTestMethods){
			String testMethodString = Identifier.getMessage(testMethod.getMethodID());
			String[] testMethodInfo = testMethodString.split("#");
			if(testMethodInfo.length < 4){
				LevelLogger.error("backupFailedTest parse test method info error : " + testMethodString);
				continue;
			}
			
			String testFile = _testSRCPath + Constant.PATH_SEPARATOR + testMethodInfo[0].replace(".", Constant.PATH_SEPARATOR) + ".java";
			CompilationUnit unit = JavaFile.genASTFromSource(JavaFile.readFileToString(testFile), ASTParser.K_COMPILATION_UNIT);
			unit.accept(removeMethodBodyVisitor);
			ExecuteCommand.moveFile(testFile, testFile + "bak");
			JavaFile.writeStringToFile(testFile, unit.toString());
		}
	}
	
	private void recoverFailedTest(List<TestMethod> allFailedTestMethods){
		for(TestMethod testMethod : allFailedTestMethods){
			String testMethodString = Identifier.getMessage(testMethod.getMethodID());
			String[] testMethodInfo = testMethodString.split("#");
			if(testMethodInfo.length < 4){
				LevelLogger.error("backupFailedTest parse test method info error : " + testMethodString);
				continue;
			}
			
			String testFile = _testSRCPath + Constant.PATH_SEPARATOR + testMethodInfo[0].replace(".", Constant.PATH_SEPARATOR) + ".java";
			File file = new File(testFile + "bak");
			if(!file.exists()){
				LevelLogger.error("recoverFailedTest backup file not found!");
				continue;
			}
			String sourceContent = JavaFile.readFileToString(file);
			JavaFile.writeStringToFile(testFile, sourceContent);
			file.delete();
		}
	}
	
	private Set<Integer> collectClazzPath(String sourceFile){
		
		File file = new File(sourceFile);
		Set<Integer> allMethodsID = new HashSet<>();
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("[junit] [INST]M")) {
					continue;
				}
				String[] info = line.split("#");
				if (info.length < 2) {
					LevelLogger.error("collectStateIntoFile pass info error : " + line);
					continue;
				}
				Integer integer = Integer.valueOf(info[1]);
				allMethodsID.add(integer);
			}
			bufferedReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return allMethodsID;
	}
	
	private List<Pair<String, Set<Integer>>> collectAllMutateFile(Set<Integer> passedTest){
		List<Pair<String, Set<Integer>>> allPassedTestWithFullClazzPath = new ArrayList<>();
		int totaltest = passedTest.size();
		for(Integer testID : passedTest){
			LevelLogger.info("ramaining " + totaltest -- + " passed test method need to collect runing methods.");
			String methodString = Identifier.getMessage(testID);
			String[] methodInfo = methodString.split("#");
			if(methodInfo.length < 4){
				LevelLogger.error("collectAllMutateFile parse method id failed : " + methodString);
				continue;
			}
			String test = methodInfo[0] + "::" + methodInfo[2];
			ExecuteCommand.executeDefects4JTest(InfoBuilder.buildDefects4JTestCommand(_dynamicRuntimeInfo, test), Constant.STR_TMP_OUTPUT_FILE);
			Set<Integer> allClazzPath = collectClazzPath(Constant.STR_TMP_OUTPUT_FILE);
			allPassedTestWithFullClazzPath.add(new Pair<String, Set<Integer>>(test, allClazzPath));
		}
		return allPassedTestWithFullClazzPath;
	}
	
	private void collectAllPositiveAndNegativeState(){
		String failedTestFilePath = Constant.STR_FAILED_DATA_COLLECT_PATH;
		File root = new File(failedTestFilePath);
		if(!root.exists()){
			LevelLogger.error("There is no failed test state data.");
			return;
		}
		Set<Method> collectMethods = new HashSet<>();
		Set<Integer> allMethodIDs = new HashSet<>();
		for(File failedTestFolder : root.listFiles()){
			if(!failedTestFolder.isDirectory()){
				LevelLogger.error("collectAllPassedTestState failed test root path is not a directory : " + failedTestFolder.getAbsolutePath());
				continue;
			}
			for(File watchMethod : failedTestFolder.listFiles()){
				String watchName = watchMethod.getName();
				int id = Identifier.getIdentifier(watchName);
				allMethodIDs.add(id);
			}
		}
		for(Integer integer : allMethodIDs){
			collectMethods.add(new Method(integer));
		}
		
		StateCollectInstrumentVisitor stateCollectInstrumentVisitor = new StateCollectInstrumentVisitor(Constant.INSTRUMENT_SOURCE, _dynamicRuntimeInfo);
		stateCollectInstrumentVisitor.setAllMethods(collectMethods);
		Instrument.execute(_sourceSRCPath, stateCollectInstrumentVisitor);
		Instrument.execute(_testSRCPath, new MethodInstrumentVisitor(Constant.INSTRUMENT_TEST));
		
		LevelLogger.info("collecting positive states ...");
		//begin to collect positive state
		ExecuteCommand.executeDefects4JTest(InfoBuilder.buildDefects4JTestCommand(_dynamicRuntimeInfo), Constant.STR_TMP_OUTPUT_FILE);
		Set<Integer> collectStateMethods = collectPositiveStateIntoFile(Constant.STR_TMP_OUTPUT_FILE, Constant.STR_POSITIVE_DATA_COLLECT_PATH);
		Instrument.execute(_sourceSRCPath, new DeInstrumentVisitor());
		//end collect positive state
		
		LevelLogger.info("collecting all run methods ...");
		//collect all method run by passed tests that can run into the collect method
		Instrument.execute(_sourceSRCPath, new MethodInstrumentVisitor());
		List<Pair<String, Set<Integer>>> allPassedTestWithMethod = collectAllMutateFile(collectStateMethods);
		Instrument.execute(_sourceSRCPath, new DeInstrumentVisitor());
		
		Set<Integer> allMethodsID = new HashSet<>();
		List<Pair<String, Set<String>>> allPassedTestWithClazzPath = new ArrayList<>();
		for(Pair<String, Set<Integer>> pair : allPassedTestWithMethod){
			allMethodsID.addAll(pair.getSecond());
			Set<String> allClazzPath = new HashSet<>();
			for(Integer methodID : pair.getSecond()){
				String methodString = Identifier.getMessage(methodID);
				String[] methodInfo = methodString.split("#");
				allClazzPath.add(methodInfo[0] + "@" + methodInfo[2]);
			}
			allPassedTestWithClazzPath.add(new Pair<String, Set<String>>(pair.getFirst(), allClazzPath));
		}
		
		//begin to collect negative state
		MutantGenerator.configuration();
		//only generate useful mutants
		Map<String, List<Mutant>> result = MutantGenerator.getMutantsByMethod(_dynamicRuntimeInfo, allMethodsID);
		
		StatementInstrumentVisitor statementInstrumentVisitor = new StatementInstrumentVisitor(Constant.INSTRUMENT_TEST);
		Instrument.execute(_testSRCPath, statementInstrumentVisitor);
		Instrument.execute(_sourceSRCPath, stateCollectInstrumentVisitor);
		
		int totalMutantClazz = result.size();
		String mutantPath = InfoBuilder.buildSingleProjectPath(_dynamicRuntimeInfo) + Constant.MUTANT_DIR;
		for(Entry<String, List<Mutant>> entry : result.entrySet()){
			LevelLogger.info("There are remain " + totalMutantClazz -- + " mutant clazz.");
			String clazzPathAndMethod = entry.getKey();
			String[] methodInfo = clazzPathAndMethod.split("@");
			if(methodInfo.length < 2){
				LevelLogger.error("mutant method parse error : " + clazzPathAndMethod);
				continue;
			}
			String mutantFullClazz = methodInfo[0];
			String originalFile = _sourceSRCPath + Constant.PATH_SEPARATOR + mutantFullClazz.replace(".", Constant.PATH_SEPARATOR) + ".java";
			//backup original file
			ExecuteCommand.moveFile(originalFile, originalFile+"bak");
			List<Mutant> mutants = entry.getValue();
			int totalMutants = mutants.size();
			for(Mutant mutant : mutants){
				LevelLogger.info("Mutants remain " + totalMutants --);
				String mutantFile = mutantPath + Constant.PATH_SEPARATOR + mutant.getReplaceSource();
				ExecuteCommand.copyFile(mutantFile, originalFile);
				int line = mutant.getStartLineNumber();
				MutantFlagInstrumentVisitor mutantFlagInstrumentVisitor = new MutantFlagInstrumentVisitor(line, _dynamicRuntimeInfo);
				mutantFlagInstrumentVisitor.setAllMethods(collectMethods);
				Instrument.execute(originalFile, mutantFlagInstrumentVisitor);
				int totalMethodCount = allPassedTestWithClazzPath.size();
				for(Pair<String, Set<String>> testMethod : allPassedTestWithClazzPath){
					LevelLogger.info("test remain " + totalMethodCount --);
					if(!testMethod.getSecond().contains(clazzPathAndMethod)){
						continue;
					}
					ExecuteCommand.executeDefects4JTest(InfoBuilder.buildDefects4JTestCommand(_dynamicRuntimeInfo, testMethod.getFirst()), Constant.STR_TMP_OUTPUT_FILE);
					collectNegativeStateIntoFile(Constant.STR_TMP_OUTPUT_FILE, Constant.STR_NEGATIVE_DATA_COLLECT_PATH);
				}
			}
			//recover original file
			ExecuteCommand.moveFile(originalFile + "bak", originalFile);
		}
		
		Instrument.execute(_testSRCPath, new DeInstrumentVisitor());
		Instrument.execute(_sourceSRCPath, new DeInstrumentVisitor());
	}
	
	private List<String> collectFailedTest(){
		ExecuteCommand.executeDefects4JTest(InfoBuilder.buildDefects4JTestCommand(_dynamicRuntimeInfo), Constant.STR_TMP_OUTPUT_FILE);
		return ExecutionPathBuider.findFailedTestFromFile(Constant.STR_TMP_OUTPUT_FILE);
	}
	
	private List<TestMethod> collectFailedTestTrace(List<String> allFailedTests){
		String testSRCPath = InfoBuilder.buildTestSRCPath(_dynamicRuntimeInfo, true);
		List<TestMethod> allFailedTestMethods = new ArrayList<>();
		for(String string : allFailedTests){
			String[] testInfo = string.split("::");
			if(testInfo.length < 2){
				LevelLogger.error("collect failed test method trace error : " + string);
				continue;
			}
			String methodString = testInfo[0] + "#void#" + testInfo[1] + "#?";
			int methodID = Identifier.getIdentifier(methodString);
			Method method = new Method(methodID);
			
			String testJavaFile = testSRCPath + "/" + testInfo[0].replace(".", "/") + ".java";
			StatementInstrumentVisitor statementInstrumentVisitor = new StatementInstrumentVisitor(Constant.INSTRUMENT_TEST);
			statementInstrumentVisitor.setMethod(method);
			Instrument.execute(testJavaFile, statementInstrumentVisitor);
			
			ExecuteCommand.executeDefects4JTest(InfoBuilder.buildDefects4JTestCommand(_dynamicRuntimeInfo, string), Constant.STR_TMP_OUTPUT_FILE);
			
			Instrument.execute(testJavaFile, new DeInstrumentVisitor(method));
			
			List<TestMethod> testMethods = ExecutionPathBuider.buildPathFromFile(_dynamicRuntimeInfo, Constant.STR_TMP_OUTPUT_FILE);
			
			if(testMethods != null && testMethods.size() > 0){
				int lastIndex = testMethods.size() - 1;
				int totalPathLength = testMethods.get(lastIndex).getExecutionPath().size();
				while(totalPathLength < Constant.TRACE_LENGTH_FOR_FAILED_TEST && lastIndex > 0){
					lastIndex --;
					totalPathLength += testMethods.get(lastIndex).getExecutionPath().size();
				}
				TestMethod toRecordTestMethod = testMethods.get(lastIndex);
				for(lastIndex ++; lastIndex < testMethods.size(); lastIndex ++){
					List<ExecutedMethod> path = testMethods.get(lastIndex).getExecutionPath();
					for(ExecutedMethod executedMethod : path){
						toRecordTestMethod.addExecutedMethod(executedMethod);
					}
				}
				allFailedTestMethods.add(toRecordTestMethod);
			}
		}
		return allFailedTestMethods;
	}
	
	
	private void collectNegativeStateIntoFile(String sourceFile, String targetFileContainer){
		File file = new File(sourceFile);
		Map<Integer, StringBuffer> allStates = new HashMap<>();
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(file));
			String line = null;
			boolean isNewLine = true;
			boolean canCollectData = false;
			String newLine = System.getProperty("line.separator");
			StringBuffer lastStringBuffer = new StringBuffer();
			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("[junit] [INST]")) {
					continue;
				}
				line = line.substring("[junit] [INST]".length());
				
				if(line.startsWith("U")){
					canCollectData = true;
					continue;
				}
				if(line.startsWith("T")){
					canCollectData = false;
					continue;
				}
				
				if (line.startsWith("M>>START")) {
					if(isNewLine){
						lastStringBuffer.append(newLine);
						isNewLine = false;
					}
					continue;
				}
				
				if(!canCollectData){
					continue;
				}
				
				String[] info = line.split("#");
				String value = "";
				if (info.length < 3) {
					LevelLogger.error("collectStateIntoFile pass info error : " + line);
					continue;
				}
				if(info.length == 4){
					value = info[3];
				}
				isNewLine = true;
				Integer integer = Integer.valueOf(info[1]);
				if(allStates.containsKey(integer)){
					StringBuffer sBuffer = allStates.get(integer);
					sBuffer.append(info[2] + ":" + value+ " ");
					lastStringBuffer = sBuffer;
				} else {
					StringBuffer sBuffer = new StringBuffer();
					sBuffer.append(info[2] + ":" + value + " ");
					allStates.put(integer, sBuffer);
					lastStringBuffer = sBuffer;
				}
			}
			bufferedReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		for(Entry<Integer, StringBuffer> entry : allStates.entrySet()){
			StringBuffer sBuffer = entry.getValue();
			if(sBuffer == null || sBuffer.toString().trim().length() < 1){
				continue;
			}
			String methodString = Identifier.getMessage(entry.getKey());
			String filePath = targetFileContainer + Constant.PATH_SEPARATOR + methodString;
			String message = sBuffer.toString().trim() + System.getProperty("line.separator");
			JavaFile.writeStringToFile(filePath, message, true);
		}
	}
	
	public void collect(){
		
		Instrument.execute(_sourceSRCPath, new DeInstrumentVisitor());
		Instrument.execute(_testSRCPath, new DeInstrumentVisitor());
		
		List<String> failedTestStrings = collectFailedTest();
		Instrument.execute(_sourceSRCPath, new MethodInstrumentVisitor(Constant.INSTRUMENT_SOURCE));
		LevelLogger.info("collecting failed test trace ...");
		List<TestMethod> failedTestMethods = collectFailedTestTrace(failedTestStrings);
		Instrument.execute(_sourceSRCPath, new DeInstrumentVisitor());
		writeTestMethodIntoFile(failedTestMethods, Constant.STR_FAILED_TEST_FILE);
		LevelLogger.info("collecting failed test states ...");
		collectAllFailedTestState(failedTestMethods);
		
		backupFailedTest(failedTestMethods);
		collectAllPositiveAndNegativeState();
		recoverFailedTest(failedTestMethods);
	}
}
