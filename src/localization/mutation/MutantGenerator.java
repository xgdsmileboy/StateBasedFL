package localization.mutation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import localization.common.config.Constant;
import localization.common.config.DynamicRuntimeInfo;
import localization.common.config.Identifier;
import localization.common.config.InfoBuilder;
import localization.common.java.JavaFile;
import localization.common.java.Mutant;
import localization.common.util.ExecuteCommand;
import localization.common.util.LevelLogger;

public class MutantGenerator {
	
	private static DynamicRuntimeInfo dynamicRuntimeInfo_;
	private static Set<Integer> allMethods_ = new HashSet<>();
	
	public static void configuration(){
		String content = "javac -XMutator=\"" + Constant.MML_BIN_FILE + "\" -J-Dmajor.export.mutants=true $MAJOR_OPT $@";
		JavaFile.writeStringToFile(Constant.STR_MML_CONFIG_FILE, content, false);
	}

	
	public static void cleanAll(DynamicRuntimeInfo dynamicRuntimeInfo){
		String projectPath = InfoBuilder.buildSingleProjectPath(dynamicRuntimeInfo).replaceAll("/", Constant.PATH_SEPARATOR);
		projectPath = projectPath + Constant.PATH_SEPARATOR + "tmp";
		ExecuteCommand.execute(buildCMD(Constant.COMMAND_RM + " -rf " + projectPath));
		cleanRepository();
	}
	/**
	 * @param method
	 *            The method to be mutated.
	 * @return Array of mutants, or null if there is some error.
	 */
	public static Map<String, List<Mutant>> getMutantsByMethod(DynamicRuntimeInfo dynamicRuntimeInfo, Set<Integer> allMethods) {
		dynamicRuntimeInfo_ = dynamicRuntimeInfo;
		allMethods_ = allMethods;
		if (!generateMMLFile()) {
			return null;
		}
		generateMutants();
		Map<String, List<Mutant>> result = processResult();
		LevelLogger.info("finish generate mutants.");
		return result;
	}

	private static String[] buildCMD(String mainCMD) {
		return new String[] { "/bin/bash", "-c", mainCMD };
	}

	private static boolean generateMMLFile() {
		LevelLogger.info("Generate mml file...");
		
//		String sourceRootPath = InfoBuilder.buildSourceSRCPath(dynamicRuntimeInfo_, true);
//		List<File> allFiles = JavaFile.ergodic(new File(sourceRootPath), new ArrayList<File>());
//		Set<Pair<String, String>> allMethods = new HashSet<>();
//		for(File file : allFiles){
//			CompilationUnit unit = JavaFile.genASTFromSource(JavaFile.readFileToString(file), ASTParser.K_COMPILATION_UNIT);
//			CollectMethodVisitor collector = new CollectMethodVisitor();
//			unit.accept(collector);
//			allMethods.addAll(collector.getAllMethods());
//		}
		
		File f = new File(Constant.MML_TEMPLATE_FILE);
		File fout = new File(Constant.MML_FILE);
		try {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			BufferedWriter writer = new BufferedWriter(new FileWriter(fout));
			String line;
			while ((line = reader.readLine()) != null) {
				writer.write(line + "\n");
			}
			HashSet<String> dup = new HashSet<String>();
			for(Integer methodID : allMethods_) {
				String methodString = Identifier.getMessage(methodID);
				String[] methodInfo = methodString.split("#");
				if(methodInfo.length < 4){
					LevelLogger.error("generateMMLFile parse method info error : " + methodString);
					continue;
				}
				String temp = methodInfo[0] + "@" + methodInfo[2];
				if (!dup.contains(temp)) {
					writer.write("groupOP<\"" + temp + "\">;\n");
					dup.add(temp);
				}
			}
//			for(Pair<String, String> method : allMethods) {
//				String temp = method.getFirst() + "@" + method.getSecond();
//				if (!dup.contains(temp)) {
//					writer.write("groupOP<\"" + temp + "\">;\n");
//					dup.add(temp);
//				}
//			}
			reader.close();
			writer.close();
			ExecuteCommand.execute(
					buildCMD(Constant.COMMAND_RM + Constant.MML_BIN_FILE));
			ExecuteCommand.execute(buildCMD(Constant.COMMAND_MML
					+ Constant.MML_FILE + " " + Constant.MML_BIN_FILE));
			File mmlBin = new File(Constant.MML_BIN_FILE);
			if (!mmlBin.exists()) {
				LevelLogger.error(
						"Fail to generate mml file " + Constant.MML_BIN_FILE);
				return false;
			}
			return true;
		} catch (FileNotFoundException e) {
			LevelLogger.error("Fail to find template mml file "
					+ Constant.MML_TEMPLATE_FILE, e);
		} catch (IOException e) {
			LevelLogger.error("", e);
		}
		return false;
	}

	private static void generateMutants() {
		LevelLogger.info("Generate mutants... This may takes some minutes.");
		cleanRepository();
		String projectPath = InfoBuilder.buildSingleProjectPath(dynamicRuntimeInfo_);
		ExecuteCommand.execute(buildCMD(Constant.COMMAND_DEFECTS4J + "mutation -w " + projectPath));
	}
	
	private static void cleanRepository() {
		String projectPath = InfoBuilder.buildSingleProjectPath(dynamicRuntimeInfo_);
		for(String filePath : Constant.MUTANT_REMOVABLE_FILES) {
			ExecuteCommand.execute(buildCMD(
					Constant.COMMAND_RM + projectPath + filePath));
		}
	}
	
