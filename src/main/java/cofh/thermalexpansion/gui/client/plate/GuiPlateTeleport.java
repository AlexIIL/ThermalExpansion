package cofh.thermalexpansion.gui.client.plate;

import cofh.core.gui.GuiBaseAdv;
import cofh.core.gui.element.TabInfo;
import cofh.core.gui.element.TabSecurity;
import cofh.lib.gui.element.ElementButton;
import cofh.lib.gui.element.ElementEnergyStored;
import cofh.lib.gui.element.ElementListBox;
import cofh.lib.gui.element.ElementSlider;
import cofh.lib.gui.element.ElementTextField;
import cofh.lib.gui.element.ElementTextFieldLimited;
import cofh.lib.gui.element.listbox.IListBoxElement;
import cofh.lib.gui.element.listbox.SliderVertical;
import cofh.lib.transport.IEnderChannelRegistry;
import cofh.lib.transport.IEnderChannelRegistry.Frequency;
import cofh.lib.util.helpers.SecurityHelper;
import cofh.thermalexpansion.block.plate.TilePlateTeleporter;
import cofh.thermalexpansion.core.TEProps;
import cofh.thermalexpansion.core.TeleportChannelRegistry;
import cofh.thermalexpansion.gui.container.ContainerTEBase;
import cofh.thermalexpansion.gui.element.ListBoxElementEnderText;

import java.util.UUID;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.input.Keyboard;

public class GuiPlateTeleport extends GuiBaseAdv {

	static final String TEX_PATH = TEProps.PATH_GUI + "plate/Teleport.png";

	TilePlateTeleporter myTile;
	UUID playerName;

	int updated;

	ElementListBox frequencies;
	ElementSlider slider;
	ElementTextField freq;
	ElementTextField name;
	ElementTextField title;

	ElementButton assign;
	ElementButton clear;
	ElementButton add;
	ElementButton remove;

	public GuiPlateTeleport(InventoryPlayer inventory, TileEntity theTile) {

		super(new ContainerTEBase(inventory, theTile, false, false), new ResourceLocation(TEX_PATH));
		myTile = (TilePlateTeleporter) theTile;
		super.name = myTile.getInventoryName();
		playerName = SecurityHelper.getID(inventory.player);
		drawInventory = false;

		// generateInfo("tab.thermalexpansion.plate.translocate", 2);
	}

