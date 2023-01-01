package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Records {
    // Stats
    public static double highestTempToday;
    public static double lowestTempToday;
    public static double maxRainAmountToday;
    public static double maxRainRateToday;
    public static int highestLightningRateToday;
    public static int highestUvToday;
    public static int maxLightningToday;
    public static int maxWindToday;

    // Time of stats, in epoch seconds
    public static long highestLightningRateTime;
    public static long highestTempTime;
    public static long highestUvTime;
    public static long lowestTempTime;
    public static long maxLightningTime;
    public static long maxRainAmountTime;
    public static long maxRainRateTime;
    public static long maxWindTime;

    @SuppressWarnings("DataFlowIssue")
    public void scheduleRecordCheck() throws FileNotFoundException {
        // Populate the variables
        String todayFilePath = "StormAlerts/records-today.json";

        Scanner fileScanner = new Scanner(new File(todayFilePath));
        String jsonObject = "{}";
        if (fileScanner.hasNextLine()) jsonObject = fileScanner.nextLine();
        JSONObject recordsToday = new JSONObject(jsonObject);
        fileScanner.close();

        if (!recordsToday.isEmpty()) {
            // Stats
            highestLightningRateToday = recordsToday.getInt("highLightningRate");
            highestTempToday = recordsToday.getDouble("highTemp");
            highestUvToday = recordsToday.getInt("highUv");
            lowestTempToday = recordsToday.getDouble("lowTemp");
            maxLightningToday = recordsToday.getInt("maxLightning");
            maxRainAmountToday = recordsToday.getDouble("maxRainAmount");
            maxRainRateToday = recordsToday.getDouble("maxRainRate");
            maxWindToday = recordsToday.getInt("maxWind");

            // Times
            highestLightningRateTime = recordsToday.getLong("highLightningRateTime");
            highestTempTime = recordsToday.getLong("highTempTime");
            highestUvTime = recordsToday.getLong("highUvTime");
            lowestTempTime = recordsToday.getLong("lowTempTime");
            maxLightningTime = recordsToday.getLong("maxLightningTime");
            maxRainAmountTime = recordsToday.getLong("maxRainAmountTime");
            maxRainRateTime = recordsToday.getLong("maxRainRateTime");
            maxWindTime = recordsToday.getLong("maxWindTime");

        } else {
            System.out.println(
                new Utils().getTime(Utils.LogColor.YELLOW) + "No records today found! Populating...");
            resetValues();
        }

        LocalDateTime future = LocalDateTime.now().withHour(23).withMinute(59);
        long delay = Duration.between(LocalDateTime.now(), future).getSeconds();
        if (delay < 0) return;

        StormAlerts.scheduledTimers.add(Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            String totalFilePath = "StormAlerts/records.json";

            try {
                Scanner totalRecords = new Scanner(new File(totalFilePath));
                JSONObject records = new JSONObject(totalRecords.nextLine());

                // Find new records
                if (highestLightningRateToday > records.getInt("highLightningRate")) {
                    sendRecordMessage("High lightning rate", highestLightningRateToday + " strikes per hour",
                        highestLightningRateTime);
                    records.put("highLightningRate", highestLightningRateToday);
                    records.put("highLightningRateTime", highestLightningRateTime);
                }

                if (highestTempToday > records.getDouble("highTemp")) {
                    sendRecordMessage("High temperature", highestTempToday + "°", highestTempTime);
                    records.put("highTemp", highestTempToday);
                    records.put("highTempTime", highestTempTime);
                }

                if (highestUvToday > records.getInt("highUv")) {
                    sendRecordMessage("High UV index", String.valueOf(highestUvToday), highestUvTime);
                    records.put("highUv", highestUvToday);
                    records.put("highUvTime", highestUvTime);
                }

                if (lowestTempToday < records.getDouble("lowTemp")) {
                    sendRecordMessage("Low temperature", lowestTempToday + "°", lowestTempTime);
                    records.put("lowTemp", lowestTempToday);
                    records.put("lowTempTime", lowestTempTime);
                }

                if (maxLightningToday > records.getInt("maxLightning")) {
                    sendRecordMessage("Daily lightning strikes", String.valueOf(maxLightningToday),
                        maxLightningTime);
                    records.put("maxLightning", maxLightningToday);
                    records.put("maxLightningTime", maxLightningTime);
                }

                if (maxRainAmountToday > records.getDouble("maxRainAmount")) {
                    sendRecordMessage("Daily rain amount", maxRainAmountToday + " inches", maxRainAmountTime);
                    records.put("maxRainAmount", maxRainAmountToday);
                    records.put("maxRainAmountTime", maxRainAmountTime);
                }

                if (maxRainRateToday > records.getDouble("maxRainRate")) {
                    sendRecordMessage("High rain rate", maxRainRateToday + " inches per hour",
                        maxRainRateTime);
                    records.put("maxRainRate", maxRainRateToday);
                    records.put("maxRainRateTime", maxRainRateTime);
                }

                if (maxWindToday > records.getInt("maxWind")) {
                    sendRecordMessage("High winds", maxWindToday + " mph", maxWindTime);
                    records.put("maxWind", maxWindToday);
                    records.put("maxWindTime", maxWindTime);
                }

                // Save the records file
                FileWriter recordsFile = new FileWriter(totalFilePath, false);
                recordsFile.write(records.toString());
                recordsFile.close();

                System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + "Updating record channels...");

                int lowTemp = records.getInt("lowTemp");
                VoiceChannel lowTempChannel = StormAlerts.jda.getVoiceChannelById(1059213663506006066L);
                if (!lowTempChannel.getName().equals("Temp Low | " + lowTemp + "°"))
                    lowTempChannel.getManager().setName("Temp Low | " + lowTemp + "°").queue();

                int highTemp = records.getInt("highTemp");
                VoiceChannel highTempChannel = StormAlerts.jda.getVoiceChannelById(1059213632531091548L);
                if (!highTempChannel.getName().equals("Temp High | " + highTemp + "°"))
                    highTempChannel.getManager().setName("Temp High | " + highTemp + "°").queue();

                int highUv = records.getInt("highUv");
                VoiceChannel highUvChannel = StormAlerts.jda.getVoiceChannelById(1059213701250560032L);
                if (!highUvChannel.getName().equals("UV Index | " + highUv))
                    highUvChannel.getManager().setName("UV Index | " + highUv).queue();

                int rainRate = records.getInt("maxRainRate");
                VoiceChannel rainRateChannel = StormAlerts.jda.getVoiceChannelById(1059213828015013948L);
                if (!rainRateChannel.getName().equals("Rain | " + rainRate + " in/hr"))
                    rainRateChannel.getManager().setName("Rain | " + rainRate + " in/hr").queue();

                int rainAmount = records.getInt("maxRainAmount");
                VoiceChannel rainAmountChannel = StormAlerts.jda.getVoiceChannelById(1059213790333382796L);
                if (!rainAmountChannel.getName().equals("Daily Rain | " + rainAmount + " in"))
                    rainAmountChannel.getManager().setName("Daily Rain | " + rainAmount + " in").queue();

                int highWind = records.getInt("maxWind");
                VoiceChannel highWindChannel = StormAlerts.jda.getVoiceChannelById(1059213855164747796L);
                if (!highWindChannel.getName().equals("Wind | " + highWind + " mph"))
                    highWindChannel.getManager().setName("Wind | " + highWind + " mph").queue();

                int lightningRate = records.getInt("highLightningRate");
                VoiceChannel lightningRateChannel = StormAlerts.jda.getVoiceChannelById(1059213581675155507L);
                if (!lightningRateChannel.getName().equals("Lightning | " + lightningRate + "/hr"))
                    lightningRateChannel.getManager().setName("Lightning | " + lightningRate + "/hr").queue();

                int dailyLightning = records.getInt("maxLightning");
                VoiceChannel dailyLightningChannel = StormAlerts.jda.getVoiceChannelById(
                    1059213753494798396L);
                if (!dailyLightningChannel.getName().equals("Daily Lightning | " + dailyLightning))
                    dailyLightningChannel.getManager().setName("Daily Lightning | " + dailyLightning).queue();

            } catch (Exception e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
            }

            // Reset the values for new day
            resetValues();
        }, delay, TimeUnit.SECONDS));

        System.out.println(new Utils().getTime(
            Utils.LogColor.GREEN) + "Scheduled record check in " + delay / 3600 + " hours.");
    }

    private void sendRecordMessage(String recordType, String record, long timeStamp) {
        TextChannel channel = StormAlerts.jda.getTextChannelById(1007060910050914304L);
        String ping = "";
        if (new Utils().shouldIPing(channel)) ping = "<@&1046149064519073813>\n";

        // 📊
        //noinspection DataFlowIssue
        channel.sendMessage(
                ping + "\uD83D\uDCCA New record! **" + recordType + "** of **" + record + "** reported on <t:" + timeStamp + ":F>")
            .complete();
    }

    private void resetValues() {
        highestLightningRateToday = -1;
        highestTempToday = -1;
        highestUvToday = -1;
        lowestTempToday = Integer.MAX_VALUE;
        maxLightningToday = -1;
        maxRainAmountToday = -1;
        maxRainRateToday = -1;
        maxWindToday = -1;

        highestLightningRateTime = 0;
        highestTempTime = 0;
        highestUvTime = 0;
        lowestTempTime = 0;
        maxLightningTime = 0;
        maxRainAmountTime = 0;
        maxRainRateTime = 0;
        maxWindTime = 0;

        System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Record values reset!");
    }
}