	private static ArrayList<String> getMutantsID(String projectPath) {
		File mutantslog = new File(projectPath + Constant.MUTANT_LOG);
		ArrayList<String> mutantsID = new ArrayList<String>();
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(mutantslog));
			String line;
			while((line = reader.readLine()) != null) {
				String parts[] = line.split(":");
				if (parts.length < 7) {
					LevelLogger.error("Wrong format in mutants.log");
					reader.close();
					return mutantsID;
				}
				String beforeParams[] = parts[4].split("\\(");
				mutantsID.add(beforeParams[0]);
			}
			reader.close();
		} catch (IOException e) {
			LevelLogger.error("", e);
		}
		return mutantsID;
	}

	private static Map<String, List<Mutant>> processResult() {
		LevelLogger.info("Process results ...");
		HashMap<String, List<Mutant>> results = new HashMap<String, List<Mutant>>();

		String projectPath = InfoBuilder.buildSingleProjectPath(dynamicRuntimeInfo_);
		String srcPath = InfoBuilder.buildSourceSRCPath(dynamicRuntimeInfo_, true);

		ArrayList<String> mutantsID = getMutantsID(projectPath);
		if (mutantsID.size() == 0) {
			LevelLogger.warn("No mutants generated");
			return results;
		}
		String prefix = projectPath + Constant.MUTANT_DIR + "/";
		for(int i = 0; i < mutantsID.size(); i++) {
			String currentMethod = mutantsID.get(i);
			String parts[] = currentMethod.split("@");
			if (parts.length != 2) {
				LevelLogger.error("Format error in mutants.log " + mutantsID.get(i));
				return null;
			}
			String mutantFilePath = parts[0].replace('.', '/') + ".java";
			String subPath = Integer.toString(i + 1) + "/" + mutantFilePath;
			File mutantFile = new File(prefix + subPath);
			File oriFile = new File(srcPath + "/" + mutantFilePath);
			
			Mutant mut;
			try {
				mut = getMutant(oriFile, mutantFile);
				if (mut != null) {
					mut.setSource(subPath);
					List<Mutant> mutants = results.get(currentMethod);
					if(mutants != null){
						mutants.add(mut);
					}else{
						mutants = new ArrayList<>();
						mutants.add(mut);
						results.put(currentMethod, mutants);
					}
				}
			} catch (IOException e) {
				LevelLogger.error("", e);
				return null;
			}
		}
		return results;
	}

	private static Mutant getMutant(File oriFile, File mutFile) throws IOException {
		LevelLogger.debug("Get mutant from " + mutFile.getPath());
		if(!oriFile.exists() || !mutFile.exists()){
			return null;
		}
		ArrayList<String> mutFileContent = new ArrayList<String>();
		ArrayList<String> oriFileContent = new ArrayList<String>();
		BufferedReader mutReader = new BufferedReader(new FileReader(mutFile));
		BufferedReader oriReader = new BufferedReader(new FileReader(oriFile));
		String line;
		while ((line = mutReader.readLine()) != null) {
			mutFileContent.add(line);
		}
		while ((line = oriReader.readLine()) != null) {
			oriFileContent.add(line);
		}
		mutReader.close();
		oriReader.close();
		int i;
		for (i = 0; i < mutFileContent.size()
				&& i < oriFileContent.size(); i++) {
			if (!mutFileContent.get(i).equals(oriFileContent.get(i))) {
				break;
			}
		}
		int mj, oj;
		for (mj = mutFileContent.size() - 1, oj = oriFileContent.size()
				- 1; mj >= i && oj >= i; mj--, oj--) {
			if (!mutFileContent.get(mj).equals(oriFileContent.get(oj))) {
				break;
			}
		}
		if (oj < i) {
			LevelLogger.error("Fail to find a diff. Should not reach here!");
			return null;
		}
//		for (int k = i; k <= oj; k++) {
//			if (!executedLines_.contains(k + 1)) {
//				return null;
//			}
//		}
//		String diff = "";
//		for (int k = i; k <= mj; k++) {
//			diff = diff.concat(mutFileContent.get(k));
//		}
		Mutant mutant = new Mutant(i + 1, oj + 1);
//		mutant.setSource(diff);
		return mutant;
	}

//	public static void main(String args[]) throws Exception {
//		
//		DynamicRuntimeInfo dynamicRuntimeInfo = new DynamicRuntimeInfo("lang", 1);
//
//		MutantGenerator.cleanAll(dynamicRuntimeInfo);
//		MutantGenerator.configuration();
//		
//		Set<Integer> methods = new HashSet<>();
//		String method1 = "org.apache.commons.lang3.math.NumberUtils#Number#createNumber#?,String";
//		String method2 = "org.apache.commons.lang3.math.NumberUtils#Integer#createInteger#?,String";
//		methods.add(Identifier.getIdentifier(method1));
//		methods.add(Identifier.getIdentifier(method2));
//		
//		Map<String, List<Mutant>> result = MutantGenerator.getMutantsByMethod(dynamicRuntimeInfo, methods);
//		
//		for(Entry<String, List<Mutant>> entry : result.entrySet()){
//			System.out.println(entry.getKey());
//			for(Mutant mutant : entry.getValue()){
//				System.out.println(mutant);
//			}
//			System.out.println("");
//		}
//		
//	}
}
