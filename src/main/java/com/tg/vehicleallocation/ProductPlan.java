package com.tg.vehicleallocation;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

//@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Document(collection = "product_plans")
public class ProductPlan {

	@Id
	public String id;

	@Field("company_id")
	public String companyId;

	public ProductPlanStatus status = ProductPlanStatus.PENDING;

	public String product;

	@Field("product_type")
	public String productType;

	@Field("start_date")
	public LocalDate startDate;

	@Field("end_date")
	public LocalDate endDate;

	@Field("product_plan_items")
	public List<ProductPlanItem> productPlanItems;

	public Date created = new Date();
}
