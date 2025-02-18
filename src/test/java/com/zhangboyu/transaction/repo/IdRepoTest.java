package com.zhangboyu.transaction.repo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class IdRepoTest {

    @Test
    public void createTransactionNo_WithSameSerialNumber_ReturnsSameId() {
        IdRepo idRepo = new IdRepo();
        String serial = "SN123";

        String id1 = idRepo.createTransactionNo(serial);
        String id2 = idRepo.createTransactionNo(serial);

        assertEquals(id1, id2);
    }

    @Test
    public void createTransactionNo_Null() {
        IdRepo idRepo = new IdRepo();
        String serial = "";
        assertNull(idRepo.createTransactionNo(serial));
    }

    @Test
    public void createTransactionNo_WithDifferentSerialNumbers_ReturnsDifferentIds() {
        IdRepo idRepo = new IdRepo();
        String id1 = idRepo.createTransactionNo("SN1");
        String id2 = idRepo.createTransactionNo("SN2");

        assertNotEquals(id1, id2);
    }

    @Test
    public void createTransactionNo_ConcurrentAccess_SameSerialNumber() throws InterruptedException {
        IdRepo idRepo = new IdRepo();
        int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        int taskNum = threadCount * 10;
        CountDownLatch latch = new CountDownLatch(taskNum);
        String sharedSerial = "SN_CONCURRENT";

        // 多线程同时请求相同 serialNumber
        for (int i = 0; i < taskNum; i++) {
            executor.submit(() -> {
                idRepo.createTransactionNo(sharedSerial);
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();

        // 验证所有线程获取的 ID 相同
        String expectedId = "1001";
        assertEquals(expectedId, idRepo.createTransactionNo(sharedSerial));
    }

    @Test
    void createTransactionNo_ConcurrentAccess_DifferentSerialNumbers() throws InterruptedException {
        IdRepo idRepo = new IdRepo();
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ConcurrentHashMap<String, String> transactionNoSerialNoMap = new ConcurrentHashMap<>();
        AtomicBoolean duplicate = new AtomicBoolean(false);
        // 多线程请求不同 serialNumber
        for (int i = 0; i < threadCount; i++) {
            String serial = "SN_" + i;
            executor.submit(() -> {
                String transactionNo = idRepo.createTransactionNo(serial);
                String old = transactionNoSerialNoMap.putIfAbsent(transactionNo, serial);
                if (Objects.nonNull(old)) {
                    duplicate.set(true);
                    log.error("duplicate, new:{}, old:{}", old, transactionNo);
                }
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();
        assertFalse(duplicate.get());
    }
}