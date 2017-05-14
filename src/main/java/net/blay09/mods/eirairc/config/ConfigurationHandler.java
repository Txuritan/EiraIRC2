// Copyright (c) 2015, Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.blay09.mods.eirairc.ConnectionManager;
import net.blay09.mods.eirairc.EiraIRC;
import net.blay09.mods.eirairc.api.bot.IBotCommand;
import net.blay09.mods.eirairc.api.irc.IRCConnection;
import net.blay09.mods.eirairc.bot.BotCommandCustom;
import net.blay09.mods.eirairc.bot.IRCBotImpl;
import net.blay09.mods.eirairc.config.base.MessageFormatConfig;
import net.blay09.mods.eirairc.config.base.ServiceConfig;
import net.blay09.mods.eirairc.util.ConfigHelper;
import net.blay09.mods.eirairc.util.Utils;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ConfigurationHandler {

    private static final Logger logger = LogManager.getLogger();

    private static final Map<String, ServerConfig> serverConfigs = Maps.newHashMap();
    private static final Map<String, MessageFormatConfig> displayFormats = Maps.newHashMap();
    private static final List<IBotCommand> customCommands = Lists.newArrayList();
    private static final List<RemoteBotCommand> remoteCommands = Lists.newArrayList();
    private static final List<SuggestedChannel> suggestedChannels = Lists.newArrayList();
    private static final Map<String, TrustedServer> trustedServers = Maps.newHashMap();
    public static final List<String> failedToLoad = Lists.newArrayList();

    private static File baseConfigDir;
    private static MessageFormatConfig defaultDisplayFormat;

    private static void loadDisplayFormats(File formatDir) {
        displayFormats.clear();
        if (!formatDir.exists()) {
            if (!formatDir.mkdirs()) {
                return;
            }
        }
        MessageFormatConfig.setupDefaultFormats(formatDir);
        File[] files = formatDir.listFiles((file, name) -> name.endsWith(".cfg"));
        for (File file : files) {
            MessageFormatConfig dfc = new MessageFormatConfig(file);
            dfc.loadFormats();
            displayFormats.put(dfc.getName(), dfc);
        }
        defaultDisplayFormat = displayFormats.get(MessageFormatConfig.DEFAULT_FORMAT);
    }

    private static void loadTrustedServers(File configDir) {
        trustedServers.clear();
        Gson gson = new Gson();
        try {
            Reader reader = new FileReader(new File(configDir, "trusted_servers.json"));
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(true);
            JsonArray serverArray = gson.fromJson(jsonReader, JsonArray.class);
            for (int i = 0; i < serverArray.size(); i++) {
                addTrustedServer(TrustedServer.loadFromJson(serverArray.get(i).getAsJsonObject()));
            }
            reader.close();
        } catch (JsonSyntaxException e) {
            logger.error("Syntax error in trusted_servers.json: ", e);
            failedToLoad.add("trusted_servers.json");
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveTrustedServers() {
        Gson gson = new Gson();
        try {
            JsonArray serverArray = new JsonArray();
            for (TrustedServer trustedServer : trustedServers.values()) {
                serverArray.add(trustedServer.toJsonObject());
            }
            JsonWriter writer = new JsonWriter(new FileWriter(new File(baseConfigDir, "eirairc/trusted_servers.json")));
            writer.setIndent("  ");
            gson.toJson(serverArray, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadServices(File configDir) {
        if (!configDir.exists()) {
            if (!configDir.mkdirs()) {
                return;
            }
        }
        Configuration serviceConfig = new Configuration(new File(configDir, "services.cfg"));
        ServiceConfig.setupDefaultServices(serviceConfig);
        ServiceConfig.load(serviceConfig);
    }

    public static void loadSuggestedChannels(IResourceManager resourceManager) throws IOException {
        suggestedChannels.clear();
        InputStream in;
        File overrideFile = new File(baseConfigDir, "eirairc/suggested-channels.json");
        if (overrideFile.exists()) {
            in = new FileInputStream(overrideFile);
        } else {
            URL remoteURL = new URL("https://raw.githubusercontent.com/blay09/EiraIRC/master/src/main/resources/assets/eirairc/suggested-channels.json");
            try {
                in = remoteURL.openStream();
            } catch (IOException e) {
                in = resourceManager.getResource(new ResourceLocation("eirairc", "suggested-channels.json")).getInputStream();
            }
        }
        Gson gson = new Gson();
        Reader reader = new InputStreamReader(in);
        JsonReader jsonReader = new JsonReader(reader);
        jsonReader.setLenient(true);
        try {
            JsonArray channelArray = gson.fromJson(jsonReader, JsonArray.class);
            for (int i = 0; i < channelArray.size(); i++) {
                suggestedChannels.add(SuggestedChannel.loadFromJson(channelArray.get(i).getAsJsonObject()));
            }
        } catch (JsonSyntaxException e) {
            logger.error("Syntax error in suggested-channels.json: ", e);
            failedToLoad.add("suggested-channels.json");
        }
        reader.close();
        in.close();
    }

    private static void loadCommands(File configDir) {
        customCommands.clear();
        if (!configDir.exists()) {
            if (!configDir.mkdirs()) {
                return;
            }
        }
        copyExampleFile("commands.json.example.txt");
        Gson gson = new Gson();
        try {
            File file = new File(configDir, "commands.json");
            if (!file.exists()) {
                JsonArray root = new JsonArray();
                JsonObject players = new JsonObject();
                players.addProperty("name", "players");
                players.addProperty("override", "who");
                players.addProperty("description", "Default alias players for the who command.");
                root.add(players);
                try {
                    JsonWriter writer = new JsonWriter(new FileWriter(new File(baseConfigDir, "eirairc/commands.json")));
                    writer.setIndent("  ");
                    gson.toJson(root, writer);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Reader reader = new FileReader(file);
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(true);
            JsonArray commandArray = gson.fromJson(jsonReader, JsonArray.class);
            for (int i = 0; i < commandArray.size(); i++) {
                JsonObject obj = commandArray.get(i).getAsJsonObject();
                String type = "custom";
                if (obj.has("type")) {
                    type = obj.get("type").getAsString();
                }
                if (type.equals("custom")) {
                    customCommands.add(BotCommandCustom.loadFromJson(obj));
                } else if (type.equals("remote")) {
                    remoteCommands.add(RemoteBotCommand.loadFromJson(obj));
                }
            }
            reader.close();
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JsonSyntaxException e) {
            logger.error("Syntax error in commands.json: ", e);
            failedToLoad.add("commands.json");
        }
    }


    private static void loadServers(File configDir) {
        serverConfigs.clear();
        if (!configDir.exists()) {
            if (!configDir.mkdirs()) {
                return;
            }
        }
        copyExampleFile("servers.json.example.txt");
        Gson gson = new Gson();
        try {
            Reader reader = new FileReader(new File(configDir, "servers.json"));
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(true);
            JsonArray serverArray = gson.fromJson(jsonReader, JsonArray.class);
            for (int i = 0; i < serverArray.size(); i++) {
                addServerConfig(ServerConfig.loadFromJson(serverArray.get(i).getAsJsonObject()));
            }
            reader.close();
        } catch (JsonSyntaxException e) {
            logger.error("Syntax error in servers.json: ", e);
            failedToLoad.add("servers.json");
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveServers() {
        Gson gson = new Gson();
        try {
            JsonArray serverArray = new JsonArray();
            for (ServerConfig serverConfig : serverConfigs.values()) {
                serverArray.add(serverConfig.toJsonObject());
            }
            JsonWriter writer = new JsonWriter(new FileWriter(new File(baseConfigDir, "eirairc/servers.json")));
            writer.setIndent("  ");
            gson.toJson(serverArray, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void copyExampleFile(String fileName) {
        String exampleFileInput = "/assets/eirairc/" + fileName;
        File exampleFileOutput = new File(baseConfigDir, "eirairc/" + fileName);
        try (InputStreamReader reader = new InputStreamReader(ConfigurationHandler.class.getResourceAsStream(exampleFileInput)); FileWriter writer = new FileWriter(exampleFileOutput)) {
            IOUtils.copy(reader, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load(File baseConfigDir) {
        ConfigurationHandler.baseConfigDir = baseConfigDir;
        File configDir = new File(baseConfigDir, "eirairc");

        loadServices(configDir);
        loadDisplayFormats(new File(configDir, "formats"));

        EiraIRC.proxy.loadConfig(configDir, true);

        loadCommands(configDir);
        loadServers(configDir);
        loadTrustedServers(configDir);

        AuthManager.load(new File("."));
        IgnoreList.load(configDir);
    }

    public static void save() {
        EiraIRC.proxy.saveConfig();

        saveServers();
        saveTrustedServers();
    }

    public static void reloadAll() {
        failedToLoad.clear();
        load(baseConfigDir);
        for (IRCConnection connection : ConnectionManager.getConnections()) {
            ((IRCBotImpl) connection.getBot()).reloadCommands();
        }
    }

    public static void lightReload() {
        File configDir = new File(baseConfigDir, "eirairc");

        EiraIRC.proxy.loadConfig(configDir, false);
    }

    public static ServerConfig getOrCreateServerConfig(String host) {
        ServerConfig serverConfig = serverConfigs.get(host.toLowerCase());
        if (serverConfig == null) {
            serverConfig = new ServerConfig(host);
        }
        return serverConfig;
    }

    public static Collection<ServerConfig> getServerConfigs() {
        return serverConfigs.values();
    }

    public static ServerConfig getServerConfig(String address) {
        return serverConfigs.get(address.toLowerCase());
    }

    public static void addServerConfig(ServerConfig serverConfig) {
        serverConfigs.put(serverConfig.getAddress().toLowerCase(), serverConfig);
    }

    public static ServerConfig removeServerConfig(String host) {
        return serverConfigs.remove(host.toLowerCase());
    }

    public static boolean hasServerConfig(String host) {
        return serverConfigs.containsKey(host.toLowerCase());
    }

    public static void addTrustedServer(TrustedServer server) {
        trustedServers.put(server.getAddress(), server);
    }

    public static TrustedServer getOrCreateTrustedServer(String address) {
        TrustedServer server = trustedServers.get(address.toLowerCase());
        if (server == null) {
            server = new TrustedServer(address);
        }
        return server;
    }

    public static void handleConfigCommand(ICommandSender sender, String target, String key, String value) {
        if (target.equals("global")) {
            boolean result = EiraIRC.proxy.handleConfigCommand(sender, key, value);
            if (result) {
                Utils.sendLocalizedMessage(sender, "commands.config.change", "Global", key, value);
                ConfigurationHandler.save();
            } else {
                Utils.sendLocalizedMessage(sender, "commands.config.invalidOption", "Global", key);
            }
        } else {
            ChannelConfig channelConfig = ConfigHelper.resolveChannelConfig(target);
            if (channelConfig != null) {
                channelConfig.handleConfigCommand(sender, key, value);
            } else {
                ServerConfig serverConfig = ConfigHelper.resolveServerConfig(target);
                if (serverConfig != null) {
                    serverConfig.handleConfigCommand(sender, key, value);
                } else {
                    Utils.sendLocalizedMessage(sender, "error.targetNotFound", target);
                }
            }
        }
    }

    public static void handleConfigCommand(ICommandSender sender, String target, String key) {
        if (target.equals("global")) {
            String result = EiraIRC.proxy.handleConfigCommand(sender, key);
            if (result != null) {
                Utils.sendLocalizedMessage(sender, "commands.config.lookup", "Global", key, result);
            } else {
                Utils.sendLocalizedMessage(sender, "commands.config.invalidOption", "Global", key);
            }
        } else {
            ChannelConfig channelConfig = ConfigHelper.resolveChannelConfig(target);
            if (channelConfig != null) {
                channelConfig.handleConfigCommand(sender, key);
            } else {
                ServerConfig serverConfig = ConfigHelper.resolveServerConfig(target);
                if (serverConfig != null) {
                    serverConfig.handleConfigCommand(sender, key);
                } else {
                    Utils.sendLocalizedMessage(sender, "error.targetNotFound", target);
                }
            }
        }
        save();
    }

    public static void addOptionsToList(List<String> list, String option, boolean autoCompleteOption) {
        EiraIRC.proxy.addConfigOptionsToList(list, option, autoCompleteOption);
    }

    public static MessageFormatConfig getMessageFormat(String displayMode) {
        MessageFormatConfig displayFormat = displayFormats.get(displayMode);
        if (displayFormat == null) {
            return defaultDisplayFormat;
        }
        return displayFormat;
    }

    public static List<IBotCommand> getCustomCommands() {
        return customCommands;
    }

    public static List<SuggestedChannel> getSuggestedChannels() {
        return suggestedChannels;
    }

    public static String[] getAvailableMessageFormats() {
        return displayFormats.keySet().toArray(new String[displayFormats.size()]);
    }

    public static boolean passesRemoteCommand(ICommandSender sender, String message) {
        for (RemoteBotCommand command : remoteCommands) {
            if (message.startsWith(command.command) && (!command.requireOp || Utils.isOP(sender))) {
                return true;
            }
        }
        return false;
    }
}
