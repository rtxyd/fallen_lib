package net.rtxyd.fallen.lib.util;

import java.util.List;

public class MiscUtil {
    public final static List<String> launcherTargets = List.of(
            "mcp",
            "srg",
            "fmlclientdev",
            "fmlclient",
            "fmlclientuserdev",
            "fmldatauserdev",
            "fmlserverdev",
            "fmlserveruserdev",
            "forgeclientdev",
            "forgeclient",
            "forgeclientuserdev",
            "forgedatadev",
            "forgedatauserdev",
            "forgegametestserverdev",
            "forgegametestserveruserdev",
            "forgeserverdev",
            "forgeserver",
            "forgeserveruserdev"
    );

    public static List<String> specialLauncherTargets = List.of("mcp", "srg");

    public static List<String> productionLauncherTargets = List.of("fmlclient","forgeclient","forgeserver");

    public static String missingModMessage(String currentModId, String missingModId, String missingModVersion) {
        return String.format(                    "§c§l==================================================\n" +
                "§c§l  REQUIRED MOD '%2$s' IS MISSING!\n" +
                "§f\n" +
                "§e  %1$s §frequires §b%2$s §fto function properly.\n" +
                "§f\n" +
                "§a  Please ensure:\n" +
                "§7  • %2$s is in your mods folder\n" +
                "§7  • %2$s is version §b%3$s §f(or above)  \n" +
                "§7  • %2$s is not corrupted\n" +
                "§f\n" +
                "§6\n" +
                "§c§l==================================================", currentModId, missingModId, missingModVersion);
    }
}
