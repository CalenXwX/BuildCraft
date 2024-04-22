package buildcraft.factory.recipe;

import buildcraft.factory.BCFactory;
import buildcraft.lib.misc.JsonUtil;
import buildcraft.lib.recipe.RefineryRecipeRegistry.CoolableRecipe;
import buildcraft.lib.recipe.RefineryRecipeRegistry.HeatExchangeRecipe;
import buildcraft.lib.recipe.RefineryRecipeRegistry.HeatableRecipe;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;

public class HeatExchangeRecipeSerializer extends ForgeRegistryEntry<RecipeSerializer<?>> implements RecipeSerializer<HeatExchangeRecipe> {
    public static final HeatExchangeRecipeSerializer HEATABLE;
    public static final HeatExchangeRecipeSerializer COOLABLE;

    static {
        HEATABLE = new HeatExchangeRecipeSerializer(EnumHeatExchangeRecipeType.HEATABLE);
        COOLABLE = new HeatExchangeRecipeSerializer(EnumHeatExchangeRecipeType.COOLABLE);
        HEATABLE.setRegistryName(HeatableRecipe.TYPE_ID);
        COOLABLE.setRegistryName(CoolableRecipe.TYPE_ID);
    }

    private final EnumHeatExchangeRecipeType type;

    private HeatExchangeRecipeSerializer(EnumHeatExchangeRecipeType type) {
        this.type = type;
    }

    @Override
    public HeatExchangeRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
        String typeStr = GsonHelper.getAsString(json, "type").replace("heat_exchange/", "");
        FluidStack in = JsonUtil.deSerializeFluidStack(json.getAsJsonObject("in"));
        FluidStack out = JsonUtil.deSerializeFluidStack(json.getAsJsonObject("out"));
        int heatFrom = json.get("heatFrom").getAsInt();
        int heatTo = json.get("heatTo").getAsInt();
        return switch (this.type) {
            case COOLABLE -> new CoolableRecipe(recipeId, in, out, heatFrom, heatTo);
            case HEATABLE -> new HeatableRecipe(recipeId, in, out, heatFrom, heatTo);
        };
    }

    public static void toJson(HeatExchangeRecipeBuilder builder, JsonObject json) {
        json.addProperty("type", BCFactory.MODID + ":heat_exchange/" + builder.type.getlowerName());
        json.add("in", JsonUtil.serializeFluidStack(builder.in));
        json.add("out", JsonUtil.serializeFluidStack(builder.out));
        json.addProperty("heatFrom", builder.heatFrom);
        json.addProperty("heatTo", builder.heatTo);
    }

    @Nullable
    @Override
    public HeatExchangeRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        FluidStack in = buffer.readFluidStack();
        FluidStack out = buffer.readFluidStack();
        int heatFrom = buffer.readInt();
        int heatTo = buffer.readInt();
        return switch (this.type) {
            case COOLABLE -> new CoolableRecipe(recipeId, in, out, heatFrom, heatTo);
            case HEATABLE -> new HeatableRecipe(recipeId, in, out, heatFrom, heatTo);
        };
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, HeatExchangeRecipe recipe) {
        buffer.writeFluidStack(recipe.in());
        buffer.writeFluidStack(recipe.out());
        buffer.writeInt(recipe.heatFrom());
        buffer.writeInt(recipe.heatTo());
    }
}
