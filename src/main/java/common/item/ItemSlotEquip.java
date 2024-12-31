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
package common.item;

import network.packet.OutPacket;

/**
 *
 * @author Eric
 */
public class ItemSlotEquip extends ItemSlotBase {
    private long sn;
    
    // TODO: Getter/Setters
    public byte ruc;//Remaining Upgrade Count
    public byte cuc;//Current Upgrade Count
    public short iSTR;
    public short iDEX;
    public short iINT;
    public short iLUK;
    public short iMaxHP;
    public short iMaxMP;
    public short iPAD;//Physical Attack Damage
    public short iMAD;//Magic Attack Damage
    public short iPDD;//Physical Defense
    public short iMDD;//Magic Defense
    public short iACC;//Accuracy Rate
    public short iEVA;//Evasion
    public short iCraft;//Hands
    public short iSpeed;
    public short iJump;
    
    public ItemSlotEquip(int itemID) {
        super(itemID);
    }
    
    @Override
    public short getItemNumber() {
        return 1;
    }
    
    @Override
    public long getSN() {
        return sn;
    }
    
    @Override
    public int getType() {
        return ItemSlotType.Equip;
    }
    
    public boolean isSameEquipItem(ItemSlotEquip src) {
        return this.ruc == src.ruc && this.cuc == src.cuc && this.iSTR == src.iSTR && this.iDEX == src.iDEX && this.iINT == src.iINT && this.iLUK == src.iLUK
                && this.iMaxHP == src.iMaxHP && this.iMaxMP == src.iMaxMP && this.iPAD == src.iPAD && this.iMAD == src.iMAD && this.iPDD == src.iPDD 
                && this.iMDD == src.iMDD && this.iACC == src.iACC && this.iEVA == src.iEVA && this.iCraft == src.iCraft && this.iSpeed == src.iSpeed 
                && this.iJump == src.iJump;
    }
    
    @Override
    public ItemSlotBase makeClone() {
        ItemSlotEquip item = (ItemSlotEquip) createItem(ItemSlotType.Equip);
        item.setItemID(this.getItemID());
        item.setCashItemSN(this.getCashItemSN());
        item.setItemSN(this.getSN());
        item.setDateExpire(this.getDateExpire());
        
        // TODO: Apply and use setters with this class.
        item.ruc = this.ruc;
        item.cuc = this.cuc;
        item.iSTR = this.iSTR;
        item.iDEX = this.iDEX;
        item.iINT = this.iINT;
        item.iLUK = this.iLUK;
        item.iMaxHP = this.iMaxHP;
        item.iMaxMP = this.iMaxMP;
        item.iPAD = this.iPAD;
        item.iMAD = this.iMAD;
        item.iPDD = this.iPDD;
        item.iMDD = this.iMDD;
        item.iACC = this.iACC;
        item.iEVA = this.iEVA;
        item.iCraft = this.iCraft;
        item.iSpeed = this.iSpeed;
        item.iJump = this.iJump;
        
        return item;
    }
    
    @Override
    public void rawEncode(OutPacket packet) {
        packet.encodeByte(ItemType.Equip);
        super.rawEncode(packet);
        packet.encodeByte(ruc);
        packet.encodeByte(cuc);
        packet.encodeShort(iSTR);
        packet.encodeShort(iDEX);
        packet.encodeShort(iINT);
        packet.encodeShort(iLUK);
        packet.encodeShort(iMaxHP);
        packet.encodeShort(iMaxMP);
        packet.encodeShort(iPAD);
        packet.encodeShort(iMAD);
        packet.encodeShort(iPDD);
        packet.encodeShort(iMDD);
        packet.encodeShort(iACC);
        packet.encodeShort(iEVA);
        packet.encodeShort(iCraft);
        packet.encodeShort(iSpeed);
        packet.encodeShort(iJump);
        packet.encodeString(""); // TODO: Item owner
        packet.encodeShort(0); // TODO: Item lock
        packet.encodeLong(0); // TODO: What is this?
    }
    
    @Override
    public void setItemNumber(int number) {
        
    }
    
    public void setItemSN(long sn) {
        this.sn = sn;
    }
}
