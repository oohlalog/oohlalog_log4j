package com.oohlalog.log4j;

import org.apache.log4j.Level;

public class CountLevel extends Level {
    public CountLevel(int level, String levelStr, int syslogEquivalent) {
        super(level, levelStr, syslogEquivalent);
    }

    public static CountLevel toLevel(int val, Level defaultLevel) {
         return COUNT;
    }

    public static CountLevel toLevel(String sArg, Level defaultLevel) {
        return COUNT;
    }

    public static final CountLevel COUNT = new CountLevel(190000, "COUNT", 0);
}
