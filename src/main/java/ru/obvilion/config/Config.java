package ru.obvilion.config;

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.io.PropertiesUtils;
import ru.obvilion.ObvilionPlugin;
import ru.obvilion.utils.ResourceUtils;

public class Config {
    public static final String fileName = "config.properties";
    public static final Fi file = ObvilionPlugin.pluginDir.child(fileName);

    private static ObjectMap<String, String> config;

    public static void init() {
        if (!file.exists()) {
            ObvilionPlugin.pluginDir.mkdirs();
            ResourceUtils.copy(fileName, file);

            Log.info("The config file for ObvilionBase was successfully generated.");
            Log.info("Configure it in " + file.path());
        }

        load();
    }

    public static void regenerate() {
        final ObjectMap<String, String> oldConfig = config.copy();

        /* Rewrite file */
        file.delete();
        ResourceUtils.copy(fileName, file);
        load();

        /* Save old config values */
        oldConfig.each((type, value) -> {
            config.put(type, value);
        });

        save();
    }

    public static void load() {
        config = new ObjectMap<>();
        PropertiesUtils.load(
                config, file.reader()
        );
    }

    public static void save() {
        try {
            file.delete();
            PropertiesUtils.store(
                    config, file.writer(false), null, true
            );
        } catch (Exception e) {
            Log.err("Error write config file: {0}", e.getLocalizedMessage());
        }
    }

    public static String get(String key) {
        return config.get(key);
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }
}
