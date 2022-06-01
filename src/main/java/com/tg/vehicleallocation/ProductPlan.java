package com.tg.vehicleallocation;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

//@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
//@Document(collection = "product_plans")
public class ProductPlan {

	public String name;

	public String type;

	@Field("product_plan_items")
	public List<ProductPlanItem> productPlanItems;
}
