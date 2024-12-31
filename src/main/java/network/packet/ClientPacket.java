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
package network.packet;

/**
 * @author Eric
 */
public class ClientPacket {

  public static final short
      BEGIN_SOCKET = 0,
      BackToWorldSelect = 2, // TODO: What is the GMS-like name for this
      PinAction = 3, // TODO: What is the GMS-like name for this
      RegisterPin = 5, // TODO: What is the GMS-like name for this
  // Both 0x8 and 0x9 are their own "AliveAck"..
  // Maybe one is an acknowledge and the other is a request? idk
  AliveAck = 10,
      AliveReq = 11, // 0058B285
      ExceptionLog = 17,
      HoverWorld = 19, // TODO: What is the GMS-like name for this
      MigrateIn = 20,
      SelectCharacter = 22,
      RequestWorldInfo = 24, // TODO: What is the GMS-like name for this
      SelectWorld = 25,
      CheckPassword = 27,
      END_SOCKET = 28,
      BEGIN_USER = 29,

  CheckDuplicatedID = 8,
      CreateNewCharacter = 6,
      DeleteCharacter = 7,
      UserTransferFieldRequest = 13,
      UserMigrateToCashShopRequest = 14,
      UserMove = 15,
      UserMeleeAttack = 16,
      UserShootAttack = 17,
      UserMagicAttack = 18,
      Unknown = 19, // Not sure if this even exists in the client?
      UserHit = 20,
      UserChat = 21,
      UserEmotion = 22,
      UserSelectNpc = 23,
      UserScriptMessageAnswer = 24,
      UserShopRequest = 25,
      UserChangeSlotPositionRequest = 26,
      UserStatChangeItemUseRequest = 27,
      UserConsumeCashItemUseRequest = 28,
      UserPortalScrollUseRequest = 29,
      UserUpgradeItemUseRequest = 30,
      UserAbilityUpRequest = 31,
      UserChangeStatRequest = 32,
      UserSkillUpRequest = 33,
      UserSkillUseRequest = 34,
      UserDropMoneyRequest = 35,
      UserGivePopularityRequest = 36,
      UserPartyRequest = 37,
      UserCharacterInfoRequest = 38,
      BroadcastMsg = 39,
      Whisper = 40,
      Messenger = 41,
      MiniRoom = 42,
      PartyRequest = 43, // 00487F0F, 00595858
      PartyResult = 44, // 00595755, 005958CA
      Admin = 45,
      END_USER = 46,
      BEGIN_FIELD = 47,
      BEGIN_LIFEPOOL = 48,
      BEGIN_MOB = 49,
      MobMove = 50,
      END_MOB = 150,
      BEGIN_NPC = 151,
      NpcMove = 152,
      END_NPC = 153,
      END_LIFEPOOL = 154,
      BEGIN_DROPPOOL = 155,
      DropPickUpRequest = 57,
      END_DROPPOOL = 157,
      END_FIELD = 158,
      BEGIN_CASHSHOP = 159,
      CashShopChargeParamRequest = 61,
      CashShopQueryCashRequest = 62,
      CashShopCashItemRequest = 63,
      END_CASHSHOP = 64,
      NO = 65;
}
