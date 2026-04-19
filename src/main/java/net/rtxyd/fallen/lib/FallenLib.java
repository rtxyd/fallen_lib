package net.rtxyd.fallen.lib;

import net.minecraftforge.fml.common.Mod;
import net.rtxyd.fallen.lib.config.BuildInfo;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(FallenLib.MODID)
public class FallenLib
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "fallen_lib";
    public static final String RUNTIME_MOD_NAME = "fallen_lib" + "-" + BuildInfo.VERSION + "-" + "runtime" + ".jar";
    public static final String MOD_NAME = "fallen_lib" + "-" + BuildInfo.VERSION + ".jar";
    public static final String RUNTIME_MOD_LOC = "META-INF/runtime/" + RUNTIME_MOD_NAME;
    public static final String SPEC_TITLE_ALL = "fallen_lib-all";
}
