package com.dianping;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class tiem
{
    public static void main(String[] args) {
        LocalDateTime time = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
        long second = time.toEpochSecond(ZoneOffset.UTC);
        System.out.println(second);
    }
}
