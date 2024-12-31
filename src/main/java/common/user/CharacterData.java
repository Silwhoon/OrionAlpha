/*
 * This file is part of OrionAlpha, a MapleStory Emulator Project.
 * Copyright (C) 2018 Eric Smith <notericsoft@gmail.com>
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package common.user;

import common.item.BodyPart;
import common.item.ItemAccessor;
import common.item.ItemSlotBase;
import common.item.ItemType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import network.packet.OutPacket;

/**
 *
 * @author Eric
 */
public class CharacterData {

    static final int 
            BodyPartCount = BodyPart.BP_Count,
            ItemTypeCount = ItemType.NO
    ;

    private final CharacterStat characterStat;
    private final List<ItemSlotBase> equipped;
    private final List<ItemSlotBase> equipped2;
    private final List<List<ItemSlotBase>> itemSlot;
    private final List<List<Integer>> itemTrading;
    private final Map<Integer, Integer> skillRecord;
    private final Map<String, String> questRecord;
    private int moneyTrading;
    private boolean onTrading;
        
    public CharacterData() {
        this.characterStat = new CharacterStat();
        this.equipped = new ArrayList<>(BodyPartCount + 1);
        this.equipped2 = new ArrayList<>(BodyPartCount + 1);
        this.itemSlot = new ArrayList<>(ItemType.NO);
        this.itemTrading = new ArrayList<>(ItemType.NO);
        this.skillRecord = new HashMap<>();
        this.questRecord = new HashMap<>();
        this.onTrading = false;
       
        for (int i = 0; i <= BodyPartCount; i++) {
            equipped.add(i, null);
            equipped2.add(i, null);
        }
        for (int i = 0; i < ItemTypeCount; i++) {
            itemSlot.add(i, new ArrayList<>());
            itemTrading.add(i, new ArrayList<>());
        }
    }

    public void backupItemSlot(List<List<ItemSlotBase>> backupItem, List<List<Integer>> backupItemTrading) {
        for (int i = 0; i < ItemType.NO; i++) {
            backupItem.add(i, new ArrayList<>());
            if (backupItemTrading != null) {
                backupItemTrading.add(i, new ArrayList<>());
            }
        }
        
        for (int ti = ItemType.Equip; ti <= ItemType.Etc; ti++) {
            for (int i = 0; i < itemSlot.get(ti).size(); i++) {
                backupItem.get(ti).add(i, null);
                
                ItemSlotBase item = itemSlot.get(ti).get(i);
                if (item != null) {
                    backupItem.get(ti).set(i, item.makeClone());
                }
            }
            if (backupItemTrading != null) {
                backupItemTrading.get(ti).addAll(itemTrading.get(ti));
            }
        }
    }

    public void clearTradingInfo() {
        this.moneyTrading = 0;
        for (int i = 0; i < ItemType.NO; i++) {
            for (int j = 0; j < itemTrading.get(i).size(); j++) {
                itemTrading.get(i).set(j, 0);
            }
        }
    }

    public void encode(OutPacket packet, int flag) {
        packet.encodeByte(flag);
        if ((flag & DBChar.Character) != 0) {
            characterStat.encode(packet);
            packet.encodeByte(25); // TODO: Buddy list capacity
            packet.encodeInt(characterStat.getMoney());
            packet.encodeByte(24); // TODO: equip slots
            packet.encodeByte(24); // TODO: use slots
            packet.encodeByte(24); // TODO: setup slots
            packet.encodeByte(24); // TODO: etc slots
            packet.encodeByte(96); // TODO: cash slots
        }
        if ((flag & DBChar.ItemSlotEquip) != 0) {
            for (int i = 1; i <= BodyPartCount; i++) {
                ItemSlotBase item = equipped.get(i);
                if (item != null) {
                    packet.encodeByte(i);
                    item.encode(packet);
                }
            }
            packet.encodeByte(0);
            for (int i = 1; i <= BodyPartCount; i++) {
                ItemSlotBase item = equipped2.get(i);
                if (item != null) {
                    packet.encodeByte(i);
                    item.encode(packet);
                }
            }
            packet.encodeByte(0);
            int slotCount = getItemSlotCount(ItemType.Equip);
            for (int i = 1; i <= slotCount; i++) {
                ItemSlotBase item = itemSlot.get(ItemType.Equip).get(i);
                if (item != null) {
                    packet.encodeByte(i);
                    item.encode(packet);
                }
            }
            packet.encodeByte(0);
        }
        for (int ti = ItemType.Consume; ti <= ItemType.Etc; ti++) {
            if ((flag & ItemAccessor.getItemTypeFromTypeIndex(ti)) != 0) {
                int slotCount = getItemSlotCount(ti);
                for (int i = 1; i <= slotCount; i++) {
                    ItemSlotBase item = itemSlot.get(ti).get(i);
                    if (item != null) {
                        packet.encodeByte(i);
                        item.encode(packet);
                    }
                }
                packet.encodeByte(0);
            }
        }
        if ((flag & DBChar.SkillRecord) != 0) {
            packet.encodeShort(skillRecord.size());
            for (Map.Entry<Integer, Integer> skillEntry : skillRecord.entrySet()) {
                packet.encodeInt(skillEntry.getKey());//nSkillID
                packet.encodeInt(skillEntry.getValue());//nSLV
            }
        }

        packet.encodeShort(0); // TODO: Cooldowns

        if ((flag & DBChar.QuestRecord) != 0) {
            packet.encodeShort(0); // How many quests started
            packet.encodeShort(0); // How many quests completed
            // TODO: Quests
//            packet.encodeShort(questRecord.size());
//            for (Map.Entry<String, String> questEntry : questRecord.entrySet()) {
//                packet.encodeString(questEntry.getKey());//Unknown
//                packet.encodeString(questEntry.getValue());//Unknown
//            }
        }

        packet.encodeShort(0); // TODO: What is this?
        packet.encodeShort(0); // TODO: Crush rings
        packet.encodeShort(0); // TODO: Friendship rings
        packet.encodeShort(0); // TODO: Marriage
        for (int x = 0; x < 15; x++) { // TODO: Teleport rocks
            packet.encodeBytes(new byte[]{(byte) 0xff, (byte) 0xc9, (byte) 0x9a, 0x3b});
        }
        packet.encodeBytes(new byte[]{-112, 99, 58, 13, -59, 93, -56, 1}); // TODO: What is this?
    }

