package com.ray.atten.middle.module.utils;

import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdWorker {

    // 起始时间戳 (2026-01-01)
    private final long twepoch = 1735689600000L;

    // 机器标识占用的位数
    private final long workerIdBits = 5L;
    // 数据中心标识占用的位数
    private final long datacenterIdBits = 5L;
    // 毫秒内序列占用的位数
    private final long sequenceBits = 12L;

    // 机器ID最大值 (31)
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    // 数据中心ID最大值 (31)
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

    private long workerId = 1;     // 默认机器ID
    private long datacenterId = 1; // 默认数据中心ID
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * 产生下一个ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回退，拒绝生成ID");
        }

        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & ((-1L ^ (-1L << sequenceBits)));
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - twepoch) << (workerIdBits + datacenterIdBits + sequenceBits))
                | (datacenterId << (workerIdBits + sequenceBits))
                | (workerId << sequenceBits)
                | sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }
}