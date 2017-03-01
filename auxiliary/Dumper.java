package auxiliary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class Dumper {
	
	private static boolean removeNewLine = true;
	private final static int MAX_DEPTH = 4;
	private final static int ARRAY_MAX_LENGTH = 5;
	private final static String OUT_FILE_NAME = "/Users/Jiajun/Code/Java/fault-localization/StateBasedFL/out/path.out";
	private static Dumper instance = new Dumper();

	protected static Dumper getInstance() {
		return instance;
	}

	class DumpContext {
		int maxDepth = 0;
		int maxArrayElements = 0;
		int callCount = 0;
//		HashMap<Object, Integer> visited = new HashMap<Object, Integer>();
		List visited = new ArrayList();
	}
	
	public static boolean write(String message) {
		if (message == null) {
			return false;
		}
		File file = new File(OUT_FILE_NAME); 
		if (!file.exists()) {
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch (IOException e) {
				return false;
			}
		}
		BufferedWriter bufferedWriter = null;
		try {
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
		} catch (IOException e) {
			return false;
		}

		try {
			bufferedWriter.write(message);
			bufferedWriter.write("\n");
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

	public static String dump(Object o) {
		return dump(o, MAX_DEPTH, ARRAY_MAX_LENGTH);
	}
	public static String dump(int o) {
		return dump(Integer.valueOf(o), MAX_DEPTH, ARRAY_MAX_LENGTH);
	}
	public static String dump(float o) {
		return dump(Float.valueOf(o), MAX_DEPTH, ARRAY_MAX_LENGTH);
	}
	public static String dump(double o) {
		return dump(Double.valueOf(o), MAX_DEPTH, ARRAY_MAX_LENGTH);
	}
	public static String dump(long o) {
		return dump(Long.valueOf(o), MAX_DEPTH, ARRAY_MAX_LENGTH);
	}
	public static String dump(byte o) {
		return dump(Byte.valueOf(o), MAX_DEPTH, ARRAY_MAX_LENGTH);
	}
	public static String dump(char o) {
		return dump(Character.valueOf(o), MAX_DEPTH, ARRAY_MAX_LENGTH);
	}
	public static String dump(boolean o) {
		return dump(Boolean.valueOf(o), MAX_DEPTH, ARRAY_MAX_LENGTH);
	}
	
	public static String dump(Object o, int maxDepth, int maxArrayElements) {
		DumpContext ctx = Dumper.getInstance().new DumpContext();
		ctx.maxDepth = maxDepth;
		ctx.maxArrayElements = maxArrayElements;

		String allFields = dump(o, ctx);
		if(removeNewLine){
			allFields = allFields.replace("\n", "");
		}
		
		return allFields;
	}

	protected static String dump(Object o, DumpContext ctx) {
		if (o == null) {
			return "<null>";
		}

		ctx.callCount++;
		StringBuffer tabs = new StringBuffer();
		tabs.append("@"+ctx.callCount+"@");
		StringBuffer buffer = new StringBuffer();
		Class oClass = o.getClass();

		if (oClass.isArray()) {
			buffer.append("[\n");
			int rowCount = ctx.maxArrayElements == 0 ? Array.getLength(o)
					: Math.min(ctx.maxArrayElements, Array.getLength(o));
			for (int i = 0; i < rowCount; i++) {
				try {
					Object value = Array.get(o, i);
					buffer.append(dumpValue(value, ctx, tabs));
				} catch (Exception e) {
					buffer.append(e.getMessage());
				}
				buffer.append("\n");
			}
			buffer.append("]");
		} else {
			buffer.append("{\n");
			while (oClass != null && oClass != Object.class) {
				
				Field[] allFields = oClass.getDeclaredFields();
				
				for(int i = 0; i < allFields.length; i++){
					Field field = allFields[i];
					int modifiers = field.getModifiers();
					if(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers)){
						continue;
					}
					//print variable name information
					String fSimpleName = getSimpleNameWithoutArrayQualifier(field.getType());
					String fName = fields.getName();
					buffer.append("(" + fName + ":" + fSimpleName + ")");
//					buffer.append("(" + fSimpleName + ")");
					
					field.setAccessible(true);
					try {
						Object value = field.get(o);
						buffer.append(dumpValue(value, ctx, tabs));
					} catch (Exception e) {
					}
					buffer.append("\n");
					
				}
				oClass = oClass.getSuperclass();
			}
			buffer.append("}");
		}
		ctx.callCount--;
		return buffer.toString();

	}

	protected static String dumpValue(Object value, DumpContext ctx, StringBuffer tabs) {
		String nullValue = tabs.toString() + "<null>";
		if (value == null) {
			return nullValue;
		}
		if (value.getClass().isPrimitive() || value.getClass() == java.lang.Short.class
				|| value.getClass() == java.lang.Long.class || value.getClass() == java.lang.String.class
				|| value.getClass() == java.lang.Integer.class || value.getClass() == java.lang.Float.class
				|| value.getClass() == java.lang.Byte.class || value.getClass() == java.lang.Character.class
				|| value.getClass() == java.lang.Double.class || value.getClass() == java.lang.Boolean.class
				|| value.getClass() == java.util.Date.class || value.getClass().isEnum()) {

			return tabs.toString() + value.toString();

		} else {
//			Integer visitedIndex = ctx.visited.get(value);
//			if (visitedIndex == null) {
			if(!ctx.visited.contains(value)){
//				ctx.visited.put(value, ctx.callCount);
				ctx.visited.add(value);
				if (ctx.maxDepth == 0 || ctx.callCount < ctx.maxDepth) {
					return dump(value, ctx);
				} else {
//					return "<Reached max recursion depth>";
					return nullValue;
				}
			} else {
//				return "<Previously visited>";
				return nullValue;
			}
		}
	}

	private static String getSimpleNameWithoutArrayQualifier(Class clazz) {
		String simpleName = clazz.getSimpleName();
		int indexOfBracket = simpleName.indexOf('[');
		if (indexOfBracket != -1)
			return simpleName.substring(0, indexOfBracket);
		return simpleName;
	}
}
