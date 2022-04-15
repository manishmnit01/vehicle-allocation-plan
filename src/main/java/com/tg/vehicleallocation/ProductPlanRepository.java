package com.tg.vehicleallocation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductPlanRepository {

	@Autowired
	private MongoTemplate mongoTemplate;

	public ProductPlan insertProductPlan(ProductPlan productPlan) {
		return mongoTemplate.insert(productPlan);
	}

	public void insertProductPlanItems(List<ProductPlanItem> productPlanItems) {
		mongoTemplate.insert(productPlanItems, ProductPlanItem.class);
	}

	public ProductPlan updateProductPlan(ProductPlan productPlan, ProductPlanStatus productPlanStatus, List<ProductPlanItem> productPlanItems) {

		Criteria criteria = Criteria.where("_id").is(productPlan.id);
		Query query = new Query(criteria);
		Update update = new Update();
		update.set("status", productPlanStatus);
		update.set("product_plan_items",productPlanItems);
		return mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), ProductPlan.class);
	}

	public List<ProductPlan> getPlansByCompanyIdAndStatus(String companyId, String status) {
		Criteria criteria = Criteria.where("company_id").is(companyId).and("status").is(status);
		Query query = new Query(criteria);
		return mongoTemplate.find(query, ProductPlan.class);
	}

	public List<ProductPlanItem> getPlanItemsByPlanId(String productPlanId) {
		Criteria criteria = Criteria.where("product_plan_id").is(productPlanId);
		Query query = new Query(criteria);
		return mongoTemplate.find(query, ProductPlanItem.class);
	}
}
