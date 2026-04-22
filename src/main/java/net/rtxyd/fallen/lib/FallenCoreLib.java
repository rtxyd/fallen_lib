package net.rtxyd.fallen.lib;

import net.minecraftforge.fml.common.Mod;
import net.rtxyd.fallen.lib.config.BuildInfo;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(FallenCoreLib.FORGE_MODID)
public class FallenCoreLib
{
    // Define mod id in a common place for everything to reference
    public static final String FORGE_MODID = "fallen_lib";
    public static final String FORGE_FILE_NAME = "fallen_lib" + "-" + BuildInfo.VERSION + "-" + "runtime" + ".jar";
    public static final String IN_ONE_FILE = "fallen_lib" + "-" + BuildInfo.VERSION + ".jar";
    public static final String FORGE_MOD_LOC = "META-INF/runtime/" + FORGE_FILE_NAME;
    public static final String SPEC_TITLE_ALL = "fallen_lib-all";
}
