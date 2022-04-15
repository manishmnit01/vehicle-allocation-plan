package com.tg.vehicleallocation;

import com.google.ortools.constraintsolver.Assignment;
import com.google.ortools.constraintsolver.RoutingIndexManager;
import com.google.ortools.constraintsolver.RoutingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderAssignmentService {

	@Autowired
	private ProductPlanRepository productPlanRepository;

	@Autowired
	private VehicleRoutingSolver vehicleRoutingSolver;

	public RoutePlanResponse startAssignment(VehicleRoutingInputData data) {
		RoutePlanResponse routePlanResponse = new RoutePlanResponse();
		try {
			RoutingIndexManager manager = new RoutingIndexManager(data.timeMatrix.length, data.vehicleCount, data.depot);
			RoutingModel routing = new RoutingModel(manager);
			Assignment solution = vehicleRoutingSolver.createRouteSolution(data, manager, routing);
			//routePlanResponse.allVehiclesRoute = vehicleRoutingSolver.getVehiclesRoutes(data, routing, manager, solution);
			//routePlanResponse.droppedOrders = data.pickupOrders.stream().filter(order -> order.status == PickupOrderStatus.PENDING).collect(Collectors.toList());
			//vehicleRoutingSolver.updateOrdersStatusInDB(routePlanResponse.allVehiclesRoute, routePlanResponse.droppedOrders, data.vehiclesMap);
			//productPlanItem.dropped = routePlanResponse.droppedOrders.size();
			//productPlanItem.assigned = productPlanItem.total - productPlanItem.dropped;
		} catch (Exception e)  {
			//productPlanItem.status = ProductPlanStatus.FAILED;
			//productPlanItem.failedMessage = e.getMessage();
			e.printStackTrace();
		}

		//ProductPlanItem productPlanItemUpdated = productPlanRepository.update(productPlanItem);
		//productPlanItemUpdated.routePlan = routePlanResponse;
		return routePlanResponse;
	}
}
