package localization.common.config;

import java.util.HashMap;
import java.util.Map;

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
	private static Map<String, String[]> projectInfo = new HashMap<>();

	public static void init(Map<String, String[]> projInfo){
		projectInfo = projInfo;
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
