package com.tg.vehicleallocation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TransporterContractRepository {

	@Autowired
	private MongoTemplate mongoTemplate;

	public List<TransporterContract> getAllTransportContracts() {
		return mongoTemplate.findAll(TransporterContract.class);
	}
}
