package com.atlassian.maven.plugin.clover.samples.moduleb;

public class ModuleBApp {

    public static ModuleBApp getApp() {
        return new ModuleBApp();
    }

    public String getName() {
        return "ModuleBApp";
    }

    public int getNumber() {

        int num = 0;
        num += 2;
        num *= 2;
        num /= 2;
        num -= 2;
        return num;


    }
}