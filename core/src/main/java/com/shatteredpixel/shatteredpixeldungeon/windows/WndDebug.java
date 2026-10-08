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
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Reflection;

//Debug menu: edit a few hero/dungeon values, and open the item menu.
public class WndDebug extends Window {

	private static final int WIDTH      = 120;
	private static final int BTN_HEIGHT = 18;
	private static final int GAP        = 2;

	//values are typed in with a text box, with at most this many digits
	private static final int MAX_DIGITS = 9;

	private interface ValueSetter {
		void set( int value );
	}

	public WndDebug() {
		super();

		RenderedTextBlock title = PixelScene.renderTextBlock( Messages.get(this, "title"), 9 );
		title.hardlight( TITLE_COLOR );
		title.setPos( (WIDTH - title.width()) / 2f, 2 );
		PixelScene.align( title );
		add( title );

		float top = title.bottom() + 4;

		Component content = new Component();
		float pos = 0;

		final Hero hero = Dungeon.hero;

		//gold
		pos = addValue( content, pos, Messages.get(this, "gold"), Dungeon.gold, true, new ValueSetter() {
			@Override
			public void set( int value ) {
				Dungeon.gold = Math.max( 0, value );
			}
		});

		//current HP
		pos = addValue( content, pos, Messages.get(this, "hp"), hero.HP, true, new ValueSetter() {
			@Override
			public void set( int value ) {
				//never set to 0, that would just kill the hero
				hero.HP = Math.max( 1, Math.min( value, hero.HT ) );
			}
		});

		//max HP, adjusted through the permanent HT boost so that it survives level ups
		pos = addValue( content, pos, Messages.get(this, "max_hp"), hero.HT, true, new ValueSetter() {
			@Override
			public void set( int value ) {
				hero.HTBoost += Math.max( 1, value ) - hero.HT;
				hero.updateHT( false );
			}
		});

		//talent points, per tier
		for (int t = 1; t <= Talent.MAX_TALENT_TIERS; t++) {
			final int tier = t;
			pos = addValue( content, pos,
					Messages.get(this, "talent", tier),
					hero.talentPointsAvailable( tier ),
					tierUnlocked( hero, tier ),
					new ValueSetter() {
						@Override
						public void set( int value ) {
							hero.debugTalentPoints[tier] += Math.max( 0, value ) - hero.talentPointsAvailable( tier );
						}
					});
		}

		//alchemy energy
		pos = addValue( content, pos, Messages.get(this, "energy"), Dungeon.energy, true, new ValueSetter() {
			@Override
			public void set( int value ) {
				Dungeon.energy = Math.max( 0, value );
			}
		});

		//keys, for the current depth
		pos = addKeyValue( content, pos, Messages.get(this, "iron_key"), IronKey.class );
		pos = addKeyValue( content, pos, Messages.get(this, "golden_key"), GoldenKey.class );
		pos = addKeyValue( content, pos, Messages.get(this, "crystal_key"), CrystalKey.class );

		//items
		pos += GAP * 2;
		RedButton items = new RedButton( Messages.get(this, "items") ) {
			@Override
			protected void onClick() {
				hide();
				GameScene.show( new WndDebugItems() );
			}
		};
		items.icon( Icons.get( Icons.BACKPACK ) );
		items.setRect( 0, pos, WIDTH, BTN_HEIGHT );
		content.add( items );
		pos += BTN_HEIGHT;

		content.setSize( WIDTH, pos );

		int maxHeight = (int)(PixelScene.uiCamera.height * 0.9f) - (int)top;
		int height = Math.min( (int)pos, maxHeight );

		ScrollPane list = new ScrollPane( content );
		add( list );
		list.setRect( 0, top, WIDTH, height );

		resize( WIDTH, (int)top + height );
	}

	//a tier's points can only be used once the tier is unlocked, so editing them before that does nothing
	private static boolean tierUnlocked( Hero hero, int tier ) {
		return !(hero.lvl < Talent.tierLevelThresholds[tier] - 1
				|| (tier == 3 && hero.subClass == HeroSubClass.NONE)
				|| (tier == 4 && hero.armorAbility == null));
	}

	private float addKeyValue( Component content, float pos, String label, final Class<? extends Key> keyClass ) {
		final int depth = Dungeon.depth;

		Key current = Reflection.newInstance( keyClass );
		current.depth = depth;

		return addValue( content, pos, label, Notes.keyCount( current ), true, new ValueSetter() {
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

	private float addValue( Component content, float pos, final String label, final int value,
	                        boolean enabled, final ValueSetter setter ) {
		RedButton btn = new RedButton( label + ": " + value ) {
			@Override
			protected void onClick() {
				hide();
				GameScene.show( new WndTextInput(
						label,
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
		btn.enable( enabled );
		btn.setRect( 0, pos, WIDTH, BTN_HEIGHT );
		content.add( btn );
		return pos + BTN_HEIGHT + GAP;
	}
}
