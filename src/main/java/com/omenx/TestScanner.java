package com.omenx;

import com.omenx.osint.UsernameScanner;

import java.util.Map;

public class TestScanner {

    public static void main(String[] args) {

        UsernameScanner scanner = new UsernameScanner();

        Map<String, String> results =
                scanner.scan("github");

        System.out.println();
        System.out.println("===== OMEN-X USERNAME SCAN =====");

        for (Map.Entry<String, String> entry : results.entrySet()) {

            System.out.println(
                    entry.getKey() + " : " + entry.getValue()
            );
        }
    }
}