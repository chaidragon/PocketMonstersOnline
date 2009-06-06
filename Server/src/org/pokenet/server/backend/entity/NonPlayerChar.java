package org.pokenet.server.backend.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.pokenet.server.GameServer;
import org.pokenet.server.battle.DataService;
import org.pokenet.server.battle.Pokemon;
import org.pokenet.server.battle.impl.NpcBattleField;

/**
 * Represents a Non Playable Character
 * @author shadowkanji
 *
 */
public class NonPlayerChar extends Char {
	/*
	 * Trainers can have an more than 6 possible Pokemon.
	 * When a battle is started with this NPC, it'll check the min party size.
	 * If you have a party bigger than min party,
	 * it'll generate a party of random size between minParty and your party size + 1.
	 * (Unless your party size is 6)
	 */
	private HashMap<String, Integer> m_possiblePokemon;
	private int m_minPartySize = 1;
	private boolean m_isBox = false;
	private boolean m_isHeal = false;
	private boolean m_isShop = false;
	private int m_badge = -1;
	private ArrayList<Integer> m_speech;
	private Shop m_shop = null;
	//Store a list of players they have battled
	private ArrayList<String> m_battles;
	
	/**
	 * Constructor
	 */
	public NonPlayerChar() {}
	
	/**
	 * Returns a string of this npcs speech ids
	 * @param p
	 */
	private String getSpeech() {
		String result = "";
		for(int i = 0; i < m_speech.size(); i++) {
			result = result + m_speech.get(i) + ",";
		}
		return result;
	}
	
	/**
	 * Returns true if this npc has battled the specified player already
	 * @param name
	 * @return
	 */
	public boolean hasBattled(String name) {
		return m_battles.contains(name);
	}
	
	/**
	 * Adds a player to its list of players battled
	 * @param name
	 */
	public void battled(String name) {
		m_battles.add(name);
	}
	
	/**
	 * Talks to a player
	 * @param p
	 */
	public void talkToPlayer(PlayerChar p) {
		if(m_possiblePokemon != null && m_minPartySize > 0) {
			if(this.hasBattled(p.getName())) {
				//TODO: Write victory message
				return;
			} else {
				String speech = this.getSpeech();
				if(!speech.equalsIgnoreCase("")) {
					p.getSession().write("Cn" + speech);
				}
				p.setBattling(true);
				p.setBattleField(new NpcBattleField(DataService.getBattleMechanics(), p, this));
				return;
			}
		}
		/* If this NPC wasn't a trainer, handle other possibilities */
		String speech = this.getSpeech();
		if(!speech.equalsIgnoreCase("")) {
			p.getSession().write("Cn" + speech);
		}
		if(m_isBox) {
			//Send the data for the player's first box, they may change this later
			p.setBoxing(true);
			p.sendBoxInfo(0);
		}
		if(m_isHeal) {
			p.healPokemon();
			p.setLastHeal(p.getX(), p.getY(), p.getMapX(), p.getMapY());
		}
		if(m_isShop) {
			//Send shop packet to display shop window clientside
			p.getSession().write("S" + m_shop.getStockData());
			p.setShopping(true);
			p.setShop(new Shop());
		}
	}
	
	/**
	 * Returns true if this npc can see the player
	 * @param p
	 * @return
	 */
	public boolean canSee(PlayerChar p) {
		return false;
	}
	
	/**
	 * Adds speech to this npc
	 * @param id
	 */
	public void addSpeech(int id) {
		if(m_speech == null)
			m_speech = new ArrayList<Integer>();
		m_speech.add(id);
	}
	
	/**
	 * Returns true if the npc is a gym leader
	 * @return
	 */
	public boolean isGymLeader() {
		return m_badge != -1;
	}
	
	/**
	 * Returns true if an NPC is a trainer
	 * @return
	 */
	public boolean isTrainer() {
		return m_possiblePokemon != null && m_minPartySize > 0;
	}
	
