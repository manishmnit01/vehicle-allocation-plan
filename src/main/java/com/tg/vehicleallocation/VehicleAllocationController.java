package com.tg.vehicleallocation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/vap")
public class VehicleAllocationController {

	@Autowired
	private VehicleRoutingSolver vehicleRoutingSolver;

	@Autowired
	private SummaryVehicleAllocationPlanRepository summaryVehicleAllocationPlanRepository;

	@RequestMapping(value = "/plan", method = RequestMethod.POST)
	public VehicleAllocationPlan createPlan(@RequestBody VehicleAllocationPlan vehicleAllocationPlan) throws Exception {

		VehicleAllocationPlan updatedVehicleAllocationPlan = vehicleRoutingSolver.createVehicleAllocationPlan(vehicleAllocationPlan);
		return updatedVehicleAllocationPlan;
	}
}
