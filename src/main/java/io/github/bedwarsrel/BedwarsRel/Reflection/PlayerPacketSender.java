package io.github.bedwarsrel.BedwarsRel.Reflection;

import org.bukkit.entity.Player;
import org.inventivetalent.reflection.minecraft.Minecraft;
import org.inventivetalent.reflection.resolver.ConstructorResolver;
import org.inventivetalent.reflection.resolver.MethodResolver;
import org.inventivetalent.reflection.resolver.ResolverQuery;

public class PlayerPacketSender {

  protected static Object EMPTY_COMPONENT;

  static Class<?> IChatBaseComponent =
      ReflectionHelper.nmsClassResolver.resolveSilent("IChatBaseComponent");
  static Class<?> ChatSerializer = ReflectionHelper.nmsClassResolver.resolveSilent("ChatSerializer",
      "IChatBaseComponent$ChatSerializer");
  static Class<?> nmsPacketPlayOutChat =
      ReflectionHelper.nmsClassResolver.resolveSilent("PacketPlayOutChat");

  static Class<?> PacketPlayOutTitle = ReflectionHelper.classResolver
      .resolveSilent("net.minecraft.server." + Minecraft.getVersion() + "PacketPlayOutTitle");
  static Class<?> EnumTitleAction = ReflectionHelper.classResolver.resolveSilent(
      "net.minecraft.server." + Minecraft.getVersion() + "PacketPlayOutTitle$EnumTitleAction",
      "net.minecraft.server." + Minecraft.getVersion() + "EnumTitleAction");
  static ConstructorResolver PacketTitleConstructorResolver =
      new ConstructorResolver(PacketPlayOutTitle);
  static MethodResolver ChatSerializerMethodResolver = new MethodResolver(ChatSerializer);

  static Class<?> PacketPlayInClientCommand = ReflectionHelper.classResolver.resolveSilent(
      "net.minecraft.server." + Minecraft.getVersion() + "PacketPlayInClientCommand");
  static Class<?> EnumClientCommand = ReflectionHelper.classResolver.resolveSilent(
      "net.minecraft.server." + Minecraft.getVersion()
          + "PacketPlayInClientCommand$EnumClientCommand",
      "net.minecraft.server." + Minecraft.getVersion() + "EnumClientCommand");
  static ConstructorResolver PacketClientCommandConstructorResolver =
      new ConstructorResolver(PacketPlayInClientCommand);


  public static void sendRawMessage(Player player, String json, int position) {
    try {
      Object serialized = serialize(json);
      Object packet =
          nmsPacketPlayOutChat.getConstructor(new Class[] {IChatBaseComponent, byte.class})
              .newInstance(new Object[] {serialized, (byte) position});
      if (packet != null) {
        ReflectionHelper.sendPacket(player, packet);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void sendTitle(Player player, String json) {
    try {
      Object serialized = serialize(json);
      Object packetTitle =
          PacketTitleConstructorResolver.resolve(new Class[] {EnumTitleAction, IChatBaseComponent})
              .newInstance(EnumTitleAction.getEnumConstants()[0], serialized);
      ReflectionHelper.sendPacket(player, packetTitle);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void sendTitle(Player player, String json, double fadeIn, double stay,
      double fadeOut) {
    sendTitleTimings(player, fadeIn, stay, fadeOut);
    sendTitle(player, json);
  }

  public static void sendSubTitle(Player player, String json) {
    try {
      Object serialized = serialize(json);
      Object packetTitle =
          PacketTitleConstructorResolver.resolve(new Class[] {EnumTitleAction, IChatBaseComponent})
              .newInstance(EnumTitleAction.getEnumConstants()[1], serialized);
      ReflectionHelper.sendPacket(player, packetTitle);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void sendSubTitle(Player player, String json, double fadeIn, double stay,
      double fadeOut) {
    sendTitleTimings(player, fadeIn, stay, fadeOut);
    sendSubTitle(player, json);
  }

  public static void sendTitleTimings(Player player, double fadeIn, double stay, double fadeOut) {
    try {
      Object packetTitle =
          PacketTitleConstructorResolver.resolve(new Class[] {int.class, int.class, int.class})
              .newInstance((int) Math.round(fadeIn * 20), (int) Math.round(stay * 20),
                  (int) Math.round(fadeOut * 20));
      ReflectionHelper.sendPacket(player, packetTitle);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void clearTitle(Player player) {
    try {
      Object packetTitle =
          PacketTitleConstructorResolver.resolve(new Class[] {EnumTitleAction, IChatBaseComponent})
              .newInstance(EnumTitleAction.getEnumConstants()[3], null);
      ReflectionHelper.sendPacket(player, packetTitle);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void resetTitle(Player player) {
    try {
      Object packetTitle =
          PacketTitleConstructorResolver.resolve(new Class[] {EnumTitleAction, IChatBaseComponent})
              .newInstance(EnumTitleAction.getEnumConstants()[4], null);
      ReflectionHelper.sendPacket(player, packetTitle);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void respawnPlayer(Player player) {
    try {
      Object packetRespawn =
          PacketClientCommandConstructorResolver.resolve(new Class[] {EnumClientCommand})
              .newInstance(EnumClientCommand.getEnumConstants()[0]);
      ReflectionHelper.a(player, packetRespawn);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static Object serialize(String json) throws ReflectiveOperationException {
    return ChatSerializerMethodResolver.resolve(new ResolverQuery("a", String.class)).invoke(null,
        json);
  }

  public static String toJson(String str) {
    if (str.startsWith("{") && str.endsWith("}"))
      return str;
    return "{\"text\":\"" + str + "\"}";
  }

}
