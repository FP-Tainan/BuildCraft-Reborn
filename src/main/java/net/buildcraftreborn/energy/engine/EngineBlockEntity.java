package net.buildcraftreborn.energy.engine;

import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.craftenergy.api.EnergyNode;
import net.craftenergy.api.EnergySource;
import net.craftenergy.api.EnergyUnits;
import net.craftenergy.api.MultimeterReadable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Estado de um motor: energia no buffer (CW·tick), calor, combustível e o que a rede puxou.
 * A energia sai só pela frente, com sinal de redstone (BuildCraft: motor sem sinal não bombeia e
 * perde 1 MJ por tick, aqui 1.000 CW·tick).
 */
public class EngineBlockEntity extends BCBlockEntity implements MultimeterReadable {
    public static final int LOW_VOLTAGE = 220;
    /** BuildCraft: motor de redstone ≈ 0,05 MJ/t. */
    public static final long REDSTONE_OUTPUT = 50;
    /** BuildCraft: motor Stirling = 1 MJ/t, buffer de 1.000 MJ. */
    public static final long STIRLING_OUTPUT = 1_000;
    public static final long STIRLING_CAPACITY = EnergyUnits.fromCWh(1_000);
    /** Motor sem sinal perde 1 MJ por tick. */
    public static final long IDLE_LOSS = 1_000;
    public static final int[] CREATIVE_VOLTAGES = {220, 1_000, 2_400, 13_800};
    public static final int CREATIVE_LEVELS = 9;

    // dados sincronizados com a tela do Stirling (valores curtos)
    public static final int DATA_BURN = 0;
    public static final int DATA_ENERGY = 1;
    public static final int DATA_STAGE = 2;
    public static final int DATA_POWER = 3;
    /** Calor do motor a combustão em décimos de CCº. */
    public static final int DATA_HEAT = 4;
    public static final int DATA_COUNT = 5;

