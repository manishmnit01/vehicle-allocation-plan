package com.tg.vehicleallocation;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "vehicle_transit_time")
public class VehicleTransitTime {

	public String place;

	@Field("empty_tansit_days")
	public int emptyTansitDays;

	@Field("loaded_tansit_days")
	public int loadedTansitDays;

	@Field("total_transit_days")
	public int totalTansitDays;

	@Field("max_vehicles")
	public int maxVehicles;

	@Field("max_load")
	public int maxLoad;

	@Field("weekly_holidays")
	public List<Integer> weeklyHolidays;

	@Field("public_holidays")
	public List<String> publicHolidays;
}
