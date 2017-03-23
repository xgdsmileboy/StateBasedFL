package localization.common.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;

import localization.common.java.JavaFile;
import localization.common.util.Debugger;
import localization.common.util.LevelLogger;

public class Configuration {

	private final static String __name__ = "@Configuration ";

	public static void config(DynamicRuntimeInfo dynamicRuntimeInfo) {
		config_dumper(dynamicRuntimeInfo);
	}

	public static void init() {
		Map<String, String[]> projectInfo = new HashMap<>();

		String[] INFO_PROJECT_LANG = new String[9]; // defects4j lang
		String[] INFO_PROJECT_CHART = new String[9]; // defects4j chart
		String[] INFO_PROJECT_MATH = new String[9]; // defects4j math
		String[] INFO_PROJECT_TIME = new String[9]; // defects4j time
		String[] INFO_PROJECT_CLOSURE = new String[9]; // defects4j closure

		Properties prop = new Properties();
		try {
			String filePath = System.getProperty("user.dir") + "/resource/conf/configure.properties";
			InputStream in = new BufferedInputStream(new FileInputStream(filePath));
			prop.load(in);
			String home = prop.getProperty("project.home");
			INFO_PROJECT_LANG[0] = home;
			// source code path
			INFO_PROJECT_LANG[1] = prop.getProperty("lang.source.src");
			// test code path
			INFO_PROJECT_LANG[2] = prop.getProperty("lang.test.src");
			// source class file path
			INFO_PROJECT_LANG[3] = prop.getProperty("lang.source.classes");
			// test class file path
			INFO_PROJECT_LANG[4] = prop.getProperty("lang.test.classes");
			// build file path for ant
			INFO_PROJECT_LANG[5] = prop.getProperty("lang.build.xml");
			// compile target for ant
			INFO_PROJECT_LANG[6] = prop.getProperty("lang.build.target");
			// included tests
			INFO_PROJECT_LANG[7] = prop.getProperty("lang.test.include");
			// excluded tests
			INFO_PROJECT_LANG[8] = prop.getProperty("lang.test.exclude");

			INFO_PROJECT_CHART[0] = home;
			INFO_PROJECT_CHART[1] = prop.getProperty("chart.source.src");
			INFO_PROJECT_CHART[2] = prop.getProperty("chart.test.src");
			INFO_PROJECT_CHART[3] = prop.getProperty("chart.source.classes");
			INFO_PROJECT_CHART[4] = prop.getProperty("chart.test.classes");
			INFO_PROJECT_CHART[5] = prop.getProperty("chart.build.xml");
			INFO_PROJECT_CHART[6] = prop.getProperty("chart.build.target");
			INFO_PROJECT_CHART[7] = prop.getProperty("chart.test.include");
			INFO_PROJECT_CHART[8] = prop.getProperty("chart.test.exclude");

			INFO_PROJECT_MATH[0] = home;
			INFO_PROJECT_MATH[1] = prop.getProperty("math.source.src");
			INFO_PROJECT_MATH[2] = prop.getProperty("math.test.src");
			INFO_PROJECT_MATH[3] = prop.getProperty("math.source.classes");
			INFO_PROJECT_MATH[4] = prop.getProperty("math.test.classes");
			INFO_PROJECT_MATH[5] = prop.getProperty("math.build.xml");
			INFO_PROJECT_MATH[6] = prop.getProperty("math.build.target");
			INFO_PROJECT_MATH[7] = prop.getProperty("math.test.include");
			INFO_PROJECT_MATH[8] = prop.getProperty("math.test.exclude");

			INFO_PROJECT_TIME[0] = home;
			INFO_PROJECT_TIME[1] = prop.getProperty("time.source.src");
			INFO_PROJECT_TIME[2] = prop.getProperty("time.test.src");
			INFO_PROJECT_TIME[3] = prop.getProperty("time.source.classes");
			INFO_PROJECT_TIME[4] = prop.getProperty("time.test.classes");
			INFO_PROJECT_TIME[5] = prop.getProperty("time.build.xml");
			INFO_PROJECT_TIME[6] = prop.getProperty("time.build.target");
			INFO_PROJECT_TIME[7] = prop.getProperty("time.test.include");
			INFO_PROJECT_TIME[8] = prop.getProperty("time.test.exclude");

			INFO_PROJECT_CLOSURE[0] = home;
			INFO_PROJECT_CLOSURE[1] = prop.getProperty("closure.source.src");
			INFO_PROJECT_CLOSURE[2] = prop.getProperty("closure.test.src");
			INFO_PROJECT_CLOSURE[3] = prop.getProperty("closure.source.classes");
			INFO_PROJECT_CLOSURE[4] = prop.getProperty("closure.test.classes");
			INFO_PROJECT_CLOSURE[5] = prop.getProperty("closure.build.xml");
			INFO_PROJECT_CLOSURE[6] = prop.getProperty("closure.build.target");
			INFO_PROJECT_CLOSURE[7] = prop.getProperty("closure.test.include");
			INFO_PROJECT_CLOSURE[8] = prop.getProperty("closure.test.exclude");

			// System commands
			Constant.COMMAND_JAVA = prop.getProperty("cmd.java").replace("/", Constant.PATH_SEPARATOR) + " ";
			Constant.COMMAND_CODE_FORMAT = Constant.COMMAND_JAVA + "-jar google-java-format-1.1-all-deps.jar ";
			Constant.COMMAND_CD = prop.getProperty("cmd.cd").replace("/", Constant.PATH_SEPARATOR) + " ";
			Constant.COMMAND_CP = prop.getProperty("cmd.cp").replace("/", Constant.PATH_SEPARATOR) + " ";
			// for ant compiling
			Constant.COMMAND_ANT = prop.getProperty("cmd.ant").replace("/", Constant.PATH_SEPARATOR) + " ";
			// for deleting files
			Constant.COMMAND_RM = prop.getProperty("cmd.rm").replace("/", Constant.PATH_SEPARATOR) + " -rf ";
			// for backup file
			Constant.COMMAND_MV = prop.getProperty("cmd.mv").replace("/", Constant.PATH_SEPARATOR) + " ";
			// for junit run
			Constant.COMMAND_JUNIT_RUN = Constant.COMMAND_JAVA + " -jar runjunit.jar ";
			// for defects4j mutation
			Constant.DEFECTS4J_HOME = prop.getProperty("defects4j.home").replace("/", Constant.PATH_SEPARATOR);
			Constant.STR_MML_CONFIG_FILE = Constant.DEFECTS4J_HOME
					+ "/major/bin/major".replace("/", Constant.PATH_SEPARATOR);
			Constant.COMMAND_DEFECTS4J = Constant.DEFECTS4J_HOME
					+ "/framework/bin/defects4j".replace("/", Constant.PATH_SEPARATOR) + " ";
			Constant.COMMAND_MML = Constant.DEFECTS4J_HOME + "/major/bin/mmlc".replace("/", Constant.PATH_SEPARATOR)
					+ " ";
			
			//for dumper configure
			Constant.DUMPER_OUT_AND_LIB_PATH = Constant.HOME;
			Constant.DUMPER_MAX_OUTPUT_FILE_SIZE = prop.getProperty("dumper.MAX_OUTPUT_FILE_SIZE");
			Constant.DUMPER_MAX_DEPTH = prop.getProperty("dumper.MAX_DEPTH");
			Constant.DUMPER_ARRAY_MAX_LENGTH = prop.getProperty("dumper.ARRAY_MAX_LENGTH");
			
			// for cluster
			Constant.CLUSTER_MAX_SIZE_FOR_ONE = Integer.parseInt(prop.getProperty("cluster.CLUSTER_MAX_SIZE_FOR_ONE"));
			Constant.CLUSTER_KEEP_TOP_N = Integer.parseInt(prop.getProperty("cluster.CLUSTER_KEEP_TOP_N"));

		} catch (IOException e) {
			if (Debugger.debugOn()) {
				Debugger.debug(__name__ + "@static get properties failed!" + e.getMessage());
			}
		}

		projectInfo.put("lang", INFO_PROJECT_LANG);
		projectInfo.put("math", INFO_PROJECT_MATH);
		projectInfo.put("chart", INFO_PROJECT_CHART);
		projectInfo.put("time", INFO_PROJECT_TIME);
		projectInfo.put("closure", INFO_PROJECT_CLOSURE);

		// initialize the project info
		InfoBuilder.init(projectInfo);
	}

