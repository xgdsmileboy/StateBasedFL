package localization.common.java;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import localization.common.util.Debugger;

/**
 * This class contains some auxiliary methods that provide convenience
 * 
 * @author Jiajun
 *
 */

public class JavaFile {

	private final static String __name__ = "@JavaFile ";

	/**
	 * generate {@code CompilationUnit} from {@code ICompilationUnit}
	 * 
	 * @param icu
	 * @return
	 */
	public static CompilationUnit genASTFromICU(ICompilationUnit icu) {
		ASTParser astParser = ASTParser.newParser(AST.JLS8);
		Map<?, ?> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
		astParser.setCompilerOptions(options);
		astParser.setSource(icu);
		astParser.setKind(ASTParser.K_COMPILATION_UNIT);
		astParser.setResolveBindings(true);
		return (CompilationUnit) astParser.createAST(null);
	}

	/**
	 * generate {@code CompilationUnit} from source code based on the specific
	 * type (e.g., {@code ASTParser.K_COMPILATION_UNIT})
	 * 
	 * @param icu
	 * @param type
	 * @return
	 */
	public static CompilationUnit genASTFromSource(String icu, int type) {
		ASTParser astParser = ASTParser.newParser(AST.JLS8);
		Map<?, ?> options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
		astParser.setCompilerOptions(options);
		astParser.setSource(icu.toCharArray());
		astParser.setKind(type);
		astParser.setResolveBindings(true);
		return (CompilationUnit) astParser.createAST(null);
	}

	/**
	 * write {@code string} into file with mode as "not append"
	 * 
	 * @param filePath
	 *            : path of file
	 * @param string
	 *            : message
	 * @return
	 */
	public static boolean writeStringToFile(String filePath, String string) {
		return writeStringToFile(filePath, string, false);
	}

	/**
	 * write {@code string} to file with mode as "not append"
	 * 
	 * @param file
	 *            : file of type {@code File}
	 * @param string
	 *            : message
	 * @return
	 */
	public static boolean writeStringToFile(File file, String string) {
		return writeStringToFile(file, string, false);
	}

	/**
	 * write {@code string} into file with specific mode
	 * 
	 * @param filePath
	 *            : file path
	 * @param string
	 *            : message
	 * @param append
	 *            : writing mode
	 * @return
	 */
	public static boolean writeStringToFile(String filePath, String string, boolean append) {
		if (filePath == null) {
			if (Debugger.debugOn()) {
				Debugger.debug(__name__ + "#writeStringToFile Illegal file path : null.");
			}
			return false;
		}
		File file = new File(filePath);
		return writeStringToFile(file, string, append);
	}

	/**
	 * write {@code string} into file with specific mode
	 * 
	 * @param file
	 *            : file of type {@code File}
	 * @param string
	 *            : message
	 * @param append
	 *            : writing mode
	 * @return
	 */
	public static boolean writeStringToFile(File file, String string, boolean append) {
		if (file == null || string == null) {
			if (Debugger.debugOn()) {
				Debugger.debug(__name__ + "#writeStringToFile Illegal arguments : null.");
			}
			return false;
		}
		if (!file.exists()) {
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch (IOException e) {
				if (Debugger.debugOn()) {
					Debugger.debug(__name__ + "#writeStringToFile Create new file failed : " + file.getAbsolutePath());
				}
				return false;
			}
		}
		BufferedWriter bufferedWriter = null;
		try {
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), "UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			bufferedWriter.write(string);
			bufferedWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bufferedWriter != null) {
				try {
					bufferedWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}

	/**
	 * read string from file
	 * 
	 * @param filePath
	 *            : file path
	 * @return : string in the file
	 */
	public static String readFileToString(String filePath) {
		if (filePath == null) {
			if (Debugger.debugOn()) {
				Debugger.debug(__name__ + "#readFileToString Illegal input file path : null.");
			}
			return new String();
		}
		File file = new File(filePath);
		if (!file.exists() || !file.isFile()) {
			if (Debugger.debugOn()) {
				Debugger.debug(__name__ + "#readFileToString Illegal input file path : " + filePath);
			}
			return new String();
		}
		return readFileToString(file);
	}

	/**
	 * read string from file
	 * 
	 * @param file
	 *            : file of type {@code File}
	 * @return : string in the file
	 */
	public static String readFileToString(File file) {
		if (file == null) {
			if (Debugger.debugOn()) {
				Debugger.debug(__name__ + "#readFileToString Illegal input file : null.");
			}
			return new String();
		}
		StringBuffer stringBuffer = new StringBuffer();
		InputStream in = null;
		InputStreamReader inputStreamReader = null;
		try {
			in = new FileInputStream(file);
			inputStreamReader = new InputStreamReader(in, "UTF-8");
			char[] ch = new char[1024];
			int readCount = 0;
			while((readCount = inputStreamReader.read(ch)) != -1){
				stringBuffer.append(ch, 0, readCount);
			}
			inputStreamReader.close();
			in.close();
			
		} catch (Exception e) {
			if (inputStreamReader != null) {
				try {
					inputStreamReader.close();
				} catch (IOException e1) {
					return new String();
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (IOException e1) {
					return new String();
				}
			}
		}
		return stringBuffer.toString();
	}

	/**
	 * iteratively search files with the root as {@code file}
	 * 
	 * @param file
	 *            : root file of type {@code File}
	 * @param fileList
	 *            : list to save all the files
	 * @return : a list of all files
	 */
	public static List<File> ergodic(File file, List<File> fileList) {
		if (file == null) {
			if (Debugger.debugOn()) {
				Debugger.debug(__name__ + "#ergodic Illegal input file : null.");
			}
			return fileList;
		}
		File[] files = file.listFiles();
		if (files == null)
			return fileList;
		for (File f : files) {
			if (f.isDirectory()) {
				ergodic(f, fileList);
			} else if (f.getName().endsWith(".java"))
				fileList.add(f);
		}
		return fileList;
	}

	/**
	 * iteratively search the file in the given {@code directory}
	 * 
	 * @param directory
	 *            : root directory
	 * @param fileList
	 *            : list of file
	 * @return : a list of file
	 */
	public static List<String> ergodic(String directory, List<String> fileList) {
		if (directory == null) {
			if (Debugger.debugOn()) {
				Debugger.debug(__name__ + "#ergodic Illegal input file : null.");
			}
			return fileList;
		}
		File file = new File(directory);
		File[] files = file.listFiles();
		if (files == null)
			return fileList;
		for (File f : files) {
			if (f.isDirectory()) {
				ergodic(f.getAbsolutePath(), fileList);
			} else if (f.getName().endsWith(".java"))
				fileList.add(f.getAbsolutePath());
		}
		return fileList;
	}

}