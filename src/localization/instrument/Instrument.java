package localization.instrument;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;

import localization.common.java.JavaFile;
import localization.common.util.Debugger;
import localization.instrument.visitor.TraversalVisitor;

/**
 * This class is a director for instrument, it will instrument or remove
 * instrument for one or more files based on the given information
 * 
 * @author Jiajun
 *
 */
public class Instrument {

	private final static String __name__ = "@Instrument ";

	public static boolean execute(String path, TraversalVisitor traversalVisitor) {
		if (path == null || path.length() <= 1) {
			if (Debugger.debugOn()) {
				Debugger.debug(__name__ + "#execute illegal input file : " + path);
			}
			return false;
		}
		File file = new File(path);
		if (!file.exists()) {
			if (Debugger.debugOn()) {
				Debugger.debug(__name__ + "#execute input file not exist : " + path);
			}
			return false;
		}
		List<File> fileList = new ArrayList<>();
		if (file.isDirectory()) {
			fileList = JavaFile.ergodic(file, fileList);
		} else if (file.isFile()) {
			fileList.add(file);
		} else {
			if (Debugger.debugOn()) {
				Debugger.debug(
						__name__ + "#execute input file is not a file nor directory : " + file.getAbsolutePath());
			}
			return false;
		}

		for (File f : fileList) {
			String source = JavaFile.readFileToString(f);
			CompilationUnit unit = JavaFile.genASTFromSource(source, ASTParser.K_COMPILATION_UNIT);
			if(unit == null){
				continue;
			}
			unit.accept(traversalVisitor);
			Formatter formatter = new Formatter();
			String formatSource = null;
			try {
				formatSource = formatter.formatSource(unit.toString());
			} catch (FormatterException e) {
				System.err.println(__name__ + "#execute Format Code Error for : " + f.getAbsolutePath());
				formatSource = unit.toString();
			}
			JavaFile.writeStringToFile(f, formatSource);
		}
		return true;
	}
}
