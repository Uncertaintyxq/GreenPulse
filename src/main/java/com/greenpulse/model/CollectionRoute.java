package com.greenpulse.model;

import java.util.List;

/** A planned collection route with the real road distance and time from the routing service. */
public class CollectionRoute {
    private final String routeID;
    private final List<SmartBin> binQueue;
    private final double totalDistanceKm;
    private final double estimatedMinutes;
    private final String driverName;
    private final String vehicleName;

    public CollectionRoute(String routeID, List<SmartBin> orderedBins, double totalDistanceKm,
                           double estimatedMinutes, String driverName, String vehicleName) {
        this.routeID = routeID;
        this.binQueue = List.copyOf(orderedBins);
        this.totalDistanceKm = totalDistanceKm;
        this.estimatedMinutes = estimatedMinutes;
        this.driverName = driverName;
        this.vehicleName = vehicleName;
    }

    public String getRouteID() { return routeID; }
    public List<SmartBin> getBinQueue() { return binQueue; }
    public double getTotalDistanceKm() { return totalDistanceKm; }
    public double getEstimatedMinutes() { return estimatedMinutes; }
    public String getDriverName() { return driverName; }
    public String getVehicleName() { return vehicleName; }
}
