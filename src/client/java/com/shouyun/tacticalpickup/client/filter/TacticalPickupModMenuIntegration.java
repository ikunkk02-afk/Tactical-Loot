package com.shouyun.tacticalpickup.client.filter;

import com.shouyun.tacticalpickup.client.ui.editor.TacticalLootSettingsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class TacticalPickupModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return TacticalLootSettingsScreen::new;
	}
}
