package com.binaris.wizardry.integration.jei;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.content.util.*;
import com.binaris.wizardry.content.item.WandItem;
import com.binaris.wizardry.content.recipe.ImbuementAltarRecipe;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.*;
import mezz.jei.api.*;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.*;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;

/** Optional JEI integration: discovered by JEI only, with no common-loader links. */
@JeiPlugin
public final class WizardryJeiPlugin implements IModPlugin {
    public static final RecipeType<ImbuementAltarRecipe> ALTAR = RecipeType.create("ebwizardry", "imbuement_altar", ImbuementAltarRecipe.class);
    public static final RecipeType<WorkbenchDisplay> WORKBENCH = RecipeType.create("ebwizardry", "arcane_workbench", WorkbenchDisplay.class);
    public record WorkbenchDisplay(ItemStack wand, ItemStack book, ItemStack result) {}

    @Override public ResourceLocation getPluginUid() { return WizardryMainMod.location("jei"); }
    @Override public void registerItemSubtypes(ISubtypeRegistration registration) {
        ISubtypeInterpreter<ItemStack> interpreter = new ISubtypeInterpreter<>() {
            @Override public Object getSubtypeData(ItemStack stack, UidContext context) {
                if (stack.getItem() instanceof WandItem) return CastItemDataHelper.getSpells(stack).stream().map(spell -> spell.getLocation().toString()).toList();
                var tag = ItemData.read(stack);
                return tag.isEmpty() ? null : tag.toString();
            }
            @Override public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                Object data = getSubtypeData(stack, context);
                return data == null ? "" : data.toString();
            }
        };
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof WandItem || item == EBItems.SPELL_BOOK.get() || item == EBItems.SCROLL.get()
                    || item == EBItems.ARCANE_TOME.get() || item == EBItems.RANDOM_SPELL_BOOK.get()) registration.registerSubtypeInterpreter(item, interpreter);
        }
    }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new AltarCategory(gui), new WorkbenchCategory(gui));
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        registration.addRecipes(ALTAR, level.getRecipeManager().getAllRecipesFor(EBRecipeTypes.IMBUEMENT_ALTAR).stream().map(holder -> holder.value()).toList());
        List<WorkbenchDisplay> displays = new ArrayList<>();
        Services.REGISTRY_UTIL.getSpells().stream().filter(spell -> spell != Spells.NONE && !spell.property(com.binaris.wizardry.content.spell.DefaultProperties.SENSIBLE)).forEach(spell -> {
            ItemStack wand = new ItemStack(RegistryUtils.getWand(spell.getTier(), Elements.MAGIC));
            ItemStack result = wand.copy();
            CastItemDataHelper.setSpells(result, List.of(spell));
            displays.add(new WorkbenchDisplay(wand, RegistryUtils.spellBookItem(spell), result));
        });
        Services.REGISTRY_UTIL.getTiers().stream().filter(tier -> tier.next() != null && tier.next() != tier).forEach(tier -> {
            Services.REGISTRY_UTIL.getElements().forEach(element -> {
                ItemStack wand = new ItemStack(RegistryUtils.getWand(tier, element));
                ItemStack result = new ItemStack(RegistryUtils.getWand(tier.next(), element));
                if (wand.isEmpty() || result.isEmpty()) return; // Add-on tiers need not supply base-mod wands.
                CastItemDataHelper.setProgression(wand, tier.next().getProgression());
                displays.add(new WorkbenchDisplay(wand, RegistryUtils.getArcaneTome(tier.next()), result));
            });
        });
        registration.addRecipes(WORKBENCH, displays);
        registration.addItemStackInfo(new ItemStack(EBItems.BLANK_SCROLL.get()), Component.literal("Bind a discovered spell from a spell book to create a single-use scroll."));
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(EBBlocks.IMBUEMENT_ALTAR.get(), ALTAR);
        registration.addRecipeCatalyst(EBBlocks.ARCANE_WORKBENCH.get(), WORKBENCH);
    }

    private abstract static class Category<T> implements IRecipeCategory<T> {
        final IDrawable background, icon;
        final Component title;
        Category(IGuiHelper gui, ItemStack icon, String title) {
            this.background = gui.createBlankDrawable(150, 76);
            this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, icon);
            this.title = Component.literal(title);
        }
        @Override public IDrawable getBackground() { return background; }
        @Override public IDrawable getIcon() { return icon; }
        @Override public Component getTitle() { return title; }
        @Override public void draw(T recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots, GuiGraphics graphics, double x, double y) {
            graphics.drawString(Minecraft.getInstance().font, "→", 96, 30, 0x404040, false);
        }
    }
    private static final class AltarCategory extends Category<ImbuementAltarRecipe> {
        AltarCategory(IGuiHelper gui) { super(gui, new ItemStack(EBBlocks.IMBUEMENT_ALTAR.get()), "Imbuement Altar"); }
        @Override public RecipeType<ImbuementAltarRecipe> getRecipeType() { return ALTAR; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, ImbuementAltarRecipe recipe, IFocusGroup focuses) {
            builder.addInputSlot(34, 29).addIngredients(recipe.getCenterIngredient());
            int[][] positions = {{34, 5}, {10, 29}, {58, 29}, {34, 53}};
            for (int i = 0; i < 4; i++) {
                var slot = builder.addInputSlot(positions[i][0], positions[i][1]);
                if (recipe.getReceptacleIngredients() != null) slot.addIngredients(recipe.getReceptacleIngredients().get(i));
                else slot.addItemStacks(recipe.getPollTags().stream().flatMap(tag -> net.minecraft.world.item.crafting.Ingredient.of(tag).getItems().length == 0 ? java.util.stream.Stream.empty() : Arrays.stream(net.minecraft.world.item.crafting.Ingredient.of(tag).getItems())).toList());
            }
            builder.addOutputSlot(126, 29).addItemStacks(recipe.getPossibleResults());
        }
        @Override public void draw(ImbuementAltarRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots, GuiGraphics graphics, double x, double y) {
            super.draw(recipe, slots, graphics, x, y);
            if (recipe.hasPoll()) graphics.drawString(Minecraft.getInstance().font, "Weighted result", 80, 57, 0x404040, false);
        }
    }
    private static final class WorkbenchCategory extends Category<WorkbenchDisplay> {
        WorkbenchCategory(IGuiHelper gui) { super(gui, new ItemStack(EBBlocks.ARCANE_WORKBENCH.get()), "Arcane Workbench"); }
        @Override public RecipeType<WorkbenchDisplay> getRecipeType() { return WORKBENCH; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, WorkbenchDisplay recipe, IFocusGroup focuses) {
            builder.addInputSlot(10, 29).addItemStack(recipe.wand());
            builder.addInputSlot(50, 29).addItemStack(recipe.book());
            builder.addOutputSlot(126, 29).addItemStack(recipe.result());
        }
        @Override public void draw(WorkbenchDisplay recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots, GuiGraphics graphics, double x, double y) {
            super.draw(recipe, slots, graphics, x, y);
            if (!(recipe.book().getItem() instanceof com.binaris.wizardry.content.item.SpellBookItem)) {
                graphics.drawString(Minecraft.getInstance().font, "Requires progression", 5, 58, 0x404040, false);
            }
        }
    }
}
