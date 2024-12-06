package com.curvelabs.combatlogout;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CombatLogoutTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CombatLogoutPlugin.class);
		RuneLite.main(args);
	}
}
