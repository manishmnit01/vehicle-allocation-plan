package com.tg;

import com.tg.vehicleallocation.NativeUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableAsync
public class MainApplication {

	private static String OS = System.getProperty("os.name").toLowerCase();

	private static boolean IS_WINDOWS = OS.contains("win");

	private static boolean IS_UNIX = OS.contains("nix") || OS.contains("nux") || OS.contains("aix");

	public static void main(String args[]) {
		SpringApplication.run(MainApplication.class, args);

		// Load native library for google or tools
		try {
			if(IS_WINDOWS) {
				//NativeLoader.loadLibrary("jniortools");
				NativeUtils.loadLibraryFromJar("/natives/windows/jniortools.dll");
			} else if (IS_UNIX) {
				//System.load("/home/ubuntu/vehicle-routing-service/libjniortools.so");
				NativeUtils.loadLibraryFromJar("/natives/linux/libortools.so");
				NativeUtils.loadLibraryFromJar("/natives/linux/libjniortools.so");
			} else {
				throw new RuntimeException("Could not determine operating system.");
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		//builder.setConnectTimeout(100000);
		//builder.setReadTimeout(5000);
		return builder.build();
	}
}