	private static void config_dumper(DynamicRuntimeInfo dynamicRuntimeInfo) {
		File file = new File(Constant.HOME + "/resource/auxiliary/Dumper.java");
		if (!file.exists()) {
			LevelLogger.error("File : " + file.getAbsolutePath() + " not exist.");
			System.exit(0);
		}
		CompilationUnit cu = JavaFile.genASTFromSource(JavaFile.readFileToString(file), ASTParser.K_COMPILATION_UNIT);
		cu.accept(new ConfigDumperVisitor());
		Formatter formatter = new Formatter();
		String formatSource = null;
		try {
			formatSource = formatter.formatSource(cu.toString());
		} catch (FormatterException e) {
			System.err.println(__name__ + "#execute Format Code Error for : " + file.getAbsolutePath());
			formatSource = cu.toString();
		}
		
		String path = InfoBuilder.buildSourceSRCPath(dynamicRuntimeInfo, true);
		String target = path + Constant.PATH_SEPARATOR + "auxiliary/Dumper.java";
		File targetFile = new File(target);
		if(!targetFile.exists()){
			targetFile.getParentFile().mkdirs();
			try {
				targetFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		JavaFile.writeStringToFile(targetFile, formatSource);
	}
}

class ConfigDumperVisitor extends ASTVisitor {
	@Override
	public boolean visit(FieldDeclaration node) {
		for (Object object : node.fragments()) {
			if (object instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment vdf = (VariableDeclarationFragment) object;
				String name = vdf.getName().getFullyQualifiedName();
				if (name.equals("OUT_AND_LIB_PATH")) {
					StringLiteral stringLiteral = node.getAST().newStringLiteral();
					stringLiteral.setLiteralValue(Constant.DUMPER_OUT_AND_LIB_PATH);
					vdf.setInitializer(stringLiteral);
				} else if (name.equals("MAX_OUTPUT_FILE_SIZE")) {
					NumberLiteral numberLiteral = node.getAST().newNumberLiteral(Constant.DUMPER_MAX_OUTPUT_FILE_SIZE);
					vdf.setInitializer(numberLiteral);
				} else if (name.equals("MAX_DEPTH")) {
					NumberLiteral numberLiteral = node.getAST().newNumberLiteral(Constant.DUMPER_MAX_DEPTH);
					vdf.setInitializer(numberLiteral);
				} else if (name.equals("ARRAY_MAX_LENGTH")) {
					NumberLiteral numberLiteral = node.getAST().newNumberLiteral(Constant.DUMPER_ARRAY_MAX_LENGTH);
					vdf.setInitializer(numberLiteral);
				}
			}
		}

		return true;
	}
}
