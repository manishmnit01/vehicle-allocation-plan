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

	private static final int TIME_LIMIT_SECONDS = 1;
	private static final int MAX_WAIT_TIME = 12*60;
	private static final int MAX_ONDUTY_TIME = 24*60;
	private static final String TIME_DIMENSION = "Time";

	@Autowired
	private SummaryVehicleAllocationPlanRepository summaryVehicleAllocationPlanRepository;

	@Autowired
	private VehicleRepository vehicleRepository;

	@Autowired
	private VehicleTransitTimeRepository vehicleTransitTimeRepository;

	@Autowired
	private TransporterContractRepository transporterContractRepository;

	@Autowired
	private DailyVehicleAllocationPlanRepository dailyVehicleAllocationPlanRepository;

	public VehicleAllocationPlan createVehicleAllocationPlan(VehicleAllocationPlan vehicleAllocationPlan) {
		List<Vehicle> vehicleList = vehicleRepository.getActiveVehicles();
		List<VehicleTransitTime> vehicleTransitTimes = vehicleTransitTimeRepository.getVehicleTransitTimeForAllPlaces();
		Map<String,VehicleTransitTime> placeTransitTimeMap = vehicleTransitTimes.stream().collect(Collectors.toMap(vtt -> vtt.place, vtt -> vtt));

		List<TransporterContract> transporterContracts = transporterContractRepository.getAllTransportContracts();
		Map<String, Integer> transporterContractMap = transporterContracts.stream().collect(Collectors.toMap(tc -> tc.name, tc -> tc.monthlyAllowedDistance));
		Map<String, Integer> transporterDistanceMap = dailyVehicleAllocationPlanRepository.getTransporterWiseCompletedDistanceInMonth(vehicleAllocationPlan.startDate);

		LocalDate startDate = vehicleAllocationPlan.startDate;
		LocalDate endDate = vehicleAllocationPlan.endDate;
		for(ProductPlan productPlan : vehicleAllocationPlan.productPlans) {
			for (ProductPlanItem productPlanItem : productPlan.productPlanItems) {
				productPlanItem.pendingQty = productPlanItem.totalQty;
				productPlanItem.liftedQty = 0;
			}
		}
		vehicleAllocationPlan = summaryVehicleAllocationPlanRepository.insertVehicleAllocationPlan(vehicleAllocationPlan);

		// Create plan for each date.
		LocalDate currentDate = startDate;
		while (currentDate.isBefore(endDate) || currentDate.equals(endDate)) {
			for(ProductPlan productPlan : vehicleAllocationPlan.productPlans) {
				String productName = productPlan.name;
				List<ProductPlanItem> productPlanItems = productPlan.productPlanItems;
				VehicleRoutingInputData data = new VehicleRoutingInputData();
				data.productPlanItems = productPlanItems;
				data.placeTransitTimeMap = placeTransitTimeMap;
				data.currentDate = currentDate;

				Set<Vehicle> usedVehicles = dailyVehicleAllocationPlanRepository.getUsedVehiclesOnDay(currentDate);
				Set<String> notAllowedTransporters = transporterContractMap.keySet().stream()
						.filter(transporter -> transporterDistanceMap.getOrDefault(transporter, 0) >= transporterContractMap.get(transporter))
						.collect(Collectors.toSet());

				data.vehicles = vehicleList.stream()
						.filter(vehicle -> !usedVehicles.contains(vehicle) && !notAllowedTransporters.contains(vehicle.transporter))
						.toArray(Vehicle[]::new);

				data.vehicleCount = data.vehicles.length;
				if (data.vehicleCount == 0) {
					System.out.println(String.format("Date: %s, Product: %s, Available Vehicle Count: 0", currentDate, productName));
					continue;
				}

				int minVehicleCapacity = getMinCapacity(data.vehicles);
				int daysAvailable = (int) ChronoUnit.DAYS.between(currentDate, endDate) + 1;

				Map<String, Set<Vehicle>> placeToNotAvailableVehiclesMap = new HashMap<>(data.productPlanItems.size());
				for (ProductPlanItem productPlanItem : data.productPlanItems) {
					int transitDaysForPlace = placeTransitTimeMap.get(productPlanItem.place).totalTansitDays;
					Set<Vehicle> notAvailableVehiclesForPlace = dailyVehicleAllocationPlanRepository.getVehiclesNotAvailableForThePlace(currentDate, transitDaysForPlace);
					placeToNotAvailableVehiclesMap.put(productPlanItem.place, notAvailableVehiclesForPlace);
				}

				data.timeMatrix = computeTimeMatrix(data, minVehicleCapacity, daysAvailable);

				data.vehicleCapacity = new long[data.vehicleCount];
				for (int i = 0; i < data.vehicleCount; i++) {
					data.vehicleCapacity[i] = Integer.parseInt(data.vehicles[i].capacity);
				}

				data.demands = new long[data.timeMatrix.length];
				for (int i = 1; i < data.timeMatrix.length; i++) {
					if (i < data.vehicleCount) {
						data.demands[i] = data.vehicleCapacity[i];
					} else {
						data.demands[i] = minVehicleCapacity;
					}

					String placeOfOrder = data.pickupOrders.get(i - 1).pickupId.split("-")[0];
					Map<String, Integer> productMaxLoadMap = placeTransitTimeMap.get(placeOfOrder).maxLoad;
					if (productMaxLoadMap != null && productMaxLoadMap.containsKey(productName)) {
						int[] allowedVehiclesForOrder = IntStream.range(0, data.vehicles.length)
								.filter(vehicleIndex -> {
									Vehicle vehicle = data.vehicles[vehicleIndex];
									return Integer.parseInt(vehicle.capacity) <= productMaxLoadMap.get(productName)
											&& !placeToNotAvailableVehiclesMap.get(placeOfOrder).contains(vehicle);
								}).toArray();
						data.allowedVehiclesForOrder.put(i, allowedVehiclesForOrder);
					} else {
						data.allowedVehiclesForOrder.put(i, IntStream.range(0, data.vehicles.length).toArray());
					}
				}

				data.orderCount = data.timeMatrix.length - 1;
				data.depot = 0;

				RoutingIndexManager manager = new RoutingIndexManager(data.timeMatrix.length, data.vehicleCount, data.depot);
				RoutingModel routing = new RoutingModel(manager);
				Assignment solution = this.createRouteSolution(data, manager, routing);
				Map<Vehicle, RouteLocation> allVehiclesRoute = this.getVehiclesRoutes(data, routing, manager, solution);
				//routePlanResponse.droppedOrders = data.pickupOrders.stream().filter(order -> order.status == PickupOrderStatus.PENDING).collect(Collectors.toList());
				List<DailyVehicleAllocationPlan> dailyVehicleAllocationPlans = new ArrayList<>();
				List<String> list = new ArrayList<>();
				for (Map.Entry<Vehicle, RouteLocation> entry : allVehiclesRoute.entrySet()) {
					Vehicle vehicle = entry.getKey();
					String transporter = vehicle.transporter;
					RouteLocation routeLocation = entry.getValue();
					DailyVehicleAllocationPlan dailyPlan = new DailyVehicleAllocationPlan();
					ProductPlanItem productPlanItem = routeLocation.productPlanItem;
					dailyPlan.place = productPlanItem.place;
					dailyPlan.tripId = UUID.randomUUID().toString();
					dailyPlan.product = productName;
					dailyPlan.vehicleNumber = entry.getKey().vehicleId;
					dailyPlan.transporter = transporter;
					dailyPlan.startDate = currentDate;
					dailyPlan.distance = placeTransitTimeMap.get(dailyPlan.place).distance;
					dailyPlan.totalTransitTime = placeTransitTimeMap.get(dailyPlan.place).totalTansitDays;
					dailyPlan.distance = placeTransitTimeMap.get(dailyPlan.place).distance;
					dailyPlan.returnDate = currentDate.plusDays(dailyPlan.totalTransitTime);
					dailyPlan.pickupDate = currentDate.plusDays(placeTransitTimeMap.get(dailyPlan.place).emptyTansitDays);
					dailyPlan.planId = vehicleAllocationPlan.id;
					int qtyToPick = Math.min(productPlanItem.pendingQty, routeLocation.qty);
					transporterDistanceMap.put(transporter, transporterDistanceMap.getOrDefault(transporter, 0) + dailyPlan.distance);
					if (transporterContractMap.containsKey(transporter) && transporterDistanceMap.get(transporter) > transporterContractMap.get(transporter)) {
						continue;
					}
					dailyPlan.qty = qtyToPick;
					productPlanItem.pendingQty = productPlanItem.pendingQty - qtyToPick;
					productPlanItem.liftedQty = productPlanItem.liftedQty + qtyToPick;
					if (qtyToPick > 0) {
						dailyVehicleAllocationPlans.add(dailyPlan);
					}
					list.add(routeLocation.orderId);
				}
				System.out.println(String.format("Date: %s, Product: %s, OrderCount: %d, VehicleCount: %d", currentDate, productName, data.pickupOrders.size(), allVehiclesRoute.size()));
				//System.out.println(Arrays.toString(list.toArray(new String[list.size()])));
				dailyVehicleAllocationPlanRepository.insert(dailyVehicleAllocationPlans);
			}
			System.out.println();
			currentDate = currentDate.plusDays(1);
		}

		vehicleAllocationPlan = summaryVehicleAllocationPlanRepository.updateVehicleAllocationPlan(vehicleAllocationPlan, ProductPlanStatus.COMPLETED);
		System.out.println("Vehicle assignment completed!!");
		return vehicleAllocationPlan;
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

	public Map<Vehicle, RouteLocation> getVehiclesRoutes(
			VehicleRoutingInputData data, RoutingModel routing, RoutingIndexManager manager, Assignment solution) {
		if(solution == null) {
			throw new RuntimeException("Route is not possible within given time slots");
		}

		Map<Vehicle, RouteLocation> allVehiclesRoute = new HashMap<>();
		for (int i = 0; i < data.vehicleCount; ++i) {
			List<RouteLocation> route = new ArrayList<>();
			long index = routing.start(i); // Depot
			index = solution.value(routing.nextVar(index)); // Source place

			if (!routing.isEnd(index)) {
				int currentIndex = manager.indexToNode(index);
				PickupOrder currentOrder = data.pickupOrders.get(currentIndex - 1);
				currentOrder.status = PickupOrderStatus.CONFIRMED;
				int qtyToPick = Integer.parseInt(data.vehicles[i].capacity);
				RouteLocation routeLocation = new RouteLocation(currentIndex, currentOrder.pickupId, currentOrder.productPlanItem, qtyToPick);
				allVehiclesRoute.put(data.vehicles[i], routeLocation);
			}
		}
		return allVehiclesRoute;
	}

	private static long[][] computeTimeMatrix(VehicleRoutingInputData data, int minVehicleCapacity, int daysAvailable) {
		//int totalSize = orderSize+1;
		int index = 1;
		data.pickupOrders = new ArrayList<>();
		for(ProductPlanItem productPlanItem : data.productPlanItems) {
			int pendingQty = productPlanItem.pendingQty;
			int totalSource = pendingQty / minVehicleCapacity;
			if(pendingQty % minVehicleCapacity != 0) {
				totalSource++;
			}

			//System.out.println("Date "+data.currentDate+" original total source "+ totalSource +" place "+productPlanItem.place );

			totalSource = Math.min(totalSource, data.placeTransitTimeMap.get(productPlanItem.place).maxVehicles);
			int reachPlaceTransitDays = data.placeTransitTimeMap.get(productPlanItem.place).emptyTansitDays;
			int dayOfWeekToReachPlace = data.currentDate.plusDays(reachPlaceTransitDays).getDayOfWeek().getValue();
			List<Integer> weeklyHolidays = data.placeTransitTimeMap.get(productPlanItem.place).weeklyHolidays;
			if(weeklyHolidays != null && weeklyHolidays.contains(dayOfWeekToReachPlace)) {
				totalSource = 0;
			}

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
			String reachDateString = data.currentDate.plusDays(reachPlaceTransitDays).format(formatter);
			List<String> publicHolidays = data.placeTransitTimeMap.get(productPlanItem.place).publicHolidays;
			if(publicHolidays != null && publicHolidays.contains(reachDateString)) {
				totalSource = 0;
			}

			//System.out.println("Date "+data.currentDate+" final total source "+ totalSource +" place "+productPlanItem.place );

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
					// If source or destination is depot then set a fixed cost.
					timeMatrix[fromNode][toNode] = 100;
				} else {
					String fromPlace = data.pickupOrders.get(fromNode-1).productPlanItem.place;
					String toPlace = data.pickupOrders.get(toNode-1).productPlanItem.place;
					if (fromPlace.equals(toPlace)) {
						timeMatrix[fromNode][toNode] = 0;
					} else {
						// vehicle not allowed to visit only 1 node from depot.
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
