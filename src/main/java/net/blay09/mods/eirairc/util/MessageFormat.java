// Copyright (c) 2015 Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.util;

import net.blay09.mods.eirairc.api.event.FormatMessage;
import net.blay09.mods.eirairc.api.event.FormatNick;
import net.blay09.mods.eirairc.api.irc.IRCChannel;
import net.blay09.mods.eirairc.api.irc.IRCConnection;
import net.blay09.mods.eirairc.api.irc.IRCContext;
import net.blay09.mods.eirairc.api.irc.IRCUser;
import net.blay09.mods.eirairc.config.SharedGlobalConfig;
import net.blay09.mods.eirairc.config.settings.BotSettings;
import net.blay09.mods.eirairc.config.settings.GeneralSettings;
import net.blay09.mods.eirairc.irc.IRCUserImpl;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageFormat {

    public enum Target {
        IRC,
        Minecraft
    }

    public enum Mode {
        Message,
        Emote,
        CTCP
    }

    private static final Pattern playerTagPattern = Pattern.compile("[\\[][^\\]]+[\\]]");
    public static final Pattern urlPattern = Pattern.compile("(?:(https?)://)?([-\\w_\\.]{2,}\\.[a-z]{2,4})(/\\S*)?");
    public static final Pattern namePattern = Pattern.compile("@([^ ]+)");

    public static String getMessageFormat(IRCContext context, boolean isEmote) {
        BotSettings botSettings = ConfigHelper.getBotSettings(context);
        if (context instanceof IRCUser) {
            if (isEmote) {
                return botSettings.getMessageFormat().ircPrivateEmote;
            } else {
                return botSettings.getMessageFormat().ircPrivateMessage;
            }
        } else {
            if (isEmote) {
                return botSettings.getMessageFormat().ircChannelEmote;
            } else {
                return botSettings.getMessageFormat().ircChannelMessage;
            }
        }
    }

    public static ITextComponent createChatComponentForMessage(String message) {
        ITextComponent rootComponent = new TextComponentString("");
        StringBuilder buffer = new StringBuilder();
        Matcher urlMatcher = urlPattern.matcher(message);
        Matcher nameMatcher = namePattern.matcher(message);
        int currentIndex = 0;
        while (currentIndex < message.length()) {
            // Find the next word in the message
            int nextWhitespace = message.indexOf(' ', currentIndex);
            if (nextWhitespace == -1) {
                nextWhitespace = message.length();
            }
            // Update Matchers to check the correct region
            urlMatcher.region(currentIndex, nextWhitespace);
            nameMatcher.region(currentIndex, nextWhitespace);
            if (urlMatcher.matches()) {
                // Flush the buffer
                if (buffer.length() > 0) {
                    rootComponent = appendTextToRoot(rootComponent, buffer.toString());
                    buffer = new StringBuilder();
                }
                // Create URL component
                String urlText = urlMatcher.group();
                TextComponentString urlComponent = new TextComponentString(urlText);
                // Make sure a protocol is specified for the ClickEvent value to prevent NPE in GuiChat (getScheme())
                if (!urlText.startsWith("http://") && !urlText.startsWith("https://")) {
                    urlText = "http://" + urlText;
                }
                urlComponent.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, urlText));
                rootComponent = appendSiblingToRoot(rootComponent, urlComponent);
                currentIndex = nextWhitespace;
            } else if (nameMatcher.matches()) {
                // Flush the buffer
                if (buffer.length() > 0) {
                    rootComponent = appendTextToRoot(rootComponent, buffer.toString());
                    buffer = new StringBuilder();
                }
                // Create Name Component
                String nameText = nameMatcher.group();
                TextComponentString nameComponent = new TextComponentString(nameText);
                nameComponent.getStyle().setItalic(true);
                nameComponent.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, nameText + " "));
                rootComponent = appendSiblingToRoot(rootComponent, nameComponent);
                currentIndex = nextWhitespace;
            } else {
                buffer.append(message.substring(currentIndex, Math.min(message.length(), nextWhitespace + 1)));
                currentIndex = nextWhitespace + 1;
            }
        }
        // Flush the buffer
        if (buffer.length() > 0) {
            rootComponent = appendTextToRoot(rootComponent, buffer.toString());
        }
        return rootComponent;
    }

    private static ITextComponent appendSiblingToRoot(ITextComponent root, ITextComponent sibling) {
        root.appendSibling(sibling);
        return root;
    }

    private static ITextComponent appendTextToRoot(ITextComponent root, String text) {
        FormatMessage emoticons = new FormatMessage(new TextComponentString(text));
        MinecraftForge.EVENT_BUS.post(emoticons);
        root.appendSibling(emoticons.component);
        return root;
    }

    public static String filterLinks(String message) {
        String[] s = message.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length; i++) {
            Matcher matcher = urlPattern.matcher(s[i]);
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(matcher.replaceAll(I19n.format("eirairc:general.linkRemoved")));
        }
        return sb.toString();
    }

    public static String filterPlayerTags(String playerName) {
        return playerTagPattern.matcher(playerName).replaceAll("");
    }

    private static String filterAllowedCharacters(String message) {
        StringBuilder sb = new StringBuilder();
        char[] charArray = message.toCharArray();
        for (char c : charArray) {
            if (isAllowedCharacter(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String formatNick(String nick, IRCContext context, Target target, Mode mode) {
        if (target == Target.IRC) {
            if (SharedGlobalConfig.hidePlayerTags.get()) {
                nick = filterPlayerTags(nick);
            }
            nick = String.format(ConfigHelper.getBotSettings(context).mcNickFormat.get(), nick);
            if (SharedGlobalConfig.preventUserPing.get()) {
                nick = nick.substring(0, 1) + '\u0081' + nick.substring(1);
            }
        }
        return nick;
    }

    public static String formatMessage(String format, IRCContext context, ICommandSender sender, String message, Target target, Mode mode) {
        String result = formatChatComponent(format, context, sender, message, target, mode).getUnformattedText();
        BotSettings botSettings = ConfigHelper.getBotSettings(context);
        if (target == Target.IRC) {
            result = IRCFormatting.toIRC(result, !botSettings.convertColors.get());
        } else if (target == Target.Minecraft) {
            result = IRCFormatting.toMC(result, !botSettings.convertColors.get());
            result = filterAllowedCharacters(result);
        }
        return result;
    }

    public static ITextComponent formatChatComponent(String format, IRCContext context, ICommandSender sender, String message, Target target, Mode mode) {
        ITextComponent root = new TextComponentString("");
        TextFormatting nextColor = null;
        StringBuilder sb = new StringBuilder();
        int currentIdx = 0;
        while (currentIdx < format.length()) {
            char c = format.charAt(currentIdx);
            if (c == '{') {
                int tokenEnd = format.indexOf('}', currentIdx);
                if (tokenEnd != -1) {
                    boolean validToken = true;
                    String token = format.substring(currentIdx + 1, tokenEnd);
                    ITextComponent component = null;
                    switch (token) {
                        case "SERVER":
                            component = new TextComponentString(Utils.getCurrentServerName());
                            break;
                        case "USER":
                            component = new TextComponentString(sender.getName());
                            break;
                        case "CHANNEL":
                            component = new TextComponentString(context != null ? context.getName() : "");
                            break;
                        case "NICK":
                            if (sender instanceof EntityPlayer) {
                                EntityPlayer player = (EntityPlayer) sender;
                                component = player.getDisplayName().createCopy();
                                String displayName = component.getUnformattedText();
                                displayName = formatNick(displayName, context, target, mode);
                                component = new TextComponentString(displayName);
                                if (mode != Mode.Emote) {
                                    TextFormatting nameColor = IRCFormatting.getColorFormattingForPlayer(player);
                                    if (nameColor != null && nameColor != TextFormatting.WHITE) {
                                        component.getStyle().setColor(nameColor);
                                    }
                                }
                            } else {
                                component = new TextComponentString(sender.getName());
                            }
                            break;
                        case "MESSAGE":
                            if (message != null) {
                                BotSettings botSettings = ConfigHelper.getBotSettings(context);
                                if (target == Target.Minecraft) {
                                    message = IRCFormatting.toMC(message, !botSettings.convertColors.get());
                                    message = filterAllowedCharacters(message);
                                } else if (target == Target.IRC) {
                                    message = IRCFormatting.toIRC(message, !botSettings.convertColors.get());
                                }
                                component = createChatComponentForMessage(message);
                            } else {
                                component = new TextComponentString("");
                            }
                            break;
                        default:
                            validToken = false;
                            break;
                    }
                    if (validToken) {
                        if (sb.length() > 0) {
                            ITextComponent newComponent;
                            newComponent = new TextComponentString(sb.toString());
                            root.appendSibling(newComponent);
                            if (nextColor != null) {
                                newComponent.getStyle().setColor(nextColor);
                            }
                            sb = new StringBuilder();
                        }
                        root.appendSibling(component);
                        if (nextColor != null) {
                            component.getStyle().setColor(nextColor);
                        }
                        currentIdx += token.length() + 2;
                        continue;
                    }
                }
            } else if (c == '\u00a7') {
                nextColor = IRCFormatting.getColorFromMCColorCode(format.charAt(currentIdx + 1));
                currentIdx += 2;
                continue;
            }
            sb.append(c);
            currentIdx++;
        }
        if (sb.length() > 0) {
            ITextComponent newComponent;
            newComponent = new TextComponentString(sb.toString());
            root.appendSibling(newComponent);
            if (nextColor != null) {
                newComponent.getStyle().setColor(nextColor);
            }
        }
        return root;
    }

    public static String formatMessage(String format, IRCConnection connection, IRCContext targetContext, IRCUser user, String message, Target target, Mode mode) {
        String result = formatChatComponent(format, connection, targetContext, user, message, target, mode).getUnformattedText();
        BotSettings botSettings = ConfigHelper.getBotSettings(targetContext);
        if (target == Target.IRC) {
            result = IRCFormatting.toIRC(result, !botSettings.convertColors.get());
        } else if (target == Target.Minecraft) {
            result = IRCFormatting.toMC(result, !botSettings.convertColors.get());
            result = filterAllowedCharacters(result);
        }
        return result;
    }

    public static ITextComponent formatChatComponent(String format, IRCConnection connection, IRCContext targetContext, IRCUser sender, String message, Target target, Mode mode) {
        ITextComponent root = new TextComponentString("");
        TextFormatting nextColor = null;
        StringBuilder sb = new StringBuilder();
        int currentIdx = 0;
        while (currentIdx < format.length()) {
            char c = format.charAt(currentIdx);
            if (c == '{') {
                int tokenEnd = format.indexOf('}', currentIdx);
                if (tokenEnd != -1) {
                    boolean validToken = true;
                    String token = format.substring(currentIdx + 1, tokenEnd);
                    ITextComponent component = null;
                    switch (token) {
                        case "SERVER":
                            component = new TextComponentString(connection.getIdentifier());
                            break;
                        case "CHANNEL":
                            component = new TextComponentString(targetContext != null ? targetContext.getName() : "#");
                            break;
                        case "USER":
                            if (sender != null) {
                                component = new TextComponentString(sender.getIdentifier());
                            } else {
                                component = new TextComponentString(connection.getIdentifier());
                            }
                            break;
                        case "NICK":
                            if (sender != null) {
                                FormatNick event = new FormatNick(sender, targetContext, new TextComponentString(((IRCUserImpl) sender).getDisplayName()));
                                MinecraftForge.EVENT_BUS.post(event);
                                GeneralSettings settings = ConfigHelper.getGeneralSettings(targetContext);
                                if (settings.showNameFlags.get() && targetContext instanceof IRCChannel) {
                                    component = new TextComponentString(sender.getChannelModePrefix((IRCChannel) targetContext));
                                    component.appendSibling(event.component);
                                } else {
                                    component = event.component;
                                }
                                if (mode != Mode.Emote) {
                                    TextFormatting nameColor = IRCFormatting.getColorFormattingForUser(targetContext instanceof IRCChannel ? (IRCChannel) targetContext : null, sender);
                                    if (nameColor != null) {
                                        component.getStyle().setColor(nameColor);
                                    }
                                }
                            } else {
                                component = new TextComponentString(connection.getIdentifier());
                            }
                            break;
                        case "MESSAGE":
                            if (message != null) {
                                BotSettings botSettings = ConfigHelper.getBotSettings(targetContext);
                                if (target == Target.Minecraft) {
                                    message = IRCFormatting.toMC(message, !botSettings.convertColors.get());
                                    message = filterAllowedCharacters(message);
                                } else if (target == Target.IRC) {
                                    message = IRCFormatting.toIRC(message, !botSettings.convertColors.get());
                                }
                                component = createChatComponentForMessage(message);
                            } else {
                                component = new TextComponentString("");
                            }
                            break;
                        default:
                            validToken = false;
                            break;
                    }
                    if (validToken) {
                        if (sb.length() > 0) {
                            ITextComponent newComponent;
                            newComponent = new TextComponentString(sb.toString());
                            root.appendSibling(newComponent);
                            if (nextColor != null) {
                                newComponent.getStyle().setColor(nextColor);
                            }
                            sb = new StringBuilder();
                        }
                        root.appendSibling(component);
                        if (nextColor != null) {
                            component.getStyle().setColor(nextColor);
                        }
                        currentIdx += token.length() + 2;
                        continue;
                    }
                }
            } else if (c == '\u00a7') {
                if (sb.length() > 0) {
                    ITextComponent newComponent = new TextComponentString(sb.toString());
                    root.appendSibling(newComponent);
                    if (nextColor != null) {
                        newComponent.getStyle().setColor(nextColor);
                    }
                    sb = new StringBuilder();
                }
                nextColor = IRCFormatting.getColorFromMCColorCode(format.charAt(currentIdx + 1));
                currentIdx += 2;
                continue;
            }
            sb.append(c);
            currentIdx++;
        }
        if (sb.length() > 0) {
            ITextComponent newComponent;
            newComponent = new TextComponentString(sb.toString());
            root.appendSibling(newComponent);
            if (nextColor != null) {
                newComponent.getStyle().setColor(nextColor);
            }
        }
        return root;
    }

    private static boolean isAllowedCharacter(char c) {
        return c >= 32 && c != 127;
    }
}
