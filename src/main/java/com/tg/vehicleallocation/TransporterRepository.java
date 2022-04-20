package com.tg.vehicleallocation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class TransporterRepository {

	@Autowired
	private MongoTemplate mongoTemplate;

	public List<Transporter> getVehicleTransitTimeForAllPlaces() {
		return mongoTemplate.findAll(Transporter.class);
	}
}
