package com.zhangboyu.transaction.service;

import com.zhangboyu.transaction.dto.dto.PageDTO;
import com.zhangboyu.transaction.entity.Transaction;
import com.zhangboyu.transaction.exception.TransactionException;
import com.zhangboyu.transaction.repo.IdRepo;
import com.zhangboyu.transaction.service.iface.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.zhangboyu.transaction.enums.ErrorEnum.*;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {
    @Autowired
    private IdRepo idRepo;
    private record SortKey(Date createdAt, String id) implements Comparable<SortKey> {
        @Override
        public int compareTo(SortKey o) {
            int timeCompare = this.createdAt.compareTo(o.createdAt);
            return timeCompare != 0 ? timeCompare : this.id.compareTo(o.id);
        }
    }
    private ConcurrentHashMap<String, Transaction> transactionConcurrentHashMap = new ConcurrentHashMap<>();

    private ConcurrentSkipListMap<SortKey, Transaction> timeIndex = new ConcurrentSkipListMap<>();

    // 细粒度锁（按交易ID）
    private final ConcurrentMap<String, ReentrantLock> keyLocks = new ConcurrentHashMap<>();

    // 全局读写锁（用于索引维护）
    private final ReentrantReadWriteLock globalLock = new ReentrantReadWriteLock();



    @Override
    public String createTransaction(String serialNumber, Transaction transaction) {
        transaction.setTransactionNo(createTransactionNo(serialNumber));
        ReentrantLock reentrantLock = keyLocks.computeIfAbsent(transaction.getTransactionNo(), a -> new ReentrantLock());
        boolean locked = false;
        boolean globalLocked = false;
        try {
            locked = reentrantLock.tryLock();
            if (!locked) {
                throw new TransactionException(CONCURRENCY_EXCEPTION);
            }
            if (transactionConcurrentHashMap.containsKey(transaction.getTransactionNo())) {
                throw new TransactionException(CONCURRENCY_EXCEPTION);
            }
            transactionConcurrentHashMap.put(transaction.getTransactionNo(), transaction);
            globalLocked = globalLock.writeLock().tryLock(1, TimeUnit.SECONDS);
            if (!globalLocked) {
                throw new TransactionException(CONCURRENCY_EXCEPTION);
            }
            timeIndex.put(new SortKey(transaction.getCreateTime(), transaction.getTransactionNo()), transaction);
        } catch (InterruptedException e) {
            log.error("createTransaction error, transactionNo:{}", transaction.getTransactionNo(), e);
            throw new TransactionException(SYSTEM_EXCEPTION);
        } finally {
            if (globalLocked) {
                globalLock.writeLock().unlock();
            } else if (locked) {
                transactionConcurrentHashMap.remove(transaction.getTransactionNo());
            }
            if (locked) {
                reentrantLock.unlock();
            }
        }
        return transaction.getTransactionNo();
    }

    @Override
    public void deleteTransaction(String transactionNo) {
        ReentrantLock reentrantLock = keyLocks.computeIfAbsent(transactionNo, a -> new ReentrantLock());
        boolean locked = false;
        boolean globalLocked = false;
        Transaction removed = null;
        try {
            locked = reentrantLock.tryLock();
            if (!locked) {
                throw new TransactionException(CONCURRENCY_EXCEPTION);
            }
            if (!transactionConcurrentHashMap.containsKey(transactionNo)) {
                throw new TransactionException(TRANSACTION_NOT_EXISTS_EXCEPTION);
            }
            removed = transactionConcurrentHashMap.remove(transactionNo);
            globalLocked = globalLock.writeLock().tryLock(1, TimeUnit.SECONDS);
            if (!globalLocked) {
                throw new TransactionException(CONCURRENCY_EXCEPTION);
            }
            timeIndex.remove(new SortKey(removed.getCreateTime(), removed.getTransactionNo()));
        } catch (InterruptedException e) {
            log.error("deleteTransaction error, transactionNo:{}", transactionNo, e);
            throw new TransactionException(SYSTEM_EXCEPTION);
        } finally {
            if (globalLocked) {
                globalLock.writeLock().unlock();
            } else if (locked && removed != null) {
                transactionConcurrentHashMap.put(transactionNo, removed);
            }
            if (locked) {
                reentrantLock.unlock();
            }
        }
    }

    @Override
    public void updateTransaction(Transaction transaction) {
        ReentrantLock reentrantLock = keyLocks.computeIfAbsent(transaction.getTransactionNo(), a -> new ReentrantLock());
        boolean locked = false;
        boolean globalLocked = false;
        Transaction oldTransaction = null;
        boolean needUpdateTimeIndex = false;
        try {
            locked = reentrantLock.tryLock();
            if (!locked) {
                throw new TransactionException(CONCURRENCY_EXCEPTION);
            }
            if (!transactionConcurrentHashMap.containsKey(transaction.getTransactionNo())) {
                throw new TransactionException(TRANSACTION_NOT_EXISTS_EXCEPTION);
            }
            oldTransaction = transactionConcurrentHashMap.putIfAbsent(transaction.getTransactionNo(), transaction);
            needUpdateTimeIndex = !transaction.getTransactionNo().equals(oldTransaction.getTransactionNo()) || transaction.getCreateTime().compareTo(oldTransaction.getCreateTime()) != 0;
            if (!needUpdateTimeIndex) {
                return;
            }
            globalLocked = globalLock.writeLock().tryLock(1, TimeUnit.SECONDS);
            if (!globalLocked) {
                throw new TransactionException(CONCURRENCY_EXCEPTION);
            }
            timeIndex.remove(new SortKey(oldTransaction.getCreateTime(), oldTransaction.getTransactionNo()));
            timeIndex.put(new SortKey(transaction.getCreateTime(), transaction.getTransactionNo()), transaction);
        } catch (InterruptedException e) {
            log.error("updateTransaction error, transactionNo:{}", transaction.getTransactionNo(), e);
            throw new TransactionException(SYSTEM_EXCEPTION);
        } finally {
            if (globalLocked) {
                globalLock.writeLock().unlock();
            } else if (locked && needUpdateTimeIndex) {
                transactionConcurrentHashMap.put(transaction.getTransactionNo(), transaction);
            }
            if (locked) {
                reentrantLock.unlock();
            }
        }
    }

    @Override
    public PageDTO<Transaction> listAllTransaction(int page, int size) {
        return null;
    }

    @Override
    public String createTransactionNo(String serialNumber) {
        return idRepo.createTransactionNo(serialNumber);
    }

    @Override
    public boolean existBySerialNo(String serialNo) {
        return transactionConcurrentHashMap.containsKey(idRepo.createTransactionNo(serialNo));
    }

    @Override
    public boolean existByTransactionNo(String transactionNo) {
        return transactionConcurrentHashMap.containsKey(transactionNo);
    }
}
