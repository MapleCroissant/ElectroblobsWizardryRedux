package com.binaris.wizardry.client.gui.screens;

import com.binaris.wizardry.WizardryMainMod;
import com.binaris.wizardry.api.client.util.ClientUtils;
import com.binaris.wizardry.api.client.util.GlyphClientHandler;
import com.binaris.wizardry.api.content.spell.Spell;
import com.binaris.wizardry.client.gui.screens.abstr.SpellInfoScreen;
import com.binaris.wizardry.content.block.IndexedLecternBlock;
import com.binaris.wizardry.content.blockentity.LecternBlockEntity;
import com.binaris.wizardry.content.data.SpellGlyphData;
import com.binaris.wizardry.core.networking.c2s.SelectLecternSpellC2S;
import com.binaris.wizardry.core.platform.Services;
import com.binaris.wizardry.setup.registries.EBSounds;
import com.binaris.wizardry.setup.registries.Spells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import com.binaris.wizardry.api.client.ParticleBuilder;
import com.binaris.wizardry.api.content.util.VecUtils;
import com.binaris.wizardry.setup.registries.client.EBParticles;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;


import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LecternScreen extends SpellInfoScreen {
    private static final ResourceLocation TEXTURE = WizardryMainMod.location("textures/gui/container/lectern.png");
    private static final int PAGE_SIZE = 18;
    private final BlockPos pos;
    private final Button[] entries = new Button[PAGE_SIZE];
    private Map<Spell, Set<BlockPos>> index = Map.of();
    private List<Spell> matches = List.of();
    private Spell selected;
    private EditBox search;
    private Button previous;
    private Button next;
    private Button sort;
    private Button order;
    private Button contents;
    private Button locate;
    private int page;
    private int sortMode;
    private boolean descending;
    private int refreshTicks;

    private LecternScreen(BlockPos pos, Spell selected) {
        super(288, 180, Component.translatable("gui.ebwizardry.lectern.title"));
        this.pos = pos;
        this.selected = selected;
        setTextureSize(512, 512);
    }

    public static void open(BlockPos pos) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.level.getBlockState(pos).getBlock() instanceof IndexedLecternBlock)) return;
        var selected = minecraft.level.getBlockEntity(pos) instanceof LecternBlockEntity lectern && lectern.selected() != null
                ? Services.REGISTRY_UTIL.getSpell(lectern.selected()) : null;
        minecraft.setScreen(new LecternScreen(pos, selected == null ? Spells.NONE : selected));
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        String query = search == null ? "" : search.getValue();
        search = new EditBox(font, x + 157, y + 18, 109, 16, Component.translatable("gui.ebwizardry.lectern.search"));
        search.setMaxLength(50);
        search.setBordered(false);
        search.setTextColor(0x443322);
        search.setTextColorUneditable(0x443322);
        search.setHint(Component.translatable("gui.ebwizardry.lectern.search"));
        search.setValue(query);
        search.setResponder(value -> { page = 0; updateEntries(); });
        addRenderableWidget(search);
        sort = addRenderableWidget(Button.builder(sortLabel(), button -> {
            sortMode = (sortMode + 1) % 3;
            button.setMessage(sortLabel());
            page = 0;
            updateEntries();
        }).bounds(x + 22, y + 16, 90, 18).build());
        order = addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            descending = !descending;
            updateEntries();
        }).bounds(x + 115, y + 16, 20, 18).tooltip(Tooltip.create(Component.translatable("gui.ebwizardry.lectern.order"))).build());
        for (int i = 0; i < PAGE_SIZE; i++) {
            int slot = i;
            int xx = x + 23 + i % 3 * 38 + (i >= 9 ? 132 : 0);
            int yy = y + 43 + i / 3 % 3 * 38;
            entries[i] = addRenderableWidget(Button.builder(Component.empty(), button -> select(matches.get(page * PAGE_SIZE + slot)))
                    .bounds(xx, yy, 29, 29).build());
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> { page--; updateEntries(); })
                .bounds(x + 22, y + 149, 24, 18).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> { page++; updateEntries(); })
                .bounds(x + 242, y + 149, 24, 18).build());
        contents = addRenderableWidget(Button.builder(Component.translatable("gui.ebwizardry.lectern.index"), button -> select(Spells.NONE))
                .bounds(x + 20, y + 149, 55, 18).build());
        locate = addRenderableWidget(Button.builder(Component.translatable("gui.ebwizardry.lectern.locate"), button -> locate())
                .bounds(x + 76, y + 149, 65, 18).build());
        refresh();
    }

    private Component sortLabel() {
        return Component.translatable("gui.ebwizardry.lectern.sort_" + switch (sortMode) {
            case 1 -> "element";
            case 2 -> "name";
            default -> "tier";
        });
    }

    private Component name(Spell spell) {
        return ClientUtils.shouldDisplayDiscovered(spell, null) ? spell.getDescriptionFormatted()
                : SpellGlyphData.getGlyphNameFormatted(spell, GlyphClientHandler.INSTANCE.getGlyphData());
    }

    private void refresh() {
        index = LecternBlockEntity.index(minecraft.level, pos);
        if (!index.containsKey(selected)) selected = Spells.NONE;
        updateEntries();
    }

    private void updateEntries() {
        String query = search.getValue().toLowerCase(Locale.ROOT).trim();
        Comparator<Spell> byName = Comparator.comparing(spell -> name(spell).getString().toLowerCase(Locale.ROOT));
        Comparator<Spell> comparator = switch (sortMode) {
            case 1 -> Comparator.comparing(spell -> ClientUtils.shouldDisplayDiscovered(spell, null) ? spell.getElement().getLocation().toString() : "");
            case 2 -> byName;
            default -> Comparator.comparingInt(spell -> spell.getTier().getLevel());
        };
        comparator = comparator.thenComparing(byName).thenComparing(Spell::getLocation);
        matches = index.keySet().stream().filter(spell -> matchesQuery(spell, query))
                .sorted(descending ? comparator.reversed() : comparator).toList();
        page = Math.clamp(page, 0, Math.max(0, (matches.size() - 1) / PAGE_SIZE));
        boolean viewingIndex = selected == Spells.NONE;
        search.visible = sort.visible = order.visible = previous.visible = next.visible = viewingIndex;
        contents.visible = locate.visible = !viewingIndex;
        order.setMessage(Component.literal(descending ? "-" : "+"));
        previous.active = page > 0;
        next.active = (page + 1) * PAGE_SIZE < matches.size();
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = page * PAGE_SIZE + i;
            var entry = entries[i];
            entry.visible = viewingIndex && index < matches.size();
            if (entry.visible) entry.setTooltip(Tooltip.create(name(matches.get(index))));
        }
    }

    private boolean matchesQuery(Spell spell, String query) {
        if (query.isEmpty()) return true;
        boolean discovered = ClientUtils.shouldDisplayDiscovered(spell, null);
        for (String condition : query.split(";")) {
            String[] parts = condition.trim().split("=", 2);
            if (parts.length != 2) return discovered && name(spell).getString().toLowerCase(Locale.ROOT).contains(query);
            String value = switch (parts[0]) {
                case "tier", "t" -> spell.getTier().getDescriptionFormatted().getString();
                case "element", "e" -> discovered ? spell.getElement().getDescriptionFormatted().getString() : "";
                case "type", "p" -> discovered ? Component.translatable(spell.getType().getDisplayName()).getString() : "";
                case "modid", "m" -> discovered ? spell.getLocation().getNamespace() : "";
                case "discovered", "d" -> Boolean.toString(discovered);
                default -> null;
            };
            if (value == null) return discovered && name(spell).getString().toLowerCase(Locale.ROOT).contains(query);
            if (value.isEmpty()) return false;
            String normalized = value.toLowerCase(Locale.ROOT);
            if (java.util.Arrays.stream(parts[1].split(",")).map(String::trim).noneMatch(normalized::contains)) return false;
        }
        return true;
    }

    private void select(Spell spell) {
        selected = spell;
        Services.NETWORK_HELPER.sendToServer(new SelectLecternSpellC2S(pos, spell.getLocation()));
        updateEntries();
    }

    private void locate() {
        var level = minecraft.level;
        if (level == null || minecraft.player == null) return;
        for (var shelf : index.getOrDefault(selected, Set.of())) {
            level.playLocalSound(shelf, EBSounds.BLOCK_LECTERN_LOCATE_SPELL.get(), SoundSource.BLOCKS, 1, 0.7f, false);
            for (Direction face : Direction.values()) {
                var position = VecUtils.getFaceCentre(shelf, face).relative(face, 0.005);
                ParticleBuilder.create(EBParticles.BLOCK_HIGHLIGHT).pos(position).face(face).spawn(level);
            }
        }
        onClose();
    }

    @Override
    public void tick() {
        if (minecraft.level == null || minecraft.player == null || !minecraft.player.canInteractWithBlock(pos, 0)
                || !(minecraft.level.getBlockState(pos).getBlock() instanceof IndexedLecternBlock)) {
            onClose();
            return;
        }
        if (++refreshTicks % 10 == 0) refresh();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public Spell getSpell() { return selected; }

    @Override
    public ResourceLocation getTexture() { return TEXTURE; }

    @Override
    protected void drawBackgroundLayer(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        if (selected != Spells.NONE) {
            super.drawBackgroundLayer(graphics, x, y, mouseX, mouseY);
            return;
        }
        for (int i = 0; i < PAGE_SIZE; i++) {
            int slot = page * PAGE_SIZE + i;
            Spell spell = slot < matches.size() ? matches.get(slot) : Spells.NONE;
            if (!ClientUtils.shouldDisplayDiscovered(spell, null)) spell = Spells.NONE;
            int iconX = x + 24 + i % 3 * 38 + (i >= 9 ? 132 : 0);
            int iconY = y + 45 + i / 3 % 3 * 38;
            graphics.blit(spell.getIcon(), iconX, iconY, 32, 32, 0, 0, 128, 128, 128, 128);
        }
        graphics.blit(TEXTURE, x, y, 0, 256, xSize, ySize, 512, 512);
    }

    @Override
    protected void drawForegroundLayer(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        if (selected != Spells.NONE) {
            super.drawForegroundLayer(graphics, x, y, mouseX, mouseY);
            return;
        }
        for (int i = 0; i < PAGE_SIZE && page * PAGE_SIZE + i < matches.size(); i++) {
            var entry = entries[i];
            var spell = matches.get(page * PAGE_SIZE + i);
            if (!ClientUtils.shouldDisplayDiscovered(spell, null)) spell = Spells.NONE;
            graphics.blit(spell.getIcon(), entry.getX() + 2, entry.getY() + 2, 25, 25, 0, 0, 128, 128, 128, 128);
        }
        graphics.drawCenteredString(font, (page + 1) + " / " + Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE),
                x + xSize / 2, y + 153, 0x413227);
    }
}
