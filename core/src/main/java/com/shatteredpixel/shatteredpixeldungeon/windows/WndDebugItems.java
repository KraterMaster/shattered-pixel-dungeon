/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
import com.shatteredpixel.shatteredpixeldungeon.items.EnergyCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.Key;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//Debug item menu, first level: a list of item categories.
public class WndDebugItems extends Window {

	private static final int WIDTH      = 120;
	private static final int BTN_HEIGHT = 18;
	private static final int GAP        = 2;

	public WndDebugItems() {
		super();

		RenderedTextBlock title = PixelScene.renderTextBlock( Messages.get(this, "title"), 9 );
		title.hardlight( TITLE_COLOR );
		title.setPos( (WIDTH - title.width()) / 2f, 2 );
		PixelScene.align( title );
		add( title );

		float top = title.bottom() + 4;

		Component content = new Component();
		float pos = 0;

		for (final Catalog catalog : categories()) {
			RedButton btn = new RedButton( Messages.titleCase( catalog.title() ) ) {
				@Override
				protected void onClick() {
					hide();
					GameScene.show( new Category( catalog ) );
				}
			};
			btn.setRect( 0, pos, WIDTH, BTN_HEIGHT );
			content.add( btn );
			pos += BTN_HEIGHT + GAP;
		}

		content.setSize( WIDTH, pos - GAP );

		int maxHeight = (int)(PixelScene.uiCamera.height * 0.9f) - (int)top;
		int height = Math.min( (int)content.height(), maxHeight );

		ScrollPane list = new ScrollPane( content );
		add( list );
		list.setRect( 0, top, WIDTH, height );

		resize( WIDTH, (int)top + height );
	}

	//enchantments and glyphs are not items, so they can't be added to the inventory
	private static ArrayList<Catalog> categories() {
		ArrayList<Catalog> result = new ArrayList<>();
		for (Catalog catalog : Catalog.values()) {
			if (catalog != Catalog.ENCHANTMENTS && catalog != Catalog.GLYPHS) {
				result.add( catalog );
			}
		}
		return result;
	}

	//gold, energy and keys have their own entries in the main debug window, and dewdrops
	//heal on pick up, so none of them are listed as items
	private static boolean addable( Class<?> cls ) {
		return Item.class.isAssignableFrom( cls )
				&& !Key.class.isAssignableFrom( cls )
				&& cls != Gold.class
				&& cls != EnergyCrystal.class
				&& cls != Dewdrop.class;
	}

	//Debug item menu, second level: every item of one category.
	public static class Category extends Window {

		private static final int WIDTH_P = 120;

		public Category( Catalog catalog ) {
			super();

			RenderedTextBlock title = PixelScene.renderTextBlock( Messages.titleCase( catalog.title() ), 9 );
			title.hardlight( TITLE_COLOR );
			title.setPos( (WIDTH_P - title.width()) / 2f, 2 );
			PixelScene.align( title );
			add( title );

			float top = title.bottom() + 4;

			final ScrollingListPane list = new ScrollingListPane();
			add( list );

			for (final Class<?> cls : catalog.items()) {
				if (!addable( cls )) continue;

				//this instance is only for showing the item, it is always shown identified
				Item shown = (Item) Reflection.newInstance( cls );
				if (shown == null) continue;
				if (shown instanceof Potion) ((Potion) shown).anonymize();
				if (shown instanceof Scroll) ((Scroll) shown).anonymize();
				if (shown instanceof Ring)   ((Ring) shown).anonymize();

				list.addItem( new ScrollingListPane.ListItem( new ItemSprite( shown ), null,
						Messages.titleCase( shown.name() ) ) {
					@Override
					public boolean onClick( float x, float y ) {
						if (inside( x, y )) {
							give( cls );
							return true;
						}
						return false;
					}
				});
			}

			int height = (int)(PixelScene.uiCamera.height * 0.9f) - (int)top;
			list.setRect( 0, top, WIDTH_P, height );

			resize( WIDTH_P, (int)top + height );
		}

		//the item that goes into the inventory is a normal one, so it follows the game's own identification
		//state, except for equipment that is identified so its level is visible
		private static void give( Class<?> cls ) {
			Hero hero = Dungeon.hero;
			Item item = (Item) Reflection.newInstance( cls );
			if (item == null) return;

			if (item instanceof Weapon || item instanceof Armor || item instanceof Wand || item instanceof Artifact) {
				item.identify( false );
			}

			String name = item.name();
			if (!item.collect()) {
				Dungeon.level.drop( item, hero.pos ).sprite.drop( hero.pos );
			}
			GLog.p( Messages.get( WndDebugItems.class, "added", name ) );
		}

		@Override
		public void onBackPressed() {
			super.onBackPressed();
			GameScene.show( new WndDebugItems() );
		}
	}
}
