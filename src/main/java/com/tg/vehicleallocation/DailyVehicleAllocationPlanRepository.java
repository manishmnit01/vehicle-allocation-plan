package com.tg.vehicleallocation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	public Map<String,Integer> getTransporterWiseCompletedDistanceInMonth(LocalDate initialDate) {
		LocalDate startDate = initialDate.withDayOfMonth(1);
		LocalDate endDate = initialDate.withDayOfMonth(initialDate.getMonth().length(initialDate.isLeapYear()));

		GroupOperation groupOperation = Aggregation.group("transporter")
				.sum("distance").as("distance");
		MatchOperation matchOperation = Aggregation.match(new Criteria("start_date").gte(startDate).lte(endDate));

		Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation);
		AggregationResults<TransporterCompletedDistance> result = mongoTemplate.aggregate(
				aggregation, DailyVehicleAllocationPlan.class, TransporterCompletedDistance.class);

		List<TransporterCompletedDistance> transporterCompletedDistances = result.getMappedResults();
		Map<String, Integer> transporterDistanceMap = transporterCompletedDistances.stream().collect(Collectors.toMap(tcd -> tcd._id, tcd -> tcd.distance));
		return transporterDistanceMap;
	}
}
