package me.jasonhorkles.stormalerts;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.awt.*;

@SuppressWarnings("DataFlowIssue")
public class Events extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println(new Utils().getTime(Utils.LogColor.GREEN) + event.getMember()
            .getEffectiveName() + " used the /" + event.getName() + " command");

        //noinspection SwitchStatementWithTooFewBranches
        switch (event.getName().toLowerCase()) {
            case "checknow" -> new Utils().updateNow(event);
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (event.getComponentId()) {
            case "viewroles" -> {
                event.deferReply(true).queue();

                StringBuilder roleList = new StringBuilder();
                StringBuilder notRoleList = new StringBuilder();
                for (SelectOption option : ((StringSelectMenu) event.getMessage().getActionRows().get(0)
                    .getComponents().get(0)).getOptions()) {
                    Role role = event.getGuild().getRoleById(option.getValue());

                    if (event.getMember().getRoles().contains(role))
                        roleList.append(role.getAsMention()).append("\n");
                    else notRoleList.append(role.getAsMention()).append("\n");
                }

                if (roleList.isEmpty()) roleList.append("None");
                if (notRoleList.isEmpty()) notRoleList.append("None");

                EmbedBuilder embed = new EmbedBuilder();
                embed.addField("You have", roleList.toString(), true);
                embed.addBlankField(true);
                embed.addField("You don't have", notRoleList.toString(), true);
                embed.setColor(new Color(47, 49, 54));

                event.getHook().editOriginalEmbeds(embed.build()).queue();
            }
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (event.getComponentId()) {
            case "role-select" -> {
                if (event.getSelectedOptions().isEmpty()) {
                    event.deferEdit().queue();
                    return;
                }

                event.deferReply(true).queue();
                Guild guild = event.getGuild();
                Member member = event.getMember();

                new Thread(() -> {
                    for (SelectOption option : event.getSelectedOptions()) {
                        Role role = guild.getRoleById(option.getValue());
                        if (member.getRoles().contains(role)) {
                            System.out.println(
                                new Utils().getTime(Utils.LogColor.YELLOW) + "Removing " + role.getName()
                                    .toLowerCase() + " role from '" + member.getEffectiveName() + "'");
                            guild.removeRoleFromMember(member, role).complete();

                        } else {
                            System.out.println(
                                new Utils().getTime(Utils.LogColor.YELLOW) + "Adding " + role.getName()
                                    .toLowerCase() + " role to '" + member.getEffectiveName() + "'");
                            guild.addRoleToMember(member, role).complete();
                        }
                    }

                    // Show user's roles
                    StringBuilder roleList = new StringBuilder();
                    StringBuilder notRoleList = new StringBuilder();
                    for (SelectOption option : ((StringSelectMenu) event.getMessage().getActionRows().get(0)
                        .getComponents().get(0)).getOptions()) {
                        Role role = event.getGuild().getRoleById(option.getValue());

                        if (event.getMember().getRoles().contains(role))
                            roleList.append(role.getAsMention()).append("\n");
                        else notRoleList.append(role.getAsMention()).append("\n");
                    }

                    if (roleList.isEmpty()) roleList.append("None");
                    if (notRoleList.isEmpty()) notRoleList.append("None");

                    EmbedBuilder embed = new EmbedBuilder();
                    embed.addField("You have", roleList.toString(), true);
                    embed.addBlankField(true);
                    embed.addField("You don't have", notRoleList.toString(), true);
                    embed.setColor(new Color(47, 49, 54));

                    event.getHook().editOriginalEmbeds(embed.build()).queue();
                }, "Add Roles - " + member.getEffectiveName()).start();
            }
        }
    }
}
