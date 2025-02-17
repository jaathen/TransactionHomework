package com.zhangboyu.transaction.repo;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class IdRepo {
    private final AtomicLong idGenerator = new AtomicLong(1000);

    private ConcurrentHashMap<String, String> serialNumMap = new ConcurrentHashMap<>();

    public String createTransactionNo(String serialNumber) {
        return serialNumMap.computeIfAbsent(serialNumber, a -> String.valueOf(idGenerator.addAndGet(1)));
    }
}