    public void encodeForLogin(OutPacket packet) {
        characterStat.encode(packet);
        packet.encodeByte(characterStat.getGender());
        packet.encodeByte(characterStat.getSkin());
        packet.encodeInt(characterStat.getFace());
        packet.encodeByte(1); // Not megaphone
        packet.encodeInt(characterStat.getHair());

        for (int i = 1; i <= BodyPartCount; i++) {
            // For some reason we handle cash weapon later
            if (i == 11) continue;

            ItemSlotBase item = equipped2.get(i);
            if (item != null) {
                packet.encodeByte(i);
                packet.encodeInt(item.getItemID());
            }
        }
        packet.encodeByte(0xFF);
        for (int i = 1; i <= BodyPartCount; i++) {
            ItemSlotBase item = equipped.get(i);
            if (item != null) {
                packet.encodeByte(i);
                packet.encodeInt(item.getItemID());
            }
        }
        packet.encodeByte(0xFF);

        // For some reason the cash weapon is handled here..?
        ItemSlotBase cashWeapon = equipped2.get(11);
        packet.encodeInt(cashWeapon != null ? cashWeapon.getItemID() : 0);

        // TODO: Pets
        packet.encodeInt(0);
        packet.encodeInt(0);
        packet.encodeInt(0);

        packet.encodeByte(1); // TODO: world rank enabled (next 4 ints are not sent if disabled)
        packet.encodeInt(1); // TODO: world rank
        packet.encodeInt(0); // TODO: move (negative is downwards)
        packet.encodeInt(1); // TODO: job rank
        packet.encodeInt(0); // TODO: move (negative is downwards)
    }

    public List<ItemSlotBase> getEquipped() {
        return equipped;
    }

    public List<ItemSlotBase> getEquipped2() {
        return equipped2;
    }

    public final CharacterStat getCharacterStat() {
        return characterStat;
    }

    public List<List<ItemSlotBase>> getItemSlot() {
        return itemSlot;
    }

    public List<ItemSlotBase> getItemSlot(int ti) {
        return itemSlot.get(ti);
    }

    public List<List<Integer>> getItemTrading() {
        return itemTrading;
    }

    public int findCashItemSlotPosition(int ti, long sn) {
        if (ti >= ItemType.Equip && ti <= ItemType.Etc && sn != 0) {
            int slotCount = getItemSlotCount(ti);
            for (int i = 1; i <= slotCount; i++) {
                ItemSlotBase item = itemSlot.get(ti).get(i);
                if (item != null) {
                    if (item.getCashItemSN() == sn) {
                        return i;
                    }
                }
            }
            if (ti == ItemType.Equip) {
                for (int bodyPart = 1; bodyPart <= BodyPartCount; bodyPart++) {
                    ItemSlotBase item = equipped2.get(bodyPart);
                    if (item != null) {
                        if (item.getCashItemSN() == sn) {
                            return -bodyPart - BodyPartCount;
                        }
                    }
                }
            }
        }
        return 0;
    }

    public int findEmptySlotPosition(int ti) {
        if (ti >= ItemType.Equip && ti <= ItemType.Etc) {
            int slotCount = getItemSlotCount(ti);
            for (int i = 1; i <= slotCount; i++) {
                ItemSlotBase item = itemSlot.get(ti).get(i);
                if (item == null) {
                    return i;
                }
            }
        }
        return 0;
    }

