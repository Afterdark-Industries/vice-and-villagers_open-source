package com.trippycraft.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.trippycraft.TrippyCraft;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TrippyCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModCommandGameTests {
    private ModCommandGameTests() {
    }

    @GameTest(template = "village/bars/oak_taproom")
    public static void currentAndLegacyCommandsWorkAndKeepResetOperatorOnly(GameTestHelper helper) throws CommandSyntaxException {
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "mod-command-test"));
        var ordinarySource = player.createCommandSourceStack().withPermission(0).withSuppressedOutput();
        var operatorSource = ordinarySource.withPermission(2);
        for (String name : new String[]{"viceandvillagers", "trippycraft"}) {
            helper.assertTrue(dispatcher.execute(name + " status", ordinarySource) == 1,
                    "Player status failed through /" + name);
            try {
                dispatcher.execute(name + " reset", ordinarySource);
                helper.fail("An ordinary player could reset through /" + name);
                return;
            } catch (CommandSyntaxException expected) {
                // Both names must retain the permission gate on the reset subcommand.
            }
            helper.assertTrue(dispatcher.execute(name + " reset", operatorSource) == 1,
                    "Operator reset failed through /" + name);
        }
        helper.succeed();
    }
}
