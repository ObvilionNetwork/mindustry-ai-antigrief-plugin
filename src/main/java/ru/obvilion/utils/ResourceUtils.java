package ru.obvilion.utils;

import arc.files.Fi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourceUtils {
    public static void copy(String path, Fi to) {
        try {
            final InputStream in = ResourceUtils.class.getClassLoader().getResourceAsStream(path);
            final OutputStream out = to.write();

            int data;
            while ((data = in.read()) != -1) {
                out.write(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isIP(String s) {
        Matcher m = Pattern.compile(
            "(?<!\\d|\\d\\.)" +
            "(?:[01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "(?:[01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "(?:[01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "(?:[01]?\\d\\d?|2[0-4]\\d|25[0-5])" +
            "(?!\\d|\\.\\d)"
        ).matcher(s);

        return m.find();
    }
}
