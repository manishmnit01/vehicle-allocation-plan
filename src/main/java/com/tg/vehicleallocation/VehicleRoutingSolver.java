package com.tg.vehicleallocation;

import com.google.ortools.constraintsolver.*;
import com.google.protobuf.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class VehicleRoutingSolver {

	private static final int SERVICE_TIME = 20;
	private static final int VEHICLE_SPEED = 20;
	private static final int TIME_LIMIT_SECONDS = 1;
	private static final int BUFFER_MINUTES = 0;
	private static final int MAX_WAIT_TIME = 12*60;
	private static final int MAX_ONDUTY_TIME = 24*60;
	private static final String TIME_DIMENSION = "Time";
	private static final String DISTANCE_DIMENSION = "Distance";
	private static final Map<String, Integer> SERVICE_TIME_MAP;

	static {
		SERVICE_TIME_MAP = new HashMap<>();
		SERVICE_TIME_MAP.put("1", 20);
		SERVICE_TIME_MAP.put("2", 30);
		SERVICE_TIME_MAP.put("3", 20);
		SERVICE_TIME_MAP.put("4", 40);
	}

	@Autowired
	private ProductPlanRepository productPlanRepository;

	@Autowired
	private VehicleRepository vehicleRepository;

	@Autowired
	private VehicleTransitTimeRepository vehicleTransitTimeRepository;

	@Autowired
	private DailyVehicleAllocationPlanRepository dailyVehicleAllocationPlanRepository;

	public ProductPlan createVehicleAllocationPlan(ProductPlan productPlan) {
		//List<ProductPlan> allPendingPlans = productPlanRepository.getPlansByCompanyIdAndStatus("testcompany","CREATED");
		List<Vehicle> vehicleList = vehicleRepository.getActiveVehicles();
		List<VehicleTransitTime> vehicleTransitTimes = vehicleTransitTimeRepository.getVehicleTransitTimeForAllPlaces();
		Map<String,VehicleTransitTime> placeTransitTimeMap = vehicleTransitTimes.stream().collect(Collectors.toMap(tt -> tt.place, tt -> tt));

		LocalDate startDate = productPlan.startDate;
		LocalDate endDate = productPlan.endDate;
		for(ProductPlanItem productPlanItem : productPlan.productPlanItems) {
			productPlanItem.pendingQty = productPlanItem.totalQty;
		}
		productPlan = productPlanRepository.insertProductPlan(productPlan);

		// Set available vehicles for all days.
		Map<LocalDate, Set<Vehicle>> dateWiseAvailableVehicles = new HashMap<>();
		LocalDate prevDate = startDate;
		while (prevDate.isBefore(endDate) || prevDate.equals(endDate)) {
			dateWiseAvailableVehicles.put(prevDate, new HashSet<>(vehicleList));
			prevDate = prevDate.plusDays(1);
		}

		//for (ProductPlan productPlan : allPendingPlans) {
			String product = productPlan.product;
			List<ProductPlanItem> productPlanItems = productPlan.productPlanItems;

			// Create plan for each date.
			LocalDate currentDate = startDate;
			while (currentDate.isBefore(endDate) || currentDate.equals(endDate)) {
				VehicleRoutingInputData data = new VehicleRoutingInputData();
				data.productPlanItems = productPlanItems;
				data.placeTransitTimeMap = placeTransitTimeMap;
				data.currentDate = currentDate;
				data.product = product;
				//Set<Vehicle> availableVehicleSet = dateWiseAvailableVehicles.get(currentDate);
				Set<Vehicle> usedVehicles = dailyVehicleAllocationPlanRepository.getUsedVehiclesOnDay(currentDate);
				data.vehicles = vehicleList.stream()
						.filter(vehicle -> !usedVehicles.contains(vehicle))
						.toArray(Vehicle[]::new);
				//data.vehicles = availableVehicleSet.toArray(new Vehicle[availableVehicleSet.size()]);


				data.vehicleCount = data.vehicles.length;
				if (data.vehicleCount == 0) {
					System.out.println("0 available vehicles on date "+currentDate);
					currentDate = currentDate.plusDays(1);
					continue;
				}

				System.out.println();System.out.println();
				int minVehicleCapacity = getMinCapacity(data.vehicles);
				int daysAvailable = (int)ChronoUnit.DAYS.between(currentDate, endDate);
				if(daysAvailable <= 0) {
					break;
				}
				data.timeMatrix = computeTimeMatrix(data, minVehicleCapacity, daysAvailable);

				data.vehicleCapacity = new long[data.vehicleCount];
				for (int i = 0; i < data.vehicleCount; i++) {
					data.vehicleCapacity[i] = Integer.parseInt(data.vehicles[i].capacity);
				}

				data.demands = new long[data.timeMatrix.length];
				for (int i = 1; i < data.timeMatrix.length; i++) {
					if(i < data.vehicleCount) {
						data.demands[i] = data.vehicleCapacity[i];
					} else {
						data.demands[i] = minVehicleCapacity;
					}

					String placefOrder = data.pickupOrders.get(i-1).pickupId.split("-")[0];
					int maxLoad = placeTransitTimeMap.get(placefOrder).maxLoad;
					int[] allowedVehiclesForOrder = IntStream.range(0, data.vehicles.length)
							.filter(vehicleIndex -> Integer.parseInt(data.vehicles[vehicleIndex].capacity) <= maxLoad)
							.toArray();
					data.allowedVehiclesForOrder.put(i, allowedVehiclesForOrder);
				}

				data.orderCount = data.timeMatrix.length - 1;
				data.depot = 0;

				RoutingIndexManager manager = new RoutingIndexManager(data.timeMatrix.length, data.vehicleCount, data.depot);
				RoutingModel routing = new RoutingModel(manager);
				Assignment solution = this.createRouteSolution(data, manager, routing);
				Map<Vehicle, List<RouteLocation>> allVehiclesRoute = this.getVehiclesRoutes(data, routing, manager, solution);
				//routePlanResponse.droppedOrders = data.pickupOrders.stream().filter(order -> order.status == PickupOrderStatus.PENDING).collect(Collectors.toList());
				List<DailyVehicleAllocationPlan> dailyVehicleAllocationPlans = new ArrayList<>();
				for (Map.Entry<Vehicle, List<RouteLocation>> entry : allVehiclesRoute.entrySet()) {
					List<RouteLocation> routeLocationList = entry.getValue();
					DailyVehicleAllocationPlan dailyPlan = new DailyVehicleAllocationPlan();
					ProductPlanItem productPlanItem = routeLocationList.get(0).productPlanItem;
					dailyPlan.place = productPlanItem.place;
					dailyPlan.tripId = UUID.randomUUID().toString();
					dailyPlan.product =  product;
					dailyPlan.vehicleNumber = entry.getKey().vehicleId;
					dailyPlan.startDate = currentDate;
					dailyPlan.totalTransitTime = placeTransitTimeMap.get(dailyPlan.place).totalTansitDays;
					dailyPlan.returnDate = currentDate.plusDays(dailyPlan.totalTransitTime);
					dailyPlan.planId = productPlan.id;
					LocalDate tempDate = currentDate;
					/*for(int day = 1; day <= dailyPlan.totalTransitTime; day++) {
						tempDate = tempDate.plusDays(1);
						if(tempDate.isBefore(endDate) || tempDate.equals(endDate)) {
							dateWiseAvailableVehicles.get(tempDate).remove(entry.getKey());
						} else {
							break;
						}
					}*/
					int qtyToPick = Math.min(productPlanItem.pendingQty, routeLocationList.get(0).qty);
					dailyPlan.qty = qtyToPick;
					productPlanItem.pendingQty = productPlanItem.pendingQty - qtyToPick;
					productPlanItem.liftedQty = productPlanItem.liftedQty + qtyToPick;
					if(qtyToPick > 0) {
						dailyVehicleAllocationPlans.add(dailyPlan);
					}
				}
				System.out.println("Total used vehicles on date "+currentDate+ " " + allVehiclesRoute.size());
				System.out.println(); System.out.println();
				System.out.println("Total orders were "+ data.pickupOrders.size());
				dailyVehicleAllocationPlanRepository.insert(dailyVehicleAllocationPlans);
				currentDate = currentDate.plusDays(1);
			}

			//productPlanRepository.insertProductPlanItems(productPlanItems);
			productPlan.productPlanItems = productPlanItems;
			productPlan = productPlanRepository.updateProductPlan(productPlan, ProductPlanStatus.COMPLETED, productPlanItems);
		//}
		return productPlan;
	}

	public Assignment createRouteSolution(VehicleRoutingInputData data, RoutingIndexManager manager, RoutingModel routing) {
		// Create and register a transit callback.
		int transitCallbackIndex =
				routing.registerTransitCallback((long fromIndex, long toIndex) -> {
					// Convert from routing variable Index to user NodeIndex.
					int fromNode = manager.indexToNode(fromIndex);
					int toNode = manager.indexToNode(toIndex);
					if (toNode !=0 && data.allowedVehiclesForOrder.get(toNode).length == 0) {
						return 100000;
					}
					return data.timeMatrix[fromNode][toNode];
				});

		// Add Time constraint.
		routing.addDimension(transitCallbackIndex, // transit callback
				MAX_WAIT_TIME, // Maximum waiting time one vehicle can wait from one order to next
				MAX_ONDUTY_TIME, // Time before this vehicle has to reach the end depot.
				true, // start cumul to zero
				TIME_DIMENSION);
		RoutingDimension timeDimension = routing.getMutableDimension(TIME_DIMENSION);
		routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);
		// timeDimension.setGlobalSpanCostCoefficient(1000);

		int capacityCallbackIndex = routing.registerUnaryTransitCallback((long fromIndex) -> {
			int fromNode = manager.indexToNode(fromIndex);
			return data.demands[fromNode];
		});
		routing.addDimensionWithVehicleCapacity(capacityCallbackIndex, 0, // null capacity slack
				data.vehicleCapacity, // vehicle maximum capacities
				true, // start cumul to zero
				"Capacity");

		// Instantiate route start and end times to produce feasible times.
		for (int i = 0; i < data.vehicleCount; ++i) {
			routing.addVariableMinimizedByFinalizer(timeDimension.cumulVar(routing.start(i)));
			routing.addVariableMinimizedByFinalizer(timeDimension.cumulVar(routing.end(i)));
		}

		long penalty = 1000;
		for (int i = 1; i < data.timeMatrix.length; ++i) {
			long orderIndex = manager.nodeToIndex(i);
			routing.addDisjunction(new long[] {orderIndex}, penalty);
			int[] allowedVehiclesForOrder = data.allowedVehiclesForOrder.get(i);
			if(allowedVehiclesForOrder.length > 0) {
				routing.setAllowedVehiclesForIndex(data.allowedVehiclesForOrder.get(i), orderIndex);
			}
		}

		RoutingSearchParameters searchParameters =
				main.defaultRoutingSearchParameters()
						.toBuilder()
						.setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
						.setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
						.setTimeLimit(Duration.newBuilder().setSeconds(TIME_LIMIT_SECONDS).build())
						.build();

		Assignment solution = routing.solveWithParameters(searchParameters);
		return solution;
	}

	public Map<Vehicle, List<RouteLocation>> getVehiclesRoutes(
			VehicleRoutingInputData data, RoutingModel routing, RoutingIndexManager manager, Assignment solution) {
		// Solution cost.
		if(solution == null) {
			throw new RuntimeException("Route is not possible within given time slots");
		}

		// Inspect solution.
		RoutingDimension timeDimension = routing.getMutableDimension(TIME_DIMENSION);
		RoutingDimension distanceDimension = routing.getMutableDimension(DISTANCE_DIMENSION);
		long totalTime = 0;
		Map<Vehicle, List<RouteLocation>> allVehiclesRoute = new HashMap<>();
		for (int i = 0; i < data.vehicleCount; ++i) {
			List<RouteLocation> route = new ArrayList<>();
			long index = routing.start(i);
			IntVar startTimeVar = timeDimension.cumulVar(index);
			int startPointIndex = manager.indexToNode(index);

			index = solution.value(routing.nextVar(index));
			PickupOrder prevOrder = null;
			int prevIndex = -1;
			int currentIndex = 0;
			PickupOrder currentOrder = null;

			while (!routing.isEnd(index)) {
				IntVar timeVar = timeDimension.cumulVar(index);
				prevIndex = currentIndex;
				currentIndex = manager.indexToNode(index);
				prevOrder = currentOrder;
				currentOrder = data.pickupOrders.get(currentIndex - 1);
				currentOrder.status = PickupOrderStatus.CONFIRMED;
				int qtyToPick = Integer.parseInt(data.vehicles[i].capacity);
				RouteLocation routeLocation = new RouteLocation(currentIndex, currentOrder.pickupId, currentOrder.productPlanItem, qtyToPick);
				route.add(routeLocation);
				index = solution.value(routing.nextVar(index));
			}
			if(!route.isEmpty()) {
				allVehiclesRoute.put(data.vehicles[i], route);
			}
		}
		return allVehiclesRoute;
	}

	private static long[][] computeTimeMatrix(VehicleRoutingInputData data, int minVehicleCapacity, int daysAvailable) {
		//int totalSize = orderSize+1;
		int index = 1;
		//Map<Integer, String> indexPlaceMap = new HashMap<>();
		data.pickupOrders = new ArrayList<>();
		for(ProductPlanItem productPlanItem : data.productPlanItems) {
			int pendingQty = (productPlanItem.pendingQty)/daysAvailable;
			int totalSource = pendingQty / minVehicleCapacity;
			if(pendingQty % minVehicleCapacity != 0) {
				totalSource++;
			}

			totalSource = Math.min(totalSource, data.placeTransitTimeMap.get(productPlanItem.place).maxVehicles);
			int dayOfWeek = data.currentDate.getDayOfWeek().getValue();
			List<Integer> weeklyHolidays = data.placeTransitTimeMap.get(productPlanItem.place).weeklyHolidays;
			if(weeklyHolidays != null && weeklyHolidays.contains(dayOfWeek)) {
				totalSource = 0;
			}

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
			String currentDateString = data.currentDate.format(formatter);
			List<String> publicHolidays = data.placeTransitTimeMap.get(productPlanItem.place).publicHolidays;
			if(publicHolidays != null && publicHolidays.contains(currentDateString)) {
				totalSource = 0;
			}

			for(int i=1; i <= totalSource; i++) {
				String pickupId = productPlanItem.place + "-" + i;
				int orderCapacity = minVehicleCapacity;
				if(i == totalSource) {
					orderCapacity = pendingQty % minVehicleCapacity;
				}
				data.pickupOrders.add(new PickupOrder(pickupId, productPlanItem, orderCapacity));
				index++;
			}
		}
		int totalSize = index;

		long[][] timeMatrix = new long[totalSize][totalSize];
		for (int fromNode = 0; fromNode < totalSize; ++fromNode) {
			for (int toNode = 0; toNode < totalSize; ++toNode) {
				if(fromNode == 0 || toNode == 0) {
					timeMatrix[fromNode][toNode] = 100;
				} else {
					String fromPlace = data.pickupOrders.get(fromNode-1).productPlanItem.place;
					String toPlace = data.pickupOrders.get(toNode-1).productPlanItem.place;
					if (fromPlace.equals(toPlace)) {
						timeMatrix[fromNode][toNode] = 0;
					} else {
						timeMatrix[fromNode][toNode] = 100000;
					}
				}
			}
		}

		return timeMatrix;
	}

	public static int getMinCapacity(Vehicle[] vehicles){
		int minCapacity = Integer.parseInt(vehicles[0].capacity);
		for(int i=1; i<vehicles.length; i++) {
			minCapacity = Math.min(minCapacity, Integer.parseInt(vehicles[i].capacity));
		}
		return minCapacity;
	}
}
