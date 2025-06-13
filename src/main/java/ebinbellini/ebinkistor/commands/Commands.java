package ebinbellini.ebinkistor.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import ebinbellini.ebinkistor.trade.Bank;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

public class Commands {

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("ek").requires(source -> source.hasPermissionLevel(0))
                // Balance command
                .then(CommandManager.literal("saldo").executes(context -> {
                    ServerCommandSource source = context.getSource();
                    ServerPlayerEntity player = source.getPlayer();

                    if (player != null) {
                        MinecraftServer server = source.getServer();
                        Bank bank = Bank.getServerState(server);
                        int balance = bank.getBalance(player.getUuid().toString());

                        player.sendMessage(
                                Text.literal("Du har " + balance + " blocksdaler tillgängliga").formatted(Formatting.YELLOW), false);
                    }
                    return 1;
                }))
                // Pay command
                .then(CommandManager.literal("betala")
                        .then(CommandManager.argument("mottagare", StringArgumentType.word())
                                .then(CommandManager.argument("summa", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            ServerCommandSource source = context.getSource();
                                            ServerPlayerEntity player = source.getPlayer();
                                            String targetName = StringArgumentType.getString(context, "mottagare");
                                            int amount = IntegerArgumentType.getInteger(context, "summa");

                                            if (player != null) {
                                                MinecraftServer server = source.getServer();
                                                ServerPlayerEntity targetPlayer
                                                        = server.getPlayerManager().getPlayer(targetName);

                                                if (targetPlayer == null) {
                                                    player.sendMessage(
                                                            Text.literal("Spelaren " + targetName + " hittades inte. Kanske inte är online?")
                                                                    .formatted(Formatting.RED),
                                                            false);
                                                    return 0;
                                                }

                                                Bank bank = Bank.getServerState(server);
                                                try {
                                                    bank.transfer(player.getUuid().toString(),
                                                            targetPlayer.getUuid().toString(), amount);

                                                    player.sendMessage(Text.literal("Du skickade " + amount
                                                            + " blocksdaler till " + targetName)
                                                            .formatted(Formatting.BLUE),
                                                            false);
                                                    targetPlayer.sendMessage(Text.literal("Du fick " + amount
                                                            + " blocksdaler från "
                                                            + player.getName().getString())
                                                            .formatted(Formatting.GREEN),
                                                            false);
                                                } catch (IllegalArgumentException e) {
                                                    player.sendMessage(
                                                            Text.literal(e.getMessage()).formatted(Formatting.RED), false);
                                                }
                                            }

                                            return 1;
                                        }))))
                // Admin commands
                .then(CommandManager.literal("admin")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("give")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer())
                                                .executes(context -> {
                                                    ServerCommandSource source = context.getSource();
                                                    String targetName = StringArgumentType.getString(context, "player");
                                                    int amount = IntegerArgumentType.getInteger(context, "amount");

                                                    MinecraftServer server = source.getServer();
                                                    ServerPlayerEntity targetPlayer
                                                            = server.getPlayerManager().getPlayer(targetName);

                                                    if (targetPlayer == null) {
                                                        source.sendMessage(Text.literal("Spelare hittades inte: " + targetName)
                                                                .formatted(Formatting.RED));
                                                        return 0;
                                                    }

                                                    Bank bank = Bank.getServerState(server);
                                                    bank.deposit(targetPlayer.getUuid().toString(), amount);

                                                    source.sendMessage(
                                                            Text.literal("Gav " + amount + " blocksdaler till " + targetName)
                                                                    .formatted(Formatting.BLUE));
                                                    targetPlayer.sendMessage(
                                                            Text.literal("Du fick " + amount + " blocksdaler från servern")
                                                                    .formatted(Formatting.GREEN),
                                                            false);

                                                    return 1;
                                                }))))
                )
        );
    }
}
