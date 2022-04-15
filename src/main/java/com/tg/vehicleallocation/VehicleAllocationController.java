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
	private OrderAssignmentService orderAssignmentService;

	@Autowired
	private ProductPlanRepository productPlanRepository;

	@RequestMapping(value = "/plan", method = RequestMethod.POST)
	public ProductPlan createPlan(@RequestBody ProductPlan productPlan) throws Exception {

		ProductPlan updatedProductPlan = vehicleRoutingSolver.createVehicleAllocationPlan(productPlan);
		return updatedProductPlan;
	}
}
