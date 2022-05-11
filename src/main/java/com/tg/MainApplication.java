package com.tg;

import com.mongodb.lang.NonNull;
import com.tg.vehicleallocation.NativeUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.convert.converter.Converter;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

@SpringBootApplication
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
	public MongoCustomConversions customConversions() {
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		List<Converter<?, ?>> converterList = new ArrayList<>();

		converterList.add(new Converter<LocalDate, String>() {
			public String convert(@NonNull LocalDate source) {
				return source.format(dateTimeFormatter);
			}
		});
		converterList.add(new Converter<String, LocalDate>() {
			public LocalDate convert(@NonNull String source) {
				return LocalDate.parse(source, dateTimeFormatter);
			}
		});

		return new MongoCustomConversions(converterList);
	}
}
