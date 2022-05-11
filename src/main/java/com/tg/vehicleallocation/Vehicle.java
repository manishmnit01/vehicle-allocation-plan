package com.tg.vehicleallocation;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Objects;

@Document(collection = "vehicle_master")
public class Vehicle {

	@Field("Vehicle No_")
	public String vehicleId;

	@Field("Max PESO Capacity")
	public String capacity;

	@Field("Running status")
	public String status;

	@Field("Transporter Name")
	public String transporter;

	public Vehicle(String vehicleId) {
		this.vehicleId = vehicleId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Vehicle)) return false;
		Vehicle vehicle = (Vehicle) o;
		return Objects.equals(vehicleId, vehicle.vehicleId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(vehicleId);
	}

	@Override
	public String toString() {
		return vehicleId;
	}
}
