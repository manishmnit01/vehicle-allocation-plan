package com.tg.vehicleallocation;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "transporter_contract")
public class TransporterContract {

	public String name;

	@Field("monthly_allowed_distance")
	public int monthlyAllowedDistance;
}