    public int findItemSlotPosition(ItemSlotBase item) {
        if (item != null) {
            int ti = item.getItemID() / 1000000;
            if (ti >= ItemType.Equip && ti <= ItemType.Etc) {
                int slotCount = getItemSlotCount(ti);
                for (int i = 1; i <= slotCount; i++) {
                    ItemSlotBase other = itemSlot.get(ti).get(i);
                    if (other != null && other.getItemID() == item.getItemID()) {
                        if (other.isSameItem(item)) {
                            return i;
                        }
                    }
                }
            }
        }
        return 0;
    }

    public int getEmptySlotCount(int ti) {
        if (ti >= ItemType.Equip && ti <= ItemType.Etc) {
            int emptySlotCount = 0;
            int slotCount = getItemSlotCount(ti);
            for (int i = 1; i <= slotCount; i++) {
                ItemSlotBase item = itemSlot.get(ti).get(i);
                if (item == null) {
                    ++emptySlotCount;
                }
            }
            return emptySlotCount;
        }
        return 0;
    }

    public ItemSlotBase getItem(byte ti, int pos) {
        if (ti >= ItemType.Equip && ti <= ItemType.Etc) {
            if (ti == ItemType.Equip) {
                if (pos != 0 && (pos >= -BodyPartCount || pos < -BodyPartCount) && pos <= getItemSlotCount(ItemType.Equip) && pos >= -BodyPartCount - BodyPartCount) {
                    if (pos >= -BodyPartCount) {
                        if (pos >= 0) {
                            return itemSlot.get(ItemType.Equip).get(pos);
                        } else {
                            return equipped.get(-pos);
                        }
                    } else {
                        return equipped2.get(-BodyPartCount - pos);
                    }
                }
            } else {
                if (pos > 0 && pos <= getItemSlotCount(ti)) {
                    return itemSlot.get(ti).get(pos);
                }
            }
        }
        return null;
    }

    public int getItemCount(byte ti, int itemID) {
        int count = 0;
        int slotCount = getItemSlotCount(ti);
        for (int i = 1; i <= slotCount; i++) {
            ItemSlotBase item = getItem(ti, i);
            if (item != null && item.getItemID() == itemID) {
                ++count;
            }
        }
        return count;
    }

    public int getItemSlotCount(int ti) {
        return itemSlot.get(ti).size() - 1;
    }

    public int getMoneyTrading() {
        return moneyTrading;
    }

    public Map<Integer, Integer> getSkillRecord() {
        return skillRecord;
    }

    public void load(ResultSet rs, byte flag) throws SQLException {
        if ((flag & DBChar.Character) != 0) {
            characterStat.load(rs);
        }

        if ((flag & DBChar.SkillRecord) != 0) {
            while (rs.next()) {
                skillRecord.put(rs.getInt("SkillID"), rs.getInt("Info"));
            }
        }
        if ((flag & DBChar.QuestRecord) != 0) {
            while (rs.next()) {
                questRecord.put(rs.getString("QRKey"), rs.getString("QRValue"));
            }
        }
    }

    public boolean setItem(byte ti, int pos, ItemSlotBase item) {
        if (ti < ItemType.Equip || ti > ItemType.Etc) {
            return false;
        }
        if (ti == ItemType.Equip) {
            if (pos == 0 || pos < -BodyPartCount && pos >= -BodyPartCount || pos > getItemSlotCount(ItemType.Equip) || pos < -BodyPartCount - BodyPartCount) {
                return false;
            }
            if (pos >= -BodyPartCount) {
                if (pos >= 0) {
                    itemSlot.get(ti).set(pos, item);
                } else {
                    equipped.set(-pos, item);
                }
            } else {
                equipped2.set(-BodyPartCount - pos, item);
            }
        } else {
            if (pos <= 0 || pos > getItemSlotCount(ti)) {
                return false;
            }
            itemSlot.get(ti).set(pos, item);
        }
        return true;
    }

    public void restoreItemSlot(List<List<ItemSlotBase>> backup, List<List<Integer>> backupItemTrading) {
        for (int ti = ItemType.Equip; ti <= ItemType.Etc; ti++) {
            itemSlot.get(ti).clear();
            itemSlot.get(ti).addAll(backup.get(ti));
            if (backupItemTrading != null) {
                itemTrading.get(ti).clear();
                itemTrading.get(ti).addAll(backupItemTrading.get(ti));
            }
        }
    }
    
    public static boolean CheckAdult(int nSSN1, int nSSN2, int nCriticalYear) {
        int nBirthYear; // [sp+0h] [bp-14h]@5
        
        if (nSSN2 / 1000000 != 1 && nSSN2 / 1000000 != 2 && nSSN2 / 1000000 != 5 && nSSN2 / 1000000 != 6)
            nBirthYear = nSSN1 / 10000 + 2000;
        else
            nBirthYear = nSSN1 / 10000 + 1900;
        return Calendar.getInstance().get(Calendar.YEAR) - nBirthYear >= nCriticalYear;
    }

    public void setMoneyTrading(int amount) {
        this.moneyTrading = amount;
    }

    public boolean setTrading(boolean trade) {
        if (this.onTrading == trade) {
            return false;
        } else {
            this.onTrading = trade;
            clearTradingInfo();
            return true;
        }
    }
}
