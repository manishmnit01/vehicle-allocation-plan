package com.tg.vehicleallocation;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Document(collection = "summary_vehicle_allocation_plan")
public class VehicleAllocationPlan {

	@Id
	public String id;

	@Field("company_id")
	public String companyId;

	@Field("start_date")
	public LocalDate startDate;

	@Field("end_date")
	public LocalDate endDate;

	@Field("product_plans")
	public List<ProductPlan> productPlans;

	public ProductPlanStatus status = ProductPlanStatus.PENDING;

	public Date created = new Date();
}
