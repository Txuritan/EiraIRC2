// Copyright (c) 2015 Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.irc;

import net.blay09.mods.eirairc.EiraIRC;
import net.blay09.mods.eirairc.api.IRCReplyCodes;
import net.blay09.mods.eirairc.api.bot.IRCBot;
import net.blay09.mods.eirairc.api.config.IConfigManager;
import net.blay09.mods.eirairc.api.irc.IRCChannel;
import net.blay09.mods.eirairc.api.irc.IRCConnection;
import net.blay09.mods.eirairc.api.irc.IRCMessage;
import net.blay09.mods.eirairc.api.irc.IRCUser;
import net.blay09.mods.eirairc.bot.IRCBotImpl;
import net.blay09.mods.eirairc.config.AuthManager;
import net.blay09.mods.eirairc.config.ServerConfig;
import net.blay09.mods.eirairc.config.SharedGlobalConfig;
import net.blay09.mods.eirairc.config.base.ServiceConfig;
import net.blay09.mods.eirairc.config.base.ServiceSettings;
import net.blay09.mods.eirairc.handler.IRCEventHandler;
import net.blay09.mods.eirairc.util.ConfigHelper;
import net.blay09.mods.eirairc.util.Globals;
import net.blay09.mods.eirairc.util.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.util.*;

public class IRCConnectionImpl implements Runnable, IRCConnection {

    private static final Logger logger = LogManager.getLogger();

    public static class ProxyAuthenticator extends Authenticator {
        private PasswordAuthentication auth;

        public ProxyAuthenticator(String username, String password) {
            auth = new PasswordAuthentication(username, password.toCharArray());
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return auth;
        }
    }

    public static final int DEFAULT_PORT = 6667;

    public static final String CTCP_START = "\u0001";
    public static final String CTCP_END = "\u0001";
    protected static final int DEFAULT_PROXY_PORT = 1080;
    private final IRCParser parser = new IRCParser();

    protected final IRCSender sender = new IRCSender(this);
    private final Map<String, IRCChannel> channels = new HashMap<>();
    private final Map<String, IRCUser> users = new HashMap<>();
    protected final ServerConfig serverConfig;

    protected final int[] ports;
    protected final String host;
    private IRCBotImpl bot;
    private String nick;
    private boolean connected;
    private int waitingReconnect;
    private int waitingFallbackNick;
    private boolean silentNickFailure;

    private String serverType;
    private String channelTypes = "#&";
    private String channelUserModes = "ov";
    private String channelUserModePrefixes = "@+";

    private boolean isTwitch;
    private final List<String> joinAfterNickServ = new ArrayList<>();
    private boolean waitingOnNickServ;
    private boolean disableLogger;

    private Socket socket;
    protected BufferedWriter writer;
    protected BufferedReader reader;

    public IRCConnectionImpl(ServerConfig serverConfig, String nick) {
        this.serverConfig = serverConfig;
        this.host = Utils.extractHost(serverConfig.getAddress());
        this.isTwitch = (this.host.equals(Globals.TWITCH_SERVER));
        this.ports = Utils.extractPorts(serverConfig.getAddress(), DEFAULT_PORT);
        this.nick = nick;
    }

    public void setBot(IRCBotImpl bot) {
        this.bot = bot;
    }

    @Override
    public String getNick() {
        return nick;
    }

    @Override
    public IRCUser getBotUser() {
        return getOrCreateUser(nick);
    }

    @Override
    public boolean isTwitch() {
        return isTwitch;
    }

    @Override
    public IRCChannel getChannel(String channelName) {
        return channels.get(channelName.toLowerCase());
    }

    public IRCChannel getOrCreateChannel(String channelName) {
        IRCChannel channel = getChannel(channelName);
        if (channel == null) {
            channel = new IRCChannelImpl(this, channelName);
            channels.put(channelName.toLowerCase(), channel);
        }
        return channel;
    }

    @Override
    public IRCUser getUser(String nick) {
        return users.get(nick.toLowerCase());
    }

