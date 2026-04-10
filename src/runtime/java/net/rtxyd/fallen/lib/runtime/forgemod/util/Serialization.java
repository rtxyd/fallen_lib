package net.rtxyd.fallen.lib.runtime.forgemod.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import org.apache.logging.log4j.Logger;

public class Serialization {
    public static boolean isNotNullOrIsJsonObj(JsonElement element, ResourceLocation id, String path, Logger logger) {
        if (element == null) {
            logger.error("Null JSON: {} in {}", id, path);
            return false;
        }
        if (!element.isJsonObject()) {
            logger.error("Root is not JSON object: {} in {}", id, path);
            return false;
        }
        return true;
    }
    public static boolean checkEmptyJsonObjAndLog(JsonObject json, ResourceLocation id, String path, Logger logger) {
        if (json.entrySet().isEmpty()) {
            logger.error("Empty JSON object: {} in {}", id, path);
            return false;
        }
        return true;
    }

    public static boolean checkConditions(JsonObject json, ResourceLocation id, String path, Logger logger, ICondition.IContext context) {
        if (CraftingHelper.processConditions(json, "conditions", context) && CraftingHelper.processConditions(json, "forge:conditions", context)) {
            return true;
        }
        logger.trace("Conditions not met: {} in {}", id, path);
        return false;
    }
}
