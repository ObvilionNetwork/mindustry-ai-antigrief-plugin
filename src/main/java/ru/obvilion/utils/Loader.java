package ru.obvilion.utils;

import ru.obvilion.config.Config;
import ru.obvilion.config.Lang;

public class Loader {
    public static boolean firstInit = true;

    public static void init() {
        Config.init();
        Lang.init();

        firstInit = false;
    }
}