    private final Source source = new Source();
    /** Tanques e calor do motor a combustão; nulo nos outros motores. */
    private final @Nullable CombustionEngine combustion;
    private final SimpleContainer fuel = new SimpleContainer(1) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return EngineBlockEntity.this.level != null && EngineBlockEntity.this.level.fuelValues().isFuel(stack);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            EngineBlockEntity.this.setChanged();
        }
    };

    private long energy;
    /** Calor do motor de redstone, de 0 a 0,8 (nunca superaquece). */
    private double heat;
    private int burnTime;
    private int totalBurnTime;
    private int creativeLevel;
    private int creativeVoltage;
    private boolean powered;
    private long drawnThisTick;
    private long lastDraw;
    // o que o cliente precisa para desenhar
    private boolean active;
    private EngineStage stage = EngineStage.BLUE;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_BURN -> EngineBlockEntity.this.totalBurnTime <= 0 ? 0
                        : EngineBlockEntity.this.burnTime * 1000 / EngineBlockEntity.this.totalBurnTime;
                case DATA_ENERGY -> (int) (heatLevel() * 1000);
                case DATA_STAGE -> EngineBlockEntity.this.stage.ordinal();
                case DATA_POWER -> (int) Math.min(Short.MAX_VALUE, EngineBlockEntity.this.lastDraw);
                case DATA_HEAT -> EngineBlockEntity.this.combustion == null ? 0 : (int) Math.round(EngineBlockEntity.this.combustion.heat() * 10);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public EngineBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.ENGINE.get(), pos, state);
        this.combustion = state.getBlock() instanceof EngineBlock engine && engine.kind() == EngineBlock.Kind.COMBUSTION
                ? new CombustionEngine(this::setChanged) : null;
    }

    public EngineBlock.Kind kind() {
        return getBlockState().getBlock() instanceof EngineBlock engine ? engine.kind() : EngineBlock.Kind.REDSTONE;
    }

    public Direction facing() {
        return getBlockState().getValue(EngineBlock.FACING);
    }

    public SimpleContainer fuel() {
        return this.fuel;
    }

    public ContainerData data() {
        return this.data;
    }

    // ── tick ──────────────────────────────────────────────────────────────
    public void serverTick() {
        if (this.level == null) return;
        this.lastDraw = this.drawnThisTick;
        this.drawnThisTick = 0;
        this.powered = this.level.hasNeighborSignal(this.worldPosition);

        switch (kind()) {
            case REDSTONE -> {
                if (this.powered) this.energy = REDSTONE_OUTPUT;
                this.heat = Math.clamp(this.heat + (this.powered && this.lastDraw > 0 ? 0.0002 : -0.001), 0.0, 0.8);
            }
            case STIRLING -> tickStirling();
            case COMBUSTION -> tickCombustion();
            case CREATIVE -> {
                if (this.powered) this.energy = creativeOutput();
            }
        }
        if (!this.powered && kind() != EngineBlock.Kind.CREATIVE) {
            this.energy = Math.max(0, this.energy - IDLE_LOSS);
        }

        EngineStage newStage = EngineStage.of(heatLevel());
        // o pistão anda enquanto há sinal e energia para entregar
        boolean newActive = this.powered && (this.energy > 0 || this.lastDraw > 0);
        if (newStage != this.stage || newActive != this.active) {
            this.stage = newStage;
            this.active = newActive;
            syncToClient();
        } else if (this.lastDraw > 0 || this.burnTime > 0) {
            setChanged();
        }
        // tanques do motor a combustão aparecem na tela do cliente
        if (this.combustion != null && this.level.getGameTime() % 10 == 0 && this.combustion.consumeDirty()) syncToClient();
    }

    private void tickStirling() {
        if (!this.powered || this.stage == EngineStage.OVERHEAT) return;
        if (this.burnTime <= 0 && this.energy < STIRLING_CAPACITY) {
            ItemStack stack = this.fuel.getItem(0);
            int burn = stack.isEmpty() ? 0 : this.level.fuelValues().burnDuration(stack);
            if (burn > 0) {
                this.burnTime = this.totalBurnTime = burn;
                ItemStackTemplate remainder = stack.getItem().getCraftingRemainder();
                stack.shrink(1);
                if (stack.isEmpty() && remainder != null) {
                    this.fuel.setItem(0, remainder.create());
                } else {
                    this.fuel.setItem(0, stack);
                }
            }
        }
        if (this.burnTime > 0) {
            this.burnTime--;
            this.energy = Math.min(STIRLING_CAPACITY, this.energy + STIRLING_OUTPUT);
        }
    }

    private void tickCombustion() {
        if (this.combustion == null) return;
        long generated = this.combustion.tick(this.powered, this.energy < CombustionEngine.CAPACITY);
        this.energy = Math.min(CombustionEngine.CAPACITY, this.energy + generated);
    }

    /** Fração de calor: Stirling pelo buffer cheio, redstone pelo próprio calor, criativo sempre frio. */
    public double heatLevel() {
        return switch (kind()) {
            case REDSTONE -> this.heat;
            case STIRLING -> (double) this.energy / STIRLING_CAPACITY;
            case COMBUSTION -> this.combustion == null ? 0.0 : this.combustion.heatLevel();
            case CREATIVE -> 0.0;
        };
    }

    // ── energia ───────────────────────────────────────────────────────────
    public int outputVoltage() {
        return switch (kind()) {
            case CREATIVE -> CREATIVE_VOLTAGES[this.creativeVoltage];
            case COMBUSTION -> CombustionEngine.VOLTAGE;
            default -> LOW_VOLTAGE;
        };
    }

    public long maxOutput() {
        return switch (kind()) {
            case REDSTONE -> REDSTONE_OUTPUT;
            case STIRLING -> STIRLING_OUTPUT;
            case COMBUSTION -> CombustionEngine.MAX_OUTPUT;
            case CREATIVE -> creativeOutput();
        };
    }

    private long creativeOutput() {
        return 1_000L << this.creativeLevel;
    }

    /** Nó do Craft Energy: só na frente. */
    public @Nullable EnergyNode energyNode(@Nullable Direction face) {
        return face == facing() ? this.source : null;
    }

    private final class Source implements EnergySource {
        @Override
        public int outputVoltage() {
            return EngineBlockEntity.this.outputVoltage();
        }

        @Override
        public long availablePower() {
            return powered ? Math.min(maxOutput(), energy) : 0;
        }

        @Override
        public void drawPower(long power) {
            if (power <= 0) return;
            if (kind() != EngineBlock.Kind.CREATIVE) energy = Math.max(0, energy - power);
            drawnThisTick += power;
        }
    }

    public void cycleCreativePower() {
        this.creativeLevel = (this.creativeLevel + 1) % CREATIVE_LEVELS;
        syncToClient();
    }

    public void cycleCreativeVoltage() {
        this.creativeVoltage = (this.creativeVoltage + 1) % CREATIVE_VOLTAGES.length;
        syncToClient();
        if (this.level != null) net.craftenergy.fabric.CraftEnergyApi.markChanged(this.level, this.worldPosition);
    }

    public Component creativeDescription() {
        return Component.translatable("message.buildcraftreborn.creative_engine",
                EnergyUnits.formatPower(creativeOutput()), EnergyUnits.formatVoltage(outputVoltage()));
    }

    public Component statusDescription() {
        return Component.translatable("gui.buildcraftreborn.engine.power", EnergyUnits.formatPower(this.lastDraw))
                .append(" · ").append(Component.translatable("gui.buildcraftreborn.engine.stage." + this.stage.name).withStyle(this.stage.color));
    }

    // ── consultas (cliente e testes) ──────────────────────────────────────
    public boolean isActive() {
        return this.active;
    }

    public EngineStage stage() {
        return this.stage;
    }

    public long storedEnergy() {
        return this.energy;
    }

    public void setStoredEnergy(long energy) {
        this.energy = Math.max(0, energy);
        this.stage = EngineStage.of(heatLevel());
    }

    public int burnTime() {
        return this.burnTime;
    }

    public @Nullable CombustionEngine combustion() {
        return this.combustion;
    }

    public EnergySource source() {
        return this.source;
    }

    @Override
    public void multimeterReading(List<Double> values, List<String> units) {
        MultimeterReadable.electric(values, units, outputVoltage(), this.lastDraw);
        if (this.combustion != null) {
            values.add((double) this.combustion.fuelTank().amountCL());
            units.add("CL");
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null) Containers.dropContents(this.level, pos, this.fuel);
    }

    // ── salvar ────────────────────────────────────────────────────────────
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Energy", this.energy);
        output.putDouble("Heat", this.heat);
        output.putInt("BurnTime", this.burnTime);
        output.putInt("TotalBurnTime", this.totalBurnTime);
        output.putInt("CreativeLevel", this.creativeLevel);
        output.putInt("CreativeVoltage", this.creativeVoltage);
        output.putBoolean("Active", this.active);
        output.store("Fuel", ItemStack.OPTIONAL_CODEC, this.fuel.getItem(0));
        if (this.combustion != null) this.combustion.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy = Math.max(0, input.getLongOr("Energy", 0L));
        this.heat = input.getDoubleOr("Heat", 0.0);
        this.burnTime = input.getIntOr("BurnTime", 0);
        this.totalBurnTime = input.getIntOr("TotalBurnTime", 0);
        this.creativeLevel = Math.clamp(input.getIntOr("CreativeLevel", 0), 0, CREATIVE_LEVELS - 1);
        this.creativeVoltage = Math.clamp(input.getIntOr("CreativeVoltage", 0), 0, CREATIVE_VOLTAGES.length - 1);
        this.active = input.getBooleanOr("Active", false);
        this.fuel.setItem(0, input.read("Fuel", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        if (this.combustion != null) this.combustion.load(input);
        this.stage = EngineStage.of(heatLevel());
    }
}
