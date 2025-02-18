package com.zhangboyu.transaction.service;

import com.zhangboyu.transaction.entity.Cursor;
import com.zhangboyu.transaction.entity.CursorPageResult;
import com.zhangboyu.transaction.entity.Transaction;
import com.zhangboyu.transaction.enums.ErrorEnum;
import com.zhangboyu.transaction.exception.TransactionException;
import com.zhangboyu.transaction.repo.IdRepo;
import com.zhangboyu.transaction.utils.TransactionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceImplTest {
    @Mock
    private IdRepo idRepo;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private ConcurrentHashMap<String, Transaction> transactionConcurrentHashMap;

    private ConcurrentSkipListMap<TransactionServiceImpl.SortKey, Transaction> timeIndex;

    // 细粒度锁（按交易ID）
    private ConcurrentMap<String, ReentrantLock> keyLocks;

    // 全局读写锁（用于索引维护）
    private ReentrantReadWriteLock globalLock;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        transactionConcurrentHashMap = new ConcurrentHashMap<>();
        timeIndex = new ConcurrentSkipListMap<>();
        keyLocks = new ConcurrentHashMap<>();
        globalLock = Mockito.spy(new ReentrantReadWriteLock());

        // 通过反射重置私有集合/锁字段
        resetPrivateField("transactionConcurrentHashMap", transactionConcurrentHashMap);
        resetPrivateField("timeIndex", timeIndex);
        resetPrivateField("keyLocks", keyLocks);
        resetPrivateField("globalLock", globalLock);
    }

    // 反射工具方法
    private void resetPrivateField(String fieldName, Object value) throws Exception {
        Field field = TransactionServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(transactionService, value);
    }

    @Test
    void createTransaction_NormalCase_ShouldSucceed() throws Exception {
        // 准备数据
        String serialNumber = "SN123";
        Transaction tx = TransactionUtils.createTransaction();
        String transactionNo = "TX1001";
        when(idRepo.createTransactionNo(serialNumber)).thenReturn(transactionNo);

        // 执行
        String result = transactionService.createTransaction(serialNumber, tx);

        // 验证
        assertEquals(transactionNo, result);
        assertSame(tx, transactionConcurrentHashMap.get(transactionNo));
        assertSame(tx, timeIndex.get(new TransactionServiceImpl.SortKey(tx.getCreateTime(), tx.getTransactionNo())));
        assertEquals(0, globalLock.writeLock().getHoldCount());
        assertFalse(keyLocks.get(transactionNo).isLocked());
    }
    @Test
    void createTransaction_LockRecordFail_ShouldThrowException() throws Exception {
        // 模拟锁已被占用
        Transaction tx = TransactionUtils.createTransaction();
        String serialNumber = "SN123";
        String transactionNo = "TX1001";
        when(idRepo.createTransactionNo(serialNumber)).thenReturn(transactionNo);


        // 模拟第二次调用时锁已被占用
        // 2. 预注入 Spy 锁
        ReentrantLock realLock = new ReentrantLock();
        ReentrantLock lockSpy = Mockito.spy(realLock); // 创建 Spy 锁
        keyLocks.put(transactionNo, lockSpy); // 手动注入锁

        // 3. 模拟 tryLock() 返回 false
        doReturn(false).when(lockSpy).tryLock();
        // 执行并验证异常
        assertThrows(TransactionException.class,
                () -> transactionService.createTransaction("SN123", new Transaction()),
                "CONCURRENCY_EXCEPTION"
        );


        assertNull(transactionConcurrentHashMap.get(transactionNo));
        assertNull(timeIndex.get(new TransactionServiceImpl.SortKey(tx.getCreateTime(), tx.getTransactionNo())));
        assertEquals(0, globalLock.writeLock().getHoldCount());
        assertFalse(keyLocks.get(transactionNo).isLocked());
    }

    @Test
    void createTransaction_GlobalLockFail_ShouldThrowException() throws Exception {
        // 模拟锁已被占用
        Transaction tx = TransactionUtils.createTransaction();
        String serialNumber = "SN123";
        String transactionNo = "TX1001";
        when(idRepo.createTransactionNo(serialNumber)).thenReturn(transactionNo);

        globalLock.readLock().lock();

        // 执行并验证异常
        assertThrows(TransactionException.class,
                () -> transactionService.createTransaction("SN123", new Transaction()),
                "CONCURRENCY_EXCEPTION"
        );

        assertNull(transactionConcurrentHashMap.get(transactionNo));
        assertNull(timeIndex.get(new TransactionServiceImpl.SortKey(tx.getCreateTime(), tx.getTransactionNo())));
        assertEquals(0, globalLock.writeLock().getHoldCount());
        assertFalse(keyLocks.get(transactionNo).isLocked());
    }


    @Test
    void deleteTransaction_Normal_shouldSuccess() {
        // 准备数据
        String serialNumber = "SN123";
        Transaction tx = TransactionUtils.createTransaction();
        String transactionNo = "TX1001";
        when(idRepo.createTransactionNo(serialNumber)).thenReturn(transactionNo);

        // 执行
        String result = transactionService.createTransaction(serialNumber, tx);
        assertNotNull(transactionConcurrentHashMap.get(result));
        transactionService.deleteTransaction(result);
        assertNull(transactionConcurrentHashMap.get(result));
        assertNull(timeIndex.get(new TransactionServiceImpl.SortKey(tx.getCreateTime(), tx.getTransactionNo())));
        assertEquals(0, globalLock.writeLock().getHoldCount());
        assertFalse(keyLocks.containsKey(result));
    }

    @Test
    void deleteTransaction_NotExist() {
        // 准备数据
        Transaction tx = TransactionUtils.createTransaction();
        String transactionNo = tx.getTransactionNo();

        // 执行并验证异常
        assertThrows(TransactionException.class,
                () -> transactionService.deleteTransaction(transactionNo),
                ErrorEnum.TRANSACTION_NOT_EXISTS_EXCEPTION.getMessage()
        );
        assertNull(transactionConcurrentHashMap.get(transactionNo));
        assertEquals(0, globalLock.writeLock().getHoldCount());
        assertFalse(keyLocks.containsKey(transactionNo));
    }

    @Test
    void deleteTransaction_LockRecordFail() {
        // 准备数据
        String serialNumber = "SN123";
        Transaction tx = TransactionUtils.createTransaction();
        String transactionNo = "TX1001";
        when(idRepo.createTransactionNo(serialNumber)).thenReturn(transactionNo);

        String result = transactionService.createTransaction(serialNumber, tx);
        assertNotNull(transactionConcurrentHashMap.get(result));

        // 模拟调用时锁已被占用
        // 2. 预注入 Spy 锁
        ReentrantLock realLock = new ReentrantLock();
        ReentrantLock lockSpy = Mockito.spy(realLock); // 创建 Spy 锁
        keyLocks.put(transactionNo, lockSpy); // 手动注入锁

        // 3. 模拟 tryLock() 返回 false
        doReturn(false).when(lockSpy).tryLock();


        // 执行并验证异常
        assertThrows(TransactionException.class,
                () -> transactionService.deleteTransaction(transactionNo),
                ErrorEnum.CONCURRENCY_EXCEPTION.getMessage()
        );
        assertNotNull(transactionConcurrentHashMap.get(result));
        assertNotNull(timeIndex.get(new TransactionServiceImpl.SortKey(tx.getCreateTime(), tx.getTransactionNo())));
        verify(lockSpy, never()).unlock();
    }

    @Test
    void deleteTransaction_GlobalRecordFail() {
        // 准备数据
        String serialNumber = "SN123";
        Transaction tx = TransactionUtils.createTransaction();
        String transactionNo = "TX1001";
        when(idRepo.createTransactionNo(serialNumber)).thenReturn(transactionNo);

        String result = transactionService.createTransaction(serialNumber, tx);
        assertNotNull(transactionConcurrentHashMap.get(result));

        // 模拟调用时锁已被占用
        globalLock.readLock().lock();
        // 执行并验证异常
        assertThrows(TransactionException.class,
                () -> transactionService.deleteTransaction(transactionNo),
                ErrorEnum.CONCURRENCY_EXCEPTION.getMessage()
        );
        assertNotNull(transactionConcurrentHashMap.get(result));
        assertNotNull(timeIndex.get(new TransactionServiceImpl.SortKey(tx.getCreateTime(), tx.getTransactionNo())));
        assertEquals(0, globalLock.writeLock().getHoldCount());
        assertFalse(keyLocks.get(transactionNo).isLocked());
    }

    @Test
    void updateTransaction_success() {
        // 准备数据
        String serialNumber = "SN123";
        Transaction tx = TransactionUtils.createTransaction();
        String transactionNo = "TX1001";
        when(idRepo.createTransactionNo(serialNumber)).thenReturn(transactionNo);

        // 执行
        String result = transactionService.createTransaction(serialNumber, tx);
        assertNotNull(transactionConcurrentHashMap.get(result));

        Transaction newTx = TransactionUtils.createTransaction();
        newTx.setTransactionNo(transactionNo);
        newTx.setCreateTime(new Date(1));

        transactionService.updateTransaction(newTx);
        assertEquals(transactionConcurrentHashMap.get(result), newTx);
        assertNull(timeIndex.get(new TransactionServiceImpl.SortKey(tx.getCreateTime(), tx.getTransactionNo())));
        assertEquals(timeIndex.get(new TransactionServiceImpl.SortKey(newTx.getCreateTime(), newTx.getTransactionNo())), newTx);
        assertEquals(0, globalLock.writeLock().getHoldCount());
        assertFalse(keyLocks.get(result).isLocked());
    }

    @Test
    void updateTransaction_successNoUpdateTimeIndex() {
        // 准备数据
        String serialNumber = "SN123";
        Transaction tx = TransactionUtils.createTransaction();
        String transactionNo = "TX1001";
        when(idRepo.createTransactionNo(serialNumber)).thenReturn(transactionNo);

        // 执行
        String result = transactionService.createTransaction(serialNumber, tx);
        assertNotNull(transactionConcurrentHashMap.get(result));

        Transaction newTx = TransactionUtils.createTransaction();
        newTx.setTransactionNo(transactionNo);
        newTx.setCreateTime(tx.getCreateTime());
        newTx.setRemark("newRemark");

        transactionService.updateTransaction(newTx);
        assertEquals(transactionConcurrentHashMap.get(result), newTx);
        assertEquals(timeIndex.get(new TransactionServiceImpl.SortKey(newTx.getCreateTime(), newTx.getTransactionNo())), newTx);
        assertEquals(0, globalLock.writeLock().getHoldCount());
        assertFalse(keyLocks.get(result).isLocked());
    }

    @Test
    void updateTransaction_Not_Exist() {
        // 准备数据

        Transaction newTx = TransactionUtils.createTransaction();
        // 执行并验证异常
        assertThrows(TransactionException.class,
                () -> transactionService.updateTransaction(newTx),
                ErrorEnum.CONCURRENCY_EXCEPTION.getMessage()
        );
        assertNull(transactionConcurrentHashMap.get(newTx.getTransactionNo()));
        assertNull(timeIndex.get(new TransactionServiceImpl.SortKey(newTx.getCreateTime(), newTx.getTransactionNo())));
        assertEquals(0, globalLock.writeLock().getHoldCount());
        assertNull(keyLocks.get(newTx.getTransactionNo()));
    }

    @Test
    void updateTransaction_lock_record() {
        // 准备数据
        // 准备数据
        String serialNumber = "SN123";
        Transaction tx = TransactionUtils.createTransaction();
        String transactionNo = "TX1001";
        when(idRepo.createTransactionNo(serialNumber)).thenReturn(transactionNo);

        // 执行
        String result = transactionService.createTransaction(serialNumber, tx);
        assertNotNull(transactionConcurrentHashMap.get(result));

        Transaction newTx = TransactionUtils.createTransaction();
        newTx.setTransactionNo(transactionNo);
        newTx.setCreateTime(new Date(1));

        // 模拟调用时锁已被占用
        // 2. 预注入 Spy 锁
        ReentrantLock realLock = new ReentrantLock();
        ReentrantLock lockSpy = Mockito.spy(realLock); // 创建 Spy 锁
        keyLocks.put(transactionNo, lockSpy); // 手动注入锁
        // 3. 模拟 tryLock() 返回 false
        doReturn(false).when(lockSpy).tryLock();
        // 执行并验证异常
        assertThrows(TransactionException.class,
                () -> transactionService.updateTransaction(newTx),
                ErrorEnum.CONCURRENCY_EXCEPTION.getMessage()
        );
        assertEquals(transactionConcurrentHashMap.get(result), tx);
        assertNull(timeIndex.get(new TransactionServiceImpl.SortKey(newTx.getCreateTime(), newTx.getTransactionNo())));
        assertEquals(timeIndex.get(new TransactionServiceImpl.SortKey(tx.getCreateTime(), tx.getTransactionNo())), tx);
        assertEquals(0, globalLock.writeLock().getHoldCount());
        assertFalse(keyLocks.get(result).isLocked());
    }

    @Test
    void updateTransaction_global_lock() {
        // 准备数据
        // 准备数据
        String serialNumber = "SN123";
        Transaction tx = TransactionUtils.createTransaction();
        String transactionNo = "TX1001";
        when(idRepo.createTransactionNo(serialNumber)).thenReturn(transactionNo);

        // 执行
        String result = transactionService.createTransaction(serialNumber, tx);
        assertNotNull(transactionConcurrentHashMap.get(result));

        Transaction newTx = TransactionUtils.createTransaction();
        newTx.setTransactionNo(transactionNo);
        newTx.setCreateTime(new Date(1));

        globalLock.readLock().lock();

        // 执行并验证异常
        assertThrows(TransactionException.class,
                () -> transactionService.updateTransaction(newTx),
                ErrorEnum.CONCURRENCY_EXCEPTION.getMessage()
        );

        assertEquals(transactionConcurrentHashMap.get(result), tx);
        assertNull(timeIndex.get(new TransactionServiceImpl.SortKey(newTx.getCreateTime(), newTx.getTransactionNo())));
        assertEquals(timeIndex.get(new TransactionServiceImpl.SortKey(tx.getCreateTime(), tx.getTransactionNo())), tx);
        assertEquals(0, globalLock.writeLock().getHoldCount());
        assertFalse(keyLocks.get(result).isLocked());
    }

    @Test
    void listAllTransaction() {
        createTransactions();

        CursorPageResult<Transaction> result = transactionService.listAllTransaction(null, 100);
        assertFalse(result.isHasNext());
        Transaction last = null;
        for (Transaction transaction : result.getItems()) {
            if (last == null) {
                last = transaction;
            } else {
                assertTrue(last.getCreateTime().compareTo(transaction.getCreateTime()) <= 0);
                if (last.getCreateTime().compareTo(transaction.getCreateTime()) == 0) {
                    assertTrue(last.getTransactionNo().compareTo(transaction.getTransactionNo()) < 0);
                }
            }
        }
    }

    @Test
    void listAllTransaction_LockFailWrite() throws Exception {
        List<Transaction> transactions = createTransactions();
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<?> future = executorService.submit(() -> {
            globalLock.writeLock().lock();
        });
        future.get();
        // 执行并验证异常
        assertThrows(TransactionException.class,
                () -> transactionService.listAllTransaction(null, 10),
                ErrorEnum.CONCURRENCY_EXCEPTION.getMessage()
        );
        executorService.shutdown();
    }

    @Test
    void listAllTransaction_LockReadSuccess() {
        List<Transaction> transactions = createTransactions();
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            globalLock.readLock().lock();
        });
        transactionService.listAllTransaction(null, 10);
        executorService.shutdown();
    }

    @Test
    void listAllTransaction_page() {
        List<Transaction> transactions = createTransactions();
        Transaction cursorTransaction = transactions.get(4);
        Cursor cursor = new Cursor(cursorTransaction.getCreateTime().getTime(), cursorTransaction.getTransactionNo());
        CursorPageResult<Transaction> result = transactionService.listAllTransaction(cursor, 10);
        TransactionServiceImpl.SortKey cursorKey = new TransactionServiceImpl.SortKey(cursorTransaction.getCreateTime(), cursorTransaction.getTransactionNo());
        Transaction last = null;
        assertTrue(result.isHasNext());
        assertEquals(transactionService.encodeCursor(result.getItems().getLast()), result.getNextCursor());
        for (Transaction transaction : result.getItems()) {
            assertTrue(cursorKey.compareTo(new TransactionServiceImpl.SortKey(transaction.getCreateTime(), transaction.getTransactionNo())) < 0);
            if (last == null) {
                last = transaction;
            } else {
                assertTrue(last.getCreateTime().compareTo(transaction.getCreateTime()) <= 0);
                if (last.getCreateTime().compareTo(transaction.getCreateTime()) == 0) {
                    assertTrue(last.getTransactionNo().compareTo(transaction.getTransactionNo()) < 0);
                }
            }
        }
    }

    private List<Transaction> createTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Transaction transaction = TransactionUtils.createTransaction();
            transaction.setCreateTime(new Date(i * 1000));
            transactions.add(transaction);
            String serialNumber = String.valueOf(i * 2);
            when(idRepo.createTransactionNo(serialNumber)).thenReturn("TX" + serialNumber);
            transactionService.createTransaction(serialNumber, transaction);

            Transaction transaction1 = TransactionUtils.createTransaction();
            transaction1.setCreateTime(transaction.getCreateTime());
            transactions.add(transaction1);
            serialNumber = String.valueOf(i * 2 + 1);
            when(idRepo.createTransactionNo(serialNumber)).thenReturn("TX" + serialNumber);
            transactionService.createTransaction(serialNumber, transaction1);
        }
        return transactions;
    }

    @Test
    void existBySerialNo() {
        String txNo = "TX123";
        String serialNumber = "S123";
        when(idRepo.createTransactionNo(serialNumber)).thenReturn(txNo);
        transactionConcurrentHashMap.put(txNo, TransactionUtils.createTransaction());
        assertTrue(transactionService.existBySerialNo(serialNumber));
        verify(idRepo).createTransactionNo(serialNumber);
        assertFalse(transactionService.existBySerialNo(""));
    }

    @Test
    void existByTransactionNo() {
        String txNo = "TX123";
        transactionConcurrentHashMap.put(txNo, TransactionUtils.createTransaction());
        assertTrue(transactionService.existByTransactionNo(txNo));
        assertFalse(transactionService.existByTransactionNo("TX1234"));
        assertFalse(transactionService.existByTransactionNo(""));
    }

    @Test
    void decodeCursor() {
        String cursorString = "MTczOTg3NTY2OTM3NixhZGEyZTQ0ZC03NjFiLTRiMmUtODZlMS01MTIyY2RkYjRiYzM=";
        Cursor cursor = transactionService.decodeCursor(cursorString);
        assertEquals(1739875669376L, cursor.getDate());
        assertEquals("ada2e44d-761b-4b2e-86e1-5122cddb4bc3", cursor.getTransactionNo());
    }

    @Test
    void decodeCursor_fail() {
        String cursorString = "afadsfjla";
        assertThrows(TransactionException.class,
                () -> transactionService.decodeCursor(cursorString),
                ErrorEnum.CURSOR_EXCEPTION.getMessage()
        );
    }

    @Test
    void encodeCursor() {
        // 创建Mock Transaction对象
        Transaction transaction = TransactionUtils.createTransaction();

        // 调用方法
        String encoded = transactionService.encodeCursor(transaction);

        // 验证Base64 URL编码是否正确
        byte[] decodedBytes = Base64.getUrlDecoder().decode(encoded);
        String decodedString = new String(decodedBytes);
        assertEquals(String.format("%d,%s", transaction.getCreateTime().getTime(), transaction.getTransactionNo()), decodedString);
    }
}