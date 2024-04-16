package me.jasonhorkles.polytrichopsida;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("DataFlowIssue")
public class Events extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");

        switch (event.getName().toLowerCase()) {
            case "ecldebug" -> {
                String message = " run the command `/ecl debug` in-game.\nOnce everything has completed, send the link it provided to this channel.";
                boolean isNull = event.getOption("replyto") == null;
                if (!isNull) if (event.getOption("replyto").getAsMember() == null) isNull = true;

                if (isNull) event.reply("Please" + message).queue();
                else event.reply(event.getOption("replyto").getAsMember()
                                     .getAsMention() + ", please" + message).queue();
            }

            case "plgh" -> event.reply("""
                **EntityClearer:** <https://github.com/SilverstoneMC/EntityClearer>
                **ExpensiveDeaths:** <https://github.com/SilverstoneMC/ExpensiveDeaths>
                **FileCleaner:** <https://github.com/SilverstoneMC/FileCleaner>
                """).queue();

            case "plugins" -> event.reply(
                    "See Jason's plugins on [Hangar](<https://hangar.papermc.io/Silverstone>) | [Modrinth](<https://modrinth.com/organization/silverstone> | [Spigot](<https://www.spigotmc.org/resources/authors/jasonhorkles.339646/>)")
                .queue();

            case "tutorials" -> event.reply(
                "JasonHorkles Tutorials: <https://www.youtube.com/channel/UCIyJ0zf3moNSRN1wIetpbmA>").queue();

            case "config" -> {
                String plugin = event.getOption("plugin").getAsString();
                event
                    .reply("See the " + plugin + " config at: <https://github.com/SilverstoneMC/" + plugin + "/blob/main/src/main/resources/config.yml>")
                    .queue();
            }

            case "close" -> {
                if (event.getChannelType() == ChannelType.GUILD_PUBLIC_THREAD)
                    // In plugin support thread and has helper role or manage threads permission
                    if (isFromStaff(event.getChannel().asThreadChannel(), event.getMember()))
                        sendThankYouMessage(event.getChannel().asThreadChannel(),
                            getOP(event.getChannel().asThreadChannel()),
                            event);
                    else event.reply("Command not available.").setEphemeral(true).queue();
                else event.reply("Command not available.").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Auto publish announcements in github-spam
        if (event.getChannel().getIdLong() == 1226929485895434271L) {
            event.getMessage().crosspost().queue(null,
                new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.ALREADY_CROSSPOSTED));
            return;
        }

        if (event.getAuthor().isBot()) return;

        // Plugin support thread
        if (event.getMessage().getChannelType() == ChannelType.GUILD_PUBLIC_THREAD) if (isFromStaff(event
            .getChannel().asThreadChannel(), event.getMember())) {
            Message message = event.getMessage();

            // Thanks for coming :)
            if (message.getContentStripped().toLowerCase().startsWith("np")) {
                sendThankYouMessage(event.getChannel().asThreadChannel(),
                    getOP(event.getChannel().asThreadChannel()),
                    null);
                return;
            }

            // Ping OP
            if (message.getMessageReference() == null) try {
                List<Message> messages = new Utils().getMessages(event.getChannel(), 2).get(30,
                    TimeUnit.SECONDS);
                if (messages.size() < 2) return;

                OffsetDateTime fiveMinsAgo = OffsetDateTime.now().minusMinutes(5);
                if (messages.get(1).getTimeCreated().isAfter(fiveMinsAgo)) return;

                Member op = getOP(event.getChannel().asThreadChannel());
                if (op == null) return;

                User author = messages.get(1).getAuthor();
                if (author != op.getUser()) return;

                event.getChannel().sendMessage(op.getAsMention()).queue(del -> del.delete().queueAfter(100,
                    TimeUnit.MILLISECONDS,
                    null,
                    new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)));

            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                System.out.print(new Utils().getTime(Utils.LogColor.RED));
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        if (event.getChannelType() != ChannelType.GUILD_PUBLIC_THREAD) return;
        ThreadChannel post = event.getChannel().asThreadChannel();
        if (post.getParentChannel().getIdLong() != 1226927981977403452L) return;

        post.sendMessage("<@277291758503723010>").queue(del -> del.delete().queueAfter(100,
            TimeUnit.MILLISECONDS,
            null,
            new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE)));
    }

    // When recent chatter leaves
    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        System.out.println("\n" + new Utils().getTime(Utils.LogColor.YELLOW) + event.getUser()
            .getName() + " left!");

        OffsetDateTime thirtyMinsAgo = OffsetDateTime.now().minusMinutes(30);
        OffsetDateTime threeDaysAgo = OffsetDateTime.now().minusDays(3);

        for (ThreadChannel thread : event.getGuild().getChannelById(ForumChannel.class, 1226927981977403452L)
            .getThreadChannels()) {
            if (thread.isArchived()) continue;

            System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Checking post '" + thread.getName() + "'");

            if (thread.getOwnerIdLong() == event.getUser().getIdLong()) {
                sendOPLeaveMessage(thread, event.getUser());
                continue;
            }

            // If the user that left sent the latest or a recent message, say so
            if (checkIfFromUser(thirtyMinsAgo, threeDaysAgo, thread, event.getUser().getIdLong()))
                sendRecentLeaveMessage(thread, event.getUser());
        }

        Long[] textChannels = {1226927642117410960L};
        for (long channelId : textChannels) {
            TextChannel channel = event.getGuild().getTextChannelById(channelId);
            System.out.println(new Utils().getTime(Utils.LogColor.YELLOW) + "Checking #" + channel.getName());

            // If the user that left sent the latest or a recent message, say so
            if (checkIfFromUser(thirtyMinsAgo, threeDaysAgo, channel, event.getUser().getIdLong()))
                sendRecentLeaveMessage(channel, event.getUser());
        }
    }

    private boolean checkIfFromUser(OffsetDateTime thirtyMinsAgo, OffsetDateTime threeDaysAgo, MessageChannel channel, Long userId) {
        boolean fromUser = false;

        try {
            // Check the past 15 messages within 30 minutes
            for (Message messages : new Utils().getMessages(channel, 15).get(30, TimeUnit.SECONDS))
                if (messages.getTimeCreated().isAfter(thirtyMinsAgo) && messages.getAuthor()
                                                                            .getIdLong() == userId) {
                    fromUser = true;
                    break;
                }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }

        // If message isn't from the past 30 minutes, see if it's at least the latest message within 3 days
        if (!fromUser) try {
            Message message = new Utils().getMessages(channel, 1).get(30, TimeUnit.SECONDS).getFirst();
            if (message.getTimeCreated().isAfter(threeDaysAgo) && message.getAuthor().getIdLong() == userId)
                fromUser = true;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            System.out.print(new Utils().getTime(Utils.LogColor.RED));
            e.printStackTrace();
        }

        return fromUser;
    }

    private void sendRecentLeaveMessage(MessageChannel channel, User user) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Recent chatter " + user.getName() + " has left the server");
        embed.setDescription(user.getAsMention());
        embed.setThumbnail(user.getAvatarUrl());
        embed.setColor(new Color(255, 200, 0));

        channel.sendMessageEmbeds(embed.build()).queue();
    }

    private void sendOPLeaveMessage(ThreadChannel channel, User user) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Original poster " + user.getName() + " has left the server");
        embed.setDescription(user.getAsMention());
        embed.setFooter("This post will now be closed and locked");
        embed.setThumbnail(user.getAvatarUrl());
        embed.setColor(new Color(255, 100, 0));

        channel.sendMessageEmbeds(embed.build()).queue(na -> channel.getManager().setArchived(true)
            .setLocked(true).queueAfter(1, TimeUnit.SECONDS));
    }

    private void sendThankYouMessage(ThreadChannel channel, Member op, @Nullable SlashCommandInteractionEvent slashEvent) {
        String resourceName;
        String resourceSpigotId;

        String tags = channel.getAppliedTags().toString();
        if (tags.contains("EntityClearer")) {
            resourceName = "EntityClearer";
            resourceSpigotId = "90802";
        } else if (tags.contains("ExpensiveDeaths")) {
            resourceName = "ExpensiveDeaths";
            resourceSpigotId = "96065";
        } else if (tags.contains("FileCleaner")) {
            resourceName = "FileCleaner";
            resourceSpigotId = "93372";
        } else {
            resourceName = "null";
            resourceSpigotId = "null";
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(resourceName);
        embed.setDescription("[Hangar](https://hangar.papermc.io/Silverstone/" + resourceName + ")\n" + "[Modrinth](https://modrinth.com/plugin/" + resourceName.toLowerCase() + ")\n" + "[Spigot](https://www.spigotmc.org/resources/" + resourceSpigotId + "/)");
        embed.setImage("https://imgur.com/xPrBGLb.png");
        embed.setColor(new Color(43, 45, 49));
        embed.setFooter("This post will now be closed. Send a message to re-open it.");

        String ping = op == null ? "dear user" : op.getAsMention();
        String message = "Thank you for coming, " + ping + ". If you enjoy the plugin and are happy with the support you received, please consider leaving a star on Hangar, a follow on Modrinth, or a review on Spigot :heart:";

        if (slashEvent != null) slashEvent.reply(message).addEmbeds(embed.build()).queue(na -> channel
            .getManager().setArchived(true).queueAfter(1, TimeUnit.SECONDS));
        else channel.sendMessage(message).addEmbeds(embed.build()).queue(na -> channel.getManager()
            .setArchived(true).queueAfter(1, TimeUnit.SECONDS));
    }

    private boolean isFromStaff(ThreadChannel channel, Member member) {
        // Plugin support channel and has helper role or manage threads permission
        return channel.getParentChannel().getIdLong() == 1226927981977403452L && (member.getRoles().contains(
            Polytrichopsida.jda.getGuildById(390942438061113344L)
                .getRoleById(606393401839190016L)) || member.hasPermission(Permission.MANAGE_THREADS));
    }

    @Nullable
    private Member getOP(ThreadChannel channel) {
        return channel.getOwner();
    }
}
