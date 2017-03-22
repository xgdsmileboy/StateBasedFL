package localization.common.config;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import localization.common.util.Debugger;

/**
 * This class is used for parse or build information related to project
 * information
 * 
 * @author Jiajun
 *
 */
public class InfoBuilder {

	private final static String __name__ = "@InforBuilder ";

	/**
	 * hold all the project information
	 */
	private final static Map<String, String[]> projectInfo = new HashMap<>();

	static {

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

			//System commands
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
	}

	/**
	 * get the included test flags, divided by "," if multiple
	 * 
	 * @param dynamicRuntimeInfo
	 * @return
	 */
	public static String buildIncludedTestPostfix(DynamicRuntimeInfo dynamicRuntimeInfo) {
		if (check(dynamicRuntimeInfo)) {
			String projectName = dynamicRuntimeInfo.getProjectName();
			return projectInfo.get(projectName)[7];
		}
		return null;
	}

	/**
	 * get excluded test flags, divided by "," if multiple
	 * 
	 * @param dynamicRuntimeInfo
	 * @return
	 */
	public static String buildIExcludedTestPostfix(DynamicRuntimeInfo dynamicRuntimeInfo) {
		if (check(dynamicRuntimeInfo)) {
			String projectName = dynamicRuntimeInfo.getProjectName();
			return projectInfo.get(projectName)[8];
		}
		return null;
	}

	/**
	 * construct the absolute path of a project
	 * 
	 * @param dynamicRuntimeInfo
	 * @return
	 */
	public static String buildSingleProjectPath(DynamicRuntimeInfo dynamicRuntimeInfo) {
		if (check(dynamicRuntimeInfo)) {
			String projectName = dynamicRuntimeInfo.getProjectName();
			int id = dynamicRuntimeInfo.getProjectId();

			if (projectName == null) {
				if (Debugger.debugOn()) {
					Debugger.debug(__name__ + "#buildSingleProjectPath Illegal input projectName : null.");
				}
				return new String();
			}

			String[] info = projectInfo.get(projectName);
			if (info == null) {
				if (Debugger.debugOn()) {
					Debugger.debug(__name__ + "#buildSingleProjectPath UNKNOWN projectName : " + projectName);
				}
			} else {
				return new String(info[0] + "/" + projectName + "_" + String.valueOf(id) + "_buggy");
			}
		}
		return new String();
	}

	/**
	 * construct the source code path of tests
	 * 
	 * @param dynamicRuntimeInfo
	 * @param absolute
	 *            : {@code true} means return a absolute path, otherwise a
	 *            relative path
	 * @return
	 */
	public static String buildSourceSRCPath(DynamicRuntimeInfo dynamicRuntimeInfo, boolean absolute) {
		if (check(dynamicRuntimeInfo)) {

			String projectName = dynamicRuntimeInfo.getProjectName();
			int id = dynamicRuntimeInfo.getProjectId();

			if (projectName == null) {
				if (Debugger.debugOn()) {
					Debugger.debug(__name__ + "#buildSourceSRCPath Illegal input projectName : null.");
				}
				return new String();
			}
			String[] info = projectInfo.get(projectName);
			if (info == null) {
				if (Debugger.debugOn()) {
					Debugger.debug(__name__ + "#buildSourceSRCPath UNKNOWN projectName : " + projectName);
				}
			} else {
				if (absolute) {
					return new String(info[0] + "/" + projectName + "_" + String.valueOf(id) + "_buggy" + info[1]);
				} else {
					return new String(info[1]);
				}
			}
		}
		return new String();
	}

	/**
	 * construct the source code path
	 * 
	 * @param dynamicRuntimeInfo
	 * @param absolute
	 *            : {@code true} means return a absolute path, otherwise a
	 *            relative path
	 * @return
	 */
	public static String buildTestSRCPath(DynamicRuntimeInfo dynamicRuntimeInfo, boolean absolute) {
		if (check(dynamicRuntimeInfo)) {

			String projectName = dynamicRuntimeInfo.getProjectName();
			int id = dynamicRuntimeInfo.getProjectId();

			if (projectName == null) {
				if (Debugger.debugOn()) {
					Debugger.debug(__name__ + "#buildTestSRCPath Illegal input projectName : null.");
				}
				return new String();
			}
			String[] info = projectInfo.get(projectName);
			if (info == null) {
				if (Debugger.debugOn()) {
					Debugger.debug(__name__ + "#buildTestSRCPath UNKNOWN projectName : " + projectName);
				}
			} else {
				if (absolute) {
					return new String(info[0] + "/" + projectName + "_" + String.valueOf(id) + "_buggy" + info[2]);
				} else {
					return new String(info[2]);
				}
			}
		}
		return new String();
	}

	/**
	 * clear old classes
	 * 
	 * @param dynamicRuntimeInfo
	 * @return
	 */
	public static String[] buildClearClazzes(DynamicRuntimeInfo dynamicRuntimeInfo) {
		if (check(dynamicRuntimeInfo)) {
			String projectName = dynamicRuntimeInfo.getProjectName();
			String projectPath = buildSingleProjectPath(dynamicRuntimeInfo);
			String[] info = projectInfo.get(projectName);
			if (info == null) {
				if (Debugger.debugOn()) {
					Debugger.debug(__name__ + "#buildCmd UNKNOWN projectName : " + projectName);
				}
			} else {
				return new String[] { "/bin/bash", "-c",
						Constant.COMMAND_RM + projectPath + info[3] + " " + projectPath + info[4] };
			}
		}
		return new String[] {};
	}

