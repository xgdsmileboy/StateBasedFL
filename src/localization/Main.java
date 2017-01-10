package localization;

import java.text.SimpleDateFormat;
import java.util.Date;

import localization.common.config.DynamicRuntimeInfo;
import localization.common.util.LevelLogger;
import localization.core.path.Collector;

public class Main {
	
	public static void main(String[] args) {
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String began = df.format(new Date());
		System.out.println("Start time : " + began);
	
		if (args.length < 2) {
			LevelLogger.error("arguments error!");
			return;
		}
		
		DynamicRuntimeInfo dynamicRuntimeInfo = new DynamicRuntimeInfo(args[0], Integer.parseInt(args[1]));
		Collector collector = new Collector(dynamicRuntimeInfo);
		collector.collect();
		
		System.out.println("Start time : " + began + "  End time : " + df.format(new Date()));
	}
}
