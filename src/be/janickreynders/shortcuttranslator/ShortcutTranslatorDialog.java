/*
 * MIT license
 *
 * Copyright (c) 2012 Janick Reynders
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package be.janickreynders.shortcuttranslator;

import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ShortcutTranslatorDialog extends DialogWrapper {
    private JPanel contentPane;
    private JComboBox sourceComboBox;
    private JLabel sourceShortcut;
    private JComboBox targetComboBox;
    private JLabel targetShortcut;

    private KeyAdapter adapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            handleKeyEventAsShortcut(e);
        }
    };

    public ShortcutTranslatorDialog(Project project) {
        super(project);
        setModal(true);
        setTitle("Translate Shortcut");
        sourceComboBox.addKeyListener(adapter);
        targetComboBox.addKeyListener(adapter);

        KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
        for (Keymap keymap : keymapManager.getAllKeymaps()) {
            sourceComboBox.addItem(keymap);
            targetComboBox.addItem(keymap);
        }

        sourceComboBox.setSelectedItem(keymapManager.getKeymap("Eclipse"));
        targetComboBox.setSelectedItem(keymapManager.getActiveKeymap());
        init();
    }

    private void handleKeyEventAsShortcut(KeyEvent e) {
        KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(e);
        sourceShortcut.setText(KeymapUtil.getKeystrokeText(stroke));

        Set<String> texts = translateShortcut(stroke, (Keymap) sourceComboBox.getSelectedItem(), (Keymap) targetComboBox.getSelectedItem());
        targetShortcut.setText(StringUtil.join(texts, " or "));

    }

    private Set<String> translateShortcut(KeyStroke stroke, Keymap sourceKeymap, Keymap targetKeymap) {
        Set<String> texts = new LinkedHashSet<String>();
        String[] actionIds = sourceKeymap.getActionIds(stroke);
        for (String actionId : actionIds) {
            Shortcut[] shortcuts = targetKeymap.getShortcuts(actionId);
            for (Shortcut shortcut : shortcuts) {
                texts.add(KeymapUtil.getShortcutText(shortcut));
            }
        }
        return texts;
    }


    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected JButton createJButtonForAction(Action action) {
        JButton button = super.createJButtonForAction(action);
        button.addKeyListener(adapter);
        return button;
    }

    @Override
    protected Action[] createActions() {
        return new Action[] { getOKAction() };
    }
}