    private IRCUserImpl getOrCreateSender(IRCMessage msg) {
        if (msg.getPrefixNick() != null) {
            IRCUserImpl user = (IRCUserImpl) getOrCreateUser(msg.getPrefixNick());
            user.setUsername(msg.getPrefixUsername());
            user.setHostname(msg.getPrefixHostname());
            return user;
        }
        return null;
    }

    @Override
    public IRCUser getOrCreateUser(String nick) {
        IRCUser user = getUser(nick);
        if (user == null) {
            user = new IRCUserImpl(this, nick);
            users.put(nick.toLowerCase(), user);
        }
        return user;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public Collection<IRCChannel> getChannels() {
        return channels.values();
    }

    public boolean start() {
        IRCEventHandler.fireConnectingEvent(this);
        Thread thread = new Thread(this, "IRC (" + host + ")");
        thread.start();
        return true;
    }

    protected Proxy createProxy() {
        if (!SharedGlobalConfig.proxyHost.get().isEmpty()) {
            if (!SharedGlobalConfig.proxyUsername.get().isEmpty() || !SharedGlobalConfig.proxyPassword.get().isEmpty()) {
                Authenticator.setDefault(new ProxyAuthenticator(SharedGlobalConfig.proxyUsername.get(), SharedGlobalConfig.proxyPassword.get()));
            }
            SocketAddress proxyAddr = new InetSocketAddress(Utils.extractHost(SharedGlobalConfig.proxyHost.get()), Utils.extractPorts(SharedGlobalConfig.proxyHost.get(), DEFAULT_PROXY_PORT)[0]);
            return new Proxy(Proxy.Type.SOCKS, proxyAddr);
        }
        return null;
    }

    protected Socket connect() throws Exception {
        for (int i = 0; i < ports.length; i++) {
            try {
                SocketAddress targetAddr = new InetSocketAddress(host, ports[i]);
                Socket newSocket;
                Proxy proxy = createProxy();
                if (proxy != null) {
                    newSocket = new Socket(proxy);
                } else {
                    newSocket = new Socket();
                }

                if (!SharedGlobalConfig.bindIP.get().isEmpty()) {
                    newSocket.bind(new InetSocketAddress(SharedGlobalConfig.bindIP.get(), ports[i]));
                }
                newSocket.connect(targetAddr);
                writer = new BufferedWriter(new OutputStreamWriter(newSocket.getOutputStream(), serverConfig.getCharset()));
                reader = new BufferedReader(new InputStreamReader(newSocket.getInputStream(), serverConfig.getCharset()));
                sender.setWriter(writer);
                return newSocket;
            } catch (UnknownHostException e) {
                throw e;
            } catch (IOException e) {
                if (i == ports.length - 1) {
                    throw e;
                }
            }
        }
        return null;
    }

    public void tick() {
        if (waitingFallbackNick > 0) {
            waitingFallbackNick--;
            if (waitingFallbackNick <= 0) {
                setSilentNickFailure(true);
                nick(serverConfig.getNick());
            }
        }
    }

    @Override
    public void run() {
        try {
            try {
                socket = connect();
            } catch (Exception e) {
                IRCEventHandler.fireConnectionFailedEvent(this, e);
                return;
            }
            register();
            sender.start();
            String line;
            while ((line = reader.readLine()) != null && sender.isRunning()) {
                if (SharedGlobalConfig.debugMode.get()) {
                    logger.info("< {}", line);
                }
                if (!line.isEmpty()) {
                    IRCMessageImpl msg = parser.parse(line);
                    if (!handleNumericMessage(msg)) {
                        handleMessage(msg);
                    }
                }
            }
        } catch (IOException e) {
            if (!e.getMessage().equals("Socket closed")) {
                e.printStackTrace();
            } else {
                closeSocket();
            }
        } catch (Exception e) {
            EiraIRC.proxy.handleException(this, e);
            closeSocket();
        }
        IRCEventHandler.fireDisconnectEvent(this);
        if (connected) {
            tryReconnect();
        }
    }

    public void tryReconnect() {
        closeSocket();
        if (waitingReconnect == 0) {
            waitingReconnect = 15000;
        } else {
            waitingReconnect *= 2;
        }
        IRCEventHandler.fireReconnectEvent(this, waitingReconnect);
        try {
            Thread.sleep(waitingReconnect);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        start();
    }

    @Override
    public void disconnect(String quitMessage) {
        connected = false;
        try {
            if (writer != null) {
                if (SharedGlobalConfig.debugMode.get()) {
                    logger.info("> QUIT :{}", quitMessage);
                }
                writer.write("QUIT :" + quitMessage + "\r\n");
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        closeSocket();
    }

    private void closeSocket() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void register() {
        try {
            String serverPassword = AuthManager.getServerPassword(getIdentifier());
            if (serverPassword != null && !serverPassword.isEmpty()) {
                if (SharedGlobalConfig.debugMode.get()) {
                    logger.info("> PASS ***************");
                }
                writer.write("PASS " + serverPassword + "\r\n");
            }
            String user = serverConfig.getBotSettings().ident.get() + " \"\" \"\" :" + serverConfig.getBotSettings().description.get();
            if (SharedGlobalConfig.debugMode.get()) {
                logger.info("> NICK {}", nick);
                logger.info("> USER {}", user);
            }
            writer.write("NICK " + nick + "\r\n");
            writer.write("USER " + user + "\r\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            IRCEventHandler.fireConnectionFailedEvent(this, e);
            if (connected) {
                tryReconnect();
            }
        }
    }

    @Override
    public void nick(String nick) {
        if (irc("NICK " + nick)) {
            this.nick = nick;
        }
        waitingFallbackNick = 0;
    }

    public void fallbackNick(String nick) {
        if (irc("NICK " + nick)) {
            this.nick = nick;
        }
        waitingFallbackNick = 3000;
    }

    @Override
    public void join(String channelName, String channelKey) {
        irc("JOIN " + channelName + (channelKey != null ? (" " + channelKey) : ""));
    }

    @Override
    public void part(String channelName) {
        if (irc("PART " + channelName)) {
            IRCChannel channel = channels.remove(channelName.toLowerCase());
            if (channel != null) {
                IRCEventHandler.fireChannelLeftEvent(this, channel);
            }
        }
    }

    @Override
    public void mode(String targetName, String flags) {
        irc("MODE " + targetName + " " + flags);
    }

    @Override
    public void mode(String targetName, String flags, String nick) {
        irc("MODE " + targetName + " " + flags + " " + nick);
    }

    @Override
    public void topic(String channelName, String topic) {
        irc("TOPIC " + channelName + " :" + topic);
    }

    private boolean handleNumericMessage(IRCMessageImpl msg) {
        int numeric = msg.getNumericCommand();
        if (numeric == -1) {
            return false;
        }
        if (numeric == IRCReplyCodes.RPL_NAMREPLY) {
            IRCChannelImpl channel = (IRCChannelImpl) getChannel(msg.arg(2));
            String[] names = msg.arg(3).split(" ");
            for (String name : names) {
                char firstChar = name.charAt(0);
                int idx = channelUserModePrefixes.indexOf(firstChar);
                IRCChannelUserMode mode = null;
                if (idx != -1) {
                    mode = IRCChannelUserMode.fromChar(channelUserModes.charAt(idx));
                    name = name.substring(1);
                }
                IRCUserImpl user = (IRCUserImpl) getOrCreateUser(name);
                if (mode != null) {
                    user.setChannelUserMode(channel, mode);
                }
                user.addChannel(channel);
                channel.addUser(user);
            }
            IRCEventHandler.fireChannelJoinedEvent(this, msg, channel);
        } else if (numeric == IRCReplyCodes.RPL_WELCOME) {
            connected = true;
            waitingReconnect = 0;
            IRCEventHandler.fireConnectedEvent(this, msg);
        } else if (numeric == IRCReplyCodes.RPL_TOPIC) {
            IRCChannelImpl channel = (IRCChannelImpl) getChannel(msg.arg(1));
            if (channel != null) {
                channel.setTopic(msg.arg(2));
                IRCEventHandler.fireChannelTopicEvent(this, msg, channel, null, channel.getTopic());
            }
        } else if (numeric == IRCReplyCodes.RPL_WHOISLOGIN) {
            IRCUserImpl user = (IRCUserImpl) getOrCreateUser(msg.arg(1));
            user.setAccountName(msg.arg(2));
        } else if (numeric == IRCReplyCodes.RPL_IDENTIFIED || numeric == IRCReplyCodes.RPL_WHOISLOGIN2) {
            IRCUserImpl user = (IRCUserImpl) getOrCreateUser(msg.arg(1));
            user.setAccountName(msg.arg(1));
        } else if (numeric == IRCReplyCodes.RPL_ENDOFWHOIS) {
            IRCUserImpl user = (IRCUserImpl) getOrCreateUser(msg.arg(1));
            if (user.getAccountName() == null || user.getAccountName().isEmpty()) {
                user.setAccountName(null);
            }
        } else if (numeric == IRCReplyCodes.RPL_MYINFO) {
            serverType = msg.arg(1);
        } else if (numeric == IRCReplyCodes.RPL_ISUPPORT) {
            for (int i = 0; i < msg.argCount(); i++) {
                if (msg.arg(i).startsWith("CHANTYPES=")) {
                    channelTypes = msg.arg(i).substring(10);
                } else if (msg.arg(i).startsWith("PREFIX=")) {
                    String value = msg.arg(i).substring(7);
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < value.length(); j++) {
                        char c = value.charAt(j);
                        if (c == ')') {
                            channelUserModes = sb.toString();
                            sb = new StringBuilder();
                        } else if (c != '(') {
                            sb.append(c);
                        }
                    }
                    channelUserModePrefixes = sb.toString();
                }
            }
        } else if (numeric == IRCReplyCodes.RPL_MOTD || numeric <= 4 || numeric == 251 || numeric == 252 || numeric == 254 || numeric == 255 || numeric == 265 || numeric == 266 || numeric == 250 || numeric == 375) {
            if (SharedGlobalConfig.debugMode.get()) {
                logger.info("Ignoring message code: {} ({} arguments)", msg.getCommand(), msg.argCount());
            }
        } else if (IRCReplyCodes.isErrorCode(numeric)) {
            IRCEventHandler.fireIRCErrorEvent(this, msg, msg.getNumericCommand(), msg.args());
        } else {
            if (SharedGlobalConfig.debugMode.get()) {
                logger.warn("Unhandled message code: {} ({} arguments)", msg.getCommand(), msg.argCount());
            }
        }
        return true;
    }

    private boolean handleMessage(IRCMessageImpl msg) {
        String cmd = msg.getCommand();
        if (cmd.equals("PING")) {
            irc("PONG " + msg.arg(0));
        } else if (cmd.equals("PRIVMSG")) {
            IRCUserImpl user = getOrCreateSender(msg);
            String target = msg.arg(0);
            String message = msg.arg(1);
            boolean isEmote = false;
            boolean isCTCP = false;
            if (message.startsWith(CTCP_START)) {
                message = message.substring(CTCP_START.length(), message.length() - CTCP_END.length());
                if (message.startsWith("ACTION ")) {
                    message = message.substring("ACTION ".length());
                    isEmote = true; // backwards compatibility
                } else {
                    isCTCP = true;
                    if (channelTypes.indexOf(target.charAt(0)) != -1) {
                        IRCEventHandler.fireChannelCTCPEvent(this, getChannel(target), user, msg, message, false);
                    } else if (target.equals(this.nick)) {
                        IRCEventHandler.firePrivateCTCPEvent(this, user, msg, message, false);
                    }
                }
            }
            if (!isCTCP) {
                if (channelTypes.indexOf(target.charAt(0)) != -1) {
                    IRCEventHandler.fireChannelChatEvent(this, getChannel(target), user, msg, message, isEmote, false);
                } else if (target.equals(this.nick)) {
                    IRCEventHandler.firePrivateChatEvent(this, user, msg, message, isEmote, false);
                }
            }
        } else if (cmd.equals("NOTICE")) {
            IRCUserImpl user = getOrCreateSender(msg);
            String target = msg.arg(0);
            String message = msg.arg(1);
            if (waitingOnNickServ && user != null) {
                ServiceSettings serviceSettings = ServiceConfig.getSettings(host, serverType);
                if (serviceSettings.getServiceName().equals(user.getName() + "@" + user.getHostname()) || serviceSettings.getServiceName().equals(user.getName())) {
                    for (String s : joinAfterNickServ) {
                        join(s, null);
                    }
                    joinAfterNickServ.clear();
                    waitingOnNickServ = false;
                }
            }
            if (message.startsWith(CTCP_START)) {
                if (channelTypes.indexOf(target.charAt(0)) != -1) {
                    IRCEventHandler.fireChannelCTCPEvent(this, getChannel(target), user, msg, message, true);
                } else if (target.equals(this.nick) || target.equals("*")) {
                    IRCEventHandler.firePrivateCTCPEvent(this, user, msg, message, true);
                }
            } else {
                if (channelTypes.indexOf(target.charAt(0)) != -1) {
                    IRCEventHandler.fireChannelChatEvent(this, getChannel(target), user, msg, message, false, true);
                } else if (target.equals(this.nick) || target.equals("*")) {
                    IRCEventHandler.firePrivateChatEvent(this, user, msg, message, false, true);
                }
            }
        } else if (cmd.equals("JOIN")) {
            IRCUserImpl user = getOrCreateSender(msg);
            if (user != null) {
                IRCChannelImpl channel = (IRCChannelImpl) getOrCreateChannel(msg.arg(0));
                channel.addUser(user);
                user.addChannel(channel);
                IRCEventHandler.fireUserJoinEvent(this, msg, channel, user);
            }
        } else if (cmd.equals("PART")) {
            IRCUserImpl user = getOrCreateSender(msg);
            if (user != null) {
                IRCChannelImpl channel = (IRCChannelImpl) getChannel(msg.arg(0));
                if (channel != null) {
                    channel.removeUser(user);
                    user.removeChannel(channel);
                    IRCEventHandler.fireUserLeaveEvent(this, msg, channel, user, msg.arg(1));
                }
            }
        } else if (cmd.equals("TOPIC")) {
            IRCUser user = getOrCreateSender(msg);
            IRCChannelImpl channel = (IRCChannelImpl) getChannel(msg.arg(0));
            if (channel != null) {
                channel.setTopic(msg.arg(1));
                IRCEventHandler.fireChannelTopicEvent(this, msg, channel, user, channel.getTopic());
            }
        } else if (cmd.equals("NICK")) {
            IRCUserImpl user = getOrCreateSender(msg);
            if (user != null) {
                String newNick = msg.arg(0);
                users.remove(user.getName().toLowerCase());
                String oldNick = user.getName();
                user.setName(newNick);
                users.put(user.getName().toLowerCase(), user);
                IRCEventHandler.fireNickChangeEvent(this, msg, user, oldNick, newNick);

            }
        } else if (cmd.equals("MODE")) {
            if (channelTypes.indexOf(msg.arg(0).charAt(0)) == -1 || msg.argCount() < 3) {
                return false;
            }
            IRCChannelImpl channel = (IRCChannelImpl) getOrCreateChannel(msg.arg(0));
            String mode = msg.arg(1);
            String param = msg.arg(2);
            boolean set = false;
            List<Character> setList = new ArrayList<>();
            List<Character> unsetList = new ArrayList<>();
            for (int i = 0; i < mode.length(); i++) {
                char c = mode.charAt(i);
                if (c == '+') {
                    set = true;
                } else if (c == '-') {
                    set = false;
                } else if (set) {
                    setList.add(c);
                } else {
                    unsetList.add(c);
                }
            }
            IRCUserImpl user = (IRCUserImpl) getOrCreateUser(param);
            IRCChannelUserMode currentMode = user.getChannelUserMode(channel);
            for (char c : setList) {
                int idx = channelUserModes.indexOf(c);
                if (idx != -1) {
                    user.setChannelUserMode(channel, IRCChannelUserMode.fromChar(c));
                }
            }
            if (currentMode != null) {
                for (char c : unsetList) {
                    if (c == currentMode.modeChar) {
                        user.setChannelUserMode(channel, null);
                    }
                }
            }
        } else if (cmd.equals("QUIT")) {
            IRCUser user = getOrCreateSender(msg);
            if (user != null) {
                IRCEventHandler.fireUserQuitEvent(this, msg, user, msg.arg(0));
                for (IRCChannel channel : user.getChannels()) {
                    ((IRCChannelImpl) channel).removeUser(user);
                }
                users.remove(user.getName().toLowerCase());
            }
        }
        return false;
    }

    public void whois(String nick) {
        irc("WHOIS " + nick);
    }

    public void message(String target, String message) {
        irc("PRIVMSG " + target + " :" + message);
    }

    public void notice(String target, String message) {
        irc("NOTICE " + target + " :" + message);
    }

    @Override
    public void kick(String channelName, String nick, String reason) {
        irc("KICK " + channelName + " " + nick + (reason != null ? (" :" + reason) : ""));
    }

    @Override
    public boolean irc(String message) {
        if (SharedGlobalConfig.debugMode.get() && !disableLogger) {
            logger.info("> {}", message);
        }
        return sender.addToSendQueue(message);
    }

    @Override
    public String getServerType() {
        return serverType;
    }

    @Override
    public String getChannelTypes() {
        return channelTypes;
    }

    @Override
    public String getChannelUserModes() {
        return channelUserModes;
    }

    @Override
    public String getChannelUserModePrefixes() {
        return channelUserModePrefixes;
    }

    @Override
    public IRCBot getBot() {
        return bot;
    }

    @Override
    public String getName() {
        return host;
    }

    @Override
    public ContextType getContextType() {
        return ContextType.IRCConnection;
    }

    @Override
    public String getIdentifier() {
        return host;
    }

    @Override
    public IRCConnection getConnection() {
        return this;
    }

    @Override
    public void message(String message) {
    }

    @Override
    public void notice(String message) {
    }

    @Override
    public void ctcpMessage(String message) {
    }

    @Override
    public void ctcpNotice(String message) {
    }

    @Override
    public IConfigManager getGeneralSettings() {
        return ConfigHelper.getGeneralSettings(this).manager;
    }

    @Override
    public IConfigManager getBotSettings() {
        return ConfigHelper.getBotSettings(this).manager;
    }

    @Override
    public IConfigManager getThemeSettings() {
        return ConfigHelper.getTheme(this).manager;
    }

    @Override
    public int[] getPorts() {
        return ports;
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isSilentNickFailure() {
        return silentNickFailure;
    }

    public void setSilentNickFailure(boolean silentNickFailure) {
        this.silentNickFailure = silentNickFailure;
    }

    public void nickServIdentify() {
        ServiceSettings settings = ServiceConfig.getSettings(host, serverType);
        AuthManager.NickServData nickServData = AuthManager.getNickServData(getIdentifier());
        if (nickServData != null) {
            waitingOnNickServ = true;
            String command = settings.getIdentifyCommand(nickServData.username, nickServData.password);
            if (SharedGlobalConfig.debugMode.get()) {
                logger.info(command.replace(nickServData.password, "***************"));
            }
            disableLogger = true;
            irc(command);
            disableLogger = false;
        }
    }

    public void joinAfterNickServ(String channelName) {
        joinAfterNickServ.add(channelName);
    }

}