	/**
	 * construct the compile command
	 * 
	 * @param dynamicRuntimeInfo
	 * @return
	 */
	public static String[] buildCompileCmd(DynamicRuntimeInfo dynamicRuntimeInfo) {
		if (check(dynamicRuntimeInfo)) {
			String projectName = dynamicRuntimeInfo.getProjectName();
			String projectPath = buildSingleProjectPath(dynamicRuntimeInfo);
			String[] info = projectInfo.get(projectName);
			if (info == null) {
				if (Debugger.debugOn()) {
					Debugger.debug(__name__ + "#buildCmd UNKNOWN projectName : " + projectName);
				}
			} else {
				// return new String[] { "/bin/bash", "-c",
				// Constant.COMMAND_ANT + " -buildfile " + projectPath + info[5]
				// + "/build.xml " + info[6] };
				return new String[] { "/bin/bash", "-c",
						Constant.COMMAND_CD + projectPath + " && " + Constant.COMMAND_DEFECTS4J + " compile" };
			}
		}
		return new String[] {};
	}

	/**
	 * construct the command for junit
	 * 
	 * @param dynamicRuntimeInfo
	 * @param clazzName
	 * @param methodName
	 * @param redirectFilePath
	 * @return
	 */
	public static String[] buildJunitCommand(DynamicRuntimeInfo dynamicRuntimeInfo, String clazzName, String methodName,
			String redirectFilePath) {
		StringBuffer args = new StringBuffer();
		if (check(dynamicRuntimeInfo)) {
			args.append(dynamicRuntimeInfo.getProjectName() + " ");
			args.append(dynamicRuntimeInfo.getProjectId() + " ");
			args.append(clazzName + " ");
			if (methodName == null) {
				args.append("null ");
			} else {
				args.append(methodName + " ");
			}
			if (redirectFilePath == null) {
				args.append("null");
			} else {
				args.append(redirectFilePath);
			}

		}

		String[] cmd = new String[] { "/bin/bash", "-c", Constant.COMMAND_JUNIT_RUN + args.toString() };
		return cmd;
	}

	public static String[] buildDefects4JTestCommand(DynamicRuntimeInfo dynamicRuntimeInfo, String clazzName,
			String methodName) {
		if (clazzName != null && methodName != null) {
			return buildDefects4JTestCommand(dynamicRuntimeInfo, clazzName + "::" + methodName);
		}
		return new String[] { "/bin/bash", "-c" };
	}

	public static String[] buildDefects4JTestCommand(DynamicRuntimeInfo dynamicRuntimeInfo, String testInfo) {
		StringBuffer args = new StringBuffer();
		if (check(dynamicRuntimeInfo) && testInfo != null) {
			// defects4j test -t
			// org.apache.commons.lang3.AnnotationUtilsTest::testEquivalence
			String projectPath = buildSingleProjectPath(dynamicRuntimeInfo);
			args.append(Constant.COMMAND_CD + projectPath + " && ");
			args.append(Constant.COMMAND_DEFECTS4J + " test -t " + testInfo);
		}
		String[] cmd = new String[] { "/bin/bash", "-c", args.toString() };
		return cmd;
	}

	public static String[] buildDefects4JTestCommandWithTimeout(DynamicRuntimeInfo dynamicRuntimeInfo, String testInfo,
			long timeoutSeconds) {
		StringBuffer args = new StringBuffer();
		if (check(dynamicRuntimeInfo) && testInfo != null) {
			// defects4j test -t
			// org.apache.commons.lang3.AnnotationUtilsTest::testEquivalence
			String projectPath = buildSingleProjectPath(dynamicRuntimeInfo);
			args.append(Constant.COMMAND_CD + projectPath + " && ");
			args.append("timeout " + String.valueOf(timeoutSeconds) + " " + Constant.COMMAND_DEFECTS4J + " test -t "
					+ testInfo);
		}
		String[] cmd = new String[] { "/bin/bash", "-c", args.toString() };
		return cmd;
	}

	public static String[] buildDefects4JTestCommand(DynamicRuntimeInfo dynamicRuntimeInfo) {
		StringBuffer args = new StringBuffer();
		if (check(dynamicRuntimeInfo)) {

			String projectPath = buildSingleProjectPath(dynamicRuntimeInfo);
			args.append(Constant.COMMAND_CD + projectPath + " && ");
			args.append(Constant.COMMAND_DEFECTS4J + " test");
		}
		System.out.println(args.toString());
		String[] cmd = new String[] { "/bin/bash", "-c", args.toString() };
		return cmd;
	}

	/**
	 * check the project information
	 * 
	 * @param dynamicRuntimeInfo
	 * @return
	 */
	private static boolean check(DynamicRuntimeInfo dynamicRuntimeInfo) {
		if (dynamicRuntimeInfo == null) {
			if (Debugger.debugOn()) {
				Debugger.debug(__name__ + "#check Illegal input dynamicRuntimeInfo : null.");
			}
			return false;
		} else if (dynamicRuntimeInfo.getProjectName() == null) {
			if (Debugger.debugOn()) {
				Debugger.debug(__name__ + "#check Illegal input dynamicRuntimeInfo : null.");
			}
			return false;
		}
		return true;
	}

	// public static void main(String[] args) {
	//
	// System.out.println(System.getProperty("file.separator"));
	//
	// for (Entry<String, String[]> entry : projectInfo.entrySet()) {
	// System.out.println(entry.getKey());
	// for (String value : entry.getValue()) {
	// System.out.println(value);
	// }
	// }
	// }

}
