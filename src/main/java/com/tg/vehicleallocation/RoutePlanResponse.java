package com.tg.vehicleallocation;

import java.util.*;

public class RoutePlanResponse {

	public Map<String, List<RouteLocation>> allVehiclesRoute = new HashMap<>();

	public List<PickupOrder> droppedOrders = new ArrayList<>();

	/*public Map<String, long[]> orderTypePresentInOrders = new HashMap<>();

	public Map<String, long[]> vehiclesServingOrderType = new HashMap<>();

	public Map<String, long[]> zonePresentInOrders = new HashMap<>();

	public Map<String, long[]> vehiclesServingZone = new HashMap<>();

	public Set<String> allOrderTypes = new HashSet<>();*/
}
