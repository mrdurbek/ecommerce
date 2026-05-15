package com.ecommerce.order.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderNumberGenerator {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
        private final AtomicInteger sequence = new AtomicInteger(0);

        public String generate() {
            String date = LocalDateTime.now().format(FORMATTER);
            int seq = sequence.incrementAndGet() % 1_000_000;
            return String.format("ORD-%s-%06d", date, seq);
        }
}