	/**
	 * Return true if this npc heals your pokemon
	 * @return
	 */
	public boolean isHealer() {
		return m_isHeal;
	}
	
	/**
	 * Sets if this npc is a healer or not
	 * @param b
	 */
	public void setHealer(boolean b) {
		m_isHeal = b;
	}
	
	/**
	 * Returns true if this npc is a shop keeper
	 * @return
	 */
	public boolean isShopKeeper() {
		return m_isShop;
	}
	
	/**
	 * Returns true if this npc allows box access
	 * @return
	 */
	public boolean isBox() {
		return m_isBox;
	}
	
	/**
	 * Sets if this npc allows box access
	 * @param b
	 */
	public void setBox(boolean b) {
		m_isBox = b;
	}
	
	/**
	 * Sets if this npc is a shop keeper
	 * @param b
	 */
	public void setShopKeeper(boolean b) {
		m_isShop = b;
		if(b) {
			m_shop = new Shop();
			m_shop.start();
		}
	}
	
	/**
	 * Sets the possible Pokemon this trainer can have
	 * @param pokes
	 */
	public void setPossiblePokemon(HashMap<String, Integer> pokes) {
		m_possiblePokemon = pokes;
		m_battles = new ArrayList<String>();
	}
	
	/**
	 * Sets the badge this npc gives, if any
	 * @param i
	 */
	public void setBadge(int i) {
		m_badge = i;
	}
	
	/**
	 * Returns the number of the badge this npc gives. -1 if no badge.
	 * @return
	 */
	public int getBadge() {
		return m_badge;
	}
	
	/**
	 * Sets the minimum sized party this npc should have
	 * @param size
	 */
	public void setPartySize(int size) {
		m_minPartySize = (size > 6 ? 6 : size);
	}
	
	/**
	 * Returns a dynamically generated Pokemon party based on how well trained a player is
	 * @param p
	 * @return
	 */
	public Pokemon [] getParty(PlayerChar p) {
		Pokemon [] party = new Pokemon[6];
		Pokemon poke;
		int level;
		String name;
		GameServer.getServiceManager().getDataService();
		Random r = DataService.getBattleMechanics().getRandom();
		if(m_minPartySize < p.getPartyCount()) {
			/*
			 * The player has more Pokemon, generate a random party
			 */
			/*
			 * First, get a random party size that is greater than m_minPartySize
			 * and less than or equal to the amount of pokemon in the player's party + 1
			 */
			int pSize = r.nextInt(p.getPartyCount() + 1 > 6 ? 6 : p.getPartyCount() + 1);
			while(pSize < m_minPartySize) {
				pSize = r.nextInt(p.getPartyCount() + 1 > 6 ? 6 : p.getPartyCount() + 1);
			}
			/*
			 * Now generate the random Pokemon
			 */
			for(int i = 0; i <= pSize; i++) {
				//Select a random Pokemon
				name = (String) m_possiblePokemon.keySet().toArray()[r.nextInt(m_possiblePokemon.keySet().size())];
				level = m_possiblePokemon.get(name);
				//Ensure levels are the similiar
				while(level < p.getHighestLevel() - 3) {
					level = r.nextInt(p.getHighestLevel() + 5);
				}
				poke = Pokemon.getRandomPokemon(name, level);
				party[i] = poke;
			}
		} else {
			/*
			 * Generate a party of size m_minPartySize
			 */
			for(int i = 0; i < m_minPartySize; i++) {
				//Select a random Pokemon from this list of possible Pokemons
				name = (String) m_possiblePokemon.keySet().toArray()[r.nextInt(m_possiblePokemon.keySet().size())];
				level = m_possiblePokemon.get(name);
				//Ensure levels are the similiar
				while(level < p.getHighestLevel() - 3) {
					level = r.nextInt(p.getHighestLevel() + 5);
				}
				poke = Pokemon.getRandomPokemon(name, level);
				party[i] = poke;
			}
		}
		return party;
	}
}