	@Override
	public void initGui() {

		super.initGui();
		Keyboard.enableRepeatEvents(true);

		if (!myInfo.isEmpty()) {
			addTab(new TabInfo(this, myInfo));
		}
		if (myTile.enableSecurity() && myTile.isSecured()) {
			addTab(new TabSecurity(this, myTile, playerName));
		}

		addElement(new ElementEnergyStored(this, 8, 8, myTile.getEnergyStorage()));
		addElement(freq = new ElementTextFieldLimited(this, 102, 34, 26, 11, (short) 3).setFilter("0123456789", false).setBackgroundColor(0, 0, 0)
				.setText(String.valueOf(myTile.getFrequency())));
		addElement(title = new ElementTextField(this, 28, 18, 108, 11, (short) 15).setBackgroundColor(0, 0, 0));

		addElement(name = new ElementTextField(this, 28, 56, 108, 11, (short) 15).setBackgroundColor(0, 0, 0).setFocusable(false));

		addElement(assign = new ElementButton(this, 131, 22, 20, 20, 176, 0, 176, 20, 176, 40, TEX_PATH) {

			@Override
			public void onClick() {

				if (myTile.setFrequency(Integer.parseInt(freq.getText()))) {
					TeleportChannelRegistry.getChannels(false).setFrequency(myTile.getChannelString(), myTile.getFrequency(),
						GuiPlateTeleport.this.title.getText());
				}
			}
		});
		addElement(clear = new ElementButton(this, 151, 22, 20, 20, 196, 0, 196, 20, 196, 40, TEX_PATH) {

			@Override
			public void onClick() {

				int freq = myTile.getFrequency();
				if (myTile.clearFrequency()) {
					TeleportChannelRegistry.getChannels(false).removeFrequency(myTile.getChannelString(), freq);
				}
			}
		});

		addElement(add = new ElementButton(this, 139, 54, 16, 16, 176, 60, 176, 76, 176, 92, TEX_PATH) {

			@Override
			public void onClick() {

				myTile.setDestination(((Frequency) frequencies.getSelectedElement().getValue()).freq);
			}
		});
		addElement(remove = new ElementButton(this, 155, 54, 16, 16, 192, 60, 192, 76, 192, 92, TEX_PATH) {

			@Override
			public void onClick() {

				myTile.clearDestination();
			}
		});

		addElement(frequencies = new ElementListBox(this, 6, 73, 130, 87) {

			@Override
			protected void onElementClicked(IListBoxElement element) {

				Frequency freq = (Frequency) element.getValue();
				GuiPlateTeleport.this.name.setText(freq.name);
			}

			@Override
			protected void onScrollV(int newStartIndex) {

				slider.setValue(newStartIndex);
			}

		}.setBackgroundColor(0, 0).setSelectionColor(1));
		frequencies.setSelectedIndex(-1);
		IEnderChannelRegistry data = TeleportChannelRegistry.getChannels(false);
		updated = data.updated();
		for (Frequency freq : data.getFrequencyList(null)) {
			frequencies.add(new ListBoxElementEnderText(freq));
			if (freq.freq == myTile.getDestination()) {
				frequencies.setSelectedIndex(frequencies.getElementCount() - 1);
				this.name.setText(freq.name);
			} else if (freq.freq == myTile.getFrequency()) {
				title.setText(freq.name);
			}
		}
		addElement(slider = new SliderVertical(this, 140, 73, 14, 87, frequencies.getLastScrollPosition()) {

			@Override
			public void onValueChanged(int value) {

				frequencies.scrollToV(value);
			}

		}.setColor(0, 0));
	}

	@Override
	public void onGuiClosed() {

		Keyboard.enableRepeatEvents(false);
		super.onGuiClosed();
	}

	@Override
	public void updateScreen() {

		super.updateScreen();

		if (!myTile.canAccess()) {
			this.mc.thePlayer.closeScreen();
		}
	}

	@Override
	protected void updateElementInformation() {

		IEnderChannelRegistry data = TeleportChannelRegistry.getChannels(false);
		if (updated != data.updated()) {
			updated = data.updated();
			IListBoxElement ele = frequencies.getSelectedElement();
			int sel = ele != null ? ((Frequency) ele.getValue()).freq : -1;
			int pos = slider.getSliderY();
			frequencies.removeAll();
			frequencies.setSelectedIndex(-1);
			for (Frequency freq : data.getFrequencyList(null)) {
				frequencies.add(new ListBoxElementEnderText(freq));
				if (freq.freq == sel) {
					frequencies.setSelectedIndex(frequencies.getElementCount() - 1);
					this.name.setText(freq.name);
				} else if (freq.freq == myTile.getFrequency()) {
					title.setText(freq.name);
				}
			}
			slider.setLimits(0, frequencies.getLastScrollPosition());
			slider.setValue(pos);
		}

		boolean hasFreq = freq.getContentLength() > 0, hasName = title.getContentLength() > 0;
		assign.setEnabled(hasFreq && hasName && myTile.getFrequency() == -1);
		clear.setEnabled(myTile.getFrequency() != -1);
		IListBoxElement ele = frequencies.getSelectedElement();
		add.setEnabled(myTile.getDestination() == -1 && name.getContentLength() > 0 && ele != null && myTile.getFrequency() != ((Frequency)ele.getValue()).freq);
		remove.setEnabled(myTile.getDestination() != -1);
	}

}
