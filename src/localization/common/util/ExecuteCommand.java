package localization.common.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import localization.common.config.Constant;

/**
 * This class is an interface to run script command background
 * 
 * @author Jiajun
 *
 */
public class ExecuteCommand {

	private final String __name__ = "@ExecuteCommand ";
	
	public static String moveFile(String source, String target){
		String[] cmd = new String[]{"/bin/bash", "-c", Constant.COMMAND_MV + source + " " + target};
		return execute(cmd);
	}
	
	public static String moveFolder(String source, String target){
		String[] cmd = new String[]{"/bin/bash", "-c", Constant.COMMAND_MV + "-f " + source + " " + target};
		return execute(cmd);
	}
	
	public static String copyFile(String source, String target){
		String[] cmd = new String[]{"/bin/bash", "-c", Constant.COMMAND_CP + source + " " + target};
		return execute(cmd);
	}

	public static String deleteDataFiles() {
		String[] cmd = new String[] { "/bin/bash", "-c", Constant.COMMAND_RM + Constant.STR_ALL_DATA_COLLECT_PATH };
		return execute(cmd);
	}
	
	public static String deleteOutputFile(){
		String[] cmd = new String[] { "/bin/bash", "-c", Constant.COMMAND_RM + Constant.STR_OUT_PATH +Constant.PATH_SEPARATOR + "*" };
		return execute(cmd);
	}
	
	public static String deleteTmpFiles() {
		String[] cmd = new String[] { "/bin/bash", "-c", Constant.COMMAND_RM + Constant.STR_MUTATION_POINT_PATH };
		return execute(cmd);
	}

	public static String execute(String... command) {
		Process process = null;
		String result = null;
		try {
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
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (process != null) {
				process.destroy();
			}
			process = null;
		}
		return result;
	}
	
	public static void executeDefects4JTest(String[] command, String outputFilePath) {
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(command);
			File file = new File(outputFilePath);
			if(!file.exists()){
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
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (process != null) {
				process.destroy();
			}
			process = null;
		}
	}
}
