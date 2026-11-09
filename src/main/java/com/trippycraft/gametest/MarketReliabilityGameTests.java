package com.trippycraft.gametest;

import com.mojang.authlib.GameProfile;
import com.trippycraft.economy.EmeraldPayoutLedger;
import com.trippycraft.economy.EmeraldWallet;
import com.trippycraft.economy.MarketContract;
import com.trippycraft.economy.MarketDataStore;
import com.trippycraft.economy.MarketPersistence;
import com.trippycraft.economy.MarketPlayerData;
import com.trippycraft.network.MarketActionPacket;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/** Payouts, market saves and action decoding must not lose or invent value when something goes wrong. */
@GameTestHolder("trippycraft_market")
@PrefixGameTestTemplate(false)
public final class MarketReliabilityGameTests {
    private MarketReliabilityGameTests() { }

    // ---- Queued payouts -----------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void marketPayoutQueuedBeforeACrashIsPaidAfterReload(GameTestHelper helper) {
        FakePlayer player = player(helper);
        Path folder = tempFolder();
        try {
            EmeraldPayoutLedger.queue(player, 30);
            EmeraldPayoutLedger.queue(player, 7);
            helper.assertTrue(EmeraldWallet.value(player) == 0, "Queued payout was paid before the market closed");

            // An autosave captures the queued payout together with the inventory the sold items left.
            File saved = folder.resolve(player.getUUID() + ".trippycraft_market").toFile();
            MarketDataStore.save(player.getUUID(), saved);
            helper.assertTrue(MarketPersistence.load(saved).map(MarketPlayerData::pendingPayoutEmeralds).orElse(0) == 37,
                    "The queued payout was not written to the player's market save");
            // A crash discards all in-memory state before the market is closed.
            MarketDataStore.clearPlayer(player.getUUID());
            MarketDataStore.load(player.getUUID(), saved);

            EmeraldPayoutLedger.flush(player);
            helper.assertTrue(EmeraldWallet.value(player) == 37, "Reloaded payout was not delivered: " + EmeraldWallet.value(player));
            EmeraldPayoutLedger.flush(player);
            helper.assertTrue(EmeraldWallet.value(player) == 37, "A payout was delivered twice");
            helper.assertTrue(MarketDataStore.getOrCreate(player).pendingPayoutEmeralds() == 0, "Delivered payout stayed queued");
        } finally {
            MarketDataStore.clearPlayer(player.getUUID());
            deleteFolder(folder);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void marketPayoutQueueSurvivesSaveCopyAndDeathClone(GameTestHelper helper) {
        FakePlayer player = player(helper);
        UUID respawned = UUID.randomUUID();
        try {
            EmeraldPayoutLedger.queue(player, 12);
            MarketPlayerData data = MarketDataStore.getOrCreate(player);
            helper.assertTrue(MarketPlayerData.load(data.save()).pendingPayoutEmeralds() == 12, "Queued payout was not saved");
            helper.assertTrue(data.copy().pendingPayoutEmeralds() == 12, "Copying market data dropped the queued payout");
            MarketDataStore.clone(player.getUUID(), respawned);
            helper.assertTrue(MarketDataStore.get(respawned).map(MarketPlayerData::pendingPayoutEmeralds).orElse(0) == 12,
                    "A death clone lost the queued payout");

            CompoundTag legacy = data.save();
            legacy.remove("pendingPayoutEmeralds");
            helper.assertTrue(MarketPlayerData.load(legacy).pendingPayoutEmeralds() == 0, "Older saves should load with nothing queued");
            legacy.putInt("pendingPayoutEmeralds", -5);
            helper.assertTrue(MarketPlayerData.load(legacy).pendingPayoutEmeralds() == 0, "A negative saved payout was accepted");

            data.takePendingPayout();
            data.addPendingPayout(Integer.MAX_VALUE - 5);
            data.addPendingPayout(10);
            helper.assertTrue(data.pendingPayoutEmeralds() == Integer.MAX_VALUE, "Queued payouts overflowed instead of saturating");
            data.addPendingPayout(0);
            data.addPendingPayout(-3);
            helper.assertTrue(data.pendingPayoutEmeralds() == Integer.MAX_VALUE, "Zero or negative payouts changed the queue");
        } finally {
            MarketDataStore.clearPlayer(player.getUUID());
            MarketDataStore.clearPlayer(respawned);
        }
        helper.succeed();
    }

    // ---- Market save files --------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void marketSaveRoundTripsAndLeavesNoTemporaryFile(GameTestHelper helper) {
        Path folder = tempFolder();
        try {
            MarketPlayerData data = sampleData();
            File file = folder.resolve("player.trippycraft_market").toFile();
            MarketPersistence.save(file, data);
            MarketPersistence.save(file, data);
            helper.assertTrue(MarketPersistence.load(file).map(loaded -> loaded.save().equals(data.save())).orElse(false),
                    "Market data changed across a save and load");
            helper.assertTrue(files(folder).equals(List.of("player.trippycraft_market")),
                    "Saving left extra files behind: " + files(folder));
        } finally {
            deleteFolder(folder);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void corruptMarketSaveIsMovedAsideInsteadOfOverwritten(GameTestHelper helper) {
        Path folder = tempFolder();
        try {
            Path file = folder.resolve("player.trippycraft_market");
            byte[] corrupt = {0x0A, 0x00, 0x7F, 0x13, 0x37, 0x42};
            Files.write(file, corrupt);

            helper.assertTrue(MarketPersistence.load(file.toFile()).isEmpty(), "Corrupt market data loaded as a profile");
            Path backup = onlyBackup(helper, folder);
            helper.assertTrue(Arrays.equals(Files.readAllBytes(backup), corrupt), "The moved-aside file did not keep the original bytes");

            MarketPersistence.save(file.toFile(), sampleData());
            helper.assertTrue(MarketPersistence.load(file.toFile()).isPresent(), "A fresh profile could not be saved after recovery");
            helper.assertTrue(Arrays.equals(Files.readAllBytes(backup), corrupt), "Saving a fresh profile overwrote the backup");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } finally {
            deleteFolder(folder);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void newerSchemaMarketSaveIsPreservedForAnUpgrade(GameTestHelper helper) {
        Path folder = tempFolder();
        try {
            Path file = folder.resolve("player.trippycraft_market");
            CompoundTag future = new CompoundTag();
            future.putInt("schema", 2);
            future.put("market", sampleData().save());
            NbtIo.write(future, file);

            helper.assertTrue(MarketPersistence.load(file.toFile()).isEmpty(), "A newer schema was read as the current one");
            Path backup = onlyBackup(helper, folder);
            helper.assertTrue(NbtIo.read(backup).equals(future), "The newer-schema save was not preserved intact");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } finally {
            deleteFolder(folder);
        }
        helper.succeed();
    }

    // ---- Action decoding ----------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void unknownMarketActionsAreRejectedNotTreatedAsSales(GameTestHelper helper) {
        for (int index : new int[]{-1, MarketActionPacket.Action.values().length, 99}) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeVarInt(index);
            buffer.writeUtf("cannabis_joint");
            buffer.writeVarInt(64);
            boolean rejected = false;
            try {
                MarketActionPacket.decode(buffer);
            } catch (DecoderException expected) {
                rejected = true;
            } finally {
                buffer.release();
            }
            helper.assertTrue(rejected, "Market action " + index + " was accepted");
        }
        for (MarketActionPacket.Action action : MarketActionPacket.Action.values()) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            MarketActionPacket.encode(new MarketActionPacket(action, "cannabis_joint", 3), buffer);
            MarketActionPacket decoded = MarketActionPacket.decode(buffer);
            buffer.release();
            helper.assertTrue(decoded.action() == action && decoded.entryId().equals("cannabis_joint") && decoded.amount() == 3,
                    "Valid market action " + action + " did not round-trip");
        }
        helper.succeed();
    }

    // ---- Fixtures -----------------------------------------------------------------------------

    private static FakePlayer player(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "market-reliability"));
        player.getInventory().clearContent();
        return player;
    }

    private static MarketPlayerData sampleData() {
        MarketPlayerData data = new MarketPlayerData();
        data.addReputation(640);
        data.addSoldUnits(91);
        data.addCompletedContract();
        data.setDemandMultiplier("cannabis_joint", 117);
        data.setLastDemandRefreshGameTime(48_000L);
        data.activeContracts().add(new MarketContract("contract_7", "cannabis_joint", "Steady Buyer", 16, 22, 14, 135));
        data.addPendingPayout(19);
        return data;
    }

    private static Path onlyBackup(GameTestHelper helper, Path folder) {
        List<String> names = files(folder);
        helper.assertTrue(names.size() == 1 && names.getFirst().startsWith("player.trippycraft_market.unreadable-"),
                "Expected exactly one moved-aside market file, found " + names);
        return folder.resolve(names.getFirst());
    }

    private static List<String> files(Path folder) {
        try (Stream<Path> stream = Files.list(folder)) {
            return stream.map(path -> path.getFileName().toString()).sorted().toList();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static Path tempFolder() {
        try {
            return Files.createTempDirectory("vv-market-test");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void deleteFolder(Path folder) {
        try (Stream<Path> stream = Files.walk(folder)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException ignored) {
            // Best effort: the folder is in the system temp directory.
        }
    }
}
