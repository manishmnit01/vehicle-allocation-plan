package com.tg.vehicleallocation;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;

@Document(collection = "vehicle_transit_time")
public class VehicleTransitTime {

	public String place;

	@Field("empty_tansit_days")
	public int emptyTansitDays;

	@Field("loaded_tansit_days")
	public int loadedTansitDays;

	@Field("total_transit_days")
	public int totalTansitDays;

	@Field("distance")
	public int distance;

	@Field("max_vehicles")
	public int maxVehicles;

	@Field("max_load")
	public Map<String, Integer> maxLoad;

	@Field("weekly_holidays")
	public List<Integer> weeklyHolidays;

	@Field("public_holidays")
	public List<String> publicHolidays;
}
