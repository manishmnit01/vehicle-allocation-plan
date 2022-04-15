package com.tg.vehicleallocation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class DailyVehicleAllocationPlanRepository {

	@Autowired
	private MongoTemplate mongoTemplate;

	public void insert(List<DailyVehicleAllocationPlan> dailyPlan) {
		mongoTemplate.insert(dailyPlan, DailyVehicleAllocationPlan.class);
	}

	public Set<Vehicle> getUsedVehiclesOnDay(LocalDate currentDate) {
		Criteria criteria = Criteria.where("start_date").lte(currentDate).and("return_date").gte(currentDate);
		Query query = new Query(criteria);
		List<DailyVehicleAllocationPlan> usedVehicles = mongoTemplate.find(query, DailyVehicleAllocationPlan.class);
		return usedVehicles.stream().map(dva -> new Vehicle(dva.vehicleNumber)).collect(Collectors.toSet());
	}
}
