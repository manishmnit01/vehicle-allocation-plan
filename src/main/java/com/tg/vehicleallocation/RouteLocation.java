package com.tg.vehicleallocation;

public class RouteLocation {

	public int sequence;

	public String orderId;

	public ProductPlanItem productPlanItem;

	public int qty;

	public RouteLocation(int sequence, String orderId, ProductPlanItem productPlanItem, int qty) {
		this.sequence = sequence;
		this.orderId = orderId;
		this.productPlanItem = productPlanItem;
		this.qty = qty;
	}

	@Override
	public String toString() {
		return "RouteLocation{" +
				"orderId=" + orderId;
	}
}
