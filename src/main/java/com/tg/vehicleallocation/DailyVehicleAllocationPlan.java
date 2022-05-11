package com.tg.vehicleallocation;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;

@Document(collection = "daily_vehicle_allocation_plan")
public class DailyVehicleAllocationPlan {

	@Field("trip_id")
	public String tripId;

	@Field("plan_id")
	public String planId;

	public String product;

	public int qty;

	public String place;

	@Field("start_date")
	public LocalDate startDate;

	@Field("total_transit_time")
	public int totalTransitTime;

	@Field("distance")
	public int distance;

	@Field("pickup_date")
	public LocalDate pickupDate;

	@Field("return_date")
	public LocalDate returnDate;

	@Field("vehicle_number")
	public String vehicleNumber;

	public String transporter;
}
