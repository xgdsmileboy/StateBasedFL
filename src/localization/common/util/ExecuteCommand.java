package localization.common.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import localization.common.config.Constant;
import localization.common.config.DynamicRuntimeInfo;
import localization.common.config.InfoBuilder;

/**
 * This class is an interface to run script command background
 * 
 * @author Jiajun
 *
 */
public class ExecuteCommand {

	private final String __name__ = "@ExecuteCommand ";

	private final static long TIME_OUT_MINUS = 60;
	private final static long TIME_OUT = 1000 * 60 * TIME_OUT_MINUS; // 1 hour

	private static Process process = null;
	
	public static void deletePathFile() {
		File file = new File(Constant.STR_TMP_INSTR_OUTPUT_FILE);
		if (file.exists()) {
			file.delete();
		}
	}

	public static String moveFile(String source, String target) {
		String[] cmd = new String[] { "/bin/bash", "-c", Constant.COMMAND_MV + source + " " + target };
		return execute(cmd);
	}

	public static String moveFolder(String source, String target) {
		String[] cmd = new String[] { "/bin/bash", "-c", Constant.COMMAND_MV + "-f " + source + " " + target };
		return execute(cmd);
	}

	public static String copyFile(String source, String target) {
		String[] cmd = new String[] { "/bin/bash", "-c", Constant.COMMAND_CP + source + " " + target };
		return execute(cmd);
	}

	public static String deleteDataFiles() {
		String[] cmd = new String[] { "/bin/bash", "-c", Constant.COMMAND_RM + Constant.STR_ALL_DATA_COLLECT_PATH };
		return execute(cmd);
	}

	public static String deleteOutputFile() {
		String[] cmd = new String[] { "/bin/bash", "-c",
				Constant.COMMAND_RM + Constant.STR_OUT_PATH + Constant.PATH_SEPARATOR + "*" };
		return execute(cmd);
	}

	public static String deleteTmpFiles() {
		String[] cmd = new String[] { "/bin/bash", "-c", Constant.COMMAND_RM + Constant.STR_MUTATION_POINT_PATH };
		return execute(cmd);
	}
	
	public static String execute(String... command) {
//		Process process = null;
		String result = null;
		try {
			setTimeOut();
			process = Runtime.getRuntime().exec(command);
			ByteArrayOutputStream resultOutStream = new ByteArrayOutputStream();
			InputStream errorInStream = new BufferedInputStream(process.getErrorStream());
			InputStream processInStream = new BufferedInputStream(process.getInputStream());
			int num = 0;
			byte[] bs = new byte[1024];
			while ((num = errorInStream.read(bs)) != -1) {
				resultOutStream.write(bs, 0, num);
			}
			while ((num = processInStream.read(bs)) != -1) {
				resultOutStream.write(bs, 0, num);
			}
			result = new String(resultOutStream.toByteArray());
			errorInStream.close();
			errorInStream = null;
			processInStream.close();
			processInStream = null;
			resultOutStream.close();
			resultOutStream = null;
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				Debugger.debug("Process interrupted ... ");
			}
		} catch (IOException e) {
			Debugger.debug("Process output redirect exception ... ");
		} finally {
			if (process != null) {
				process.destroy();
			}
			process = null;
		}
		return result;
	}

	public static void newExecutedDefects4JTest(List<String> command) {
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true);
		File file = new File(Constant.STR_TMP_D4J_OUTPUT_FILE);
		processBuilder.redirectOutput(file);
		try {
			Process process = processBuilder.start();
			process.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		DynamicRuntimeInfo _dynamicRuntimeInfo = new DynamicRuntimeInfo("chart", 1);
		String[] cmds = InfoBuilder.buildDefects4JTestCommand(_dynamicRuntimeInfo,
				"org.jfree.chart.renderer.category.junit.AbstractCategoryItemRendererTests", "test2947660");
//		newExecutedDefects4JTest(cmds);
		executeDefects4JTest(cmds, Constant.STR_TMP_D4J_OUTPUT_FILE);
	}

	public static void executeDefects4JTest(String[] command, String outputFilePath) {
//		Process process = null;
		try {
			deletePathFile();
			setTimeOut();
			process = Runtime.getRuntime().exec(command);
			File file = new File(outputFilePath);
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			FileOutputStream resultOutStream = new FileOutputStream(file);
			InputStream errorInStream = new BufferedInputStream(process.getErrorStream());
			InputStream processInStream = new BufferedInputStream(process.getInputStream());
			int num = 0;
			byte[] bs = new byte[1024];
			while ((num = errorInStream.read(bs)) != -1) {
				resultOutStream.write(bs, 0, num);
				resultOutStream.flush();
			}
			while ((num = processInStream.read(bs)) != -1) {
				resultOutStream.write(bs, 0, num);
				resultOutStream.flush();
			}
			errorInStream.close();
			errorInStream = null;
			processInStream.close();
			processInStream = null;
			resultOutStream.close();
			resultOutStream = null;
			
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				Debugger.debug("Process interrupted ... ");
			}
		} catch (IOException e) {
			Debugger.debug("Procss output redirect exception ... ");
		} finally {
			if (process != null) {
				process.destroy();
			}
			process = null;
		}
	}
	
	private static void setTimeOut(){
		Timer timer = new Timer();
		timer.schedule(new KillProcessTask(timer), TIME_OUT);
	}

	private static class KillProcessTask extends TimerTask {

		private Timer _timer;
		public KillProcessTask(Timer timer) {
			_timer = timer;
		}
		
		@Override
		public void run() {
			if(process != null){
				process.destroy();
			}
			if(_timer != null){
				_timer.cancel();
			}
		}
		
	}
}
