package localization.common.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import localization.common.config.Constant;

/**
 * Used for debugging
 * 
 * @author Jiajun
 *
 */
public class Debugger {

	private final static String __name__ = "@Debugger ";
	private static boolean debugOn = true;

	public static OutputStream log = null;

	public static boolean debugOn() {
		return debugOn;
	}

	public static void start() {
		if (debugOn()) {

			File file = new File(Constant.STR_LOG_FILE);
			if (!file.exists()) {
				try {
					file.createNewFile();
				} catch (IOException e) {
					System.err.println(__name__ + "#start Create new file failed : " + file.getAbsolutePath());
				}
			}

			try {
				log = new FileOutputStream(file, false);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

	}

	public static void debug(String message) {
		if (log == null) {
			return;
		}
		try {
			log.write(message.getBytes());
			log.write(System.getProperty("line.separator").getBytes());
		} catch (IOException e) {
			System.err.println(__name__ + "#debug  Output debug message failed : " + message);
		}
	}

	public static void stop() {
		if (debugOn() && log != null) {
			try {
				log.close();
			} catch (IOException e) {
				System.err.println(__name__ + "#stop  Close log file failded.");
			} finally {
				if (log != null) {
					try {
						log.close();
					} catch (IOException e) {
					}
				}
			}
		}
	}
	
}
