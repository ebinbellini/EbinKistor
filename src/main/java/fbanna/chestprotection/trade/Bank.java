package fbanna.chestprotection.trade;


import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.UnboundedMapCodec;

import java.util.HashMap;
import java.util.UUID;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtEnd;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import fbanna.chestprotection.ChestProtection;

public class Bank extends PersistentState {
    // A map to store the bank balances of players
    public static HashMap<String, Integer> accounts = new HashMap<>();

    static final int START_BALANCE = 1000;

	/*public static final Codecs.StrictUnboundedMapCodec<String,Integer> BANK_CODEC = Codec.strictUnboundedMap(
            Codec.STRING,
            Codec.INT
    );*/
    //public static final MapCodec<HashMap<String, Integer>> ACCOUNT_CODEC = new UnboundedMapCodec<String, Integer>(
    public static final UnboundedMapCodec<String, Integer> ACCOUNT_CODEC = new UnboundedMapCodec<String, Integer>(
            Codec.STRING,
            Codec.INT
    );
    //.fieldOf("accounts");
                //ACCOUNT_CODEC.forGetter(bank -> bank.accounts)

    public static final Codec<Bank> BANK_CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
                ACCOUNT_CODEC.fieldOf("accounts").forGetter(bank -> bank.accounts)
			).apply(instance, bank -> new Bank())
	);

    private static PersistentStateType<Bank> type = new PersistentStateType<Bank>(
            ChestProtection.MOD_ID + ":bank", // Unique identifier for the bank state
            Bank::createNew, // If there's no 'Bank' yet create one and initialize variables
            BANK_CODEC,
            null // Supposed to be an 'DataFixTypes' enum, but we can just pass null
    );

    public static PersistentStateType<Bank> getPersistentStateType() {
		return new PersistentStateType<>(
				ChestProtection.MOD_ID,
				context -> new Bank(),
				context -> new Codec<>() {
					@Override
					public <T> DataResult<Pair<Bank, T>> decode(DynamicOps<T> ops, T input) {
						return DataResult.success(Pair.of(Bank.createFromNbt((NbtCompound) input), input));
					}

					@Override
					@SuppressWarnings("unchecked")
					public <T> DataResult<T> encode(Bank input, DynamicOps<T> ops, T prefix) {
						if (!(prefix instanceof NbtEnd)) {
							throw new RuntimeException();
						}
						return DataResult.success((T) input.writeNbt(new NbtCompound()));
					}
				},
				null
		);
	}

    public static void deposit(String playerUUID, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Insättningsbeloppet måste vara positivt");
        }
        accounts.put(playerUUID, accounts.getOrDefault(playerUUID, START_BALANCE) + amount);
    }

    public static void withdraw(String playerUUID, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Uttagningsbeloppet måste vara positiv");
        }
        if (!accounts.containsKey(playerUUID)) {
            // Player does not have an account, create one with the starting balance
            accounts.put(playerUUID, START_BALANCE);
        }
        if (accounts.get(playerUUID) < amount) {
            throw new IllegalArgumentException("Otillräckligt med pengar på kontot");
        }
        accounts.put(playerUUID, accounts.get(playerUUID) - amount);
    }

    public static int getBalance(String playerUUID) {
        return accounts.getOrDefault(playerUUID, START_BALANCE);
    }

    public static void transfer(String fromPlayerUUID, String toPlayerUUID, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Överföringsbeloppet måste vara positivt");
        }
        if (!accounts.containsKey(fromPlayerUUID) || accounts.get(fromPlayerUUID) < amount) {
            throw new IllegalArgumentException("Otillräckligt med pengar på kontot för överföring");
        }
        withdraw(fromPlayerUUID, amount);
        deposit(toPlayerUUID, amount);
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        ChestProtection.LOGGER.info("Saving bank state with {} accounts", accounts.size());
        NbtCompound bankNbt = new NbtCompound();
        accounts.forEach((key, balance) -> {
            bankNbt.putInt(key, balance);
        });
        nbt.put("bank", bankNbt);
 
        return nbt;
    }

    public static Bank createFromNbt(NbtCompound tag) {
        Bank bank = new Bank();
        bank.accounts = new HashMap<>();
        NbtCompound bankNbt = tag.getCompound("bank").orElse(new NbtCompound());
        bankNbt.getKeys().forEach(key -> {
            Integer balance = bankNbt.getInt(key).orElse(START_BALANCE);
            ChestProtection.LOGGER.info("Loading account for UUID {} with balance {}", key, balance);
            bank.accounts.put(key, balance);
        });
 
        return bank;
    }

    public static Bank createNew() {
        Bank bank = new Bank();
        bank.accounts = new HashMap<>();
        return bank;
    }

    public static Bank getServerState(MinecraftServer server) {
        // (Note: arbitrary choice to use 'World.OVERWORLD' instead of 'World.END' or 'World.NETHER'.  Any work)
        ServerWorld serverWorld = server.getWorld(World.OVERWORLD);
        assert serverWorld != null;
 
        return serverWorld.getPersistentStateManager().getOrCreate(getPersistentStateType());
    }

    @Override
	public boolean isDirty() {
		return true;
	}
}