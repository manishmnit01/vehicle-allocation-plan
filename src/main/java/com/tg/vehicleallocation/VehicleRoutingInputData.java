package com.tg.vehicleallocation;

import java.time.LocalDate;
import java.util.*;

/** VRPTW. */
public class VehicleRoutingInputData {

  public String product;

  public long[][] timeMatrix;

  //public long[][] distanceMatrix;

  //public long[][] timeWindows;

  List<ProductPlanItem> productPlanItems;

  Map<String,VehicleTransitTime> placeTransitTimeMap;

  LocalDate currentDate;

  List<PickupOrder> pickupOrders;

  Vehicle[] vehicles;

  long[] demands;

  long[] vehicleCapacity;

  Map<Integer, int[]> allowedVehiclesForOrder = new HashMap<>();

  //Map<String, Vehicle> vehiclesMap = new HashMap<>();

  //Set<String> allZones = new HashSet<>();

  //Set<String> allOrderTypes = new HashSet<>();

  //Map<String, long[]> orderTypePresentInOrders = new HashMap<>();

  //Map<String, long[]> vehiclesServingOrderType = new HashMap<>();

  //Map<String, long[]> zonePresentInOrders = new HashMap<>();

  //Map<String, long[]> vehiclesServingZone = new HashMap<>();

  public int vehicleCount;

  public int orderCount;

  public int depot;

  //public int[] starts;

  //public int[] ends;
}

