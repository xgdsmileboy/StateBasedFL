package localization.common.config;

public class Constant {

	// used for instrument
	public final static String INSTRUMENT_DOT_SEPARATOR = ".";
	public final static String INSTRUMENT_FLAG = "[INST]";
	public final static String INSTRUMENT_TEST = "T";
	public final static String INSTRUMENT_SOURCE = "M";
	public final static String INSTRUMENT_MUTANT = "U";
	public final static String HOME = System.getProperty("user.dir");

	// common info
	public final static String SOURCE_FILE_SUFFIX = ".java";
	public final static String PATH_SEPARATOR = System.getProperty("file.separator");

	// least length for failed test trace
	public final static int TRACE_LENGTH_FOR_FAILED_TEST = 20;

	// build flags
	public final static String ANT_BUILD_FAILED = "BUILD FAILED";
	public final static String ANT_BUILD_SUCCESS = "BUILD SUCCESSFUL";

	// system command
	public static String COMMAND_CD = null;
	public static String COMMAND_ANT = null;
	public static String COMMAND_RM = null;
	public static String COMMAND_MV = null;
	public static String COMMAND_CP = null;
	public static String COMMAND_JAVA = null;
	public static String COMMAND_JUNIT_RUN = null;
	public static String COMMAND_CODE_FORMAT = null;

	// system properties
	public final static String STR_OUT_PATH = HOME + "/out";
	public final static String STR_LOG_FILE = STR_OUT_PATH + "/debug.log";
	public final static String STR_TMP_D4J_OUTPUT_FILE = STR_OUT_PATH + "/d4j.out";
	public final static String STR_TMP_INSTR_OUTPUT_FILE = STR_OUT_PATH + "/path.out";
	public final static String STR_FAILED_TEST_FILE = STR_OUT_PATH + "/failed.test";
	public final static String STR_PASSED_TEST_FILE = STR_OUT_PATH + "/passed.test";
	public final static String STR_ALL_DATA_COLLECT_PATH = STR_OUT_PATH + "/data";
	public final static String STR_NEGATIVE_DATA_COLLECT_PATH = STR_OUT_PATH + "/data/negative";
	public final static String STR_POSITIVE_DATA_COLLECT_PATH = STR_OUT_PATH + "/data/positive";
	public final static String STR_FAILED_DATA_COLLECT_PATH = STR_OUT_PATH + "/data/failed";

	//********************configuration for mutant generation*******************//
	public final static String STR_MUTATION_POINT_PATH = STR_OUT_PATH + "/mutation";
	public static String STR_MML_CONFIG_FILE = "";

	// mutation configuration
	public final static String MML_PATH = HOME + "/resource/mml/";
	public final static String MML_TEMPLATE_FILE = MML_PATH + "all.mml";
	public final static String MML_FILE = MML_PATH + "spec.mml";
	public final static String MML_BIN_FILE = MML_PATH + "spec.mml.bin";
	public static String COMMAND_MML = "NOT USED";
	public static String DEFECTS4J_HOME = "/home/jiajun/d4j/defects4j";
	public static String COMMAND_DEFECTS4J = DEFECTS4J_HOME + "/framework/bin/defects4j ";

	// project info
	public final static String MUTANT_DIR = "/mutants";
	public final static String MUTANT_LOG = "/mutants.log";
	public final static String[] MUTANT_REMOVABLE_FILES = { MUTANT_DIR, "/mutants.log", "/target", "/mml",
			"/.classes_mutated" };
}