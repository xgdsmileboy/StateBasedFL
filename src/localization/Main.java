package localization;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import localization.common.config.Configuration;
import localization.common.config.Constant;
import localization.common.config.DynamicRuntimeInfo;
import localization.common.util.ExecuteCommand;
import localization.common.util.LevelLogger;
import localization.core.path.Collector;

public class Main {

	public static void main(String[] args) {

		SimpleDateFormat df = new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");
		String begin = df.format(new Date());
		System.out.println("Start time : " + begin);
		// two arguments are required
		if (args.length < 2) {
			LevelLogger.error("arguments error!");
			return;
		}
		// initialize configuration from "configure.properties" file
		Configuration.init();
		// if "/out" path exist, backup
		File file = new File(Constant.STR_OUT_PATH);
		if (file.exists()) {
			ExecuteCommand.moveFolder(file.getAbsolutePath(), file.getAbsolutePath() + "_" + begin);
		}
		// instantiate current project info
		DynamicRuntimeInfo dynamicRuntimeInfo = new DynamicRuntimeInfo(args[0], Integer.parseInt(args[1]));
		// finish all configurations before running
		Configuration.config(dynamicRuntimeInfo);
		// start collecting state
		Collector collector = new Collector(dynamicRuntimeInfo);
		collector.collect();

		System.out.println("Start time : " + begin + "  End time : " + df.format(new Date()));
	}
}
