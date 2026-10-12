package com.orderservice.config;

import com.orderservice.model.Order;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
@RequiredArgsConstructor
public class DatabaseIndexConfig {
    private final MongoTemplate mongoTemplate;
    @PostConstruct public void ensureIndexes() {
        mongoTemplate.indexOps(Order.class).ensureIndex(new Index().on("userId", Sort.Direction.ASC).named("idx_order_user_id"));
        mongoTemplate.indexOps(Order.class).ensureIndex(new Index().on("orderStatus", Sort.Direction.ASC).named("idx_order_status"));
        mongoTemplate.indexOps(Order.class).ensureIndex(new Index().on("orderTime", Sort.Direction.DESC).named("idx_order_time"));
    }
}
