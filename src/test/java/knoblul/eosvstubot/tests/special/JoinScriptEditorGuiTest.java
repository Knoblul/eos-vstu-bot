/*
 * Copyright 2020 Knoblul
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package knoblul.eosvstubot.tests.special;

import knoblul.eosvstubot.gui.profile.ProfileTable;
import knoblul.eosvstubot.gui.scripting.ScriptEditorDialog;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 29.04.2020 19:44
 *
 * @author Knoblul
 */
public class JoinScriptEditorGuiTest extends Assert {
    @Test
    public void testJoinScript() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        ScriptEditorDialog dialog = new ScriptEditorDialog();
        dialog.setEditingScript(ProfileTable.getDefaultChatJoinScriptContent());
        dialog.setVisible(true);
        dialog.dispose();
    }
}
