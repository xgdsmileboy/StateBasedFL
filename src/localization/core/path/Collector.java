package localization.core.path;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
import localization.core.path.filter.Cluster;
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
	private String _projectPath;

	public Collector(DynamicRuntimeInfo dynamicRuntimeInfo) {
		_dynamicRuntimeInfo = dynamicRuntimeInfo;
		_testSRCPath = InfoBuilder.buildTestSRCPath(_dynamicRuntimeInfo, true);
		_sourceSRCPath = InfoBuilder.buildSourceSRCPath(_dynamicRuntimeInfo, true);
		_projectPath = InfoBuilder.buildSingleProjectPath(dynamicRuntimeInfo);
	}

	/**
	 * write test method into file with format as :
	 * 
	 * @testmethod1 executed method 1 executed method 2
	 * 
	 * @testmethod2 executed method 1 executed method 2
	 * 
	 * @param testMethods:
	 *            all test methods with their execution traces that need to be
	 *            written into file
	 * @param filePath:
	 *            the target file path
	 */
	private void writeTestMethodIntoFile(List<TestMethod> testMethods, String filePath) {
		StringBuffer stringBuffer = new StringBuffer("");
		String newLine = System.getProperty("line.separator");
		for (TestMethod testMethod : testMethods) {
			stringBuffer.append("@" + testMethod.toString() + newLine);
			// write all executed methods into file for each test method
			for (ExecutedMethod executedMethod : testMethod.getExecutionPath()) {
				stringBuffer.append(executedMethod.toString());
				stringBuffer.append(newLine);
			}
			stringBuffer.append(newLine);
		}
		JavaFile.writeStringToFile(Constant.STR_FAILED_TEST_FILE, stringBuffer.toString(), false);
	}

	/**
	 * collect all states for one failed test and write them into the given
	 * folder
	 * 
	 * @param sourceFilePath
	 *            : path for file that contains the states
	 * @param targetFileContainer
	 *            : the container for files of states
	 * @param testStatement
	 *            : which test statement to start collecting states
	 * @param sampleCycle
	 *            : which entry to start collecting states for a method
	 */
	private void collectFailedTestStateIntoFile(String sourceFilePath, String targetFileContainer, int testStatement,
			int sampleCycle) {
		File file = new File(sourceFilePath);
		if (!file.exists()) {
			return;
		}
		Map<Integer, StringBuffer> allStates = new HashMap<>();
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("[INST]T")) {
					testStatement--;
				}
				if (testStatement <= 0) {
					break;
				}
			}
			if (sampleCycle > 0) {
				while ((line = bufferedReader.readLine()) != null) {
					line = line.trim();
					if (line.startsWith("[INST]M>>START")) {
						sampleCycle--;
					}
					if (sampleCycle <= 0) {
						break;
					}
				}
			}
			String newLine = System.getProperty("line.separator");
			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("[INST]M>>START")) {
					continue;
				}
				if (line.startsWith("[INST]M>>END")) {
					String[] guardStrings = line.split("#");
					if (guardStrings.length < 3) {
						LevelLogger.error("#collectFailedTestStateIntoFile Parse End info error : " + line);
						System.exit(0);
					}
					int id = Integer.parseInt(guardStrings[1]);
					if (allStates.containsKey(id)) {
						String message = allStates.get(id).toString().trim();
						// message += newLine;
						if (message.length() > 0) {
							message += newLine;
							message += newLine;
							String methodString = Identifier.getMessage(id);
							String filePath = targetFileContainer + Constant.PATH_SEPARATOR + methodString;
							JavaFile.writeStringToFile(filePath, message, true);
							// message = "";
						}
						StringBuffer stringBuffer = new StringBuffer();
						// stringBuffer.append(message);
						allStates.put(id, stringBuffer);
					}
					continue;
				}
				if (!line.startsWith("[INST]M")) {
					continue;
				}
				String[] info = line.split("#");
				String value = "";
				if (info.length < 2) {
					LevelLogger.error("collectStateIntoFile pass info error : " + line);
					continue;
				}
				if (info.length == 3) {
					value = info[2];
				} else {
					// [INST]M#5816#{[@2@<null>@2@<null>@2@<null>@2@<null>@2@<null>]@1@0@1@8}
					value = line.substring(info[0].length() + info[1].length() + 2);
				}

				Integer integer = Integer.valueOf(info[1]);
				if (allStates.containsKey(integer)) {
					StringBuffer sBuffer = allStates.get(integer);
					// sBuffer.append(info[2] + ":" + value + "\t");
					sBuffer.append(value);
					sBuffer.append(newLine);
				} else {
					StringBuffer sBuffer = new StringBuffer();
					// sBuffer.append(info[2] + ":" + value + "\t");
					sBuffer.append(value);
					sBuffer.append(newLine);
					allStates.put(integer, sBuffer);
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

		for (Entry<Integer, StringBuffer> entry : allStates.entrySet()) {
			StringBuffer sBuffer = entry.getValue();
			if (sBuffer == null || sBuffer.toString().trim().length() <= 0) {
				continue;
			}
			String methodString = Identifier.getMessage(entry.getKey());
			String filePath = targetFileContainer + Constant.PATH_SEPARATOR + methodString;
			String message = sBuffer.toString().trim() + System.getProperty("line.separator");
			JavaFile.writeStringToFile(filePath, message, true);
		}
	}

	/**
	 * refer to method "collectFailedTestStateIntoFile"
	 * 
	 * @param sourceFilePath
	 * @param targetFileContainer
	 * @return
	 */
	private Set<Integer> collectPositiveStateIntoFile(String sourceFilePath, String targetFileContainer) {
		File file = new File(sourceFilePath);
		Map<Integer, StringBuffer> allStates = new HashMap<>();
		Set<Integer> collectStateMethod = new HashSet<>();
		if (!file.exists()) {
			return collectStateMethod;
		}
		Integer currentTest = 0;
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(file));
			String line = null;
			// boolean isNewLine = true;
			String newLine = System.getProperty("line.separator");
			// StringBuffer lastStringBuffer = new StringBuffer();
			// Integer lastMethodID = 0;
			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("[INST]M>>END")) {
					String[] guardStrings = line.split("#");
					if (guardStrings.length < 3) {
						LevelLogger.error("#collectFailedTestStateIntoFile Parse End info error : " + line);
						System.exit(0);
					}
					int id = Integer.parseInt(guardStrings[1]);
					if (allStates.containsKey(id)) {
						String message = allStates.get(id).toString().trim();
						// message += newLine;
						if (message.length() > 0) {
							message += newLine;
							message += newLine;
							String methodString = Identifier.getMessage(id);
							String filePath = targetFileContainer + Constant.PATH_SEPARATOR + methodString;
							JavaFile.writeStringToFile(filePath, message, true);
							// message = "";
						}
						StringBuffer stringBuffer = new StringBuffer();
						// stringBuffer.append(message);
						allStates.put(id, stringBuffer);
					}
					continue;
				}
				if (line.startsWith("[INST]M>>START")) {
					continue;
				}
				// if (line.startsWith("[INST]M>>START")) {
				// if(isNewLine){
				// lastStringBuffer.append(newLine);
				// isNewLine = false;
				// //to avoid memory overload
				// if(lastStringBuffer.toString().length() > 1000){
				// String methodString = Identifier.getMessage(lastMethodID);
				// String filePath = targetFileContainer +
				// Constant.PATH_SEPARATOR + methodString;
				// String message = lastStringBuffer.toString() +
				// System.getProperty("line.separator");
				// JavaFile.writeStringToFile(filePath, message, true);
				// lastStringBuffer = new StringBuffer();
				// allStates.put(lastMethodID, lastStringBuffer);
				// }
				// }
				// continue;
				// }
				// isNewLine = true;
				if (line.startsWith("[INST]T")) {
					String[] info = line.split("#");
					if (info.length >= 2) {
						currentTest = Integer.valueOf(info[1]);
					}
					continue;
				}
				if (!line.startsWith("[INST]M")) {
					continue;
				}
				String[] info = line.split("#");
				String value = "";
				if (info.length < 2) {
					LevelLogger.error("collectStateIntoFile pass info error : " + line);
					continue;
				}
				if (info.length == 3) {
					value = info[2];
				} else {
					value = line.substring(info[0].length() + info[1].length() + 2);
				}

				collectStateMethod.add(currentTest);
				Integer integer = Integer.valueOf(info[1]);
				if (allStates.containsKey(integer)) {
					StringBuffer sBuffer = allStates.get(integer);
					// sBuffer.append(info[2] + ":" + value+ "\t");
					sBuffer.append(value);
					sBuffer.append(newLine);
					// lastStringBuffer = sBuffer;
					// lastMethodID = integer;
				} else {
					StringBuffer sBuffer = new StringBuffer();
					// sBuffer.append(info[2] + ":" + value + "\t");
					sBuffer.append(value);
					sBuffer.append(newLine);
					allStates.put(integer, sBuffer);
					// lastStringBuffer = sBuffer;
					// lastMethodID = integer;
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

		for (Entry<Integer, StringBuffer> entry : allStates.entrySet()) {
			StringBuffer sBuffer = entry.getValue();
			if (sBuffer == null || sBuffer.toString().trim().length() <= 0) {
				continue;
			}
			String methodString = Identifier.getMessage(entry.getKey());
			String filePath = targetFileContainer + Constant.PATH_SEPARATOR + methodString;
			String message = sBuffer.toString().trim() + System.getProperty("line.separator");
			JavaFile.writeStringToFile(filePath, message, true);
		}
		return collectStateMethod;
	}

	/**
	 * 
	 * @param failedTestMethods
	 * @return
	 */
	private Set<Method> collectAllFailedTestState(List<TestMethod> failedTestMethods) {
		Instrument.execute(_testSRCPath, new StatementInstrumentVisitor(Constant.INSTRUMENT_TEST));

		Set<Method> allExecutedMethods = new HashSet<>();
		Set<Integer> allMethodID = new HashSet<>();
		for (TestMethod testMethod : failedTestMethods) {
			for (ExecutedMethod executedMethod : testMethod.getExecutionPath()) {
				allMethodID.add(executedMethod.getMethodID());
			}
		}
		for (Integer integer : allMethodID) {
			allExecutedMethods.add(new Method(integer));
		}
		StateCollectInstrumentVisitor stateCollectInstrumentVisitor = new StateCollectInstrumentVisitor(
				Constant.INSTRUMENT_SOURCE, _dynamicRuntimeInfo);
		stateCollectInstrumentVisitor.setAllMethods(allExecutedMethods);
		Instrument.execute(_sourceSRCPath, stateCollectInstrumentVisitor);

		for (TestMethod testMethod : failedTestMethods) {
			String methodString = Identifier.getMessage(testMethod.getMethodID());
			String[] methodInfo = methodString.split("#");
			if (methodInfo.length < 4) {
				LevelLogger.error("collectAllFailedTestState parse test method string error : " + methodString);
				continue;
			}
			ExecuteCommand.executeDefects4JTest(
					InfoBuilder.buildDefects4JTestCommand(_dynamicRuntimeInfo, methodInfo[0], methodInfo[2]),
					Constant.STR_TMP_D4J_OUTPUT_FILE);
			String testDataFile = Constant.STR_FAILED_DATA_COLLECT_PATH + Constant.PATH_SEPARATOR + methodInfo[0] + "_"
					+ methodInfo[2];
			collectFailedTestStateIntoFile(Constant.STR_TMP_INSTR_OUTPUT_FILE, testDataFile,
					testMethod.getTestStatementNumber(), 0);
			
			// save failing information into file
			ExecuteCommand.moveFile(_projectPath + "/failing_tests", testDataFile + "/failing_tests");

		}
		Instrument.execute(_testSRCPath, new DeInstrumentVisitor());
		// this state collecting instrument can be reused for collecting
		// positive state, which can reduce the de/instrument cost
		// even though it may hard to understand and maintain
		// Instrument.execute(_sourceSRCPath, new DeInstrumentVisitor());
		return allExecutedMethods;
	}

	/**
	 * copy the test file containing failed test to *.javabak and than remove
	 * the failed test methods from the *java file
	 * 
	 * @param allFailedTestMethods
	 */
	private void backupFailedTest(List<TestMethod> allFailedTestMethods) {
		Set<Method> allTestMethod = new HashSet<>();
		allTestMethod.addAll(allFailedTestMethods);
		RomoveMethodBodyVisitor removeMethodBodyVisitor = new RomoveMethodBodyVisitor(allTestMethod);
		for (TestMethod testMethod : allFailedTestMethods) {
			String testMethodString = Identifier.getMessage(testMethod.getMethodID());
			String[] testMethodInfo = testMethodString.split("#");
			if (testMethodInfo.length < 4) {
				LevelLogger.error("backupFailedTest parse test method info error : " + testMethodString);
				continue;
			}

			String testFile = _testSRCPath + Constant.PATH_SEPARATOR
					+ testMethodInfo[0].replace(".", Constant.PATH_SEPARATOR) + ".java";
			CompilationUnit unit = JavaFile.genASTFromSource(JavaFile.readFileToString(testFile),
					ASTParser.K_COMPILATION_UNIT);
			unit.accept(removeMethodBodyVisitor);
			ExecuteCommand.moveFile(testFile, testFile + "bak");
			JavaFile.writeStringToFile(testFile, unit.toString());
		}
	}

	/**
	 * move *.javabak to *.java to recover the tests
	 * 
	 * @param allFailedTestMethods
	 */
	private void recoverFailedTest(List<TestMethod> allFailedTestMethods) {
		for (TestMethod testMethod : allFailedTestMethods) {
			String testMethodString = Identifier.getMessage(testMethod.getMethodID());
			String[] testMethodInfo = testMethodString.split("#");
			if (testMethodInfo.length < 4) {
				LevelLogger.error("backupFailedTest parse test method info error : " + testMethodString);
				continue;
			}

			String testFile = _testSRCPath + Constant.PATH_SEPARATOR
					+ testMethodInfo[0].replace(".", Constant.PATH_SEPARATOR) + ".java";
			File file = new File(testFile + "bak");
			if (!file.exists()) {
				LevelLogger.error("recoverFailedTest backup file not found!");
				continue;
			}
			String sourceContent = JavaFile.readFileToString(file);
			JavaFile.writeStringToFile(testFile, sourceContent);
			file.delete();
		}
	}

	/**
	 * collect all methods executed by tests
	 * 
	 * @param sourceFile
	 *            : file path containing executed methods
	 * @return
	 */
	private Set<Integer> collectClazzPath(String sourceFile) {

		File file = new File(sourceFile);
		Set<Integer> allMethodsID = new HashSet<>();
		if (!file.exists()) {
			return allMethodsID;
		}
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(file));
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("[INST]M")) {
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

	/**
	 * 
	 * @param passedTest
	 *            : collect all methods executed by given passed test methods,
	 *            which can be mutation point
	 * @return : a list of pair<Stirng, Set<Integer>>, the "String" represent
	 *         the passed test method and Set<Integer> contains all method IDs
	 *         executed by the passed test
	 */
	private List<Pair<String, Set<Integer>>> collectAllMutateFile(Set<Integer> passedTest) {
		List<Pair<String, Set<Integer>>> allPassedTestWithFullClazzPath = new ArrayList<>();
		int totaltest = passedTest.size();
		for (Integer testID : passedTest) {
			LevelLogger.info("ramaining " + totaltest-- + " passed test method need to collect runing methods.");
			String methodString = Identifier.getMessage(testID);
			String[] methodInfo = methodString.split("#");
			if (methodInfo.length < 4) {
				LevelLogger.error("collectAllMutateFile parse method id failed : " + methodString);
				continue;
			}
			String test = methodInfo[0] + "::" + methodInfo[2];
			ExecuteCommand.executeDefects4JTest(InfoBuilder.buildDefects4JTestCommand(_dynamicRuntimeInfo, test),
					Constant.STR_TMP_D4J_OUTPUT_FILE);
			Set<Integer> allClazzPath = collectClazzPath(Constant.STR_TMP_INSTR_OUTPUT_FILE);
			allPassedTestWithFullClazzPath.add(new Pair<String, Set<Integer>>(test, allClazzPath));
		}
		return allPassedTestWithFullClazzPath;
	}

	private void collectAllPositiveAndNegativeState(Set<Method> collectMethods) {
		// in the state collecting process of failed test, the state collector
		// has been instrumented, here we can reuse
		Instrument.execute(_testSRCPath, new MethodInstrumentVisitor(Constant.INSTRUMENT_TEST));

		LevelLogger.info("collecting positive states ...");
		// begin to collect positive state
		ExecuteCommand.executeDefects4JTest(InfoBuilder.buildDefects4JTestCommand(_dynamicRuntimeInfo),
				Constant.STR_TMP_D4J_OUTPUT_FILE);
		Set<Integer> collectStateMethods = collectPositiveStateIntoFile(Constant.STR_TMP_INSTR_OUTPUT_FILE,
				Constant.STR_POSITIVE_DATA_COLLECT_PATH);
		// remove state collect instrument
		Instrument.execute(_sourceSRCPath, new DeInstrumentVisitor());
		// end collect positive state

		LevelLogger.info("collecting all run methods ...");
		// collect all method run by passed tests that can run into the collect
		// method
		Instrument.execute(_sourceSRCPath, new MethodInstrumentVisitor());
		List<Pair<String, Set<Integer>>> allPassedTestWithMethod = collectAllMutateFile(collectStateMethods);
		Instrument.execute(_sourceSRCPath, new DeInstrumentVisitor());
		Instrument.execute(_testSRCPath, new DeInstrumentVisitor());

		LevelLogger.info("Test Methods before apply K_means : " + allPassedTestWithMethod.size());

		// select partial test methods via K_Means, given the max size for one
		// cluster and select top N test methods from each.
		allPassedTestWithMethod = Cluster.K_Means(allPassedTestWithMethod, new ArrayList<>(collectMethods),
				Constant.CLUSTER_MAX_SIZE_FOR_ONE, Constant.CLUSTER_KEEP_TOP_N);

		LevelLogger.info("Test Methods after apply K_means : " + allPassedTestWithMethod.size());

		Set<Integer> allMethodsID = new HashSet<>();
		List<Pair<String, Set<String>>> allPassedTestWithClazzPath = new ArrayList<>();
		for (Pair<String, Set<Integer>> pair : allPassedTestWithMethod) {
			allMethodsID.addAll(pair.getSecond());
			Set<String> allClazzPath = new HashSet<>();
			for (Integer methodID : pair.getSecond()) {
				String methodString = Identifier.getMessage(methodID);
				String[] methodInfo = methodString.split("#");
				allClazzPath.add(methodInfo[0] + "@" + methodInfo[2]);
			}
			allPassedTestWithClazzPath.add(new Pair<String, Set<String>>(pair.getFirst(), allClazzPath));
		}

		// begin to collect negative state
		MutantGenerator.configuration();
		// only generate useful mutants
		Map<String, List<Mutant>> result = MutantGenerator.getMutantsByMethod(_dynamicRuntimeInfo, allMethodsID);

		StatementInstrumentVisitor statementInstrumentVisitor = new StatementInstrumentVisitor(
				Constant.INSTRUMENT_TEST);
		Instrument.execute(_testSRCPath, statementInstrumentVisitor);
		StateCollectInstrumentVisitor stateCollectInstrumentVisitor = new StateCollectInstrumentVisitor(
				Constant.INSTRUMENT_SOURCE, _dynamicRuntimeInfo);
		stateCollectInstrumentVisitor.setAllMethods(collectMethods);
		Instrument.execute(_sourceSRCPath, stateCollectInstrumentVisitor);

		int totalMutantClazz = result.size();
		String mutantPath = InfoBuilder.buildSingleProjectPath(_dynamicRuntimeInfo) + Constant.MUTANT_DIR;
		for (Entry<String, List<Mutant>> entry : result.entrySet()) {
			LevelLogger.info("There are remain " + totalMutantClazz-- + " mutant clazz.");
			String clazzPathAndMethod = entry.getKey();
			String[] methodInfo = clazzPathAndMethod.split("@");
			if (methodInfo.length < 2) {
				LevelLogger.error("mutant method parse error : " + clazzPathAndMethod);
				continue;
			}
			String mutantFullClazz = methodInfo[0];
			int indexForDollar = mutantFullClazz.indexOf("$");
			if (indexForDollar > 0) {
				mutantFullClazz = mutantFullClazz.substring(0, indexForDollar);
			}
			String originalFile = _sourceSRCPath + Constant.PATH_SEPARATOR
					+ mutantFullClazz.replace(".", Constant.PATH_SEPARATOR) + ".java";
			// backup original file
			ExecuteCommand.moveFile(originalFile, originalFile + "bak");
			List<Mutant> mutants = entry.getValue();
			int totalMutants = mutants.size();
			for (Mutant mutant : mutants) {
				LevelLogger.info("Mutants remain " + totalMutants--);
				String mutantFile = mutantPath + Constant.PATH_SEPARATOR + mutant.getReplaceSource();
				ExecuteCommand.copyFile(mutantFile, originalFile);
				int line = mutant.getStartLineNumber();
				MutantFlagInstrumentVisitor mutantFlagInstrumentVisitor = new MutantFlagInstrumentVisitor(line,
						_dynamicRuntimeInfo);
				// mutantFlagInstrumentVisitor.setAllMethods(collectMethods);
				Instrument.execute(originalFile, mutantFlagInstrumentVisitor);
				Instrument.execute(originalFile, stateCollectInstrumentVisitor);
				int totalMethodCount = allPassedTestWithClazzPath.size();
				for (Pair<String, Set<String>> testMethod : allPassedTestWithClazzPath) {
					LevelLogger.info("test remain " + totalMethodCount--);
					if (!testMethod.getSecond().contains(clazzPathAndMethod)) {
						continue;
					}
					// limiting the running time as 5 minutes for each test case
					ExecuteCommand.executeDefects4JTest(InfoBuilder.buildDefects4JTestCommandWithTimeout(
							_dynamicRuntimeInfo, testMethod.getFirst(), 60 * 5L), Constant.STR_TMP_D4J_OUTPUT_FILE);
					if (isFailedTest(Constant.STR_TMP_D4J_OUTPUT_FILE)) {
						// save current mutant to file, change the output path
						// to a single one
						String idForMutant = mutant.getReplaceSource().substring(0,
								mutant.getReplaceSource().indexOf("/"));
						String fileContainer = Constant.STR_NEGATIVE_DATA_COLLECT_PATH + "/" + testMethod.getFirst()
								+ "/" + idForMutant;
						File file = new File(fileContainer);
						if (!file.exists()) {
							file.mkdirs();
						}
						ExecuteCommand.copyFile(mutantFile, fileContainer);
						ExecuteCommand.copyFile(_projectPath + "/failing_tests", fileContainer + "/failing_tests");
						
						collectNegativeStateIntoFile(Constant.STR_TMP_INSTR_OUTPUT_FILE, fileContainer);
					}
				}
			}
			// recover original file
			ExecuteCommand.moveFile(originalFile + "bak", originalFile);
		}

		Instrument.execute(_testSRCPath, new DeInstrumentVisitor());
		Instrument.execute(_sourceSRCPath, new DeInstrumentVisitor());
	}

	/**
	 * judging if the test failed based on the test output info
	 * 
	 * @param outputFile
	 *            : output file
	 * @return
	 */
	private boolean isFailedTest(String outputFile) {
		boolean isFailed = false;
		if (outputFile == null) {
			return isFailed;
		}
		File file = new File(outputFile);
		BufferedReader bReader = null;
		try {
			bReader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			return isFailed;
		}

		String line = null;
		List<String> failedTest = new ArrayList<>();
		try {
			while ((line = bReader.readLine()) != null) {
				String trimed = line.trim();
				if (trimed.startsWith(Constant.ANT_BUILD_FAILED)) {
					isFailed = false;
					break;
				}
				if (trimed.startsWith("Failing tests:")) {
					String count = trimed.substring("Failing tests:".length());
					int failingCount = Integer.parseInt(count.trim());
					if (failingCount > 0) {
						isFailed = true;
						break;
					}
				}
			}
			bReader.close();
		} catch (IOException e) {

		} finally {
			if (bReader != null) {
				try {
					bReader.close();
				} catch (IOException e) {
				}
			}
		}
		return isFailed;
	}

	/**
	 * execute all test and collect all failed test
	 * 
	 * @return a list of failed test
	 */
	private List<String> collectFailedTest() {
		ExecuteCommand.executeDefects4JTest(InfoBuilder.buildDefects4JTestCommand(_dynamicRuntimeInfo),
				Constant.STR_TMP_D4J_OUTPUT_FILE);
		return ExecutionPathBuider.findFailedTestFromFile(Constant.STR_TMP_D4J_OUTPUT_FILE);
	}

	/**
	 * collecting trace for each failed test
	 * 
	 * @param allFailedTests
	 *            : all failed tests
	 * @return
	 */
	private List<TestMethod> collectFailedTestTrace(List<String> allFailedTests) {
		String testSRCPath = InfoBuilder.buildTestSRCPath(_dynamicRuntimeInfo, true);
		List<TestMethod> allFailedTestMethods = new ArrayList<>();
		for (String string : allFailedTests) {
			String[] testInfo = string.split("::");
			if (testInfo.length < 2) {
				LevelLogger.error("collect failed test method trace error : " + string);
				continue;
			}
			String methodString = testInfo[0] + "#void#" + testInfo[1] + "#?";
			int methodID = Identifier.getIdentifier(methodString);
			Method method = new Method(methodID);

			String testJavaFile = testSRCPath + "/" + testInfo[0].replace(".", "/") + ".java";
			StatementInstrumentVisitor statementInstrumentVisitor = new StatementInstrumentVisitor(
					Constant.INSTRUMENT_TEST);
			statementInstrumentVisitor.setMethod(method);
			Instrument.execute(testJavaFile, statementInstrumentVisitor);

			ExecuteCommand.executeDefects4JTest(InfoBuilder.buildDefects4JTestCommand(_dynamicRuntimeInfo, string),
					Constant.STR_TMP_D4J_OUTPUT_FILE);

			Instrument.execute(testJavaFile, new DeInstrumentVisitor(method));

			List<TestMethod> testMethods = ExecutionPathBuider.buildPathFromFile(_dynamicRuntimeInfo,
					Constant.STR_TMP_INSTR_OUTPUT_FILE);

			if (testMethods != null && testMethods.size() > 0) {
				int lastIndex = testMethods.size() - 1;
				int totalPathLength = testMethods.get(lastIndex).getExecutionPath().size();
				while (totalPathLength < Constant.TRACE_LENGTH_FOR_FAILED_TEST && lastIndex > 0) {
					lastIndex--;
					totalPathLength += testMethods.get(lastIndex).getExecutionPath().size();
				}
				TestMethod toRecordTestMethod = testMethods.get(lastIndex);
				for (lastIndex++; lastIndex < testMethods.size(); lastIndex++) {
					List<ExecutedMethod> path = testMethods.get(lastIndex).getExecutionPath();
					for (ExecutedMethod executedMethod : path) {
						toRecordTestMethod.addExecutedMethod(executedMethod);
					}
				}
				allFailedTestMethods.add(toRecordTestMethod);
			}
		}
		return allFailedTestMethods;
	}

	private void collectNegativeStateIntoFile(String sourceFile, String targetFileContainer) {
		File file = new File(sourceFile);
		if (!file.exists()) {
			return;
		}
		Map<Integer, StringBuffer> allStates = new HashMap<>();
		BufferedReader bufferedReader = null;
		try {
			bufferedReader = new BufferedReader(new FileReader(file));
			String line = null;
			boolean canCollectData = false;
			String newLine = System.getProperty("line.separator");
			int START_count = 0;
			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();
				line = line.substring("[INST]".length());

				if (line.startsWith("U")) {
					canCollectData = true;
					continue;
				}
				if (line.startsWith("T")) {
					canCollectData = false;
					START_count = 0;
					continue;
				}

				if (line.startsWith("M>>START")) {
					START_count++;
					continue;
				}
				if (line.startsWith("M>>END")) {
					if (START_count > 0) {
						START_count--;
					}
					String[] guardStrings = line.split("#");
					if (guardStrings.length < 3) {
						LevelLogger.error("#collectFailedTestStateIntoFile Parse End info error : " + line);
						System.exit(0);
					}
					int id = Integer.parseInt(guardStrings[1]);
					if (allStates.containsKey(id)) {
						if (canCollectData) {
							String message = allStates.get(id).toString().trim();
							if (message.length() > 0) {
								message += newLine;
								message += newLine;
								String methodString = Identifier.getMessage(id);
								String filePath = targetFileContainer + Constant.PATH_SEPARATOR + methodString;
								JavaFile.writeStringToFile(filePath, message, true);
							}
						}
						allStates.remove(id);
					}
					continue;
				}
				if (START_count <= 0) {
					continue;
				}

				String[] info = line.split("#");
				String value = "";
				if (info.length < 2) {
					LevelLogger.error("collectStateIntoFile pass info error : " + line);
					continue;
				}
				if (info.length == 3) {
					value = info[2];
				} else {
					value = line.substring(info[0].length() + info[1].length() + 2);
				}

				Integer integer = Integer.valueOf(info[1]);
				if (allStates.containsKey(integer)) {
					StringBuffer sBuffer = allStates.get(integer);
					sBuffer.append(value);
					sBuffer.append(newLine);
				} else {
					StringBuffer sBuffer = new StringBuffer();
					sBuffer.append(value);
					sBuffer.append(newLine);
					allStates.put(integer, sBuffer);
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
		// if a method did not finish running, just drop the data
		// for(Entry<Integer, StringBuffer> entry : allStates.entrySet()){
		// StringBuffer sBuffer = entry.getValue();
		// if(sBuffer == null || sBuffer.toString().trim().length() <= 0){
		// continue;
		// }
		// String methodString = Identifier.getMessage(entry.getKey());
		// String filePath = targetFileContainer + Constant.PATH_SEPARATOR +
		// methodString;
		// String message = sBuffer.toString().trim() +
		// System.getProperty("line.separator");
		// JavaFile.writeStringToFile(filePath, message, true);
		// }
	}

	/**
	 * This is the public interface for collecting all states (failed, positive
	 * and negative)
	 */
	public void collect() {
		// remove all previous instrument and format source code
		Instrument.execute(_sourceSRCPath, new DeInstrumentVisitor());
		Instrument.execute(_testSRCPath, new DeInstrumentVisitor());
		// collect traces for failed tests
		List<String> failedTestStrings = collectFailedTest();
		Instrument.execute(_sourceSRCPath, new MethodInstrumentVisitor(Constant.INSTRUMENT_SOURCE));
		LevelLogger.info("collecting failed test trace ...");
		List<TestMethod> failedTestMethods = collectFailedTestTrace(failedTestStrings);
		Instrument.execute(_sourceSRCPath, new DeInstrumentVisitor());
		// write traces for failed tests into file
		writeTestMethodIntoFile(failedTestMethods, Constant.STR_FAILED_TEST_FILE);
		LevelLogger.info("collecting failed test states ...");
		// collect states for failed tests
		Set<Method> allCollectedMethod = collectAllFailedTestState(failedTestMethods);
		// backup original test file to *.javabak and remove failed test methods
		// in *.java
		backupFailedTest(failedTestMethods);
		// collect positive and negative states based on the trace of failed
		// tests
		collectAllPositiveAndNegativeState(allCollectedMethod);
		recoverFailedTest(failedTestMethods);
	}
}
