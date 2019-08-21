package com.ztbsuper.dingtalk;

import java.text.SimpleDateFormat;

public class Util {
    static String convertMs2HourType(long ms) {
        if (ms < 1000)  {
            return convert2Ms(ms);
        }
        else {
            int s = (int) ms / 1000;
            if (s < 60) {
                return convert2S(s);
            }
            else if (s < 3600) {
                return convert2Min(s);
            }
            else {
                return convert2H(s);
            }
        }
    }

    static String convert2Ms(long ms) {
        return ms + "ms";
    }

    static String convert2S(int s) {
        return s + "s";
    }

    static String convert2Min(int s) {
        return (s / 60) + "min" + convert2S(s % 60);
    }

    static String convert2H(int s) {
        return (s / 3600) + "h" + convert2Min(s % 3600);
    }

    public static String generateHelixStageName(String name) {
        return name.contains("-") ? name.substring(0, name.lastIndexOf("-")) : name;
    }
}
