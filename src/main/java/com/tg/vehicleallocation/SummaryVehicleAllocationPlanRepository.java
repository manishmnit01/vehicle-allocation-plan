package com.tg.vehicleallocation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SummaryVehicleAllocationPlanRepository {

	@Autowired
	private MongoTemplate mongoTemplate;

	public VehicleAllocationPlan insertVehicleAllocationPlan(VehicleAllocationPlan vehicleAllocationPlan) {
		return mongoTemplate.insert(vehicleAllocationPlan);
	}

	public VehicleAllocationPlan updateVehicleAllocationPlan(VehicleAllocationPlan vehicleAllocationPlan, ProductPlanStatus productPlanStatus) {

		Criteria criteria = Criteria.where("_id").is(vehicleAllocationPlan.id);
		Query query = new Query(criteria);
		Update update = new Update();
		update.set("status", productPlanStatus);
		update.set("product_plans",vehicleAllocationPlan.productPlans);
		return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), VehicleAllocationPlan.class);
	}

	public List<VehicleAllocationPlan> getAllocationPlansByCompanyIdAndStatus(String companyId, String status) {
		Criteria criteria = Criteria.where("company_id").is(companyId).and("status").is(status);
		Query query = new Query(criteria);
		return mongoTemplate.find(query, VehicleAllocationPlan.class);
	}
}
