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

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedHashSet;
import java.util.Set;

public class ShortcutTranslatorDialog extends DialogWrapper {
    public static final String SOURCE_KEYMAP = "ShortcutTranslatorSourceKeymap";
    public static final String TARGET_KEYMAP = "ShortcutTranslatorTargetKeymap";
    private JPanel contentPane;
    private JComboBox sourceComboBox;
    private JLabel sourceShortcut;
    private JComboBox targetComboBox;
    private JPanel translatedShortcuts;

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

        addKeymaps(sourceComboBox, "Eclipse", SOURCE_KEYMAP);
        addKeymaps(targetComboBox, "$default", TARGET_KEYMAP);

        init();
    }

    private KeymapManagerEx addKeymaps(JComboBox comboBox, String defaultKeymap, String property) {
        comboBox.addKeyListener(adapter);
        PropertiesComponent properties = PropertiesComponent.getInstance();
        KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
        for (Keymap keymap : keymapManager.getAllKeymaps()) {
            comboBox.addItem(keymap);
        }
        String keymap = properties.getValue(property, defaultKeymap);
        comboBox.setSelectedItem(keymapManager.getKeymap(keymap));
        return keymapManager;
    }

    private void handleKeyEventAsShortcut(KeyEvent e) {
        KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(e);
        sourceShortcut.setText(KeymapUtil.getKeystrokeText(stroke));

        Set<ShortcutDescription> descriptions = translateShortcut(stroke, getSource(), getTarget());
        displayDescriptions(descriptions);

        pack();
        repaint();
        centerRelativeToParent();
    }

    private void displayDescriptions(Set<ShortcutDescription> descriptions) {
        for (Component component : translatedShortcuts.getComponents()) {
            translatedShortcuts.remove(component);
        }

        translatedShortcuts.setLayout(new GridLayoutManager(Math.max(1,descriptions.size()), 2));

        int i = 0;
        for (ShortcutDescription description : descriptions) {
            translatedShortcuts.add(newLabel(description.shortcut, null), constraints(0, i));
            translatedShortcuts.add(newLabel(description.getDescription(), description.getIcon()), constraints(1, i));
            i++;
        }
        translatedShortcuts.repaint();
    }

    private GridConstraints constraints(int column, int row) {
        GridConstraints constraints = new GridConstraints();
        constraints.setColumn(column);
        constraints.setRow(row);
        constraints.setAnchor(GridConstraints.ANCHOR_WEST);
        return constraints;
    }


    private JLabel newLabel(String s, @Nullable Icon icon) {
        JLabel label = new JLabel(s);
        label.setHorizontalAlignment(JLabel.LEADING);
        label.setIcon(icon);
        return label;
    }

    private Keymap getTarget() {
        return (Keymap) targetComboBox.getSelectedItem();
    }

    private Keymap getSource() {
        return (Keymap) sourceComboBox.getSelectedItem();
    }

    private Set<ShortcutDescription> translateShortcut(KeyStroke stroke, Keymap sourceKeymap, Keymap targetKeymap) {
        Set<ShortcutDescription> descriptions = new LinkedHashSet<ShortcutDescription>();
        String[] actionIds = sourceKeymap.getActionIds(stroke);

        for (String actionId : actionIds) {
            Shortcut[] shortcuts = targetKeymap.getShortcuts(actionId);
            for (Shortcut shortcut : shortcuts) {
                descriptions.add(new ShortcutDescription(KeymapUtil.getShortcutText(shortcut), actionId, getAction(actionId)));
            }
        }
        return descriptions;
    }

    private AnAction getAction(String actionId) {
        return ActionManagerEx.getInstanceEx().getAction(actionId);
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

    @Override
    protected void dispose() {
        PropertiesComponent.getInstance().setValue(SOURCE_KEYMAP, getSource().getName());
        PropertiesComponent.getInstance().setValue(TARGET_KEYMAP, getTarget().getName());
        super.dispose();
    }
    
    private static class ShortcutDescription {
        private String shortcut, actionId;
        private AnAction action;

        private ShortcutDescription(String shortcut, String actionId, AnAction action) {
            this.shortcut = shortcut;
            this.actionId = actionId;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ShortcutDescription that = (ShortcutDescription) o;

            return !(action != null ? !action.equals(that.action) : that.action != null) &&
                   !(shortcut != null ? !shortcut.equals(that.shortcut) : that.shortcut != null);

        }

        @Override
        public int hashCode() {
            int result = shortcut != null ? shortcut.hashCode() : 0;
            result = 31 * result + (action != null ? action.hashCode() : 0);
            return result;
        }
        
        private String getDescription() {
            if (action == null) return "";
            Presentation presentation = action.getTemplatePresentation();
            
            if (StringUtil.isEmpty(presentation.getText())) return actionId.replace("$", "") + getActionGroup(actionId);
            if (StringUtil.isEmpty(presentation.getDescription())) return presentation.getText() + getActionGroup(actionId);
            
            return presentation.getText()+ ": " + presentation.getDescription() + getActionGroup(actionId);
        }

        private String getActionGroup(String actionId) {
            ActionManager manager = ActionManager.getInstance();
            ActionGroup mainMenu = (ActionGroup)manager.getActionOrStub(IdeActions.GROUP_MAIN_MENU);
            
            return getActionDescription(actionId, mainMenu, mainMenu.getTemplatePresentation().getText());
        }

        private String getActionDescription(String actionId, ActionGroup group, String groupName) {
            ActionManager manager = ActionManager.getInstance();
            AnAction[] actions = group.getChildren(null);
            for (AnAction action : actions) {
                if (action != null) {
                    if (StringUtil.equals(actionId, manager.getId(action))) {
                        return StringUtil.isNotEmpty(groupName) ? " ("+groupName+")" : "";
                    } else if (action instanceof ActionGroup) {
                        String newGroupName = action.getTemplatePresentation().getText();
                        String description = getActionDescription(actionId, (ActionGroup) action, StringUtil.isNotEmpty(newGroupName) ? newGroupName : groupName);
                        if (StringUtil.isNotEmpty(description)) {
                            return description;
                        }
                    }
                }
            }
            return "";
        }

        private Icon getIcon() {
            if (action == null) return null;
            return action.getTemplatePresentation().getIcon();
        }
    }
}
