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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.CrystalKey;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.GoldenKey;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.Key;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Image;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//Debug menu: edit a few hero/dungeon values, and open the item menu.
public class WndDebug extends Window {

	private static final int WIDTH = 120;

	//matches the row height of ScrollingListPane
	public static final int ROW_HEIGHT = 18;

	//values are typed in with a text box, with at most this many digits
	private static final int MAX_DIGITS = 9;

	private interface ValueSetter {
		void set( int value );
	}

	//A tappable row of a scrolling list. Scrolling panes pass taps on to their list items
	//rather than to buttons placed inside them, so rows are used instead of buttons.
	public static abstract class Row extends ScrollingListPane.ListItem {

		private final boolean enabled;

		public Row( Image icon, String text, boolean enabled ) {
			super( icon, null, text );
			this.enabled = enabled;
			if (!enabled) {
				hardlight( 0x999999 );
				hardlightIcon( 0x999999 );
			}
		}

		@Override
		public boolean onClick( float x, float y ) {
			if (enabled && inside( x, y )) {
				onSelect();
				return true;
			}
			return false;
		}

		protected abstract void onSelect();
	}

	public WndDebug() {
		super();

		RenderedTextBlock title = PixelScene.renderTextBlock( Messages.get(this, "title"), 9 );
		title.hardlight( TITLE_COLOR );
		title.setPos( (WIDTH - title.width()) / 2f, 2 );
		PixelScene.align( title );
		add( title );

		float top = title.bottom() + 4;

		ArrayList<Row> rows = new ArrayList<>();

		final Hero hero = Dungeon.hero;

		//gold
		rows.add( valueRow( Icons.get( Icons.COIN_SML ), Messages.get(this, "gold"), Dungeon.gold, true,
				new ValueSetter() {
					@Override
					public void set( int value ) {
						Dungeon.gold = Math.max( 0, value );
					}
				}));

		//current HP
		rows.add( valueRow( Icons.get( Icons.STATS ), Messages.get(this, "hp"), hero.HP, true,
				new ValueSetter() {
					@Override
					public void set( int value ) {
						//never set to 0, that would just kill the hero
						hero.HP = Math.max( 1, Math.min( value, hero.HT ) );
					}
				}));

		//max HP, adjusted through the permanent HT boost so that it survives level ups
		rows.add( valueRow( Icons.get( Icons.STATS ), Messages.get(this, "max_hp"), hero.HT, true,
				new ValueSetter() {
					@Override
					public void set( int value ) {
						hero.HTBoost += Math.max( 1, value ) - hero.HT;
						hero.updateHT( false );
					}
				}));

		//talent points, per tier
		for (int t = 1; t <= Talent.MAX_TALENT_TIERS; t++) {
			final int tier = t;
			rows.add( valueRow( Icons.get( Icons.TALENT ),
					Messages.get(this, "talent", tier),
					hero.talentPointsAvailable( tier ),
					tierUnlocked( hero, tier ),
					new ValueSetter() {
						@Override
						public void set( int value ) {
							hero.debugTalentPoints[tier] += Math.max( 0, value ) - hero.talentPointsAvailable( tier );
						}
					}));
		}

		//alchemy energy
		rows.add( valueRow( Icons.get( Icons.ENERGY_SML ), Messages.get(this, "energy"), Dungeon.energy, true,
				new ValueSetter() {
					@Override
					public void set( int value ) {
						Dungeon.energy = Math.max( 0, value );
					}
				}));

		//keys, for the current depth
		rows.add( keyRow( Messages.get(this, "iron_key"), IronKey.class ) );
		rows.add( keyRow( Messages.get(this, "golden_key"), GoldenKey.class ) );
		rows.add( keyRow( Messages.get(this, "crystal_key"), CrystalKey.class ) );

		//items
		rows.add( new Row( Icons.get( Icons.BACKPACK_LRG ), Messages.get(this, "items"), true ) {
			@Override
			protected void onSelect() {
				hide();
				GameScene.show( new WndDebugItems() );
			}
		});

		int maxHeight = (int)(PixelScene.uiCamera.height * 0.9f) - (int)top;
		int height = Math.min( rows.size() * ROW_HEIGHT, maxHeight );

		//the window must be resized before the scroll pane is positioned, as resizing moves the window's camera
		resize( WIDTH, (int)top + height );

		ScrollingListPane list = new ScrollingListPane();
		add( list );
		for (Row row : rows) {
			list.addItem( row );
		}
		list.setRect( 0, top, WIDTH, height );
	}

	//a tier's points can only be used once the tier is unlocked, so editing them before that does nothing
	private static boolean tierUnlocked( Hero hero, int tier ) {
		return !(hero.lvl < Talent.tierLevelThresholds[tier] - 1
				|| (tier == 3 && hero.subClass == HeroSubClass.NONE)
				|| (tier == 4 && hero.armorAbility == null));
	}

	private Row keyRow( String label, final Class<? extends Key> keyClass ) {
		final int depth = Dungeon.depth;

		Key current = Reflection.newInstance( keyClass );
		current.depth = depth;

		return valueRow( new ItemSprite( current ), label, Notes.keyCount( current ), true, new ValueSetter() {
			@Override
			public void set( int value ) {
				Key key = Reflection.newInstance( keyClass );
				key.depth = depth;

				int diff = Math.max( 0, value ) - Notes.keyCount( key );
				if (diff > 0) {
					key.quantity( diff );
					Notes.add( key );
				} else if (diff < 0) {
					key.quantity( -diff );
					Notes.remove( key );
				}
				GameScene.updateKeyDisplay();
			}
		});
	}

	//note that the name of the value can't be called "label" here, as rows already have a field with that name
	private Row valueRow( Image icon, final String valueName, final int value,
	                      boolean enabled, final ValueSetter setter ) {
		return new Row( icon, valueName + ": " + value, enabled ) {
			@Override
			protected void onSelect() {
				hide();
				GameScene.show( new WndTextInput(
						valueName,
						Messages.get( WndDebug.class, "input_body" ),
						Integer.toString( value ),
						MAX_DIGITS,
						false,
						Messages.get( WndDebug.class, "confirm" ),
						Messages.get( WndDebug.class, "cancel" ) ) {
					@Override
					public void onSelect( boolean positive, String text ) {
						if (positive) {
							try {
								setter.set( Integer.parseInt( text.trim() ) );
							} catch (NumberFormatException e) {
								//not a number, leave the value alone
							}
						}
						//reopen so the new values are shown
						GameScene.show( new WndDebug() );
					}
				});
			}
		};
	}
}
