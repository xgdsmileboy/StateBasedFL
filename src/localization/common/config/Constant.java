package localization.common.config;

import com.sun.org.apache.xml.internal.resolver.helpers.Debug;

/**
 * This class contains all constant variables
 * @author Jiajun
 * @date Mar 24, 2017
 */
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

	// dumper configuration
	public static String DUMPER_MAX_OUTPUT_FILE_SIZE = null;
	public static String DUMPER_MAX_DEPTH = null;
	public static String DUMPER_ARRAY_MAX_LENGTH = null;
	public static String DUMPER_OUT_AND_LIB_PATH = null;

	// cluster configuration
	public static int CLUSTER_MAX_SIZE_FOR_ONE = 20;
	public static int CLUSTER_KEEP_TOP_N = 10;

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
	/**
	 * out
	 */
	public final static String STR_OUT_PATH = HOME + "/out";
	/**
	 * out/debug.log 
	 */
	public final static String STR_LOG_FILE = STR_OUT_PATH + "/debug.log";
	/**
	 * out/d4j.out
	 */
	public final static String STR_TMP_D4J_OUTPUT_FILE = STR_OUT_PATH + "/d4j.out";
	/**
	 * out/path.out
	 */
	public final static String STR_TMP_INSTR_OUTPUT_FILE = STR_OUT_PATH + "/path.out";
	/**
	 * out/failed.test
	 */
	public final static String STR_FAILED_TEST_FILE = STR_OUT_PATH + "/failed.test";
	/**
	 * out/passed.test
	 */
	public final static String STR_PASSED_TEST_FILE = STR_OUT_PATH + "/passed.test";
	/**
	 * out/data
	 */
	public final static String STR_ALL_DATA_COLLECT_PATH = STR_OUT_PATH + "/data";
	/**
	 * out/data/negative
	 */
	public final static String STR_NEGATIVE_DATA_COLLECT_PATH = STR_OUT_PATH + "/data/negative";
	/**
	 * out/data/positive
	 */
	public final static String STR_POSITIVE_DATA_COLLECT_PATH = STR_OUT_PATH + "/data/positive";
	/**
	 * out/data/failed
	 */
	public final static String STR_FAILED_DATA_COLLECT_PATH = STR_OUT_PATH + "/data/failed";

	// *********configuration for mutant generation********//
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