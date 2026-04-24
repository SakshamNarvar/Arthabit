package com.nstrange.expenseservice.consumer;

import com.nstrange.expenseservice.dto.ExpenseDto;
import com.nstrange.expenseservice.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ExpenseConsumer
{

    private static final Logger log = LoggerFactory.getLogger(ExpenseConsumer.class);

    private final ExpenseService expenseService;

    @KafkaListener(topics = "${spring.kafka.topic-json.name}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(ExpenseDto eventData) {
        log.info("Received Kafka expense event for userId={}", eventData.getUserId());
        try{
            // Todo: Make it transactional, and check if duplicate event (Handle idempotency)
            expenseService.createExpense(eventData);
            log.info("Successfully processed Kafka expense event for userId={}", eventData.getUserId());
        }catch(Exception ex){
            log.error("Failed to process Kafka expense event for userId={}: {}",
                    eventData.getUserId(), ex.getMessage(), ex);
        }
    }
}