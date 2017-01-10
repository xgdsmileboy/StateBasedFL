package localization.common.util;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class LevelLogger {
	private static Logger logger_ = Logger.getLogger("state-based");
	
	private static LevelLogger levelLogger_ = new LevelLogger();
	
	private LevelLogger(){
		File f = new File("log4j.properties");
		if (f.exists()) {
			PropertyConfigurator.configure("log4j.properties");
		} else {
			BasicConfigurator.configure();
		}
	}
	
	public static void debug(Object message) {
		logger_.debug(message);
	}
	
	public static void debug(Object message, Throwable t) {
		logger_.debug(message, t);
	}
	
	public static void info(Object message) {
		logger_.info(message);
	}
	
	public static void info(Object message, Throwable t) {
		logger_.info(message, t);
	}
	
	public static void warn(Object message) {
		logger_.warn(message);
	}
	
	public static void warn(Object message, Throwable t) {
		logger_.warn(message, t);
	}
	
	public static void error(Object message) {
		logger_.error(message);
	}
	
	public static void error(Object message, Throwable t) {
		logger_.error(message, t);
	}
	
	public static void fatal(Object message) {
		logger_.fatal(message);
	}
	
	public static void fatal(Object message, Throwable t) {
		logger_.fatal(message, t);
	}
}
