package com.tg.vehicleallocation;

public class PickupOrder {

	public PickupOrderStatus status = PickupOrderStatus.PENDING;

	public String pickupId;

	public ProductPlanItem productPlanItem;

	public int qty;

	public PickupOrder(String pickupId, ProductPlanItem productPlanItem, int qty) {
		this.pickupId = pickupId;
		this.productPlanItem = productPlanItem;
		this.qty = qty;
	}

	@Override
	public String toString() {
		return "PickupOrder{" +
				"pickupId='" + pickupId + '\'' +
				'}';
	}
}
