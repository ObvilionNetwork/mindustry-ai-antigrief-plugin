package ru.obvilion;

import arc.Events;
import arc.files.Fi;
import arc.util.*;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.mod.Plugin;

import mindustry.world.Tile;
import mindustry.world.blocks.environment.OreBlock;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.Perceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.neuroph.nnet.learning.CompetitiveLearning;
import org.neuroph.util.TransferFunctionType;
import ru.obvilion.utils.LimitedQueue;
import ru.obvilion.utils.Loader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ObvilionPlugin extends Plugin {
    public static final Fi pluginDir = new Fi("./plugins/ObvilionAntiGrief");
    public static final String VERSION = "0.1.0";

    public static short[][] worldContent;
    public static NeuralNetwork neuralNetwork;
    public static DataSet neuralData;
    public static Learning learning = Learning.NONE;
    public static boolean autolearn = false;
    private static ExecutorService pool = Executors.newFixedThreadPool(4);

    @Override
    public void init() {
        //Loader.init();

        pluginDir.mkdirs();

        neuralNetwork = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, 7, 15, 10, 2);
        neuralData = new DataSet(7, 2);

        Events.on(EventType.WorldLoadEvent.class, e -> {
            worldContent = new short[Vars.world.width()][Vars.world.height()];
            for (Tile t : Vars.world.tiles) {
                worldContent[t.x][t.y] = t.block().id;
            }
        });

        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            if (!e.unit.isPlayer()) return;

            double ore = e.tile.overlay() instanceof OreBlock ? 1 : 0;
            double block = e.tile.build.block.id;
            double type = e.breaking ? 0 : 1;

            double tl = 15, rl = 15, bl = 15, ll = 15;
            double tb = 0, rb = 0, bb = 0, lb = 0;

            for (int i = 1; i < 15; i++) {
                Tile t = e.tile.nearby(0, i);
                if (t == null) break;
                if (t.build == null) continue;
                tb = t.blockID();
                tl = i - 1; break;
            }

            for (int i = 1; i < 15; i++) {
                Tile t = e.tile.nearby(i, 0);
                if (t == null) break;
                if (t.build == null) continue;
                rb = t.blockID();
                rl = i - 1; break;
            }

            for (int i = 1; i < 15; i++) {
                Tile t = e.tile.nearby(0, -i);
                if (t == null) break;
                if (t.build == null) continue;
                bb = t.blockID();
                bl = i - 1; break;
            }

            for (int i = 1; i < 15; i++) {
                Tile t = e.tile.nearby(-i, 0);
                if (t == null) break;
                if (t.build == null) continue;
                lb = t.blockID();
                ll = i - 1; break;
            }

            if (e.breaking) {
                block = worldContent[e.tile.x][e.tile.y];
                worldContent[e.tile.x][e.tile.y] = 0;
            } else {
                worldContent[e.tile.x][e.tile.y] = (short) block;
            }

            if (learning == Learning.LEARN_GAMEPLAY) {
                boolean ok = true;
                for (DataSetRow r : neuralData.getRows()) {
                    double[] i = r.getInput();
                    double[] o = r.getDesiredOutput();
                    if (i[0] == type / 3f
                        && i[1] == block / Blocks.class.getDeclaredFields().length
                        && i[2] == ore
                        && i[3] == tl / 15
                        && i[4] == rl / 15
                        && i[5] == bl / 15
                        && i[6] == ll / 15
                        && o[0] == 0
                    ) ok = false;
                }

                if (ok) {
                    neuralData.addRow(new double[] {
                            type / 3f, block / Blocks.class.getDeclaredFields().length, ore,
                            tl / 15, rl / 15, bl / 15, ll / 15
                    }, new double[] {0, 1});

                    Log.info("Type: @, BlockId: @, OnOre: @, Top: @, Right: @, Bottom: @, Left: @",
                            type, block, ore, tl, rl, bl, ll);
                }

                if (autolearn) {
                    double finalBlock = block;
                    double finalTl = tl;
                    double finalRl = rl;
                    double finalBl = bl;
                    double finalLl = ll;
                    new Thread(() -> {
                        long time = System.currentTimeMillis();
                        BackPropagation backPropagation = new BackPropagation();

                        neuralData.shuffle();
                        neuralNetwork.learn(neuralData, backPropagation);

                        Log.info("[AAI]: Learned successfully! Completed on @ seconds.", (System.currentTimeMillis() - time) / 1000);

                        neuralNetwork.setInput(type / 3f, finalBlock / Blocks.class.getDeclaredFields().length, ore, finalTl / 15, finalRl / 15, finalBl / 15, finalLl / 15);
                        neuralNetwork.calculate();
                        double[] out = neuralNetwork.getOutput();

                        Log.info("[AAI]: Result: @, @", out[0], out[1]);
                        Thread.currentThread().interrupt();
                    }).start();
                }
            } else if (learning == Learning.LEARN_GRIEF) {
                boolean ok = true;
                for (DataSetRow r : neuralData.getRows()) {
                    double[] i = r.getInput();
                    double[] o = r.getDesiredOutput();
                    if (i[0] == type / 3f
                            && i[1] == block / Blocks.class.getDeclaredFields().length
                            && i[2] == ore
                            && i[3] == tl / 15
                            && i[4] == rl / 15
                            && i[5] == bl / 15
                            && i[6] == ll / 15
                            && o[0] == 1
                    ) ok = false;
                }

                if (ok) {
                    neuralData.addRow(new double[] {
                            type / 3f, block / Blocks.class.getDeclaredFields().length, ore,
                            tl / 15, rl / 15, bl / 15, ll / 15
                    }, new double[] {1, 0});

                    Log.info("Type: @, BlockId: @, OnOre: @, Top: @, Right: @, Bottom: @, Left: @",
                            type, block, ore, tl, rl, bl, ll);
                }

                if (autolearn) {
                    double finalBlock = block;
                    double finalTl = tl;
                    double finalRl = rl;
                    double finalBl = bl;
                    double finalLl = ll;
                    new Thread(() -> {
                        long time = System.currentTimeMillis();
                        BackPropagation backPropagation = new BackPropagation();

                        neuralData.shuffle();
                        neuralNetwork.learn(neuralData, backPropagation);

                        Log.info("[AAI]: Learned successfully! Completed on @ seconds.", (System.currentTimeMillis() - time) / 1000);

                        neuralNetwork.setInput(type / 3f, finalBlock / Blocks.class.getDeclaredFields().length, ore, finalTl / 15, finalRl / 15, finalBl / 15, finalLl / 15);
                        neuralNetwork.calculate();
                        double[] out = neuralNetwork.getOutput();

                        Log.info("[AAI]: Result: @, @", out[0], out[1]);
                        Thread.currentThread().interrupt();
                    }).start();
                }
            } else {
                neuralNetwork.setInput(type / 3f, block / Blocks.class.getDeclaredFields().length, ore, tl / 15, rl / 15, bl / 15, ll / 15);
                neuralNetwork.calculate();
                double[] out = neuralNetwork.getOutput();
                Log.info("Ok: @, @", out[0], out[1]);
            }
        });

        Events.on(EventType.ConfigEvent.class, e -> {
            double block = e.tile.block.id;
            double ore = e.tile.tile.overlay() instanceof OreBlock ? 1 : 0;

            double tl = 15, rl = 15, bl = 15, ll = 15;
            double tb = 0, rb = 0, bb = 0, lb = 0;

            for (int i = 1; i < 15; i++) {
                Tile t = e.tile.tile.nearby(0, i);
                if (t == null) break;
                if (t.build == null) continue;
                tb = t.blockID();
                tl = i - 1; break;
            }

            for (int i = 1; i < 15; i++) {
                Tile t = e.tile.tile.nearby(i, 0);
                if (t == null) break;
                if (t.build == null) continue;
                rb = t.blockID();
                rl = i - 1; break;
            }

            for (int i = 1; i < 15; i++) {
                Tile t = e.tile.tile.nearby(0, -i);
                if (t == null) break;
                if (t.build == null) continue;
                bb = t.blockID();
                bl = i - 1; break;
            }

            for (int i = 1; i < 15; i++) {
                Tile t = e.tile.tile.nearby(-i, 0);
                if (t == null) break;
                if (t.build == null) continue;
                lb = t.blockID();
                ll = i - 1; break;
            }

            if (learning == Learning.LEARN_GAMEPLAY) {
                boolean ok = true;
                for (DataSetRow r : neuralData.getRows()) {
                    double[] i = r.getInput();
                    double[] o = r.getDesiredOutput();
                    if (i[0] == 2 / 3f
                        && i[1] == block / Blocks.class.getDeclaredFields().length
                        && i[2] == ore
                        && i[3] == tl / 15
                        && i[4] == rl / 15
                        && i[5] == bl / 15
                        && i[6] == ll / 15
                        && o[0] == 0
                    ) ok = false;
                }

                if (ok) {
                    neuralData.addRow(new double[] {
                            2 / 3f, block / Blocks.class.getDeclaredFields().length, ore,
                            tl / 15, rl / 15, bl / 15, ll / 15
                    }, new double[] {0, 1});

                    Log.info("Type: @, BlockId: @, OnOre: @, Top: @, Right: @, Bottom: @, Left: @",
                            2, block, ore, tl, rl, bl, ll);
                }

                if (autolearn) {
                    double finalTl = tl;
                    double finalRl = rl;
                    double finalBl = bl;
                    double finalLl = ll;
                    new Thread(() -> {
                        long time = System.currentTimeMillis();
                        BackPropagation backPropagation = new BackPropagation();

                        neuralData.shuffle();
                        neuralNetwork.learn(neuralData, backPropagation);

                        Log.info("[AAI]: Learned successfully! Completed on @ seconds.", (System.currentTimeMillis() - time) / 1000);

                        neuralNetwork.setInput(2 / 3f, block / Blocks.class.getDeclaredFields().length, ore, finalTl / 15, finalRl / 15, finalBl / 15, finalLl / 15);
                        neuralNetwork.calculate();
                        double[] out = neuralNetwork.getOutput();

                        Log.info("[AAI]: Result: @, @", out[0], out[1]);
                        Thread.currentThread().interrupt();
                    }).start();
                }
            } else if (learning == Learning.LEARN_GRIEF) {
                boolean ok = true;
                for (DataSetRow r : neuralData.getRows()) {
                    double[] i = r.getInput();
                    double[] o = r.getDesiredOutput();
                    if (i[0] == 2 / 3f
                        && i[1] == block / Blocks.class.getDeclaredFields().length
                        && i[2] == ore
                        && i[3] == tl / 15
                        && i[4] == rl / 15
                        && i[5] == bl / 15
                        && o[0] == 1
                    ) ok = false;
                }

                if (ok) {
                    neuralData.addRow(new double[] {
                            2 / 3f, block / Blocks.class.getDeclaredFields().length, ore,
                            tl / 15, rl / 15, bl / 15, ll / 15
                    }, new double[] {1, 0});

                    Log.info("Type: @, BlockId: @, OnOre: @, Top: @, Right: @, Bottom: @, Left: @",
                            2, block, ore, tl, rl, bl, ll);
                }

                if (autolearn) {
                    double finalTl = tl;
                    double finalRl = rl;
                    double finalBl = bl;
                    double finalLl = ll;
                    new Thread(() -> {
                        long time = System.currentTimeMillis();
                        BackPropagation backPropagation = new BackPropagation();

                        neuralData.shuffle();
                        neuralNetwork.learn(neuralData, backPropagation);

                        Log.info("[AAI]: Learned successfully! Completed on @ seconds.", (System.currentTimeMillis() - time) / 1000);

                        neuralNetwork.setInput(2 / 3f, block / Blocks.class.getDeclaredFields().length, ore, finalTl / 15, finalRl / 15, finalBl / 15, finalLl / 15);
                        neuralNetwork.calculate();
                        double[] out = neuralNetwork.getOutput();

                        Log.info("[AAI]: Result: @, @", out[0], out[1]);
                        Thread.currentThread().interrupt();
                    }).start();
                }
            } else {
                neuralNetwork.setInput(2 / 3f, block / Blocks.class.getDeclaredFields().length, ore, tl / 15, rl / 15, bl / 15, ll / 15);
                neuralNetwork.calculate();
                double[] out = neuralNetwork.getOutput();
                Log.info("Ok: @, @", out[0], out[1]);
            }
        });

