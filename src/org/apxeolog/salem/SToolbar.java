package org.apxeolog.salem;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apxeolog.salem.SToolbarConfig.SToolbarConfigSlot;

import haven.Config;
import haven.Coord;
import haven.DTarget;
import haven.DropTarget;
import haven.GOut;
import haven.Glob.Pagina;
import haven.Loading;
import haven.MenuGrid;
import haven.Resource;
import haven.RichText;
import haven.Tex;
import haven.Text;
import haven.UI;
import haven.Widget;

public class SToolbar extends SWindow implements DTarget, DropTarget {
	public static String barName;
	public int actKey;
	public Coord barSize = new Coord(1, 1);
	private boolean isVert = false;
	private Slot[] slotList;
	private int slotCount;
	private Pagina dragPag, pressPag;
	public final static Tex backGround = Resource.loadtex("gfx/hud/invsq");
	public final static Coord bGSize = backGround.sz().add(-1, -1);
	public final static RichText.Foundry ttFoundry = new RichText.Foundry(
			TextAttribute.FAMILY, "SansSerif", TextAttribute.SIZE, 10);
	private static final Properties tbConfig = new Properties();
	
	public SToolbar(Coord c, Widget w, String name, int slots) {
		super(c, new Coord(10, 10), w, name);
		barName = name;
		slotCount = slots;
		setClosable(false);
		initBar(slots);
		loadBar();
		fillBar();
	}
	
	public SToolbar(Coord c, Widget w, SToolbarConfig cfg) {
		this(c, w, cfg.tbName, cfg.slotList.size());
		for (int slot = 0; slot < slotCount; slot++) {
			//slotList[slot]
		}
	}

	@Override
	public boolean dropthing(Coord cc, Object thing) {
		if(!(thing instanceof Resource)) return false;
		if(slotIndex(cc) > slotCount - 1 || slotIndex(cc) < 0) return false;
		//creating new item at bar;
		slotList[slotIndex(cc)] = new Slot((Resource)thing);
		setBarSlot(slotIndex(cc), ((Resource)thing).name);
		return false;
	}
	
	private int slotIndex(Coord c) {
		if(isMinimized) return -1;
		if(isVert) {
			for(int i = 0; i < barSize.y; i++){
				Coord stl = new Coord(5, 5 + windowHeader.sz.y + bGSize.y*i); //slot top left corner
				if(c.isect(stl, bGSize)) return i;
			}
		} else {
			for(int i = 0; i < barSize.x; i++){
				Coord stl = new Coord(5 + bGSize.x*i, 5 + windowHeader.sz.y); //slot top left corner
				if(c.isect(stl, bGSize)) return i;
			}
		}
		return -1;
	}
	
	public void draw(GOut initGL){
		super.draw(initGL);
		if(isMinimized) return;
		Coord backDraw;
		int limit;
		if(isVert)
			limit = barSize.y;
		else
			limit = barSize.x;
		for(int i = 0; i < limit; i++) {
			if(isVert) backDraw = new Coord(0, bGSize.y*i);
			else backDraw = new Coord(bGSize.x*i, 0);
			backDraw = backDraw.add(5, 5 + windowHeader.sz.y);
			initGL.image(backGround, backDraw);
			if(slotList[i] != null) {
				Tex slottex = slotList[i].getRes().layer(Resource.imgc).tex();
				if(slottex == null) continue;
				initGL.image(slottex, backDraw.add(1, 1));
				//drawing slot as pressed
				if(slotList[i].getSlotPagina() == pressPag) {
					initGL.chcolor(new Color(0, 0, 0, 128));
					initGL.frect(backDraw.add(1, 1), bGSize);
					initGL.chcolor();
				}
			}
		}
		if (dragPag != null) {
			final Tex dt = dragPag.img.tex();
			ui.drawafter(new UI.AfterDraw() {
				public void draw(GOut g) {
					g.image(dt, ui.mc.add(dt.sz().div(2).inv()));
				}
			});
		}
	}
	
	private void calcBarSize() {
		Coord newSize;
		if(isVert) {
			int height = 0;
			for(int i = 0; i < barSize.y; i++)
				height += bGSize.y - 1;
			newSize = new Coord(bGSize.x, height + 15);
		} else {
			int width = 0;
			for(int i = 0; i < barSize.x; i++)
				width += bGSize.x - 1;
			newSize = new Coord(width + 15, bGSize.y);
		}
		resize(newSize);
	}
	
	private void initBar(int slotsCount) {
		if(slotsCount < 1) slotsCount = 1;
		if(isVert)
			barSize = new Coord(1, slotsCount);
		else
			barSize = new Coord(slotsCount, 1);
		//redrawing bar size
		calcBarSize();
		slotList = new Slot[slotsCount];
	}
	
	public boolean mousedown(Coord c, int button) {
		if(slotIndex(c) < 0 || slotIndex(c) > slotCount) {
			return super.mousedown(c, button);
		} else {
			if(button == 1) {
				//marking pressPag with current item at slot
				Resource slotRes = null;
				if(slotList[slotIndex(c)] != null) slotRes = slotList[slotIndex(c)].getRes();
				if(slotRes != null) {
					pressPag = slotList[slotIndex(c)].getSlotPagina();
					ui.grabmouse(this);
				}
				else return super.mousedown(c, button);
				return true;
			}
			if(button == 3) {
				//removing item from toolbar
				slotList[slotIndex(c)] = null;
				setBarSlot(slotIndex(c), "");
			}
		}
		return true;
	}
	
