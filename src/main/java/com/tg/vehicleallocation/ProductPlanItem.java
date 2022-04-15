package com.tg.vehicleallocation;

import org.springframework.data.mongodb.core.mapping.Field;

//@Document(collection = "product_plan_items")
public class ProductPlanItem {

	public String place;

	@Field("total_qty")
	public int totalQty;

	@Field("pending_qty")
	public int pendingQty;

	@Field("lifted_qty")
	public int liftedQty;
}
