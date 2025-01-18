package xyz.elspeth;

import discord4j.common.util.Snowflake;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class Constants {
	
	public static String TOKEN            = null;
	public static String postgresUrl      = null;
	public static String postgresUsername = null;
	public static String postgresPassword = null;
	
	public static Snowflake DAILY_CHANNEL = null;
	public static Snowflake MOD_CHANNEL   = null;
	public static long      BABAMOTES     = -1;
	
	public static void load() throws IOException {
		
		File file = new File("./config.properties");
		if (!file.exists()) {
			var stream = ClassLoader.getSystemClassLoader()
									.getResourceAsStream("application.properties");
			if (stream == null) {
				throw new IOException("application.properties not found");
			}
			Files.copy(stream, file.toPath());
			System.out.println("Config was not found. Created new config at " + file.getAbsolutePath());
			System.exit(1);
		}
		
		Properties properties = new Properties();
		
		properties.load(new FileInputStream(file));
		
		Constants.TOKEN = properties.get("TOKEN")
									.toString();
		Constants.postgresUrl = properties.get("POSTGRES_URL")
										  .toString();
		Constants.postgresUsername = properties.get("POSTGRES_USER")
											   .toString();
		Constants.postgresPassword = properties.get("POSTGRES_PASSWORD")
											   .toString();
		Constants.DAILY_CHANNEL = Snowflake.of(Long.parseLong(properties.get("DAILY_CHANNEL_ID")
																		.toString()));
		Constants.MOD_CHANNEL = Snowflake.of(Long.parseLong(properties.get("MOD_CHANNEL_ID")
																	  .toString()));
		Constants.BABAMOTES = Long.parseLong(properties.get("BABAMOTES_ID")
													   .toString());
		
		if (System.getenv()
				  .containsKey("SECURITY_DEL_CONF")) {
			file.delete();
		}
	}
	
}
