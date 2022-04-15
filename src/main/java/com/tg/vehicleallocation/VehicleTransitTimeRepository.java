package com.tg.vehicleallocation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VehicleTransitTimeRepository {

	@Autowired
	private MongoTemplate mongoTemplate;

	public List<VehicleTransitTime> getVehicleTransitTimeForAllPlaces() {
		return mongoTemplate.findAll(VehicleTransitTime.class);
	}
}
