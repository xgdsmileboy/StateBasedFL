package localization.common.config;

import java.util.HashMap;
import java.util.Map;

public class Identifier {
	private static Map<Integer, String> identifiers = new HashMap<>();
	private static Map<String, Integer> inverseIdentifier = new HashMap<>();
	private static Integer counter = 0;
	
	public static void  resetAll(){
		identifiers = new HashMap<>();
		inverseIdentifier = new HashMap<>();
		counter = 0;
	}
	
	public static Integer getIdentifier(String message){
		Integer value = inverseIdentifier.get(message);
		if(value != null){
			return value;
		} else {
			identifiers.put(counter, message);
			inverseIdentifier.put(message, counter);
			counter ++;
			return counter - 1;
		}
	}
	
	public static String getMessage(int id){
		String message = identifiers.get(Integer.valueOf(id));
		if(message == null){
			message = "ERROR";
		}
		return message;
	}
	
}
