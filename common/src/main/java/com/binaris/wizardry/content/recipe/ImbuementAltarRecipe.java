package com.binaris.wizardry.content.recipe;

import com.binaris.wizardry.api.content.item.IElementValue;
import com.binaris.wizardry.api.content.spell.Element;
import com.binaris.wizardry.setup.registries.EBRecipeTypes;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/// Defines a crafting recipe for the Imbuement Altar, requiring a center item placed on the
/// altar block and exactly four receptacle ingredients placed in surrounding Wall Receptacle
/// pedestals to produce a result.
///
/// The recipe uses custom matching logic separate from Minecraft's standard shaped/shapeless
/// recipes. Items are placed in the altar's five slots (one center plus four receptacle
/// pedestals). Matching greedily pairs each receptacle item with the first available unmatched
/// recipe ingredient, rejecting any arrangement where any pedestal is empty or any recipe
/// ingredient goes unmatched.
///
/// This recipe type is looked up at runtime by {@code ImbuementAltarBlockEntity} via
/// {@code EBRecipeTypes.IMBUEMENT_ALTAR} during both {@code checkRecipe()} (to validate
/// and start the imbuement timer) and {@code craftRecipe()} (to re-validate and produce
/// the output).
public class ImbuementAltarRecipe implements Recipe<ImbuementAltarRecipe.Input> {
    public record Input(ItemStack center, List<ItemStack> receptacles) implements RecipeInput {
        @Override public ItemStack getItem(int slot) { return slot == 0 ? center : receptacles.get(slot - 1); }
        @Override public int size() { return 1 + receptacles.size(); }
    }
    private final ResourceLocation id;
    @Nullable
    private final NonNullList<Ingredient> receptacleIngredients;
    private final Ingredient centerIngredient;
    private final ItemStack output;
    @Nullable
    private final List<TagKey<Item>> poll;
    @Nullable
    private final LinkedHashMap<TagKey<Item>, ItemStack> results;

    public ImbuementAltarRecipe(ResourceLocation id, NonNullList<Ingredient> receptacleIngredients, Ingredient centerIngredient, ItemStack output) {
        this(id, receptacleIngredients, centerIngredient, output, null, null);
    }

    public ImbuementAltarRecipe(ResourceLocation id, @Nullable NonNullList<Ingredient> receptacleIngredients, Ingredient centerIngredient, ItemStack output,
                                @Nullable List<TagKey<Item>> poll, @Nullable LinkedHashMap<TagKey<Item>, ItemStack> results) {
        this.id = id;
        this.receptacleIngredients = receptacleIngredients;
        this.centerIngredient = centerIngredient;
        this.output = output;
        this.poll = poll;
        this.results = results;
    }

