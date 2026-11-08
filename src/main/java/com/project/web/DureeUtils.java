package com.project.web;

public final class DureeUtils {
    private DureeUtils() { }
    public static String formater(long secondes) {
        return String.format("%02d:%02d:%02d", secondes / 3600, secondes / 60 % 60, secondes % 60);
    }
}
