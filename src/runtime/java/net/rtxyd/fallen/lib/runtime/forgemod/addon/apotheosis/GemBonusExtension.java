package net.rtxyd.fallen.lib.runtime.forgemod.addon.apotheosis;

import dev.shadowsoffire.apotheosis.adventure.socket.gem.bonus.GemBonus;

public interface GemBonusExtension {
    void fallen_lib$appendExtraBonus(GemBonus bonus);
    void fallen_lib$clearExtraBonuses();
}