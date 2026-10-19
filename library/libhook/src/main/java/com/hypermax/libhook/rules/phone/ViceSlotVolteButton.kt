/*
  * This file is part of HyperMax.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.hypermax.libhook.rules.phone

import android.provider.Settings
import com.hypermax.libhook.base.BaseHook
import com.hypermax.libhook.utils.hookapi.tool.AppsTool.FlAG_ONLY_ANDROID
import com.hypermax.libhook.utils.hookapi.tool.AppsTool.findContext
import io.github.lingqiqi5211.ezhooktool.core.findMethod
import io.github.lingqiqi5211.ezhooktool.core.loadClass
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook

object ViceSlotVolteButton : BaseHook() {
    override fun init() {
        runCatching {
            // exec("settings put global vice_slot_volte_data_enabled 1")
            Settings.Global.putInt(
                findContext(FlAG_ONLY_ANDROID).contentResolver,
                "vice_slot_volte_data_enabled",
                1
            )
            loadClass("com.android.phone.MiuiPhoneUtils").findMethod { name("shouldHideViceSlotVolteDataButton") }.createHook {
                    returnConstant(false)
                }
        }
        runCatching {
            loadClass("com.android.phone.MiuiPhoneUtils").findMethod { name("shouldHideSmartDualSimButton") }.createHook {
                    returnConstant(false)
                }
        }
    }
}