    /// Reads an item stack from a JSON object.
    ///
    /// Checks for a valid item, optional count (default 1), an optional nbt data from that item and
    /// then returns the item stack. This is specially made by hand because Minecraft doesn't support
    /// nbt data in recipe JSONs for items.
    private static ItemStack itemStackFromJson(JsonObject stackObject) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(GsonHelper.getAsString(stackObject, "item")));
        int count = GsonHelper.getAsInt(stackObject, "count", 1);

        if (count < 1) {
            throw new JsonSyntaxException("Invalid output count: " + count);
        }

        ItemStack stack = new ItemStack(item, count);

        if (stackObject.has("nbt")) {
            try {
                CompoundTag nbt = TagParser.parseTag(GsonHelper.getAsString(stackObject, "nbt"));
                com.binaris.wizardry.api.content.util.ItemData.set(stack, nbt);
            } catch (Exception e) {
                throw new JsonParseException("Invalid NBT data: " + e.getMessage());
            }
        }

        return stack;
    }

    /// Checks whether the given center item and four receptacle items satisfy this recipe's
    /// ingredient requirements.
    ///
    /// Immediately rejects if fewer than exactly four receptacle items are provided or the
    /// center fails the ingredient test. For recipes with explicit receptacle ingredients, each
    /// receptacle item is greedily paired with the first available (unmatched) recipe ingredient
    /// that accepts it, requiring every recipe ingredient to be matched exactly once. Poll recipes
    /// without receptacle ingredients instead require every receptacle item to match at least one
    /// poll tag.
    ///
    /// @param centerStack the item placed in the altar's center slot.
    /// @param receptacleStacks an array of exactly four items from the surrounding receptacle
    ///                          pedestals.
    /// @return true if all ingredients are satisfied, false otherwise.
    public boolean matches(ItemStack centerStack, ItemStack[] receptacleStacks) {
        if (receptacleStacks.length != 4) return false;
        if (!centerIngredient.test(centerStack)) return false;

        if (receptacleIngredients == null) {
            for (ItemStack stack : receptacleStacks) {
                if (stack.isEmpty()) return false;
                boolean matchesPoll = false;
                if (poll != null) {
                    for (TagKey<Item> tag : poll) {
                        if (stack.is(tag)) {
                            matchesPoll = true;
                            break;
                        }
                    }
                }
                if (!matchesPoll) return false;
            }
            return true;
        }

        boolean[] matched = new boolean[4];
        for (int i = 0; i < 4; i++) {
            if (receptacleStacks[i].isEmpty()) return false;

            for (int j = 0; j < receptacleIngredients.size(); j++) {
                if (!matched[j] && receptacleIngredients.get(j).test(receptacleStacks[i])) {
                    matched[j] = true;
                    break;
                }
            }
        }

        for (boolean m : matched) {
            if (!m) return false;
        }

        return true;
    }

    @Override
    public boolean matches(@NotNull Input container, @NotNull Level level) {
        return matches(container.center(), container.receptacles().toArray(ItemStack[]::new));
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull Input container, @NotNull net.minecraft.core.HolderLookup.Provider access) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(@NotNull net.minecraft.core.HolderLookup.Provider access) {
        return output.copy();
    }

    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return EBRecipeTypes.IMBUEMENT_ALTAR_SERIALIZER;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return EBRecipeTypes.IMBUEMENT_ALTAR;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    /// Returns the list of four receptacle ingredients required by this recipe, or null for poll
    /// recipes that validate receptacle items against their poll tags instead.
    ///
    /// @return the NonNullList of receptacle ingredients, or null.
    @Nullable
    public NonNullList<Ingredient> getReceptacleIngredients() {
        return receptacleIngredients;
    }

    /// Returns the center ingredient required by this recipe.
    ///
    /// @return the center Ingredient, never null.
    public Ingredient getCenterIngredient() {
        return centerIngredient;
    }

    /// Checks whether the given item stack is used as a receptacle ingredient by any registered
    /// imbuement altar recipe.
    ///
    /// Used to decide whether an item can be placed on a receptacle pedestal. Poll recipes without
    /// receptacle ingredients accept any item matching one of their poll tags.
    ///
    /// @param level the level whose recipe manager is queried.
    /// @param stack the item stack to test.
    /// @return true if the item is required by at least one recipe, false otherwise.
    public static boolean isReceptacleItem(Level level, ItemStack stack) {
        return level.getRecipeManager().getAllRecipesFor(EBRecipeTypes.IMBUEMENT_ALTAR).stream()
                .filter(Objects::nonNull)
                .anyMatch(r -> r.value().acceptsReceptacleItem(stack));
    }

    private boolean acceptsReceptacleItem(ItemStack stack) {
        if (receptacleIngredients != null) {
            return receptacleIngredients.stream().anyMatch(ingredient -> ingredient.test(stack));
        }
        if (poll != null) {
            for (TagKey<Item> tag : poll) {
                if (stack.is(tag)) return true;
            }
        }
        return false;
    }

    /// Returns true if this recipe uses a poll of tags to select a weighted result instead of a
    /// fixed output. Poll recipes consume the receptacle items and randomly pick a winning tag,
    /// then use the result bound to that tag.
    public boolean hasPoll() {
        return poll != null;
    }

    /** Read-only display inputs for recipe viewers, preserving weighted output NBT. */
    public List<TagKey<Item>> getPollTags() {
        return poll == null ? List.of() : List.copyOf(poll);
    }

    public List<ItemStack> getPossibleResults() {
        return results == null ? List.of(output.copy()) : results.values().stream().map(ItemStack::copy).toList();
    }

    /// Rolls a winning poll tag using each receptacle item as a vote for every tag it matches.
    ///
    /// Each receptacle stack increments the weight of each poll tag whose items accept it, so the
    /// probability of a tag winning is proportional to the number of matching stacks (e.g. one
    /// healing dust, two lightning dusts and one fire dust give 25%/50%/25% odds).
    ///
    /// @param random the random source used for the weighted roll.
    /// @param receptacleStacks the four receptacle items.
    /// @return the winning tag, or null if no receptacle item matches any poll tag.
    @Nullable
    public TagKey<Item> rollWinner(RandomSource random, ItemStack[] receptacleStacks) {
        if (poll == null) return null;

        int[] weights = new int[poll.size()];
        int total = 0;
        for (ItemStack stack : receptacleStacks) {
            if (stack.isEmpty()) continue;
            for (int i = 0; i < poll.size(); i++) {
                if (stack.is(poll.get(i))) {
                    weights[i]++;
                    total++;
                }
            }
        }

        if (total == 0) return null;

        int roll = random.nextInt(total);
        for (int i = 0; i < poll.size(); i++) {
            roll -= weights[i];
            if (roll < 0) return poll.get(i);
        }
        return null;
    }

    /// Rolls a winning tag and returns a copy of the result item bound to it.
    ///
    /// @param random the random source used for the weighted roll.
    /// @param receptacleStacks the four receptacle items.
    /// @return the winning result stack, or {@link ItemStack#EMPTY} if nothing matches.
    public ItemStack getRandomResult(RandomSource random, ItemStack[] receptacleStacks) {
        TagKey<Item> winner = rollWinner(random, receptacleStacks);
        if (winner == null || results == null) return ItemStack.EMPTY;
        ItemStack result = results.get(winner);
        return result == null ? ItemStack.EMPTY : result.copy();
    }

    /// Rolls a winning tag and returns the element of the item contained in that tag, if any.
    ///
    /// Used by the imbuement altar to show the correct element particles while the imbuement is in
    /// progress and to resolve the result deterministically once the process finishes.
    ///
    /// @param random the random source used for the weighted roll.
    /// @param receptacleStacks the four receptacle items.
    /// @return the winning element, or null if nothing matches or the tag has no element item.
    @Nullable
    public Element getPollElement(RandomSource random, ItemStack[] receptacleStacks) {
        TagKey<Item> winner = rollWinner(random, receptacleStacks);
        if (winner == null) return null;
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(winner)) {
            if (holder.value() instanceof IElementValue elementValue) {
                return elementValue.getElement();
            }
        }
        return null;
    }

    /// Resolves the result of a poll recipe from a previously selected element, falling back to a
    /// fresh weighted roll if the element does not match any result tag.
    ///
    /// @param element the element selected when the imbuement started, may be null.
    /// @param random the random source used for the fallback weighted roll.
    /// @param receptacleStacks the four receptacle items.
    /// @return a copy of the resolved result stack, or {@link ItemStack#EMPTY} if nothing matches.
    public ItemStack getResultForElement(@Nullable Element element, RandomSource random, ItemStack[] receptacleStacks) {
        if (poll == null || results == null) return output.copy();

        if (element != null) {
            for (var entry : results.entrySet()) {
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(entry.getKey())) {
                    if (holder.value() instanceof IElementValue elementValue && elementValue.getElement().is(element.getLocation())) {
                        return entry.getValue().copy();
                    }
                }
            }
        }

        return getRandomResult(random, receptacleStacks);
    }

    /// Reads and writes {@code ImbuementAltarRecipe} instances from JSON, network packets, and
    /// data generation output.
    ///
    /// Registered as the map value behind {@code EBRecipeTypes.IMBUEMENT_ALTAR_SERIALIZER}
    /// (key {@code "imbuement_altar"}). Minecraft's recipe loader calls {@code fromJson} to
    /// parse JSON recipe files from data packs and generated resources, while {@code toNetwork}
    /// and {@code fromNetwork} synchronize recipes between server and client during login and
    /// datapack reloads. The Forge data generation pipeline ({@code ImbuementAltarRecipeBuilder})
    /// serializes recipe definitions to JSON using the same field names expected by
    /// {@code fromJson}.
    public static class Serializer implements RecipeSerializer<ImbuementAltarRecipe> {
        public static final com.mojang.serialization.MapCodec<ImbuementAltarRecipe> CODEC = new com.mojang.serialization.MapCodec<>() {
            @Override public <T> com.mojang.serialization.DataResult<ImbuementAltarRecipe> decode(
                    com.mojang.serialization.DynamicOps<T> ops, com.mojang.serialization.MapLike<T> input) {
                try {
                    JsonObject json = ops.convertTo(com.mojang.serialization.JsonOps.INSTANCE,
                            ops.createMap(input.entries())).getAsJsonObject();
                    return com.mojang.serialization.DataResult.success(new Serializer().fromJson(
                            com.binaris.wizardry.WizardryMainMod.location("imbuement_altar"), json));
                } catch (RuntimeException error) {
                    return com.mojang.serialization.DataResult.error(error::getMessage);
                }
            }
            @Override public <T> com.mojang.serialization.RecordBuilder<T> encode(ImbuementAltarRecipe recipe,
                    com.mojang.serialization.DynamicOps<T> ops, com.mojang.serialization.RecordBuilder<T> prefix) {
                JsonObject json = toJson(recipe);
                json.entrySet().forEach(entry -> prefix.add(entry.getKey(),
                        com.mojang.serialization.JsonOps.INSTANCE.convertTo(ops, entry.getValue())));
                return prefix;
            }
            @Override public <T> java.util.stream.Stream<T> keys(com.mojang.serialization.DynamicOps<T> ops) {
                return java.util.stream.Stream.of("center", "receptacles", "result", "poll", "results").map(ops::createString);
            }
        };
        public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ImbuementAltarRecipe> STREAM_CODEC =
                net.minecraft.network.codec.StreamCodec.of((buffer, recipe) -> new Serializer().toNetwork(buffer, recipe),
                        buffer -> new Serializer().fromNetwork(com.binaris.wizardry.WizardryMainMod.location("imbuement_altar"), buffer));
        @Override public com.mojang.serialization.MapCodec<ImbuementAltarRecipe> codec() { return CODEC; }
        @Override public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ImbuementAltarRecipe> streamCodec() { return STREAM_CODEC; }

        private static JsonObject stackJson(ItemStack stack) {
            JsonObject json = new JsonObject();
            json.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            json.addProperty("count", stack.getCount());
            CompoundTag data = com.binaris.wizardry.api.content.util.ItemData.read(stack);
            if (!data.isEmpty()) json.addProperty("nbt", data.toString());
            return json;
        }
        private static JsonObject toJson(ImbuementAltarRecipe recipe) {
            JsonObject json = new JsonObject();
            json.add("center", Ingredient.CODEC_NONEMPTY.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, recipe.centerIngredient).getOrThrow());
            if (recipe.receptacleIngredients != null) {
                com.google.gson.JsonArray receptacles = new com.google.gson.JsonArray();
                recipe.receptacleIngredients.forEach(ingredient -> receptacles.add(Ingredient.CODEC_NONEMPTY.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, ingredient).getOrThrow()));
                json.add("receptacles", receptacles);
            }
            if (recipe.poll == null) json.add("result", stackJson(recipe.output));
            else {
                com.google.gson.JsonArray poll = new com.google.gson.JsonArray();
                recipe.poll.forEach(tag -> { JsonObject entry = new JsonObject(); entry.addProperty("tag", tag.location().toString()); poll.add(entry); });
                json.add("poll", poll);
                com.google.gson.JsonArray results = new com.google.gson.JsonArray();
                recipe.results.forEach((tag, stack) -> { JsonObject entry = stackJson(stack); entry.addProperty("key", tag.location().toString()); results.add(entry); });
                json.add("results", results);
            }
            return json;
        }

        /// Expects a {@code "center"} ingredient object, and either a {@code "result"} item object or,
        /// when a {@code "poll"} array is present, a {@code "results"} array. The {@code "receptacles"}
        /// array (exactly 4 entries) is only required for fixed-result recipes; poll recipes validate
        /// the receptacle items against their poll tags instead, so the field can be omitted. The
        /// result may optionally include an {@code "nbt"} string for NBT-tagged outputs (used by
        /// ruined spell book repair recipes that attach a loot table). Each poll entry is a
        /// {@code "tag"} whose weight comes from the matching receptacle stacks, and each entry of
        /// {@code "results"} binds a poll tag (via its {@code "key"}) to the item (with optional
        /// {@code "count"} and {@code "nbt"}) produced when that tag wins the roll. Throws
        /// {@link JsonParseException} if the receptacle count is not exactly 4, if the NBT data is
        /// malformed, or if a result key does not match any poll tag.
        ///
        /// @param id the resource location for this recipe.
        /// @param json the JSON object to parse.
        /// @return a fully constructed ImbuementAltarRecipe.
        public @NotNull ImbuementAltarRecipe fromJson(@NotNull ResourceLocation id, @NotNull JsonObject json) {
            Ingredient centerIngredient = Ingredient.CODEC_NONEMPTY.parse(com.mojang.serialization.JsonOps.INSTANCE, GsonHelper.getAsJsonObject(json, "center")).getOrThrow();

            if (json.has("poll")) {
                List<TagKey<Item>> poll = new ArrayList<>();
                for (JsonElement element : GsonHelper.getAsJsonArray(json, "poll")) {
                    JsonObject pollObject = GsonHelper.convertToJsonObject(element, "poll entry");
                    poll.add(tagFromJson(pollObject));
                }

                LinkedHashMap<TagKey<Item>, ItemStack> results = new LinkedHashMap<>();
                for (JsonElement element : GsonHelper.getAsJsonArray(json, "results")) {
                    JsonObject resultObject = GsonHelper.convertToJsonObject(element, "results entry");
                    TagKey<Item> key = TagKey.create(Registries.ITEM, parseLocation(GsonHelper.getAsString(resultObject, "key")));
                    if (!poll.contains(key)) {
                        throw new JsonSyntaxException("Results key '" + key.location() + "' is not present in the poll tags");
                    }
                    results.put(key, itemStackFromJson(resultObject));
                }

                NonNullList<Ingredient> receptacleIngredients = null;
                if (json.has("receptacles")) {
                    receptacleIngredients = readReceptacles(json);
                }

                return new ImbuementAltarRecipe(id, receptacleIngredients, centerIngredient, ItemStack.EMPTY, poll, results);
            }

            NonNullList<Ingredient> receptacleIngredients = readReceptacles(json);
            ItemStack output = itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new ImbuementAltarRecipe(id, receptacleIngredients, centerIngredient, output);
        }

        /// Reads the {@code "receptacles"} JSON array, which must contain exactly 4 ingredients.
        ///
        /// @param json the recipe JSON object.
        /// @return the list of exactly four receptacle ingredients.
        private static NonNullList<Ingredient> readReceptacles(JsonObject json) {
            NonNullList<Ingredient> receptacleIngredients = NonNullList.withSize(4, Ingredient.EMPTY);
            var receptaclesArray = GsonHelper.getAsJsonArray(json, "receptacles");

            if (receptaclesArray.size() != 4) {
                throw new JsonParseException("Imbuement recipe must have exactly 4 receptacle ingredients");
            }

            for (int i = 0; i < 4; i++) {
                receptacleIngredients.set(i, Ingredient.CODEC_NONEMPTY.parse(com.mojang.serialization.JsonOps.INSTANCE, receptaclesArray.get(i)).getOrThrow());
            }

            return receptacleIngredients;
        }

        /// Reads an item tag from a JSON object expecting a single {@code "tag"} string property.
        ///
        /// @param tagObject the JSON object to parse.
        /// @return the parsed item tag.
        private static TagKey<Item> tagFromJson(JsonObject tagObject) {
            return TagKey.create(Registries.ITEM, parseLocation(GsonHelper.getAsString(tagObject, "tag")));
        }

        /// Parses a resource location string, throwing a {@link JsonSyntaxException} if it is invalid.
        ///
        /// @param name the resource location string to parse.
        /// @return the parsed resource location.
        private static ResourceLocation parseLocation(String name) {
            ResourceLocation location = ResourceLocation.tryParse(name);
            if (location == null) {
                throw new JsonSyntaxException("Invalid resource location: '" + name + "'");
            }
            return location;
        }

        /// Reads the center Ingredient and either the poll tags with their results or the four
        /// receptacle Ingredients and the output ItemStack, in the exact order written by
        /// {@code toNetwork}.
        ///
        /// @param id the resource location for this recipe.
        /// @param buf the packet buffer to read from.
        /// @return a fully constructed ImbuementAltarRecipe.
        public @NotNull ImbuementAltarRecipe fromNetwork(@NotNull ResourceLocation id, @NotNull net.minecraft.network.RegistryFriendlyByteBuf buf) {
            Ingredient centerIngredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);

            if (buf.readBoolean()) {
                NonNullList<Ingredient> receptacleIngredients = buf.readBoolean() ? readReceptaclesFromNetwork(buf) : null;

                int pollSize = buf.readVarInt();
                List<TagKey<Item>> poll = new ArrayList<>();
                for (int i = 0; i < pollSize; i++) {
                    poll.add(TagKey.create(Registries.ITEM, buf.readResourceLocation()));
                }

                int resultsSize = buf.readVarInt();
                LinkedHashMap<TagKey<Item>, ItemStack> results = new LinkedHashMap<>();
                for (int i = 0; i < resultsSize; i++) {
                    results.put(TagKey.create(Registries.ITEM, buf.readResourceLocation()), ItemStack.STREAM_CODEC.decode(buf));
                }

                return new ImbuementAltarRecipe(id, receptacleIngredients, centerIngredient, ItemStack.EMPTY, poll, results);
            }

            NonNullList<Ingredient> receptacleIngredients = readReceptaclesFromNetwork(buf);
            ItemStack output = ItemStack.STREAM_CODEC.decode(buf);
            return new ImbuementAltarRecipe(id, receptacleIngredients, centerIngredient, output);
        }

        private static NonNullList<Ingredient> readReceptaclesFromNetwork(net.minecraft.network.RegistryFriendlyByteBuf buf) {
            NonNullList<Ingredient> receptacleIngredients = NonNullList.withSize(4, Ingredient.EMPTY);
            for (int i = 0; i < 4; i++) {
                receptacleIngredients.set(i, Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            }
            return receptacleIngredients;
        }

        /// Writes the center Ingredient and then either the poll tags with their results or the
        /// four receptacle Ingredients and the output ItemStack, in a fixed order. Must match the
        /// read order in {@code fromNetwork}.
        ///
        /// @param buf the packet buffer to write to.
        /// @param recipe the recipe to serialize.
        public void toNetwork(@NotNull net.minecraft.network.RegistryFriendlyByteBuf buf, ImbuementAltarRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.centerIngredient);

            if (recipe.poll != null) {
                buf.writeBoolean(true);
                buf.writeBoolean(recipe.receptacleIngredients != null);
                if (recipe.receptacleIngredients != null) {
                    for (Ingredient ingredient : recipe.receptacleIngredients) {
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
                    }
                }
                buf.writeVarInt(recipe.poll.size());
                for (TagKey<Item> tag : recipe.poll) {
                    buf.writeResourceLocation(tag.location());
                }
                buf.writeVarInt(recipe.results.size());
                for (var entry : recipe.results.entrySet()) {
                    buf.writeResourceLocation(entry.getKey().location());
                    ItemStack.STREAM_CODEC.encode(buf, entry.getValue());
                }
                return;
            }

            buf.writeBoolean(false);
            for (Ingredient ingredient : recipe.receptacleIngredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
            ItemStack.STREAM_CODEC.encode(buf, recipe.output);
        }
    }
}
