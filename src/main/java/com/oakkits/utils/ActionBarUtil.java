package com.oakkits.utils;

import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ActionBarUtil {
    
    private static boolean useSpigotApi = true;
    private static boolean initialized = false;
    
    private ActionBarUtil() {}
    
    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null) return;
        
        String coloredMessage = ColorUtil.colorize(message);
        
        if (!initialized) {
            initialize();
        }
        
        if (useSpigotApi) {
            try {
                player.spigot().sendMessage(
                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(coloredMessage)
                );
                return;
            } catch (Exception e) {
                useSpigotApi = false;
            }
        }
        
        sendActionBarNMS(player, coloredMessage);
    }
    
    private static void initialize() {
        initialized = true;
        try {
            Class.forName("net.md_5.bungee.api.ChatMessageType");
            useSpigotApi = true;
        } catch (ClassNotFoundException e) {
            useSpigotApi = false;
        }
    }
    
    private static void sendActionBarNMS(Player player, String message) {
        try {
            String version = player.getClass().getPackage().getName().split("\\.")[3];
            
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            Object craftPlayer = craftPlayerClass.cast(player);
            Method getHandleMethod = craftPlayerClass.getMethod("getHandle");
            Object entityPlayer = getHandleMethod.invoke(craftPlayer);
            
            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            Class<?> chatComponentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> chatSerializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            
            Method serializeMethod = chatSerializerClass.getMethod("a", String.class);
            Object chatComponent = serializeMethod.invoke(null, "{\"text\":\"" + message + "\"}");
            
            Constructor<?> packetConstructor;
            Object packet;
            
            try {
                Class<?> chatMessageTypeClass = Class.forName("net.minecraft.server." + version + ".ChatMessageType");
                Field gameInfoField = chatMessageTypeClass.getField("GAME_INFO");
                Object gameInfo = gameInfoField.get(null);
                packetConstructor = packetClass.getConstructor(chatComponentClass, chatMessageTypeClass);
                packet = packetConstructor.newInstance(chatComponent, gameInfo);
            } catch (Exception e) {
                packetConstructor = packetClass.getConstructor(chatComponentClass, byte.class);
                packet = packetConstructor.newInstance(chatComponent, (byte) 2);
            }
            
            Field playerConnectionField = entityPlayer.getClass().getField("playerConnection");
            Object playerConnection = playerConnectionField.get(entityPlayer);
            Method sendPacketMethod = playerConnection.getClass().getMethod("sendPacket", 
                Class.forName("net.minecraft.server." + version + ".Packet"));
            sendPacketMethod.invoke(playerConnection, packet);
            
        } catch (Exception e) {
            player.sendMessage(message);
        }
    }
}