//        ds.addRow(new double[] {0.5, 1, 1}, new double[] {0});
//        ds.addRow(new double[] {0.9, 1, 2}, new double[] {0});
//        ds.addRow(new double[] {0.8, 0, 1}, new double[] {0});
//        ds.addRow(new double[] {0.3, 1, 1}, new double[] {1});
//        ds.addRow(new double[] {0.6, 1, 2}, new double[] {1});
//        ds.addRow(new double[] {0.4, 0, 1}, new double[] {1});
//        ds.addRow(new double[] {0.9, 1, 7}, new double[] {2});
//        ds.addRow(new double[] {0.6, 1, 4}, new double[] {2});
//        ds.addRow(new double[] {0.1, 0, 1}, new double[] {2});
//        ds.addRow(new double[] {0.6, 0, 0}, new double[] {3});
//        ds.addRow(new double[] {1, 0, 0}, new double[] {3});
//
//        BackPropagation backPropagation = new BackPropagation();
//        backPropagation.setMaxIterations(2000);
//        nn.learn(ds, backPropagation);
//
//        nn.setInput(0, 1);
//        nn.calculate();
//        double[] networkOutputOne = nn.getOutput();
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {

    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("antigrief", "[args...]", "ObvilionAntiGrief settings", arg -> {
            if (arg.length == 0) {
                Log.warn("Too few command arguments. Usage:");
                Log.warn("> antigrief version - Show version ObvilionAntiGrief plugin");
                Log.warn("> antigrief learn gameplay - ");
                Log.warn("> antigrief learn grief - ");
                return;
            }

            String[] args = arg[0].split(" ");

            if (args[0].equals("autolearn")) {
                if (args.length > 1 && args[1].equals("true")) {
                    autolearn = true;
                    Log.info("Automatic learning mode enabled!");
                    return;
                }

                if (args.length > 1 && args[1].equals("false")) {
                    autolearn = false;
                    Log.info("Automatic learning mode disabled!");
                    return;
                }

                Log.info("Automatic learning mode is @!", autolearn ? "enabled" : "disabled");
                return;
            }

            if (args.length == 2 && args[0].equals("data") && args[1].equals("gameplay")) {
                Log.info("Learning mode (Gameplay) enabled!");
                learning = Learning.LEARN_GAMEPLAY;
                return;
            }

            if (args.length == 2 && args[0].equals("data") && args[1].equals("grief")) {
                Log.info("Learning mode (Griefering) enabled!");
                learning = Learning.LEARN_GRIEF;
                return;
            }

            if (args.length == 2 && args[0].equals("learn") && args[1].equals("stop")) {
                neuralNetwork.stopLearning();
                learning = Learning.NONE;
                return;
            }

            if (args[0].equals("learn")) {
                new Thread(() -> {
                    Log.info("[AAI] Starting learning AI in another thread. Please wait...");
                    long time = System.currentTimeMillis();
                    BackPropagation backPropagation = new BackPropagation();
                    // backPropagation.setMaxIterations(25000);

                    int removed = 0;
                    DataSet target = new DataSet(7, 2);
                    for (int i1 = 0; neuralData.getRows().size() > i1; i1++) {
                        DataSetRow r1 = neuralData.getRows().get(i1);
                        if (Arrays.equals(r1.getDesiredOutput(), new double[]{0, 1})) {
                            boolean find = false;
                            for (int i2 = 0; neuralData.getRows().size() > i2; i2++) {
                                DataSetRow r2 = neuralData.getRows().get(i2);
                                if (Arrays.equals(r2.getInput(), r1.getInput())
                                        && Arrays.equals(r2.getDesiredOutput(), new double[]{1, 0})) {
                                    find = true;
                                }
                            }

                            if (!find) target.addRow(r1);
                            continue;
                        }

                        boolean find = false;
                        for (int i2 = 0; neuralData.getRows().size() > i2; i2++) {
                            DataSetRow r2 = neuralData.getRows().get(i2);
                            if (Arrays.equals(r2.getInput(), r1.getInput())
                                    && Arrays.equals(r2.getDesiredOutput(), new double[]{0, 1})) {

                                removed++;
                                find = true;

                                neuralData.removeRowAt(i2);
                                i2--;
                            }
                        }

                        if (find) {
                            target.addRow(r1.getInput(), new double[] {0.6, 0});
                        } else {
                            target.addRow(r1);
                        }
                    }
                    Log.info("[AAI] @ fields replaced to remove eternal cycle.", removed);
                    neuralData = target;

                    neuralData.shuffle();
                    neuralNetwork.learn(neuralData, backPropagation);

                    Log.info("[AAI] Learned successfully! Completed for @ seconds.", (System.currentTimeMillis() - time) / 1000);
                    Thread.currentThread().interrupt();
                }).start();

                return;
            }

            if (args[0].equals("save")) {
                Fi out = pluginDir.child("output.dataset");
                neuralData.save(out.path());

                out = pluginDir.child("output.ai");
                neuralNetwork.save(out.path());

                Log.info("Neural network data saved successfully!");
                return;
            }

            if (arg[0].equals("load")) {
                Fi out = pluginDir.child("output.dataset");
                neuralData = DataSet.load(out.path());
                Log.info("Neural network data loaded successfully! (@ objects)", neuralData.size());
                return;
            }

            if (arg[0].equals("version")) {
                Log.info("ObvilionAntiGrief v@ by FatonnDev", VERSION);
                Log.info("> Github link: https://github.com/ObvilionNetwork/mindustry-antigrief-ai");
                Log.info("> Discord server link: https://discord.gg/cg82mjh");
                return;
            }

            if (arg[0].equals("reload")) {
                Loader.init();
                Log.info("Plugin ObvilionAntiGrief reloaded!");
                return;
            }

            Log.warn("Command not found!");
        });
    }
}
