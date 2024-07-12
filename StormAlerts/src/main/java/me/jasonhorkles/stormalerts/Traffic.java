package me.jasonhorkles.stormalerts;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Traffic {
    public void checkTraffic(boolean north) {
        try {
            JSONArray input;
            if (StormAlerts.testing) input = new JSONObject(Files.readString(Path.of(
                "StormAlerts/Tests/traffic.json"))).getJSONArray("results");
            else {
                InputStream url = new URI("https://data.traffic.hereapi.com/v7/flow?in=circle:" + Secrets.trafficCoords() + ";r=10&locationReferencing=none&apiKey=" + Secrets.trafficApiKey())
                    .toURL().openStream();
                JSONObject obj = new JSONObject(new String(url.readAllBytes(), StandardCharsets.UTF_8));
                url.close();

                input = obj.getJSONArray("results");
            }

            // Find the correct direction
            JSONObject traffic = new JSONObject();
            String direction = Secrets.apiRoadName(north);
            for (int i = 0; i < input.length(); i++) {
                JSONObject obj = input.getJSONObject(i);
                if (obj.getJSONObject("location").getString("description").equals(direction)) {
                    traffic = obj;
                    break;
                }
            }

            // Get speed and jam factor
            int currentSpeed = Math.toIntExact(Math.round(traffic.getJSONObject("currentFlow")
                .getDouble("speedUncapped") * 2.23694));
            double jamFactor = traffic.getJSONObject("currentFlow").getDouble("jamFactor");

            System.out.println(Utils.getTime(Utils.LogColor.GREEN) + Secrets.roadName(north) + " is currently ~" + currentSpeed + " mph with a jam factor of " + jamFactor);

            if (currentSpeed <= 55 && currentSpeed >= 40) StormAlerts.jda.openPrivateChannelById(
                    277291758503723010L)
                .flatMap(channel -> channel.sendMessage("**" + Secrets.roadName(north) + "** has a slowdown @ **" + currentSpeed + " mph**!\nJam factor: **" + jamFactor + "/10.0** :yellow_circle:"))
                .queue();

            else if (currentSpeed < 40) StormAlerts.jda.openPrivateChannelById(277291758503723010L).flatMap(
                    channel -> channel.sendMessage("**" + Secrets.roadName(north) + "** has a slowdown @ **" + currentSpeed + " mph**!\nJam factor: **" + jamFactor + "/10.0** :red_circle:"))
                .queue();

        } catch (Exception e) {
            System.out.print(Utils.getTime(Utils.LogColor.RED));
            e.printStackTrace();
            Utils.logError(e);

            StormAlerts.jda.openPrivateChannelById(277291758503723010L)
                .flatMap(channel -> channel.sendMessage("**Failed to check traffic!** :warning:")).queue();
        }
    }

    public void scheduleTrafficCheck(String time, boolean toWork) throws ParseException {
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.US);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(format.parse(LocalDate.now() + " " + time));

            long delay = calendar.getTimeInMillis() - System.currentTimeMillis();

            if (delay >= 0) new Thread(() -> {
                try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
                    StormAlerts.scheduledTimers.add(executor.schedule(() -> new Traffic().checkTraffic(toWork),
                        delay,
                        TimeUnit.MILLISECONDS));
                    System.out.println(Utils.getTime(Utils.LogColor.GREEN) + "Scheduled traffic check in " + Math.round(
                        delay / 3600000.0) + " hours.");
                }
            }, "Traffic Check").start();
        }
    }
}