	public void mousemove(Coord c) {
		if ((dragPag == null) && (pressPag != null)) {
			//moving items on toolbar
			//removing old one
			Pagina p = null;
			if(slotList[slotIndex(c)] != null) p = slotList[slotIndex(c)].getSlotPagina();
			if (p != pressPag) {
				dragPag = pressPag;
			}
			slotList[slotIndex(c)] = null;
			setBarSlot(slotIndex(c), "");
		}
		else super.mousemove(c);
	}
	
	public boolean mouseup(Coord c, int button) {
		if(button == 1) {
			if(dragPag != null) {
				//drop after moving
				ui.dropthing(ui.root, ui.mc, dragPag.res());
				dragPag = null;
				pressPag = null;
			} else if(pressPag != null) {
				//pressing at slot
				Pagina p = null;
				if(slotIndex(c) > -1 && slotIndex(c) < slotCount) {
					if(slotList[slotIndex(c)] != null) p = slotList[slotIndex(c)].getSlotPagina();
				} 
				//������� ���
				if(pressPag == p) {
					//activating slot
					@SuppressWarnings("deprecation")
					MenuGrid mg = ui.root.findchild(MenuGrid.class);
					if(mg != null)
						mg.use(p);
				}
				pressPag = null;
			}
			ui.grabmouse(null);
		}
		return super.mouseup(c, button);
	}
	
	private Resource curttr = null;
	private boolean curttl = false;
	private Text curtt = null;
	private long hoverstart;
	
	private static Text renderTip(Resource res, boolean withpg) {
		Resource.AButton ad = res.layer(Resource.action);
		Resource.Pagina pg = res.layer(Resource.pagina);
		String tt;
		if (ad != null) {
			tt = ad.name;
		} else {
			tt = res.layer(Resource.tooltip).t;
		}
		if (withpg && (pg != null)) {
			tt += "\n\n" + pg.text;
		}
		return (ttFoundry.render(tt, 0));
	}
	
	public Object tooltip(Coord c, boolean again) {
		if(slotIndex(c) < 0 || slotIndex(c) > slotCount) return null;
		Slot slot = slotList[slotIndex(c)];
		Resource res = (slot == null) ? null : slot.getRes();
		long now = System.currentTimeMillis();
		if ((res != null)
				&& ((res.layer(Resource.action) != null) || (res
						.layer(Resource.tooltip) != null))) {
			if (!again)
				hoverstart = now;
			boolean ttl = (now - hoverstart) > 500;
			if ((res != curttr) || (ttl != curttl)) {
				curtt = renderTip(res, ttl);
				curttr = res;
				curttl = ttl;
			}
			return (curtt);
		} else {
			hoverstart = now;
			return null;
		}
	}
	
	public boolean keydown(KeyEvent ev) {
		if(ev.getKeyCode() == 70 && ui.modctrl) {
			isVert = !isVert;
			barSize = new Coord(barSize.y, barSize.x);
			calcBarSize();
			return true;
		}
		return super.keydown(ev);
	}
	
	private static void setBarSlot(int slot, String value) {
		synchronized (tbConfig) {
			tbConfig.setProperty(barName + "_slot_" + slot, value);
		}
		saveBar();
	}
	
	private static void loadBar() {
		tbConfig.clear();
		String configFileName = "tbar_"
				+ Config.currentCharName.replaceAll("[^a-zA-Z()]", "_")
				+ ".conf";
		try {
			synchronized (tbConfig) {
				tbConfig.load(new FileInputStream(configFileName));
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
	
	private void fillBar() {
		synchronized (tbConfig) {
			for (int slot = 0; slot < slotCount; slot++) {
				slotList[slot] = null;
				String itemName = tbConfig.getProperty(barName + "_slot_" + slot, "");
				if (itemName.length() > 0) {
					try {
						Resource sres = Resource.load(itemName);
						sres.loadwait();
						slotList[slot] = new Slot(sres);
					} catch (Loading ex) {
						slotList[slot] = null;
					}
				}
			}//for
		}//sync
	}
	
	private static void saveBar() {
		synchronized (tbConfig) {
			String configFileName = "tbar_"
					+ Config.currentCharName.replaceAll("[^a-zA-Z()]", "_")
					+ ".conf";
			try {
				tbConfig.store(new FileOutputStream(configFileName),
						"Toolbars for " + Config.currentCharName);
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
		}
	}
	
	//�� ���� ��� ��� ������� � �������� ����� ��� ��������
	public static class Slot {
		private Resource resSlot;
		private String action;
		private Pagina slotPagina;
		private SToolbarConfigSlot slotConfig;
		
		public void setHotkey(SToolbarConfigSlot scfg) {
			slotConfig = scfg;
		}
		
		public boolean hotkey(KeyEvent ev) {
			if (slotConfig != null) {
				return (slotConfig.sKey == ev.getKeyCode() && slotConfig.sMode == ev.getModifiersEx());
			} else return false;
		}
		
		public String getString() {
			if (slotConfig != null)
				return slotConfig.getString();
			else return null;
		}
		
		public Slot(Resource r) {
			resSlot = r;
			action = r.name;
			slotPagina = new Pagina(r);
		}
		
		public Resource getRes() {
			return resSlot;
		}
		
		public Pagina getSlotPagina() {
			return slotPagina;
		}
		
		public String getAction() {
			return action;
		}
	}

}
