package com.tg.vehicleallocation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VehicleRepository {

	@Autowired
	private MongoTemplate mongoTemplate;

	public List<Vehicle> getActiveVehicles() {
		Criteria criteria = Criteria.where("Running status").is("Active");
		Query query = new Query(criteria);
		return mongoTemplate.find(query, Vehicle.class);
	}
}
