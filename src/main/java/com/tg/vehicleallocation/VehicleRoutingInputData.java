package com.tg.vehicleallocation;

import java.time.LocalDate;
import java.util.*;

/** VRPTW. */
public class VehicleRoutingInputData {

  public String product;

  public long[][] timeMatrix;

  List<ProductPlanItem> productPlanItems;

  Map<String,VehicleTransitTime> placeTransitTimeMap;

  LocalDate currentDate;

  List<PickupOrder> pickupOrders;

  Vehicle[] vehicles;

  long[] demands;

  long[] vehicleCapacity;

  Map<Integer, int[]> allowedVehiclesForOrder = new HashMap<>();

  public int vehicleCount;

  public int orderCount;

  public int depot;
}

